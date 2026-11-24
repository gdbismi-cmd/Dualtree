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

/**
 * Theme choices. [dark] = content is dark (null = follow system);
 * [darkTop]/[darkBottom] = whether the top/bottom bars are dark, so status/nav bar icons stay readable.
 */
enum class ThemeMode(
    val label: String,
    val description: String,
    val dark: Boolean?,
    val darkTop: Boolean? = dark,
    val darkBottom: Boolean? = dark,
) {
    SYSTEM("Follow system", "Material You, light or dark with the phone", null),
    LIGHT("Light", "Material You, always light", false),
    DARK("Dark", "Material You, always dark", true),
    BLACK("Pure black (AMOLED)", "True black backgrounds", true),
    GINGERBREAD("Android 2.3 Gingerbread", "Black lists, orange-gradient select", true),
    ICS("Android 4.0 ICS", "Holo dark, holo-blue press", true),
    XPLORE("X-plore", "Graphite panes, blue glow with a yellow edge", true),
    XP("Windows XP", "Luna blue bars, orange hot-track glow", false, darkTop = true, darkBottom = true),
    VISTA("Windows Vista", "Black Aero glass, aqua glass glow", false, darkTop = true, darkBottom = true),
    WIN7("Windows 7", "Light glass toolbar, sky-blue Aero glow", false, darkTop = false, darkBottom = true),
    IOS1("iPhone OS 1", "Pinstripes, glossy steel bars, blue gloss select", false, darkTop = true, darkBottom = true),
    IOS("iOS", "Clean white, grey press highlight", false),
    BLACKBERRY("BlackBerry OS", "Charcoal, glowing blue focus bar", true),
    SYMBIAN("Symbian", "S60 deep blue, light-sweep highlight", true),
    SNAPTU("Snaptu", "Midnight navy, orange neon ripple ring", true),
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

/** Whether the current theme's content is dark. */
@Composable
fun isAppInDarkTheme(): Boolean = ThemeSettings.mode.dark ?: isSystemInDarkTheme()

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
    surfaceTint = Color.Transparent,
)

/** Material You skins (System / Light / Dark / Black). */
fun materialSkin(mode: ThemeMode, systemDark: Boolean, ctx: Context): Skin {
    val dark = mode.dark ?: systemDark
    val dynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val base = when {
        dynamic && dark -> dynamicDarkColorScheme(ctx)
        dynamic -> dynamicLightColorScheme(ctx)
        dark -> DarkColors
        else -> LightColors
    }
    val scheme = if (mode == ThemeMode.BLACK) base.toPureBlack() else base
    return Skin(
        mode = mode,
        scheme = scheme,
        dark = dark,
        glow = GlowStyle(GlowKind.BLOOM, scheme.primary, scheme.primary),
    )
}

fun skinFor(mode: ThemeMode, systemDark: Boolean, ctx: Context): Skin {
    val skin = retroSkin(mode) ?: materialSkin(mode, systemDark, ctx)
    val font = themeFontFor(mode)
    return skin.copy(fontFamily = loadFontFamily(ctx, font.spec), fontName = font.name)
}

@Composable
fun DualTreeTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val mode = ThemeSettings.mode
    val skin = remember(mode, systemDark) { skinFor(mode, systemDark, ctx) }
    val typography = remember(skin.fontFamily) { typographyFor(skin.fontFamily) }
    MaterialTheme(colorScheme = skin.scheme, typography = typography) {
        // Each skin brings its own touch glow, used by every clickable row and button.
        val glow = remember(skin.glow) { GlowIndication(skin.glow) }
        CompositionLocalProvider(
            LocalSkin provides skin,
            LocalIndication provides glow,
            content = content,
        )
    }
}
