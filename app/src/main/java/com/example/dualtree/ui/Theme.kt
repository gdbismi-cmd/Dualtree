package com.example.dualtree.ui

import android.content.Context
import android.os.Build
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode(val label: String) {
    SYSTEM("Follow system"),
    LIGHT("Light"),
    DARK("Dark"),
    BLACK("Pure black (AMOLED)"),
}

/** App-wide theme choice, saved in SharedPreferences. */
object ThemeSettings {
    private const val PREFS = "settings"
    private const val KEY = "theme_mode"

    var mode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    fun load(ctx: Context) {
        val saved = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        mode = ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.SYSTEM
    }

    fun set(ctx: Context, m: ThemeMode) {
        mode = m
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, m.name).apply()
    }
}

/** Whether the current theme choice resolves to dark. */
@Composable
fun isAppInDarkTheme(): Boolean = when (ThemeSettings.mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK, ThemeMode.BLACK -> true
}

private val LightColors = lightColorScheme(primary = Color(0xFF1E5AA8))
private val DarkColors = darkColorScheme(primary = Color(0xFF9EC2FF))

private fun ColorScheme.toPureBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF101010),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF222222),
    surfaceVariant = Color(0xFF1C1C1C),
    surfaceTint = Color.Transparent, // no grey wash on elevated bars
)

@Composable
fun DualTreeTheme(content: @Composable () -> Unit) {
    val dark = isAppInDarkTheme()
    val ctx = LocalContext.current
    val dynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val base = when {
        dynamic && dark -> dynamicDarkColorScheme(ctx)
        dynamic -> dynamicLightColorScheme(ctx)
        dark -> DarkColors
        else -> LightColors
    }
    val scheme = if (ThemeSettings.mode == ThemeMode.BLACK) base.toPureBlack() else base
    MaterialTheme(colorScheme = scheme) {
        // Replace the default ripple with the touch glow for all clickable rows/buttons.
        val glow = remember(scheme.primary) { GlowIndication(scheme.primary) }
        CompositionLocalProvider(LocalIndication provides glow, content = content)
    }
}
