package com.example.dualtree.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Theme list with a mini mock-up of each skin. Tapping applies it immediately. */
@Composable
fun ThemePickerDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val skins = remember(systemDark) { ThemeMode.entries.map { skinFor(it, systemDark, ctx) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme") },
        text = {
            LazyColumn(Modifier.heightIn(max = 520.dp)) {
                items(skins, key = { it.mode.name }) { skin ->
                    val current = skin.mode == ThemeSettings.mode
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { ThemeSettings.set(ctx, skin.mode) }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SkinPreview(skin)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                skin.mode.label,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = skin.fontFamily ?: MaterialTheme.typography.bodyLarge.fontFamily,
                                ),
                            )
                            Text(
                                "${skin.mode.description} · ${skin.fontName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (current) {
                            Icon(Icons.Filled.CheckCircle, "Current theme", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun SkinPreview(skin: Skin) {
    val s = skin.scheme
    val shape = RoundedCornerShape(6.dp)
    Column(
        Modifier
            .size(width = 64.dp, height = 48.dp)
            .clip(shape)
            .border(1.dp, s.outline.copy(alpha = 0.6f), shape),
    ) {
        Box(Modifier.fillMaxWidth().height(10.dp).background(skin.topBar ?: SolidColor(s.surfaceVariant)))
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(s.background)
                .drawBehind { skin.paneBackground?.invoke(this) }
                .padding(horizontal = 4.dp, vertical = 3.dp),
        ) {
            Column {
                Box(Modifier.fillMaxWidth(0.7f).height(3.dp).background(s.onBackground.copy(alpha = 0.45f)))
                Spacer(Modifier.height(3.dp))
                // A row showing the theme's glow colour, as if it had just been tapped.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Brush.horizontalGradient(listOf(skin.glow.accent, skin.glow.color))),
                )
                Spacer(Modifier.height(3.dp))
                Box(Modifier.fillMaxWidth(0.5f).height(3.dp).background(s.onBackground.copy(alpha = 0.45f)))
            }
        }
        Box(
            Modifier.fillMaxWidth().height(7.dp)
                .background(skin.bottomBar ?: SolidColor(s.surfaceVariant)),
        ) {
            Box(Modifier.fillMaxHeight().width(10.dp).background(Color.Transparent))
        }
    }
}
