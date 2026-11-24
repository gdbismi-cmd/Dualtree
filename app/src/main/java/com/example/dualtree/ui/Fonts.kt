package com.example.dualtree.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

/**
 * Open-licence stand-ins for each OS's system font (the originals can't be redistributed).
 * Files live in res/font (see scripts/fetch-fonts.sh) and are looked up by name, so a
 * missing file simply falls back to the default font instead of breaking the build.
 */
sealed interface FontSpec {
    data class Static(val regular: String, val bold: String) : FontSpec
    data class Variable(val res: String) : FontSpec
    data object SystemSans : FontSpec
    data object Default : FontSpec
}

data class ThemeFont(val spec: FontSpec, val name: String)

fun themeFontFor(mode: ThemeMode): ThemeFont = when (mode) {
    ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.BLACK -> ThemeFont(FontSpec.Default, "Roboto")
    ThemeMode.ICS -> ThemeFont(FontSpec.SystemSans, "Roboto (as introduced in ICS)")
    ThemeMode.GINGERBREAD -> ThemeFont(FontSpec.Variable("open_sans"), "Open Sans ≈ Droid Sans")
    ThemeMode.XPLORE -> ThemeFont(FontSpec.Variable("roboto_condensed"), "Roboto Condensed")
    ThemeMode.XP -> ThemeFont(FontSpec.Static("pt_sans_regular", "pt_sans_bold"), "PT Sans ≈ Tahoma")
    ThemeMode.VISTA, ThemeMode.WIN7 -> ThemeFont(FontSpec.Variable("noto_sans"), "Noto Sans ≈ Segoe UI")
    ThemeMode.IOS1 -> ThemeFont(FontSpec.Variable("arimo"), "Arimo ≈ Helvetica")
    ThemeMode.IOS -> ThemeFont(FontSpec.Variable("inter"), "Inter ≈ San Francisco")
    ThemeMode.BLACKBERRY -> ThemeFont(FontSpec.Variable("source_sans_3"), "Source Sans 3 ≈ Slate Pro")
    ThemeMode.SYMBIAN -> ThemeFont(FontSpec.Static("fira_sans_regular", "fira_sans_bold"), "Fira Sans ≈ Nokia Sans")
    ThemeMode.SNAPTU -> ThemeFont(FontSpec.Variable("nunito"), "Nunito")
}

private val cache = HashMap<FontSpec, FontFamily?>()

@SuppressLint("DiscouragedApi")
@OptIn(ExperimentalTextApi::class)
fun loadFontFamily(ctx: Context, spec: FontSpec): FontFamily? = synchronized(cache) {
    cache.getOrPut(spec) {
        fun id(name: String) = ctx.resources.getIdentifier(name, "font", ctx.packageName)
        when (spec) {
            FontSpec.Default -> null
            FontSpec.SystemSans -> FontFamily.SansSerif
            is FontSpec.Static -> {
                val r = id(spec.regular)
                val b = id(spec.bold)
                if (r == 0) null else FontFamily(
                    buildList {
                        add(Font(r, FontWeight.Normal))
                        add(Font(if (b != 0) b else r, FontWeight.Bold))
                    }
                )
            }
            is FontSpec.Variable -> {
                val v = id(spec.res)
                if (v == 0) null else FontFamily(
                    listOf(300, 400, 500, 600, 700).map { w ->
                        Font(
                            resId = v,
                            weight = FontWeight(w),
                            variationSettings = FontVariation.Settings(FontVariation.weight(w)),
                        )
                    }
                )
            }
        }
    }
}

/** Material type scale with every style switched to [family]. */
fun typographyFor(family: FontFamily?): Typography {
    val t = Typography()
    if (family == null) return t
    return t.copy(
        displayLarge = t.displayLarge.copy(fontFamily = family),
        displayMedium = t.displayMedium.copy(fontFamily = family),
        displaySmall = t.displaySmall.copy(fontFamily = family),
        headlineLarge = t.headlineLarge.copy(fontFamily = family),
        headlineMedium = t.headlineMedium.copy(fontFamily = family),
        headlineSmall = t.headlineSmall.copy(fontFamily = family),
        titleLarge = t.titleLarge.copy(fontFamily = family),
        titleMedium = t.titleMedium.copy(fontFamily = family),
        titleSmall = t.titleSmall.copy(fontFamily = family),
        bodyLarge = t.bodyLarge.copy(fontFamily = family),
        bodyMedium = t.bodyMedium.copy(fontFamily = family),
        bodySmall = t.bodySmall.copy(fontFamily = family),
        labelLarge = t.labelLarge.copy(fontFamily = family),
        labelMedium = t.labelMedium.copy(fontFamily = family),
        labelSmall = t.labelSmall.copy(fontFamily = family),
    )
}
