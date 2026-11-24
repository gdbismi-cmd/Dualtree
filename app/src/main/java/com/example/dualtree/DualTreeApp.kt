package com.example.dualtree

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.dualtree.data.ApkIconFetcher
import com.example.dualtree.data.ApkIconKeyer

class DualTreeApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                add(ApkIconKeyer())
                add(ApkIconFetcher.Factory(this@DualTreeApp))
            }
            .crossfade(true)
            .build()
}
