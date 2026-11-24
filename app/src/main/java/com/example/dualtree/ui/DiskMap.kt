package com.example.dualtree.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dualtree.data.DiskScanner
import com.example.dualtree.data.FileTypes
import com.example.dualtree.data.Fs
import com.example.dualtree.data.Kind
import com.example.dualtree.data.SizeNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * X-plore-style disk map: a squarified treemap where every box's area is its size on disk.
 * Tap = select, double-tap / "Open" = zoom into a folder, long-press = show it in the pane.
 */
@Composable
fun DiskMapScreen(root: File, onClose: () -> Unit, onReveal: (File) -> Unit) {
    var tree by remember(root) { mutableStateOf<SizeNode?>(null) }
    val progress = remember(root) { MutableStateFlow(0 to 0L) }
    val stack = remember(root) { mutableStateListOf<SizeNode>() }
    var selected by remember(root) { mutableStateOf<SizeNode?>(null) }

    LaunchedEffect(root) {
        val t = withContext(Dispatchers.IO) { DiskScanner.scan(root) { f, b -> progress.value = f to b } }
        tree = t
        stack.clear(); stack.add(t)
    }

    fun back() {
        if (stack.size > 1) { stack.removeAt(stack.lastIndex); selected = null } else onClose()
    }
    fun open(n: SizeNode) {
        if (n.isDir && n.children.isNotEmpty()) { stack.add(n); selected = null }
    }

    Dialog(onDismissRequest = ::back, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val skin = LocalSkin.current
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                val current = stack.lastOrNull()
                // ---- top bar (uses the theme's bar styling)
                val barFg = if (skin.topBar != null) skin.onTopBar else MaterialTheme.colorScheme.onSurface
                CompositionLocalProvider(LocalContentColor provides barFg) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(skin.topBar ?: SolidColor(MaterialTheme.colorScheme.surfaceVariant))
                            .statusBarsPadding(),
                    ) {
                        Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = ::back) {
                                Icon(if (stack.size > 1) Icons.Filled.ArrowBack else Icons.Filled.Close, "Back")
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Disk map", style = MaterialTheme.typography.titleMedium, color = barFg)
                                Text(
                                    current?.let { "${it.name} · ${Fs.formatSize(it.size)} · ${"%,d".format(it.files)} files" }
                                        ?: root.absolutePath,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = barFg.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (stack.size > 1) {
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                stack.forEachIndexed { i, n ->
                                    if (i > 0) Icon(Icons.Filled.ChevronRight, null, Modifier.size(14.dp))
                                    Text(
                                        n.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = barFg,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable {
                                                while (stack.size > i + 1) stack.removeAt(stack.lastIndex)
                                                selected = null
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                if (current == null) {
                    val p by progress.collectAsState()
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Measuring ${root.name.ifEmpty { "/" }}…", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${"%,d".format(p.first)} files · ${Fs.formatSize(p.second)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Box(Modifier.weight(1f).fillMaxWidth().padding(4.dp)) {
                        Treemap(
                            node = current,
                            selected = selected,
                            glow = skin.glow,
                            onSelect = { selected = it },
                            onOpen = ::open,
                            onReveal = onReveal,
                        )
                    }
                    InfoPanel(
                        node = selected ?: current,
                        parent = current,
                        canOpen = selected?.let { it.isDir && it.children.isNotEmpty() && it !== current } == true,
                        onOpen = { selected?.let(::open) },
                        onReveal = { (selected ?: current).let { if (!it.isOther) onReveal(it.file) } },
                    )
                }
            }
        }
    }
}

private class Tile(val node: SizeNode, val rect: Rect, val depth: Int, val color: Color, val nested: Boolean)

@Composable
private fun Treemap(
    node: SizeNode,
    selected: SizeNode?,
    glow: GlowStyle,
    onSelect: (SizeNode) -> Unit,
    onOpen: (SizeNode) -> Unit,
    onReveal: (File) -> Unit,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val headerPx = with(density) { 15.dp.toPx() }
        val padPx = with(density) { 2.dp.toPx() }
        val minNest = with(density) { 56.dp.toPx() }
        val tiles = remember(node, w, h) { buildTiles(node, Rect(0f, 0f, w, h), headerPx, padPx, minNest) }

        val labelStyle = TextStyle(
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            shadow = Shadow(Color.Black.copy(alpha = 0.8f), Offset(1f, 1f), 3f),
        )
        val labels: List<TextLayoutResult?> = remember(tiles) {
            val minW = with(density) { 36.dp.toPx() }
            val minH = with(density) { 14.dp.toPx() }
            val twoLine = with(density) { 32.dp.toPx() }
            tiles.map { t ->
                val r = t.rect
                if (r.width < minW || r.height < minH) return@map null
                val text = if (!t.nested && r.height > twoLine) "${t.node.name}\n${Fs.formatSize(t.node.size)}"
                else "${t.node.name}  ${Fs.formatSize(t.node.size)}"
                measurer.measure(
                    text = text,
                    style = labelStyle,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = if (t.nested) 1 else 2,
                    constraints = Constraints(maxWidth = (r.width - 6 * density.density).toInt().coerceAtLeast(1)),
                )
            }
        }

        fun hit(p: Offset): Tile? = tiles.lastOrNull { it.rect.contains(p) }
        fun hitDir(p: Offset): Tile? =
            tiles.lastOrNull { it.rect.contains(p) && it.node.isDir && it.node.children.isNotEmpty() }

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(tiles) {
                    detectTapGestures(
                        onTap = { p -> hit(p)?.let { onSelect(it.node) } },
                        onDoubleTap = { p -> hitDir(p)?.let { onOpen(it.node) } },
                        onLongPress = { p -> hit(p)?.node?.takeIf { !it.isOther }?.let { onReveal(it.file) } },
                    )
                },
        ) {
            val border = 1.dp.toPx()
            tiles.forEachIndexed { i, t ->
                val r = t.rect
                if (r.width < 1f || r.height < 1f) return@forEachIndexed
                // Cushion shading so neighbouring boxes read as separate blocks.
                drawRect(
                    Brush.linearGradient(
                        listOf(lerp(t.color, Color.White, 0.28f), t.color, lerp(t.color, Color.Black, 0.3f)),
                        start = r.topLeft, end = r.bottomRight,
                    ),
                    r.topLeft, r.size,
                )
                drawRect(Color.Black.copy(alpha = 0.45f), r.topLeft, r.size, style = Stroke(border))
                labels[i]?.let { lay ->
                    clipRect(r.left, r.top, r.right, r.bottom) {
                        drawText(lay, topLeft = Offset(r.left + 3.dp.toPx(), r.top + 1.dp.toPx()))
                    }
                }
            }
            // Selected box glows in the theme's glow colours.
            tiles.firstOrNull { it.node === selected }?.let { t ->
                val r = t.rect
                for (k in 3 downTo 1) {
                    val e = k * 2.dp.toPx()
                    drawRect(
                        glow.accent.copy(alpha = 0.18f),
                        Offset(r.left - e, r.top - e),
                        androidx.compose.ui.geometry.Size(r.width + 2 * e, r.height + 2 * e),
                        style = Stroke(2.dp.toPx()),
                    )
                }
                drawRect(Color.White.copy(alpha = 0.18f), r.topLeft, r.size)
                drawRect(glow.color, r.topLeft, r.size, style = Stroke(2.5.dp.toPx()))
                drawRect(Color.White, r.topLeft, r.size, style = Stroke(1.dp.toPx()))
            }
        }
    }
}

