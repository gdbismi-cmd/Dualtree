package com.example.dualtree.data

import android.os.SystemClock
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

typealias Report = (detail: String, fraction: Float?) -> Unit

/** All blocking file operations. Call from Dispatchers.IO; every loop is cancellable. */
object FileOps {
    private const val BUF = 256 * 1024

    data class Stats(val bytes: Long, val files: Int, val dirs: Int)

    private class Tracker(private val total: Long, private val report: Report) {
        private var done = 0L
        private var last = 0L
        fun add(n: Long, name: String) {
            done += n
            val now = SystemClock.uptimeMillis()
            if (now - last > 120) {
                last = now
                report(name, if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else null)
            }
        }
    }

    fun isUnder(child: String, parent: String): Boolean =
        if (parent == "/") child.length > 1 && child.startsWith("/") else child.startsWith("$parent/")

    /** Never follow symlinks when recursing — avoids loops and deleting a link's target. */
    internal fun isLink(f: File) = try { Files.isSymbolicLink(f.toPath()) } catch (e: Exception) { false }

    private fun sizeOf(f: File): Long = when {
        isLink(f) -> 0
        f.isDirectory -> f.listFiles()?.sumOf { sizeOf(it) } ?: 0
        else -> f.length()
    }

    fun stats(files: List<File>): Stats {
        var bytes = 0L; var nf = 0; var nd = 0
        fun walk(x: File) {
            if (x.isDirectory && !isLink(x)) { nd++; x.listFiles()?.forEach { walk(it) } }
            else { nf++; bytes += x.length() }
        }
        files.forEach { walk(it) }
        return Stats(bytes, nf, nd)
    }

    fun uniqueTarget(dir: File, name: String, isDir: Boolean): File {
        var t = File(dir, name)
        if (!t.exists()) return t
        val base = if (isDir) name else name.substringBeforeLast('.', name)
        val ext = if (isDir) "" else name.substringAfterLast('.', "")
        var i = 1
        while (true) {
            t = File(dir, if (ext.isEmpty()) "$base ($i)" else "$base ($i).$ext")
            if (!t.exists()) return t
            i++
        }
    }

    /** Copy or move [sources] into [destDir]. Returns number of items processed. */
    suspend fun transfer(sources: List<File>, destDir: File, move: Boolean, report: Report): Int {
        val destCanon = destDir.canonicalPath
        for (s in sources) {
            val sc = s.canonicalPath
            if (s.isDirectory && (destCanon == sc || destCanon.startsWith("$sc/"))) {
                throw IOException("Can't ${if (move) "move" else "copy"} \"${s.name}\" into itself")
            }
        }
        if (!destDir.isDirectory || !destDir.canWrite()) throw IOException("Destination is not writable")

        var count = 0
        val pending = mutableListOf<File>()
        if (move) {
            // Same-volume moves are instant renames.
            for (s in sources) {
                if (s.parentFile?.canonicalPath == destCanon) continue
                if (s.renameTo(uniqueTarget(destDir, s.name, s.isDirectory))) count++ else pending += s
            }
        } else {
            pending += sources
        }

        val tracker = Tracker(pending.sumOf { sizeOf(it) }, report)
        for (src in pending) {
            currentCoroutineContext().ensureActive()
            val target = uniqueTarget(destDir, src.name, src.isDirectory)
            copyRecursive(src, target, tracker)
            if (move) deleteRecursive(src)
            count++
        }
        return count
    }

