package com.example.dualtree.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dualtree.ExplorerViewModel
import com.example.dualtree.model.DialogState
import java.io.File

@Composable
fun DialogHost(state: DialogState, vm: ExplorerViewModel) {
    val dismiss = { vm.dialog = null }
    when (state) {
        is DialogState.Confirm -> AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(state.title) },
            text = { Text(state.message) },
            confirmButton = {
                TextButton(onClick = { vm.dialog = null; state.onConfirm() }) { Text(state.confirmLabel) }
            },
            dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
        )

        is DialogState.Input -> InputDialog(state, dismiss) { text ->
            vm.dialog = null
            state.onConfirm(text)
        }

        is DialogState.Info -> AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(state.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    state.lines.forEach { (k, v) ->
                        Text(k, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SelectionContainer { Text(v, style = MaterialTheme.typography.bodyMedium) }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = dismiss) { Text("OK") } },
        )

        is DialogState.Search -> SearchDialog(state.dir, vm)
    }
}

@Composable
private fun InputDialog(state: DialogState.Input, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    // Pre-select the name without its extension, like most file managers.
    var value by remember(state) {
        val end = state.initial.lastIndexOf('.').takeIf { it > 0 } ?: state.initial.length
        mutableStateOf(TextFieldValue(state.initial, TextRange(0, end)))
    }
    val focus = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (value.text.isNotBlank()) onConfirm(value.text.trim())
                }),
            )
            LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.text.trim()) }, enabled = value.text.isNotBlank()) {
                Text(state.confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SearchDialog(dir: File, vm: ExplorerViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    val run = { if (query.isNotBlank()) vm.search(dir, query) }
    AlertDialog(
        onDismissRequest = vm::closeSearch,
        title = { Text("Search in ${dir.name.ifEmpty { "/" }}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("name, or *.jpg") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { run() }),
                    trailingIcon = { IconButton(onClick = run) { Icon(Icons.Filled.Search, "Search") } },
                )
                if (vm.searching) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                } else if (vm.searchResults.isNotEmpty()) {
                    Text(
                        "${vm.searchResults.size} found — tap to reveal",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(vm.searchResults, key = { it.absolutePath }) { f ->
                        Column(Modifier.fillMaxWidth().clickable { vm.reveal(f) }.padding(vertical = 6.dp)) {
                            Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                f.parent ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = vm::closeSearch) { Text("Close") } },
    )
}
