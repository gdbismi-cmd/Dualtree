package com.example.dualtree.model

import java.io.File

/** A storage volume shown at the top level of each pane (internal, SD card, root). */
data class StorageRoot(val file: File, val label: String, val removable: Boolean) {
    val path: String get() = file.absolutePath
}

/** One visible row in a pane's flattened tree. */
data class Node(
    val file: File,
    val depth: Int,
    val isDir: Boolean,
    val expanded: Boolean,
    val size: Long,
    val modified: Long,
    val hidden: Boolean,
    val rootIndex: Int,
    val root: StorageRoot? = null,
    val freeBytes: Long = 0,
    val totalBytes: Long = 0,
) {
    val path: String = file.absolutePath
    val name: String get() = root?.label ?: file.name
    val key: String get() = "$rootIndex|$path"
}

enum class SortMode(val label: String) {
    NAME("Name"), SIZE("Size"), DATE("Date modified"), TYPE("Type")
}

data class Progress(val title: String, val detail: String = "", val fraction: Float? = null)

sealed interface DialogState {
    data class Confirm(
        val title: String,
        val message: String,
        val confirmLabel: String,
        val onConfirm: () -> Unit,
    ) : DialogState

    data class Input(
        val title: String,
        val initial: String,
        val confirmLabel: String,
        val onConfirm: (String) -> Unit,
    ) : DialogState

    data class Info(val title: String, val lines: List<Pair<String, String>>) : DialogState

    data class Search(val dir: File) : DialogState
}
