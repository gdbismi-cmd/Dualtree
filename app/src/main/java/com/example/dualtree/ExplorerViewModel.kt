package com.example.dualtree

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.StatFs
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dualtree.data.Apk
import com.example.dualtree.data.FileOps
import com.example.dualtree.data.FileTypes
import com.example.dualtree.data.Fs
import com.example.dualtree.data.Kind
import com.example.dualtree.data.Report
import com.example.dualtree.model.DialogState
import com.example.dualtree.model.Node
import com.example.dualtree.model.Progress
import com.example.dualtree.model.SortMode
import com.example.dualtree.model.StorageRoot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** State of one of the two panes. The tree is a set of expanded folder paths, flattened into [nodes]. */
class PaneState {
    var expanded by mutableStateOf(setOf<String>())
    var cursor by mutableStateOf<String?>(null)          // last tapped item
    var selected by mutableStateOf(setOf<String>())      // multi-selection
    var nodes by mutableStateOf(listOf<Node>())
    var scrollTarget by mutableStateOf<String?>(null)
    internal var refreshJob: Job? = null
}

class ExplorerViewModel(app: Application) : AndroidViewModel(app) {
    val roots: List<StorageRoot> = Fs.detectRoots(app)
    val panes = listOf(PaneState(), PaneState())

    var activePane by mutableIntStateOf(0); private set
    var showHidden by mutableStateOf(false); private set
    var sortMode by mutableStateOf(SortMode.NAME); private set
    var dialog by mutableStateOf<DialogState?>(null)
    var viewer by mutableStateOf<File?>(null)

    val progress = MutableStateFlow<Progress?>(null)
    private var opJob: Job? = null

    var searchResults by mutableStateOf<List<File>>(emptyList()); private set
    var searching by mutableStateOf(false); private set
    private var searchJob: Job? = null

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private val authority = "${app.packageName}.fileprovider"

    init {
        val home = roots.first()
        panes.forEach { it.expanded = setOf(home.path); it.cursor = home.path }
        refreshAll()
    }

    private fun msg(s: String) { _messages.trySend(s) }

    // ---------------------------------------------------------------- tree

    fun refreshAll() { refresh(0); refresh(1) }

    fun refresh(i: Int) {
        val p = panes[i]
        val expanded = p.expanded
        val hidden = showHidden
        val sort = sortMode
        p.refreshJob?.cancel()
        p.refreshJob = viewModelScope.launch {
            val nodes = withContext(Dispatchers.IO) { buildTree(expanded, hidden, sort) }
            p.nodes = nodes
            val existing = nodes.mapTo(HashSet()) { it.path }
            p.selected = p.selected.filterTo(HashSet()) { it in existing }
            val c = p.cursor
            if (c != null && c !in existing) {
                var f = File(c).parentFile
                while (f != null && f.absolutePath !in existing) f = f.parentFile
                p.cursor = f?.absolutePath ?: roots.first().path
            }
        }
    }

    private fun buildTree(expanded: Set<String>, hidden: Boolean, sort: SortMode): List<Node> {
        val out = ArrayList<Node>()
        fun walk(dir: File, depth: Int, rootIndex: Int) {
            for (c in Fs.list(dir, hidden, sort)) {
                val isDir = c.isDirectory
                val exp = isDir && c.absolutePath in expanded
                out += Node(
                    file = c, depth = depth, isDir = isDir, expanded = exp,
                    size = if (isDir) 0 else c.length(), modified = c.lastModified(),
                    hidden = c.name.startsWith("."), rootIndex = rootIndex,
                )
                if (exp) walk(c, depth + 1, rootIndex)
            }
        }
        roots.forEachIndexed { ri, r ->
            val exp = r.path in expanded
            val stat = runCatching { StatFs(r.path) }.getOrNull()
            out += Node(
                file = r.file, depth = 0, isDir = true, expanded = exp, size = 0,
                modified = r.file.lastModified(), hidden = false, rootIndex = ri, root = r,
                freeBytes = stat?.availableBytes ?: 0, totalBytes = stat?.totalBytes ?: 0,
            )
            if (exp) walk(r.file, 1, ri)
        }
        return out
    }

