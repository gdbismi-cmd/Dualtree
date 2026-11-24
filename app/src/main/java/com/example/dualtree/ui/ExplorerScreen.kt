package com.example.dualtree.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.dualtree.ExplorerViewModel
import com.example.dualtree.data.ApkIcon
import com.example.dualtree.data.FileTypes
import com.example.dualtree.data.Fs
import com.example.dualtree.data.Kind
import com.example.dualtree.model.Node
import com.example.dualtree.model.Progress
import com.example.dualtree.model.SortMode
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(vm: ExplorerViewModel = viewModel()) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        vm.messages.collect { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
    }
    val progress by vm.progress.collectAsStateWithLifecycle()
    val selectedCount = vm.panes[vm.activePane].selected.size
    val anySelected = vm.panes.any { it.selected.isNotEmpty() }
    var sortMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var themePicker by remember { mutableStateOf(false) }
    val skin = LocalSkin.current

    BackHandler(enabled = anySelected) { vm.clearSelection() }

    Scaffold(
        topBar = {
            val barFg = skin.onTopBar
            TopAppBar(
                modifier = if (skin.topBar != null) Modifier.background(skin.topBar) else Modifier,
                colors = if (skin.topBar != null) {
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = barFg,
                        actionIconContentColor = barFg,
                        navigationIconContentColor = barFg,
                    )
                } else TopAppBarDefaults.topAppBarColors(),
                title = { Text(if (selectedCount > 0) "$selectedCount selected" else "DualTree") },
                navigationIcon = {
                    if (anySelected) {
                        IconButton(onClick = vm::clearSelection) { Icon(Icons.Filled.Close, "Clear selection") }
                    }
                },
                actions = {
                    IconButton(onClick = vm::openSearch) { Icon(Icons.Filled.Search, "Search") }
                    IconButton(onClick = vm::toggleHidden) {
                        Icon(
                            if (vm.showHidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            if (vm.showHidden) "Hide hidden files" else "Show hidden files",
                        )
                    }
                    Box {
                        IconButton(onClick = { sortMenu = true }) { Icon(Icons.Filled.SortByAlpha, "Sort") }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            SortMode.entries.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.label) },
                                    onClick = { vm.setSort(m); sortMenu = false },
                                    trailingIcon = { if (m == vm.sortMode) Icon(Icons.Filled.Check, null) },
                                )
                            }
                        }
                    }
                    Box {
                        IconButton(onClick = { moreMenu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                        DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Refresh") },
                                leadingIcon = { Icon(Icons.Filled.Refresh, null) },
                                onClick = { vm.refreshAll(); moreMenu = false },
                            )
                            DropdownMenuItem(
                                text = { Text("Disk map") },
                                leadingIcon = { Icon(Icons.Filled.Dashboard, null) },
                                onClick = { vm.openDiskMap(); moreMenu = false },
                            )
                            DropdownMenuItem(
                                text = { Text("Theme…") },
                                leadingIcon = { Icon(Icons.Filled.Palette, null) },
                                onClick = { themePicker = true; moreMenu = false },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = { ActionBar(vm) },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            progress?.let { ProgressStrip(it, vm::cancelOp) }
            BoxWithConstraints(Modifier.fillMaxSize()) {
                if (maxWidth >= 600.dp) {
                    // Tablets / landscape: both panes side by side.
                    Row(Modifier.fillMaxSize()) {
                        PaneView(vm, 0, Modifier.weight(1f))
                        VerticalDivider()
                        PaneView(vm, 1, Modifier.weight(1f))
                    }
                } else {
                    // Phones: one pane at a time, slide to reach the other (X-plore style).
                    SlidingPanes(vm)
                }
            }
        }
    }

    vm.dialog?.let { DialogHost(it, vm) }
    if (themePicker) ThemePickerDialog { themePicker = false }
    vm.diskMapRoot?.let { root ->
        DiskMapScreen(
            root = root,
            onClose = { vm.diskMapRoot = null },
            onReveal = { f -> vm.diskMapRoot = null; vm.reveal(f) },
        )
    }
    vm.viewer?.let { f -> FileViewer(f, onClose = { vm.viewer = null }, onOpenExternal = vm::openExternal) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SlidingPanes(vm: ExplorerViewModel) {
    val pager = rememberPagerState(initialPage = vm.activePane) { 2 }

    // Swiping makes that pane active…
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { vm.activate(it) }
    }
    // …and activating a pane (tab tap, etc.) slides to it.
    LaunchedEffect(vm.activePane) {
        if (pager.currentPage != vm.activePane && !pager.isScrollInProgress) {
            pager.animateScrollToPage(vm.activePane)
        }
    }

    Column(Modifier.fillMaxSize()) {
        PaneTabs(vm, pager)
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            key = { it },
        ) { page ->
            PaneView(
                vm, page,
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Slight depth effect while sliding between panes.
                        val off = ((pager.currentPage - page) + pager.currentPageOffsetFraction)
                            .absoluteValue.coerceIn(0f, 1f)
                        alpha = 1f - 0.35f * off
                        val sc = 1f - 0.06f * off
                        scaleX = sc
                        scaleY = sc
                    },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PaneTabs(vm: ExplorerViewModel, pager: PagerState) {
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val skin = LocalSkin.current
    val onTab = if (skin.header != null) skin.onHeader else colors.primary
    val offTab = if (skin.header != null) skin.onHeader.copy(alpha = 0.6f) else colors.onSurfaceVariant
    Row(Modifier.fillMaxWidth().background(skin.header ?: SolidColor(colors.surface))) {
        for (i in 0..1) {
            val pane = vm.panes[i]
            val name = remember(pane.cursor, pane.nodes) {
                vm.focusDir(i).let { f -> vm.roots.firstOrNull { it.path == f.absolutePath }?.label ?: f.name.ifEmpty { "/" } }
            }
            // How "selected" this tab is, following the finger while sliding.
            val pos = pager.currentPage + pager.currentPageOffsetFraction
            val sel = (1f - (pos - i).absoluteValue).coerceIn(0f, 1f)
            val count = pane.selected.size
            Column(
                Modifier
                    .weight(1f)
                    .clickable {
                        vm.activate(i)
                        scope.launch { pager.animateScrollToPage(i) }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${i + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(lerp(colors.outline, colors.primary, sel))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        name + if (count > 0) "  ($count)" else "",
                        style = MaterialTheme.typography.labelLarge,
                        color = lerp(offTab, onTab, sel),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth(0.6f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(onTab.copy(alpha = sel)),
                )
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun PaneView(vm: ExplorerViewModel, index: Int, modifier: Modifier) {
    val pane = vm.panes[index]
    val active = vm.activePane == index
    val listState = rememberLazyListState()
    val focus = remember(pane.cursor, pane.nodes) { vm.focusDir(index).absolutePath }

    LaunchedEffect(pane.nodes, pane.scrollTarget) {
        val target = pane.scrollTarget ?: return@LaunchedEffect
        val idx = pane.nodes.indexOfFirst { it.path == target }
        if (idx >= 0) {
            listState.animateScrollToItem(maxOf(0, idx - 3))
            pane.scrollTarget = null
        }
    }

    val skin = LocalSkin.current
    val headerBg: Brush = when {
        active && skin.header != null -> skin.header
        active -> SolidColor(MaterialTheme.colorScheme.primaryContainer)
        else -> SolidColor(MaterialTheme.colorScheme.surfaceVariant)
    }
    val headerFg = when {
        active && skin.header != null -> skin.onHeader
        active -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(modifier.fillMaxHeight()) {
        Text(
            text = if (focus.length > 34) "…" + focus.takeLast(33) else focus,
            style = MaterialTheme.typography.labelMedium,
            color = headerFg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .clickable { vm.activate(index) }
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { skin.paneBackground?.invoke(this) },
        ) {
            items(pane.nodes, key = { it.key }) { node ->
                NodeRow(
                    node = node,
                    isCursor = active && node.path == pane.cursor,
                    isSelected = node.path in pane.selected,
                    onClick = { vm.onClick(index, node) },
                    onLongClick = { vm.onLongClick(index, node) },
                    onToggle = { vm.toggleExpand(index, node) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NodeRow(
    node: Node,
    isCursor: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggle: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val bg = when {
        isSelected -> colors.secondaryContainer
        isCursor -> colors.surfaceVariant
        else -> Color.Transparent
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(bg)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
            .padding(start = (node.depth * 10).dp + 2.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (node.isDir) {
            Icon(
                if (node.expanded) Icons.Filled.ExpandMore else Icons.Filled.ChevronRight,
                contentDescription = if (node.expanded) "Collapse" else "Expand",
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onToggle),
            )
        } else {
            Spacer(Modifier.width(20.dp))
        }
        FileIcon(node, Modifier.size(28.dp))
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(
                node.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (node.root != null) FontWeight.SemiBold else null,
                color = if (node.hidden) colors.onSurface.copy(alpha = 0.5f) else colors.onSurface,
            )
            val sub = when {
                node.root != null && node.totalBytes > 0 ->
                    "${Fs.formatSize(node.freeBytes)} free / ${Fs.formatSize(node.totalBytes)}"
                node.root != null -> ""
                node.isDir -> Fs.formatDate(node.modified)
                else -> "${Fs.formatSize(node.size)} · ${Fs.formatDate(node.modified)}"
            }
            if (sub.isNotEmpty()) {
                Text(sub, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1)
            }
            if (node.root != null && node.totalBytes > 0) {
                val used = 1f - node.freeBytes.toFloat() / node.totalBytes
                LinearProgressIndicator(
                    progress = { used },
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp).height(3.dp),
                )
            }
        }
        if (isSelected) {
            Icon(Icons.Filled.CheckCircle, "Selected", tint = colors.primary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun FileIcon(node: Node, modifier: Modifier) {
    val kind = if (node.isDir) Kind.DIR else FileTypes.kind(node.file)
    if (kind == Kind.IMAGE) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(node.file).size(96).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(4.dp)),
        )
        return
    }
    if (kind == Kind.APK) {
        // Show the app's own launcher icon; fall back to the robot if it can't be read.
        var failed by remember(node.path) { mutableStateOf(false) }
        if (!failed) {
            AsyncImage(
                model = ApkIcon(node.file),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onError = { failed = true },
                modifier = modifier,
            )
            return
        }
    }
    val (icon: ImageVector, tint: Color) = when {
        node.root?.removable == true -> Icons.Filled.SdStorage to Color(0xFF43A047)
        node.root != null && node.path == "/" -> Icons.Filled.Storage to Color(0xFF757575)
        node.root != null -> Icons.Filled.PhoneAndroid to MaterialTheme.colorScheme.primary
        kind == Kind.DIR -> (if (node.expanded) Icons.Filled.FolderOpen else Icons.Filled.Folder) to Color(0xFFF9A825)
        kind == Kind.VIDEO -> Icons.Filled.Movie to Color(0xFFE53935)
        kind == Kind.AUDIO -> Icons.Filled.MusicNote to Color(0xFF8E24AA)
        kind == Kind.TEXT -> Icons.Filled.Description to Color(0xFF1E88E5)
        kind == Kind.ARCHIVE -> Icons.Filled.FolderZip to Color(0xFF8D6E63)
        kind == Kind.APK -> Icons.Filled.Android to Color(0xFF7CB342)
        kind == Kind.PDF -> Icons.Filled.PictureAsPdf to Color(0xFFD32F2F)
        else -> Icons.Filled.InsertDriveFile to Color(0xFF90A4AE)
    }
    Icon(icon, null, modifier, tint = tint)
}

@Composable
private fun ActionBar(vm: ExplorerViewModel) {
    val arrow = if (vm.activePane == 0) "→" else "←"
    val skin = LocalSkin.current
    val bg = skin.bottomBar ?: SolidColor(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
    val fg = if (skin.bottomBar != null) skin.onBottomBar else MaterialTheme.colorScheme.onSurface
    CompositionLocalProvider(LocalContentColor provides fg) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(bg)
                .navigationBarsPadding()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            ToolButton(Icons.Filled.ContentCopy, "Copy $arrow") { vm.requestTransfer(move = false) }
            ToolButton(Icons.Filled.DriveFileMove, "Move $arrow") { vm.requestTransfer(move = true) }
            ToolButton(Icons.Filled.Delete, "Delete", vm::requestDelete)
            ToolButton(Icons.Filled.DriveFileRenameOutline, "Rename", vm::requestRename)
            ToolButton(Icons.Filled.CreateNewFolder, "New folder", vm::requestNewFolder)
            ToolButton(Icons.Filled.FolderZip, "Zip $arrow", vm::requestZip)
            ToolButton(Icons.Filled.Unarchive, "Unzip $arrow", vm::requestUnzip)
            ToolButton(Icons.Filled.Share, "Share", vm::share)
            ToolButton(Icons.Filled.SelectAll, "Select all", vm::selectAll)
            ToolButton(Icons.Filled.Info, "Details", vm::showProperties)
            ToolButton(Icons.Filled.Dashboard, "Disk map", vm::openDiskMap)
        }
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun ProgressStrip(p: Progress, onCancel: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    val pct = p.fraction?.let { "  ${(it * 100).toInt()}%" } ?: ""
                    Text(p.title + pct, style = MaterialTheme.typography.labelLarge)
                    if (p.detail.isNotEmpty()) {
                        Text(p.detail, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
            val f = p.fraction
            if (f != null) {
                LinearProgressIndicator(progress = { f }, modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