    private suspend fun copyRecursive(src: File, dst: File, t: Tracker) {
        currentCoroutineContext().ensureActive()
        if (src.isDirectory && !isLink(src)) {
            if (!dst.mkdirs() && !dst.isDirectory) throw IOException("Can't create ${dst.name}")
            src.listFiles()?.forEach { copyRecursive(it, File(dst, it.name), t) }
            dst.setLastModified(src.lastModified())
        } else {
            try {
                src.inputStream().use { input ->
                    dst.outputStream().use { out ->
                        val buf = ByteArray(BUF)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            t.add(n.toLong(), src.name)
                            currentCoroutineContext().ensureActive()
                        }
                    }
                }
                dst.setLastModified(src.lastModified())
            } catch (e: Throwable) {
                dst.delete() // don't leave a half-written file behind
                throw e
            }
        }
    }

    suspend fun delete(files: List<File>, report: Report) {
        files.forEachIndexed { i, f ->
            report(f.name, i.toFloat() / files.size)
            deleteRecursive(f)
        }
    }

    private suspend fun deleteRecursive(f: File) {
        currentCoroutineContext().ensureActive()
        if (f.isDirectory && !isLink(f)) f.listFiles()?.forEach { deleteRecursive(it) }
        if (!f.delete() && f.exists()) throw IOException("Couldn't delete ${f.name}")
    }

    suspend fun zip(sources: List<File>, out: File, report: Report) {
        val t = Tracker(sources.sumOf { sizeOf(it) }, report)
        val outCanon = out.canonicalPath
        val buf = ByteArray(BUF)
        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(out))).use { zos ->
                suspend fun add(f: File, name: String) {
                    currentCoroutineContext().ensureActive()
                    if (f.canonicalPath == outCanon) return
                    if (f.isDirectory && !isLink(f)) {
                        zos.putNextEntry(ZipEntry("$name/").apply { time = f.lastModified() })
                        zos.closeEntry()
                        f.listFiles()?.forEach { add(it, "$name/${it.name}") }
                    } else {
                        zos.putNextEntry(ZipEntry(name).apply { time = f.lastModified() })
                        f.inputStream().use { inp ->
                            while (true) {
                                val n = inp.read(buf)
                                if (n < 0) break
                                zos.write(buf, 0, n)
                                t.add(n.toLong(), f.name)
                                currentCoroutineContext().ensureActive()
                            }
                        }
                        zos.closeEntry()
                    }
                }
                for (s in sources) add(s, s.name)
            }
        } catch (e: Throwable) {
            out.delete()
            throw e
        }
    }

    suspend fun unzip(zip: File, dest: File, report: Report) {
        ZipFile(zip).use { zf ->
            val entries = zf.entries().toList()
            val t = Tracker(entries.sumOf { maxOf(it.size, 0L) }, report)
            if (!dest.mkdirs() && !dest.isDirectory) throw IOException("Can't create ${dest.name}")
            val destCanon = dest.canonicalPath
            val buf = ByteArray(BUF)
            for (e in entries) {
                currentCoroutineContext().ensureActive()
                val outFile = File(dest, e.name)
                val c = outFile.canonicalPath
                // Zip-slip protection
                if (c != destCanon && !c.startsWith("$destCanon/")) throw IOException("Unsafe entry: ${e.name}")
                if (e.isDirectory) { outFile.mkdirs(); continue }
                outFile.parentFile?.mkdirs()
                zf.getInputStream(e).use { inp ->
                    outFile.outputStream().use { o ->
                        while (true) {
                            val n = inp.read(buf)
                            if (n < 0) break
                            o.write(buf, 0, n)
                            t.add(n.toLong(), outFile.name)
                            currentCoroutineContext().ensureActive()
                        }
                    }
                }
                if (e.time > 0) outFile.setLastModified(e.time)
            }
        }
    }

    /** Recursive name search. Supports plain substring or * / ? wildcards. */
    suspend fun search(root: File, query: String, showHidden: Boolean, limit: Int = 1000): List<File> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val rx: Regex? = if (q.contains('*') || q.contains('?')) {
            Regex(
                q.split("*").joinToString(".*") { part ->
                    part.split("?").joinToString(".") { Regex.escape(it) }
                },
                RegexOption.IGNORE_CASE,
            )
        } else null
        fun matches(name: String) = rx?.matches(name) ?: name.contains(q, ignoreCase = true)

        val out = ArrayList<File>()
        val stack = java.util.ArrayDeque<File>()
        stack.push(root)
        while (stack.isNotEmpty() && out.size < limit) {
            currentCoroutineContext().ensureActive()
            val kids = stack.pop().listFiles() ?: continue
            for (k in kids) {
                if (!showHidden && k.name.startsWith(".")) continue
                if (matches(k.name)) { out += k; if (out.size >= limit) break }
                if (k.isDirectory && !isLink(k)) stack.push(k)
            }
        }
        return out
    }
}
