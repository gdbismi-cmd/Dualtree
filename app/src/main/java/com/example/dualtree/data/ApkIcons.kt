package com.example.dualtree.data

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.request.Options
import java.io.File

/** Coil model for "the launcher icon inside this .apk file". */
data class ApkIcon(val file: File)

object Apk {
    /** Reads the manifest of an APK on disk, with paths fixed up so resources (icon, label) load. */
    fun info(ctx: Context, file: File): PackageInfo? {
        val pm = ctx.packageManager
        val path = file.absolutePath
        val info = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(path, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(path, 0)
            }
        } catch (e: Exception) {
            null
        } ?: return null
        info.applicationInfo?.let {
            it.sourceDir = path
            it.publicSourceDir = path
        }
        return info
    }

    fun label(ctx: Context, info: PackageInfo): String? =
        info.applicationInfo?.loadLabel(ctx.packageManager)?.toString()
}

/** Loads an APK's app icon for Coil (runs on Coil's IO dispatcher). */
class ApkIconFetcher(private val data: ApkIcon, private val ctx: Context) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val app = Apk.info(ctx, data.file)?.applicationInfo
            ?: throw IllegalStateException("Not a readable APK: ${data.file.name}")
        val drawable = app.loadIcon(ctx.packageManager)
        return DrawableResult(drawable = drawable, isSampled = false, dataSource = DataSource.DISK)
    }

    class Factory(private val ctx: Context) : Fetcher.Factory<ApkIcon> {
        override fun create(data: ApkIcon, options: Options, imageLoader: ImageLoader): Fetcher =
            ApkIconFetcher(data, ctx.applicationContext)
    }
}

/** Memory-cache key so icons aren't re-extracted while scrolling. */
class ApkIconKeyer : Keyer<ApkIcon> {
    override fun key(data: ApkIcon, options: Options): String =
        "apk:${data.file.absolutePath}:${data.file.lastModified()}:${data.file.length()}"
}
