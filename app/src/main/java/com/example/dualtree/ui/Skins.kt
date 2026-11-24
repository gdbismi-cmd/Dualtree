package com.example.dualtree.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** Which touch-glow animation a skin uses (see Glow.kt). */
enum class GlowKind { BLOOM, AERO, XP, BLACKBERRY, SYMBIAN, IOS, IOS1, XPLORE, SNAPTU, HOLO, GINGERBREAD }

@Immutable
data class GlowStyle(val kind: GlowKind, val color: Color, val accent: Color)

/**
 * A full look: Material colours plus the decorative bits Material doesn't cover —
 * glossy bar gradients, pane header strips, patterned pane backgrounds and the glow.
 * A null brush means "plain Material surface".
 */
@Immutable
data class Skin(
    val mode: ThemeMode,
    val scheme: ColorScheme,
    val dark: Boolean,
    val topBar: Brush? = null,
    val onTopBar: Color = Color.Unspecified,
    val bottomBar: Brush? = topBar,
    val onBottomBar: Color = onTopBar,
    val header: Brush? = null,
    val onHeader: Color = Color.Unspecified,
    val paneBackground: (DrawScope.() -> Unit)? = null,
    val glow: GlowStyle,
    val fontFamily: FontFamily? = null,
    val fontName: String = "Roboto",
)

val LocalSkin = staticCompositionLocalOf<Skin> { error("DualTreeTheme not applied") }

// ------------------------------------------------------------------ helpers

private fun c(hex: Long) = Color(hex)

private fun v(vararg stops: Pair<Float, Long>) =
    Brush.verticalGradient(*stops.map { it.first to Color(it.second) }.toTypedArray())

private fun h(vararg colors: Long) = Brush.horizontalGradient(colors.map { Color(it) })

private fun scheme(
    dark: Boolean,
    primary: Long, onPrimary: Long,
    bg: Long, onBg: Long,
    variant: Long, onVariant: Long,
    container: Long, onContainer: Long,
    select: Long, onSelect: Long,
    dialog: Long,
): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = c(primary), onPrimary = c(onPrimary),
        primaryContainer = c(container), onPrimaryContainer = c(onContainer),
        secondary = c(primary), onSecondary = c(onPrimary),
        secondaryContainer = c(select), onSecondaryContainer = c(onSelect),
        tertiary = c(primary),
        background = c(bg), onBackground = c(onBg),
        surface = c(bg), onSurface = c(onBg),
        surfaceVariant = c(variant), onSurfaceVariant = c(onVariant),
        surfaceTint = Color.Transparent,
        surfaceDim = c(bg), surfaceBright = c(bg),
        surfaceContainerLowest = c(bg), surfaceContainerLow = c(bg),
        surfaceContainer = c(dialog), surfaceContainerHigh = c(dialog), surfaceContainerHighest = c(dialog),
        outline = c(onVariant).copy(alpha = 0.6f), outlineVariant = c(variant),
    )
}

/** iPhone OS 1 grey pinstripes. */
private val pinstripes: DrawScope.() -> Unit = {
    drawRect(c(0xFFC5CCD4))
    val period = 5.dp.toPx()
    val stripe = 2.dp.toPx()
    var x = 0f
    while (x < size.width) {
        drawRect(c(0xFFCBD2D8), Offset(x, 0f), Size(stripe, size.height))
        x += period
    }
}

private val symbianBackdrop: DrawScope.() -> Unit = {
    drawRect(Brush.verticalGradient(listOf(c(0xFF143566), c(0xFF07162E), c(0xFF030A18))))
}

private val snaptuBackdrop: DrawScope.() -> Unit = {
    drawRect(c(0xFF0D1B33))
    drawRect(
        Brush.radialGradient(
            listOf(c(0xFF22437A).copy(alpha = 0.55f), Color.Transparent),
            center = Offset(size.width * 0.3f, size.height * 0.15f),
            radius = size.maxDimension * 0.8f,
        )
    )
}

