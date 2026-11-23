package com.example.dualtree.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.dualtree.data.FileTypes
import com.example.dualtree.data.Fs
import com.example.dualtree.data.Kind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FileViewer(file: File, onClose: () -> Unit, onOpenExternal: (File) -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        when (FileTypes.kind(file)) {
            Kind.IMAGE -> ImageViewer(file, onClose, onOpenExternal)
            else -> TextViewer(file, onClose, onOpenExternal)
        }
    }
}

@Composable
private fun ViewerTopBar(
    title: String,
    subtitle: String,
    dark: Boolean,
    onClose: () -> Unit,
    onOpen: () -> Unit,
) {
    val fg = if (dark) Color.White else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (dark) Color.Black.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceVariant)
            .statusBarsPadding()
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Close", tint = fg) }
        Column(Modifier.weight(1f)) {
            Text(title, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = fg.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
        IconButton(onClick = onOpen) { Icon(Icons.Filled.OpenInNew, "Open with…", tint = fg) }
    }
}

// ------------------------------------------------------------------ images

/** Swipe between all images in the folder; pinch or double-tap to zoom. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageViewer(file: File, onClose: () -> Unit, onOpenExternal: (File) -> Unit) {
    val images = remember(file) {
        file.parentFile?.listFiles()
            ?.filter { it.isFile && FileTypes.kind(it) == Kind.IMAGE }
            ?.sortedBy { it.name.lowercase() }
            ?.takeIf { it.isNotEmpty() }
            ?: listOf(file)
    }
    val start = images.indexOfFirst { it.absolutePath == file.absolutePath }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = start) { images.size }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), key = { images[it].absolutePath }) { page ->
            ZoomableImage(images[page])
        }
        val current = images.getOrElse(pager.currentPage) { file }
        ViewerTopBar(
            title = current.name,
            subtitle = "${pager.currentPage + 1} / ${images.size} · ${Fs.formatSize(current.length())}",
            dark = true,
            onClose = onClose,
            onOpen = { onOpenExternal(current) },
        )
    }
}

@Composable
private fun ZoomableImage(file: File) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    AsyncImage(
        model = file,
        contentDescription = file.name,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var lastTap = 0L
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var moved = false
                    var multiTouch = false
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.count { it.pressed }
                        if (pressed >= 2) multiTouch = true
                        // Only take over the gesture when pinching or already zoomed,
                        // so single-finger swipes still page between images.
                        if (pressed >= 2 || scale > 1f) {
                            scale = (scale * event.calculateZoom()).coerceIn(1f, 6f)
                            offset = if (scale == 1f) Offset.Zero else offset + event.calculatePan()
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                        if (event.changes.any { (it.position - down.position).getDistance() > viewConfiguration.touchSlop }) {
                            moved = true
                        }
                    } while (event.changes.any { it.pressed })

                    if (!moved && !multiTouch) {
                        val now = down.uptimeMillis
                        if (now - lastTap < 300) {
                            if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f
                            lastTap = 0L
                        } else {
                            lastTap = now
                        }
                    }
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
    )
}

// ------------------------------------------------------------------ text

private const val MAX_TEXT_BYTES = 2_000_000

@Composable
private fun TextViewer(file: File, onClose: () -> Unit, onOpenExternal: (File) -> Unit) {
    val lines by produceState<List<String>?>(null, file) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val len = minOf(file.length(), MAX_TEXT_BYTES.toLong()).toInt()
                val buf = ByteArray(len)
                var off = 0
                file.inputStream().use { s ->
                    while (off < len) {
                        val n = s.read(buf, off, len - off)
                        if (n < 0) break
                        off += n
                    }
                }
                val text = String(buf, 0, off, Charsets.UTF_8)
                val l = text.lines()
                if (file.length() > MAX_TEXT_BYTES) l + "… (truncated, file is ${Fs.formatSize(file.length())})" else l
            }.getOrElse { listOf("Couldn't read file: ${it.message}") }
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ViewerTopBar(
                title = file.name,
                subtitle = "${Fs.formatSize(file.length())}${lines?.let { " · ${it.size} lines" } ?: ""}",
                dark = false,
                onClose = onClose,
                onOpen = { onOpenExternal(file) },
            )
            val l = lines
            if (l == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(Modifier.fillMaxSize().navigationBarsPadding()) {
                    itemsIndexed(l) { i, line ->
                        Row(Modifier.padding(horizontal = 6.dp)) {
                            Text(
                                "${i + 1}",
                                modifier = Modifier.width(44.dp).padding(end = 8.dp),
                                textAlign = TextAlign.End,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                            Text(line, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
