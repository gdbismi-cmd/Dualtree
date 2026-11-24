package com.example.dualtree.data

import android.os.SystemClock
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File

/** One box in the disk map. Children are sorted largest first. */
class SizeNode(
    val file: File,
    val name: String,
    val size: Long,
    val isDir: Boolean,
    val children: List<SizeNode>,
    val files: Int,
    /** Synthetic "N smaller items" bucket. */
    val isOther: Boolean = false,
)

object DiskScanner {
    /** Keep the map readable (and memory bounded): beyond this, small items are lumped together. */
    private const val MAX_CHILDREN = 120
    private val SKIP = setOf("/proc", "/sys", "/dev")

    suspend fun scan(root: File, progress: (files: Int, bytes: Long) -> Unit): SizeNode {
        var files = 0
        var bytes = 0L
        var last = 0L

        suspend fun walk(f: File, isRoot: Boolean): SizeNode {
            currentCoroutineContext().ensureActive()
            val link = !isRoot && FileOps.isLink(f)
            if (!f.isDirectory || link || f.absolutePath in SKIP) {
                val len = if (link || f.isDirectory) 0L else f.length()
                files++
                bytes += len
                val now = SystemClock.uptimeMillis()
                if (now - last > 150) { last = now; progress(files, bytes) }
                return SizeNode(f, f.name, len, false, emptyList(), 1)
            }
            val kids = (f.listFiles() ?: emptyArray()).map { walk(it, false) }.sortedByDescending { it.size }
            val shown = if (kids.size > MAX_CHILDREN) {
                val rest = kids.drop(MAX_CHILDREN - 1)
                kids.take(MAX_CHILDREN - 1) + SizeNode(
                    f, "${rest.size} smaller items", rest.sumOf { it.size }, false,
                    emptyList(), rest.sumOf { it.files }, isOther = true,
                )
            } else kids
            return SizeNode(f, f.name.ifEmpty { "/" }, kids.sumOf { it.size }, true, shown, kids.sumOf { it.files })
        }

        val result = walk(root, true)
        progress(files, bytes)
        return result
    }
}