private val xploreBackdrop: DrawScope.() -> Unit = {
    drawRect(Brush.verticalGradient(listOf(c(0xFF1B1E23), c(0xFF131518))))
}

// ------------------------------------------------------------------ the retro skins

fun retroSkin(mode: ThemeMode): Skin? = when (mode) {
    ThemeMode.XP -> Skin(
        mode, dark = false,
        scheme = scheme(
            false, 0xFF0054E3, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000, 0xFFECE9D8, 0xFF4D4D4D,
            0xFFD6DFF7, 0xFF215DC6, 0xFF316AC5, 0xFFFFFFFF, 0xFFECE9D8,
        ),
        // Luna title bar
        topBar = v(0f to 0xFF3A8CFF, 0.08f to 0xFF0A66F5, 0.45f to 0xFF0058EE, 0.9f to 0xFF0049D6, 1f to 0xFF003BB8),
        onTopBar = c(0xFFFFFFFF),
        // Luna taskbar
        bottomBar = v(0f to 0xFF5A9BF6, 0.12f to 0xFF3A7FEA, 0.5f to 0xFF245EDC, 1f to 0xFF1941A5),
        onBottomBar = c(0xFFFFFFFF),
        header = h(0xFFFFFFFF, 0xFFC6D3F7),
        onHeader = c(0xFF215DC6),
        glow = GlowStyle(GlowKind.XP, c(0xFFE68B2C), c(0xFFFFE3A6)),
    )

    ThemeMode.VISTA -> Skin(
        mode, dark = false,
        scheme = scheme(
            false, 0xFF1E7FCB, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF1E1E1E, 0xFFEEF3FA, 0xFF5A6B7D,
            0xFFDDEEFB, 0xFF0F4C81, 0xFFC4E1FA, 0xFF0B2A4A, 0xFFF7F9FC,
        ),
        // Black Aero glass with the hard sheen line half-way down
        topBar = v(0f to 0xFF5C6A78, 0.48f to 0xFF27333F, 0.5f to 0xFF0C141C, 1f to 0xFF1B2A38),
        onTopBar = c(0xFFFFFFFF),
        header = v(0f to 0xFFFFFFFF, 1f to 0xFFE1ECF7),
        onHeader = c(0xFF1E395B),
        glow = GlowStyle(GlowKind.AERO, c(0xFFC4E5F9), c(0xFF3C9FE0)),
    )

    ThemeMode.WIN7 -> Skin(
        mode, dark = false,
        scheme = scheme(
            false, 0xFF3399FF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF1E1E1E, 0xFFF0F5FB, 0xFF5B6E87,
            0xFFDCEBFC, 0xFF1E395B, 0xFFCCE8FF, 0xFF000000, 0xFFF5F8FC,
        ),
        // Explorer command bar
        topBar = v(0f to 0xFFFDFEFF, 0.5f to 0xFFEAF1FA, 0.5f to 0xFFDCE6F4, 1f to 0xFFD3E0F1),
        onTopBar = c(0xFF1E395B),
        // Taskbar glass
        bottomBar = v(0f to 0xFF7591B0, 0.5f to 0xFF3F5F80, 0.5f to 0xFF2A4766, 1f to 0xFF1C3550),
        onBottomBar = c(0xFFFFFFFF),
        header = v(0f to 0xFFFFFFFF, 1f to 0xFFE9F0F8),
        onHeader = c(0xFF1E395B),
        glow = GlowStyle(GlowKind.AERO, c(0xFFCDE8FF), c(0xFF7DA2CE)),
    )

    ThemeMode.IOS1 -> Skin(
        mode, dark = false,
        scheme = scheme(
            false, 0xFF3D6CB4, 0xFFFFFFFF, 0xFFC5CCD4, 0xFF000000, 0xFFD9DEE4, 0xFF4C566C,
            0xFF8EA2BE, 0xFFFFFFFF, 0xFFB8C8DE, 0xFF000000, 0xFFE9EDF2,
        ),
        // Glossy steel-blue navigation bar
        topBar = v(0f to 0xFFB5C3D5, 0.5f to 0xFF8C9FB8, 0.5f to 0xFF7B90AC, 1f to 0xFF6D84A2),
        onTopBar = c(0xFFFFFFFF),
        // Black glossy toolbar / dock
        bottomBar = v(0f to 0xFF6B6B6B, 0.5f to 0xFF353535, 0.5f to 0xFF1E1E1E, 1f to 0xFF000000),
        onBottomBar = c(0xFFFFFFFF),
        header = v(0f to 0xFFEEF1F5, 1f to 0xFFC9D1DC),
        onHeader = c(0xFF2C3E5C),
        paneBackground = pinstripes,
        glow = GlowStyle(GlowKind.IOS1, c(0xFF015EE6), c(0xFF058CF5)),
    )

    ThemeMode.IOS -> Skin(
        mode, dark = false,
        scheme = scheme(
            false, 0xFF007AFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000, 0xFFF2F2F7, 0xFF8E8E93,
            0xFFE5F0FF, 0xFF007AFF, 0xFFDDEBFF, 0xFF000000, 0xFFF9F9F9,
        ),
        topBar = v(0f to 0xFFF9F9F9, 1f to 0xFFF4F4F6),
        onTopBar = c(0xFF007AFF),
        header = v(0f to 0xFFF9F9F9, 1f to 0xFFF9F9F9),
        onHeader = c(0xFF007AFF),
        glow = GlowStyle(GlowKind.IOS, c(0xFFD1D1D6), c(0xFFD1D1D6)),
    )

    ThemeMode.BLACKBERRY -> Skin(
        mode, dark = true,
        scheme = scheme(
            true, 0xFF2A9FE8, 0xFFFFFFFF, 0xFF1C1C1C, 0xFFEDEDED, 0xFF2C2C2C, 0xFFA8A8A8,
            0xFF24384A, 0xFFCFE8FF, 0xFF0E5A9C, 0xFFFFFFFF, 0xFF262626,
        ),
        topBar = v(0f to 0xFF4D4D4D, 0.5f to 0xFF2D2D2D, 0.5f to 0xFF1F1F1F, 1f to 0xFF111111),
        onTopBar = c(0xFFFFFFFF),
        header = v(0f to 0xFF333333, 1f to 0xFF1F1F1F),
        onHeader = c(0xFFFFFFFF),
        glow = GlowStyle(GlowKind.BLACKBERRY, c(0xFF0A5CA8), c(0xFF4FB3F2)),
    )

    ThemeMode.SYMBIAN -> Skin(
        mode, dark = true,
        scheme = scheme(
            true, 0xFF5FB4FF, 0xFF00224A, 0xFF0A1A33, 0xFFF0F6FF, 0xFF132A4F, 0xFF9FB6D6,
            0xFF1D3F73, 0xFFFFFFFF, 0xFF1E5FB4, 0xFFFFFFFF, 0xFF102445,
        ),
        topBar = v(0f to 0xFF4A86D4, 0.5f to 0xFF21539A, 0.5f to 0xFF173F7B, 1f to 0xFF0D2A57),
        onTopBar = c(0xFFFFFFFF),
        header = v(0f to 0xFF1D4279, 1f to 0xFF0F2750),
        onHeader = c(0xFFFFFFFF),
        paneBackground = symbianBackdrop,
        glow = GlowStyle(GlowKind.SYMBIAN, c(0xFF1F66C9), c(0xFF8FD0FF)),
    )

    ThemeMode.XPLORE -> Skin(
        mode, dark = true,
        scheme = scheme(
            true, 0xFF4C8DFF, 0xFFFFFFFF, 0xFF17191D, 0xFFE8E8E8, 0xFF262A31, 0xFF9AA3AF,
            0xFF2D3D57, 0xFFFFFFFF, 0xFF1F4E8F, 0xFFFFFFFF, 0xFF20242A,
        ),
        topBar = v(0f to 0xFF46566F, 1f to 0xFF262F3D),
        onTopBar = c(0xFFFFFFFF),
        header = v(0f to 0xFF2E3748, 1f to 0xFF20262F),
        onHeader = c(0xFFFFD34D),
        paneBackground = xploreBackdrop,
        glow = GlowStyle(GlowKind.XPLORE, c(0xFF4C8DFF), c(0xFFFFD34D)),
    )

    ThemeMode.SNAPTU -> Skin(
        mode, dark = true,
        scheme = scheme(
            true, 0xFFFF9A1F, 0xFF2B1600, 0xFF0D1B33, 0xFFF3F6FB, 0xFF18294A, 0xFF9DB0CC,
            0xFF2A3F66, 0xFFFFD699, 0xFF6B4513, 0xFFFFFFFF, 0xFF142444,
        ),
        topBar = v(0f to 0xFF2C5694, 0.5f to 0xFF1C3D70, 1f to 0xFF142B52),
        onTopBar = c(0xFFFFFFFF),
        bottomBar = v(0f to 0xFF1C3D70, 1f to 0xFF0E2042),
        onBottomBar = c(0xFFFFFFFF),
        header = h(0xFF1B355F, 0xFF132848),
        onHeader = c(0xFFFFB54D),
        paneBackground = snaptuBackdrop,
        glow = GlowStyle(GlowKind.SNAPTU, c(0xFFFF9A1F), c(0xFFFFD07A)),
    )

    ThemeMode.ICS -> Skin(
        mode, dark = true,
        scheme = scheme(
            true, 0xFF33B5E5, 0xFF000000, 0xFF111111, 0xFFFFFFFF, 0xFF1F1F1F, 0xFFB3B3B3,
            0xFF0E3A4A, 0xFF33B5E5, 0xFF0F4F66, 0xFFFFFFFF, 0xFF282828,
        ),
        // Holo action bar: charcoal with the holo-blue rule along the bottom
        topBar = v(0f to 0xFF2C2C2C, 0.94f to 0xFF1E1E1E, 0.94f to 0xFF33B5E5, 1f to 0xFF33B5E5),
        onTopBar = c(0xFFFFFFFF),
        bottomBar = v(0f to 0xFF3A3A3A, 0.03f to 0xFF262626, 1f to 0xFF1A1A1A),
        onBottomBar = c(0xFFFFFFFF),
        header = v(0f to 0xFF222222, 1f to 0xFF1A1A1A),
        onHeader = c(0xFF33B5E5),
        glow = GlowStyle(GlowKind.HOLO, c(0xFF33B5E5), c(0xFF0099CC)),
    )

    ThemeMode.GINGERBREAD -> Skin(
        mode, dark = true,
        scheme = scheme(
            true, 0xFFFF9900, 0xFF000000, 0xFF000000, 0xFFFFFFFF, 0xFF1A1A1A, 0xFFAAAAAA,
            0xFF333333, 0xFFA4C639, 0xFF5A3A00, 0xFFFFFFFF, 0xFF2B2B2B,
        ),
        // 2.x title bar and options-menu panel
        topBar = v(0f to 0xFF5E5E5E, 0.5f to 0xFF3C3C3C, 0.5f to 0xFF2C2C2C, 1f to 0xFF1A1A1A),
        onTopBar = c(0xFFFFFFFF),
        bottomBar = v(0f to 0xFF6E6E6E, 0.5f to 0xFF474747, 1f to 0xFF2E2E2E),
        onBottomBar = c(0xFFFFFFFF),
        header = v(0f to 0xFF3C3C3C, 1f to 0xFF262626),
        onHeader = c(0xFFA4C639),
        glow = GlowStyle(GlowKind.GINGERBREAD, c(0xFFFF8A00), c(0xFFFFC81E)),
    )

    else -> null
}
