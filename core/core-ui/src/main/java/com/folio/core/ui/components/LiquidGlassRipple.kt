package com.folio.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.folio.core.ui.theme.PlasmaCyan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class RippleState(scope: CoroutineScope) {
    private data class ActiveRipple(
        val x: Float,
        val y: Float,
        val maxRadius: Float,
        val progress: Animatable<Float, AnimationVector1D>
    )

    private val ripples = mutableStateListOf<ActiveRipple>()
    val scope = scope

    val activeRipples: List<*> get() = ripples.toList()

    fun spawn(x: Float, y: Float, maxRadius: Float = 90f) {
        val ripple = ActiveRipple(x, y, maxRadius, Animatable<Float, AnimationVector1D>(0f))
        ripples.add(ripple)
        scope.launch {
            ripple.progress.animateTo(1f, tween(600))
            ripples.remove(ripple)
        }
    }

    fun getProgress(index: Int): Float? {
        val ripple = ripples.getOrNull(index) ?: return null
        return ripple.progress.value
    }

    fun getPosition(index: Int): Offset? {
        val ripple = ripples.getOrNull(index) ?: return null
        return Offset(ripple.x, ripple.y)
    }

    fun getRadius(index: Int): Float? {
        val ripple = ripples.getOrNull(index) ?: return null
        return ripple.maxRadius
    }

    val size: Int get() = ripples.size
}

@Composable
fun rememberRippleState(): RippleState {
    val scope = rememberCoroutineScope()
    return remember { RippleState(scope) }
}

@Composable
fun LiquidGlassRipple(
    state: RippleState,
    modifier: Modifier = Modifier,
    color: Color = PlasmaCyan
) {
    Canvas(modifier = modifier) {
        for (i in 0 until state.size) {
            val pct = state.getProgress(i) ?: continue
            val pos = state.getPosition(i) ?: continue
            val maxR = state.getRadius(i) ?: continue
            val radius = maxR * pct
            val alpha = (1f - pct).coerceIn(0f, 1f)

            drawCircle(
                color = color.copy(alpha = alpha * 0.6f),
                radius = radius,
                center = pos,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = alpha * 0.15f),
                        Color.Transparent
                    ),
                    center = pos,
                    radius = radius
                ),
                radius = radius,
                center = pos
            )
        }
    }
}