@Composable
private fun InfoPanel(
    node: SizeNode,
    parent: SizeNode,
    canOpen: Boolean,
    onOpen: () -> Unit,
    onReveal: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(14.dp).clip(RoundedCornerShape(3.dp))
                        .background(tileColor(node, 210f, 0)),
                )
                Spacer(Modifier.width(8.dp))
                Text(node.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val pct = if (parent.size > 0) node.size * 100.0 / parent.size else 0.0
            Text(
                buildString {
                    append(Fs.formatSize(node.size))
                    if (node !== parent) append(" · %.1f%% of ${parent.name}".format(pct))
                    if (node.isDir || node.isOther) append(" · ${"%,d".format(node.files)} files")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!node.isOther) {
                Text(
                    node.file.absolutePath,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onOpen, enabled = canOpen) { Text("Open") }
                OutlinedButton(onClick = onReveal, enabled = !node.isOther) { Text("Show in pane") }
            }
            Text(
                "Tap to select · double-tap to zoom in · long-press to show in pane",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

// ------------------------------------------------------------------ layout

private fun tileColor(n: SizeNode, hue: Float, depth: Int): Color = when {
    n.isOther -> Color(0xFF607D8B)
    n.isDir -> Color.hsl(hue, 0.55f, (0.38f + depth * 0.09f).coerceAtMost(0.68f))
    else -> when (FileTypes.kind(n.file)) {
        Kind.IMAGE -> Color(0xFF43A047)
        Kind.VIDEO -> Color(0xFFE53935)
        Kind.AUDIO -> Color(0xFF8E24AA)
        Kind.TEXT -> Color(0xFF1E88E5)
        Kind.ARCHIVE -> Color(0xFF8D6E63)
        Kind.APK -> Color(0xFF7CB342)
        Kind.PDF -> Color(0xFFD32F2F)
        else -> Color(0xFF78909C)
    }
}

private fun buildTiles(root: SizeNode, bounds: Rect, header: Float, pad: Float, minNest: Float): List<Tile> {
    val out = ArrayList<Tile>()
    fun place(children: List<SizeNode>, area: Rect, depth: Int, hue: Float?) {
        squarify(children, area).forEachIndexed { i, (n, r) ->
            val hh = hue ?: ((i * 47f + 200f) % 360f)
            val nest = n.isDir && n.children.isNotEmpty() && depth < 3 &&
                r.width > minNest && r.height > minNest + header
            out += Tile(n, r, depth, tileColor(n, hh, depth), nest)
            if (nest) place(n.children, Rect(r.left + pad, r.top + header, r.right - pad, r.bottom - pad), depth + 1, hh)
        }
    }
    place(root.children, bounds, 0, null)
    return out
}

/** Squarified treemap (Bruls et al.): keeps boxes close to square. Input must be sorted largest first. */
private fun squarify(items: List<SizeNode>, rect: Rect): List<Pair<SizeNode, Rect>> {
    val list = items.filter { it.size > 0 }
    val total = list.sumOf { it.size }.toDouble()
    if (total <= 0 || rect.width <= 1f || rect.height <= 1f) return emptyList()
    val scale = rect.width.toDouble() * rect.height / total
    val out = ArrayList<Pair<SizeNode, Rect>>(list.size)
    var r = rect
    var i = 0
    while (i < list.size) {
        val side = min(r.width, r.height).toDouble()
        if (side <= 0.5) break
        val row = ArrayList<SizeNode>()
        var rowSum = 0.0
        var best = Double.MAX_VALUE
        while (i < list.size) {
            val a = list[i].size * scale
            val sum = rowSum + a
            val maxA = if (row.isEmpty()) a else row[0].size * scale
            val worst = max(side * side * maxA / (sum * sum), sum * sum / (side * side * a))
            if (row.isNotEmpty() && worst > best) break
            row += list[i]; rowSum = sum; best = worst; i++
        }
        if (r.width >= r.height) {
            val colW = (rowSum / r.height).toFloat()
            var y = r.top
            for (n in row) {
                val hh = (n.size * scale / colW).toFloat()
                out += n to Rect(r.left, y, r.left + colW, y + hh)
                y += hh
            }
            r = Rect(r.left + colW, r.top, r.right, r.bottom)
        } else {
            val rowH = (rowSum / r.width).toFloat()
            var x = r.left
            for (n in row) {
                val ww = (n.size * scale / rowH).toFloat()
                out += n to Rect(x, r.top, x + ww, r.top + rowH)
                x += ww
            }
            r = Rect(r.left, r.top + rowH, r.right, r.bottom)
        }
    }
    return out
}
