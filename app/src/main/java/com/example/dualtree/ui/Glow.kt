package com.example.dualtree.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Touch-to-glow indication: a soft radial light blooms from the finger,
 * spreads across the item and fades out after release.
 * Installed app-wide through LocalIndication in [DualTreeTheme].
 */
class GlowIndication(private val color: Color) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        GlowNode(interactionSource, color)

    override fun equals(other: Any?) = other is GlowIndication && other.color == color
    override fun hashCode() = color.hashCode()
}

private class GlowNode(
    private val source: InteractionSource,
    private val color: Color,
) : Modifier.Node(), DrawModifierNode {

    private val intensity = Animatable(0f)
    private val spread = Animatable(0f)
    private var center: Offset? = null
    private var pressJob: Job? = null

    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        center = interaction.pressPosition
                        pressJob = launch { intensity.animateTo(1f, tween(90)) }
                        launch {
                            spread.snapTo(0f)
                            spread.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
                        }
                    }
                    is PressInteraction.Release, is PressInteraction.Cancel -> {
                        val fadeIn = pressJob
                        launch {
                            fadeIn?.join() // let quick taps still flash fully
                            intensity.animateTo(0f, tween(650, easing = LinearOutSlowInEasing))
                        }
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val a = intensity.value
        val c = center ?: return
        if (a <= 0f) return

        val radius = size.maxDimension * (0.25f + 0.85f * spread.value)
        // Bright core that blooms outward from the touch point.
        drawRect(
            Brush.radialGradient(
                0f to color.copy(alpha = 0.42f * a),
                0.45f to color.copy(alpha = 0.18f * a),
                1f to Color.Transparent,
                center = c,
                radius = radius.coerceAtLeast(1f),
            )
        )
        // Faint wash over the whole item so the glow reads on any background.
        drawRect(color.copy(alpha = 0.07f * a))
    }
}