    fun activate(i: Int) { activePane = i }

    fun onClick(i: Int, node: Node) {
        activePane = i
        val p = panes[i]
        if (p.selected.isNotEmpty() && node.root == null) { toggleSelect(i, node); return }
        p.cursor = node.path
        if (node.isDir) toggleExpand(i, node) else openFile(node.file)
    }

    fun onLongClick(i: Int, node: Node) {
        activePane = i
        if (node.root == null) toggleSelect(i, node) else panes[i].cursor = node.path
    }

    private fun toggleSelect(i: Int, node: Node) {
        val p = panes[i]
        p.selected = if (node.path in p.selected) p.selected - node.path else p.selected + node.path
        p.cursor = node.path
    }

    fun toggleExpand(i: Int, node: Node) {
        activePane = i
        val p = panes[i]
        p.expanded = if (node.expanded) {
            p.expanded.filterNot { it == node.path || FileOps.isUnder(it, node.path) }.toSet()
        } else {
            p.expanded + node.path
        }
        p.cursor = node.path
        refresh(i)
    }

    fun clearSelection() { panes.forEach { it.selected = emptySet() } }

    fun selectAll() {
        val p = panes[activePane]
        val dir = focusDir(activePane).absolutePath
        p.selected = p.nodes.filter { it.root == null && it.file.parent == dir }.map { it.path }.toSet()
        if (p.selected.isEmpty()) msg("Expand a folder first")
    }

    fun toggleHidden() { showHidden = !showHidden; refreshAll() }
    fun setSort(m: SortMode) { sortMode = m; refreshAll() }

    /** Folder that pane [i] is "in": the cursor if it's a folder, otherwise its parent. */
    fun focusDir(i: Int): File {
        val c = panes[i].cursor?.let(::File) ?: return roots.first().file
        return if (c.isDirectory) c else c.parentFile ?: roots.first().file
    }

    /** Items an operation acts on: selection, or else the highlighted item. */
    private fun targets(): List<File> {
        val p = panes[activePane]
        if (p.selected.isNotEmpty()) return p.selected.map(::File)
        val c = p.cursor ?: return emptyList()
        if (roots.any { it.path == c }) return emptyList()
        return listOf(File(c))
    }

    private fun describe(files: List<File>) =
        if (files.size == 1) "\"${files[0].name}\"" else "${files.size} items"

    private fun validateName(name: String): String? = when {
        name.isBlank() -> "Name can't be empty"
        name.contains('/') || name.contains('\u0000') -> "Name can't contain /"
        name == "." || name == ".." -> "Invalid name"
        else -> null
    }

    // ---------------------------------------------------------------- opening

    fun openFile(f: File) {
        when (FileTypes.kind(f)) {
            Kind.IMAGE -> viewer = f
            Kind.TEXT -> if (f.length() < 4_000_000) viewer = f else openExternal(f)
            Kind.ARCHIVE -> if (f.extension.equals("zip", true)) askUnzip(f) else openExternal(f)
            else -> openExternal(f)
        }
    }

