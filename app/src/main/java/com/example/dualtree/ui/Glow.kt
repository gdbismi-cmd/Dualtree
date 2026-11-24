package com.example.dualtree.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Touch-to-glow indication. Each theme supplies a [GlowStyle] that picks how the
 * press is drawn, from a soft Material bloom to Aero glass, Luna hot-track or a Snaptu neon ring.
 * Installed app-wide through LocalIndication in [DualTreeTheme].
 */
class GlowIndication(private val style: GlowStyle) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        GlowNode(interactionSource, style)

    override fun equals(other: Any?) = other is GlowIndication && other.style == style
    override fun hashCode() = style.hashCode()
}

private class GlowNode(
    private val source: InteractionSource,
    private val style: GlowStyle,
) : Modifier.Node(), DrawModifierNode {

    private val intensity = Animatable(0f)
    private val spread = Animatable(0f)
    private var center: Offset? = null
    private var pressJob: Job? = null

    private val fadeOutMs = when (style.kind) {
        GlowKind.IOS -> 260
        GlowKind.IOS1, GlowKind.BLACKBERRY -> 420
        else -> 650
    }
    private val spreadMs = when (style.kind) {
        GlowKind.SYMBIAN -> 700
        GlowKind.SNAPTU -> 620
        else -> 520
    }

    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        center = interaction.pressPosition
                        pressJob = launch { intensity.animateTo(1f, tween(90)) }
                        launch {
                            spread.snapTo(0f)
                            spread.animateTo(1f, tween(spreadMs, easing = FastOutSlowInEasing))
                        }
                    }
                    is PressInteraction.Release, is PressInteraction.Cancel -> {
                        val fadeIn = pressJob
                        launch {
                            fadeIn?.join() // quick taps still flash fully
                            intensity.animateTo(0f, tween(fadeOutMs, easing = LinearOutSlowInEasing))
                        }
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val a = intensity.value
        val c = center
        if (a <= 0f || c == null) { drawContent(); return }
        val s = spread.value
        val col = style.color
        val acc = style.accent
        val w = size.width
        val h = size.height

        when (style.kind) {
            GlowKind.BLOOM -> {
                drawContent()
                bloom(c, col, a, s, 0.42f)
                drawRect(col.copy(alpha = 0.07f * a))
            }

            // Vista / 7: light-blue glass pill, white sheen on top half, crisp border, faint bloom.
            GlowKind.AERO -> {
                val inset = 1.dp.toPx()
                val tl = Offset(inset, inset)
                val sz = Size(w - 2 * inset, h - 2 * inset)
                val r = CornerRadius(3.dp.toPx())
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.9f * a), col.copy(alpha = a), col.copy(alpha = a))),
                    tl, sz, r,
                )
                drawRoundRect(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.55f * a),
                        0.5f to Color.White.copy(alpha = 0.15f * a),
                        0.5f to Color.Transparent,
                        1f to Color.Transparent,
                    ),
                    tl, sz, r,
                )
                drawRoundRect(acc.copy(alpha = a), tl, sz, r, style = Stroke(1.dp.toPx()))
                drawContent()
                bloom(c, acc, a, s, 0.2f)
            }

            // XP Luna: pale orange wash with the orange "hot-track" bar on top.
            GlowKind.XP -> {
                drawRect(acc.copy(alpha = 0.55f * a))
                drawContent()
                val bar = 3.dp.toPx()
                drawRect(
                    Brush.verticalGradient(listOf(Color(0xFFFFD98A).copy(alpha = a), col.copy(alpha = a)), 0f, bar),
                    size = Size(w, bar),
                )
                val line = 1.dp.toPx()
                drawRect(col.copy(alpha = 0.7f * a), Offset(0f, h - line), Size(w, line))
                bloom(c, col, a, s, 0.22f)
            }

            // BlackBerry: rounded blue focus bar with a soft halo around it.
            GlowKind.BLACKBERRY -> {
                val inset = 4.dp.toPx()
                val r = 6.dp.toPx()
                for (k in 1..3) {
                    val e = k * 1.dp.toPx()
                    drawRoundRect(
                        acc.copy(alpha = 0.16f * a),
                        Offset(inset - e, inset - e),
                        Size(w - 2 * (inset - e), h - 2 * (inset - e)),
                        CornerRadius(r + e),
                    )
                }
                val tl = Offset(inset, inset)
                val sz = Size(w - 2 * inset, h - 2 * inset)
                drawRoundRect(Brush.verticalGradient(listOf(acc.copy(alpha = a), col.copy(alpha = a))), tl, sz, CornerRadius(r))
                drawRoundRect(Color.White.copy(alpha = 0.18f * a), tl, Size(sz.width, sz.height / 2), CornerRadius(r))
                drawContent()
            }

            // Symbian S60: glossy blue highlight with a bright light sweep travelling across.
            GlowKind.SYMBIAN -> {
                val inset = 1.dp.toPx()
                val tl = Offset(inset, inset)
                val sz = Size(w - 2 * inset, h - 2 * inset)
                val r = CornerRadius(4.dp.toPx())
                drawRoundRect(Brush.verticalGradient(listOf(acc.copy(alpha = 0.95f * a), col.copy(alpha = 0.95f * a))), tl, sz, r)
                drawRoundRect(Color.White.copy(alpha = 0.22f * a), tl, Size(sz.width, sz.height / 2), r)
                drawContent()
                val band = 48.dp.toPx()
                val x = -band + (w + 2 * band) * s
                clipRect {
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.5f * a), Color.Transparent),
                            startX = x - band, endX = x + band,
                        )
                    )
                }
            }

            // iPhone OS 1: the classic glossy blue selected-row gradient.
            GlowKind.IOS1 -> {
                drawRect(Brush.verticalGradient(listOf(acc.copy(alpha = a), col.copy(alpha = a))))
                drawRect(Color.White.copy(alpha = 0.14f * a), size = Size(w, h / 2))
                drawContent()
            }

            // Modern iOS: flat grey press, quick fade.
            GlowKind.IOS -> {
                drawRect(col.copy(alpha = 0.9f * a))
                drawContent()
            }

            // X-plore: blue bloom plus a yellow edge marker.
            GlowKind.XPLORE -> {
                drawContent()
                bloom(c, col, a, s, 0.4f)
                drawRect(acc.copy(alpha = a), size = Size(3.dp.toPx(), h))
                val line = 1.dp.toPx()
                drawRect(col.copy(alpha = 0.6f * a), Offset(0f, h - line), Size(w, line))
            }

            // Snaptu: warm bloom with neon rings rippling out from the finger.
            GlowKind.SNAPTU -> {
                drawContent()
                bloom(c, acc, a, s, 0.3f)
                val maxR = size.maxDimension
                val ring = maxR * 0.12f + maxR * 0.75f * s
                val fade = (1f - s).coerceAtLeast(0f) * a
                clipRect {
                    drawCircle(col.copy(alpha = fade), ring, c, style = Stroke(3.dp.toPx()))
                    drawCircle(acc.copy(alpha = 0.6f * fade), ring * 0.65f, c, style = Stroke(1.5.dp.toPx()))
                }
            }
        }
    }

    private fun DrawScope.bloom(c: Offset, color: Color, a: Float, s: Float, strength: Float) {
        val radius = size.maxDimension * (0.25f + 0.85f * s)
        drawRect(
            Brush.radialGradient(
                0f to color.copy(alpha = strength * a),
                0.45f to color.copy(alpha = strength * 0.43f * a),
                1f to Color.Transparent,
                center = c,
                radius = radius.coerceAtLeast(1f),
            )
        )
    }
}
