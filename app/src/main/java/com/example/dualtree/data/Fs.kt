package com.example.dualtree.data

import android.content.Context
import android.os.Environment
import android.webkit.MimeTypeMap
import com.example.dualtree.model.SortMode
import com.example.dualtree.model.StorageRoot
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Kind { DIR, IMAGE, VIDEO, AUDIO, TEXT, ARCHIVE, APK, PDF, OTHER }

object FileTypes {
    private val image = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")
    private val video = setOf("mp4", "mkv", "webm", "avi", "mov", "3gp", "m4v", "ts")
    private val audio = setOf("mp3", "wav", "ogg", "flac", "m4a", "aac", "opus", "amr")
    private val text = setOf(
        "txt", "log", "md", "json", "xml", "csv", "html", "htm", "js", "css", "kt", "kts",
        "java", "py", "sh", "ini", "conf", "cfg", "yml", "yaml", "properties", "gradle", "srt", "toml",
    )
    private val archive = setOf("zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz")

    fun kind(f: File): Kind {
        val e = f.extension.lowercase()
        return when {
            e in image -> Kind.IMAGE
            e in video -> Kind.VIDEO
            e in audio -> Kind.AUDIO
            e in text -> Kind.TEXT
            e in archive -> Kind.ARCHIVE
            e == "apk" -> Kind.APK
            e == "pdf" -> Kind.PDF
            else -> Kind.OTHER
        }
    }

    fun mime(f: File): String {
        val e = f.extension.lowercase()
        if (e == "apk") return "application/vnd.android.package-archive"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(e) ?: "*/*"
    }
}

object Fs {
    /** Internal storage, any SD cards / USB drives, and the filesystem root. */
    fun detectRoots(ctx: Context): List<StorageRoot> {
        val result = mutableListOf<StorageRoot>()
        result += StorageRoot(Environment.getExternalStorageDirectory(), "Internal storage", false)
        ctx.getExternalFilesDirs(null).filterNotNull().forEach { dir ->
            val p = dir.absolutePath
            val idx = p.indexOf("/Android/data/")
            if (idx > 0) {
                val root = File(p.substring(0, idx))
                if (result.none { it.path == root.absolutePath }) {
                    result += StorageRoot(root, "SD card · ${root.name}", removable = true)
                }
            }
        }
        result += StorageRoot(File("/"), "Root  /", false)
        return result
    }

    fun list(dir: File, showHidden: Boolean, sort: SortMode): List<File> {
        val all = dir.listFiles() ?: return emptyList()
        val visible = if (showHidden) all.toList() else all.filter { !it.name.startsWith(".") }
        val bySort: Comparator<File> = when (sort) {
            SortMode.NAME -> compareBy<File, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
            SortMode.SIZE -> compareByDescending<File> { it.length() }.thenBy { it.name.lowercase() }
            SortMode.DATE -> compareByDescending<File> { it.lastModified() }
            SortMode.TYPE -> compareBy<File>({ it.extension.lowercase() }, { it.name.lowercase() })
        }
        // Folders always first, like X-plore.
        return visible.sortedWith(compareBy<File> { !it.isDirectory }.then(bySort))
    }

    fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var v = bytes / 1024.0
        var i = 0
        while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
        return if (v >= 100) "%.0f %s".format(v, units[i]) else "%.1f %s".format(v, units[i])
    }

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    fun formatDate(t: Long): String = synchronized(dateFmt) { dateFmt.format(Date(t)) }
}