    fun openExternal(f: File) {
        val ctx = getApplication<Application>()
        try {
            val uri = FileProvider.getUriForFile(ctx, authority, f)
            val view = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, FileTypes.mime(f))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ctx.startActivity(
                Intent.createChooser(view, "Open with")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )
        } catch (e: Exception) {
            msg("No app can open this file")
        }
    }

    fun share() {
        val files = targets().filter { it.isFile }
        if (files.isEmpty()) { msg("Select files to share"); return }
        val ctx = getApplication<Application>()
        try {
            val uris = ArrayList<Uri>(files.map { FileProvider.getUriForFile(ctx, authority, it) })
            val send = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0]).setType(FileTypes.mime(files[0]))
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris).setType("*/*")
            }
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ctx.startActivity(Intent.createChooser(send, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            msg("Couldn't share: ${e.message}")
        }
    }

    // ---------------------------------------------------------------- operations

    private fun runOp(title: String, block: suspend (Report) -> String?) {
        if (opJob?.isActive == true) { msg("Another operation is running"); return }
        opJob = viewModelScope.launch {
            progress.value = Progress(title)
            try {
                val result = withContext(Dispatchers.IO) {
                    block { detail, fraction -> progress.value = Progress(title, detail, fraction) }
                }
                result?.let(::msg)
            } catch (e: CancellationException) {
                msg("$title cancelled")
                throw e
            } catch (e: Exception) {
                msg(e.message ?: "Operation failed")
            } finally {
                progress.value = null
                clearSelection()
                refreshAll()
            }
        }
    }

    fun cancelOp() { opJob?.cancel() }

    /** Copy/move from the active pane to the folder open in the other pane (X-plore style). */
    fun requestTransfer(move: Boolean) {
        val src = targets()
        if (src.isEmpty()) { msg("Tap or long-press items first"); return }
        val dest = focusDir(1 - activePane)
        val verb = if (move) "Move" else "Copy"
        dialog = DialogState.Confirm("$verb ${describe(src)}?", "To: ${dest.absolutePath}", verb) {
            runOp(if (move) "Moving" else "Copying") { r ->
                val n = FileOps.transfer(src, dest, move, r)
                "${if (move) "Moved" else "Copied"} $n item(s)"
            }
        }
    }

    fun requestDelete() {
        val t = targets()
        if (t.isEmpty()) { msg("Tap or long-press items first"); return }
        dialog = DialogState.Confirm("Delete ${describe(t)}?", "This can't be undone.", "Delete") {
            runOp("Deleting") { r -> FileOps.delete(t, r); "Deleted ${describe(t)}" }
        }
    }

    fun requestRename() {
        val t = targets()
        if (t.size != 1) { msg("Select one item to rename"); return }
        val f = t[0]
        dialog = DialogState.Input("Rename", f.name, "Rename") { name ->
            val err = validateName(name)
            val dst = File(f.parentFile, name)
            when {
                err != null -> msg(err)
                name == f.name -> Unit
                dst.exists() -> msg("Something with that name already exists")
                f.renameTo(dst) -> remap(f.absolutePath, dst.absolutePath)
                else -> msg("Rename failed")
            }
            refreshAll()
        }
    }

    private fun remap(old: String, new: String) {
        fun m(s: String) = when {
            s == old -> new
            FileOps.isUnder(s, old) -> new + s.substring(old.length)
            else -> s
        }
        panes.forEach { p ->
            p.expanded = p.expanded.map { m(it) }.toSet()
            p.cursor = p.cursor?.let { m(it) }
            p.selected = emptySet()
        }
    }

    fun requestNewFolder() {
        val i = activePane
        val dir = focusDir(i)
        dialog = DialogState.Input("New folder in ${dir.name.ifEmpty { "/" }}", "New folder", "Create") { name ->
            val err = validateName(name)
            val nf = File(dir, name)
            when {
                err != null -> msg(err)
                nf.exists() -> msg("Already exists")
                nf.mkdir() -> {
                    val p = panes[i]
                    p.expanded = p.expanded + dir.absolutePath
                    p.cursor = nf.absolutePath
                    p.scrollTarget = nf.absolutePath
                    refreshAll()
                }
                else -> msg("Couldn't create folder here")
            }
        }
    }

    fun requestZip() {
        val t = targets()
        if (t.isEmpty()) { msg("Tap or long-press items first"); return }
        val dest = focusDir(1 - activePane)
        val def = (if (t.size == 1) t[0].nameWithoutExtension.ifEmpty { t[0].name } else "archive") + ".zip"
        dialog = DialogState.Input("Create ZIP in ${dest.absolutePath}", def, "Create") { name ->
            val n = if (name.endsWith(".zip", ignoreCase = true)) name else "$name.zip"
            val err = validateName(n)
            if (err != null) msg(err) else runOp("Compressing") { r ->
                val out = FileOps.uniqueTarget(dest, n, isDir = false)
                FileOps.zip(t, out, r)
                "Created ${out.name}"
            }
        }
    }

    fun requestUnzip() {
        val z = targets().singleOrNull()?.takeIf { it.isFile && it.extension.equals("zip", true) }
        if (z == null) { msg("Select a single .zip file"); return }
        askUnzip(z)
    }

    private fun askUnzip(z: File) {
        val dest = focusDir(1 - activePane)
        dialog = DialogState.Confirm(
            "Extract \"${z.name}\"?", "To: ${dest.absolutePath}/${z.nameWithoutExtension}", "Extract",
        ) {
            runOp("Extracting") { r ->
                val out = FileOps.uniqueTarget(dest, z.nameWithoutExtension, isDir = true)
                FileOps.unzip(z, out, r)
                "Extracted to ${out.name}"
            }
        }
    }

    fun showProperties() {
        val p = panes[activePane]
        val t = targets().ifEmpty { p.cursor?.let { listOf(File(it)) } ?: emptyList() }
        if (t.isEmpty()) return
        val f = t[0]
        val title = if (t.size == 1) f.name.ifEmpty { "/" } else "${t.size} items"
        val base = buildList {
            if (t.size == 1) {
                add("Path" to f.absolutePath)
                add("Type" to if (f.isDirectory) "Folder" else FileTypes.mime(f))
                add("Modified" to Fs.formatDate(f.lastModified()))
                if (FileTypes.kind(f) == Kind.APK) {
                    val ctx = getApplication<Application>()
                    Apk.info(ctx, f)?.let { info ->
                        Apk.label(ctx, info)?.let { add("App" to it) }
                        add("Package" to info.packageName)
                        add("Version" to "${info.versionName ?: "?"} (${info.longVersionCode})")
                    }
                }
                add("Permissions" to buildString {
                    append(if (f.canRead()) "r" else "-")
                    append(if (f.canWrite()) "w" else "-")
                    append(if (f.canExecute()) "x" else "-")
                })
                roots.firstOrNull { it.path == f.absolutePath }?.let {
                    runCatching { StatFs(it.path) }.getOrNull()?.let { s ->
                        add("Free" to "${Fs.formatSize(s.availableBytes)} of ${Fs.formatSize(s.totalBytes)}")
                    }
                }
            }
        }
        dialog = DialogState.Info(title, base + ("Size" to "Calculating…"))
        viewModelScope.launch {
            val st = withContext(Dispatchers.IO) { FileOps.stats(t) }
            val d = dialog
            if (d is DialogState.Info && d.title == title) {
                dialog = d.copy(
                    lines = base + listOf(
                        "Size" to "${Fs.formatSize(st.bytes)}  (${"%,d".format(st.bytes)} bytes)",
                        "Contains" to "${st.files} files, ${maxOf(0, st.dirs - t.count { it.isDirectory })} folders",
                    )
                )
            }
        }
    }

    // ---------------------------------------------------------------- search

    fun openSearch() {
        searchResults = emptyList()
        dialog = DialogState.Search(focusDir(activePane))
    }

    fun search(dir: File, query: String) {
        searchJob?.cancel()
        searchResults = emptyList()
        searching = true
        val hidden = showHidden
        val job = viewModelScope.launch {
            try {
                val r = withContext(Dispatchers.IO) { FileOps.search(dir, query, hidden) }
                searchResults = r
                if (r.isEmpty()) msg("Nothing found")
            } finally {
                if (searchJob === coroutineContext[Job]) searching = false
            }
        }
        searchJob = job
    }

    fun closeSearch() {
        searchJob?.cancel()
        searching = false
        dialog = null
    }

    /** Expand the tree down to [f] in the active pane and scroll to it. */
    fun reveal(f: File) {
        closeSearch()
        val i = activePane
        val p = panes[i]
        val path = f.absolutePath
        val root = roots.filter { path == it.path || FileOps.isUnder(path, it.path) }
            .maxByOrNull { it.path.length } ?: return
        val add = mutableSetOf(root.path)
        var cur = f.parentFile
        while (cur != null && cur.absolutePath != root.path) { add += cur.absolutePath; cur = cur.parentFile }
        p.expanded = p.expanded + add
        p.cursor = path
        p.scrollTarget = path
        refresh(i)
    }
}
