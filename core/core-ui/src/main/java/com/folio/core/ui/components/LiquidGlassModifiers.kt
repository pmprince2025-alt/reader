package com.folio.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.folio.core.ui.theme.PlasmaCyan
import kotlinx.coroutines.launch

fun Modifier.dimplePress(
    scaleFactor: Float = 0.94f,
    animationSpec: SpringSpec<Float> = spring(dampingRatio = 0.7f, stiffness = 500f),
    onTap: (() -> Unit)? = null
): Modifier = composed {
    val scale = remember { Animatable(1f) }
    val currentOnTap = rememberUpdatedState(onTap)

    this
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown()
                launch { scale.animateTo(scaleFactor, animationSpec) }
                do {
                    val event = awaitPointerEvent()
                    val released = event.changes.all { !it.pressed }
                    if (released) {
                        currentOnTap.value?.invoke()
                        launch { scale.animateTo(1f, animationSpec) }
                        break
                    }
                } while (true)
            }
        }
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
}

fun Modifier.liquidFill(
    targetFraction: Float,
    color: Color = PlasmaCyan,
    animationSpec: SpringSpec<Float> = spring(dampingRatio = 0.6f, stiffness = 400f),
    glowHeight: Dp = 4.dp
): Modifier = composed {
    val fraction = remember { Animatable(0f) }

    LaunchedEffect(targetFraction) {
        fraction.animateTo(targetFraction.coerceIn(0f, 1f), animationSpec)
    }

    this.drawBehind {
        val f = fraction.value
        if (f > 0.001f) {
            val fillH = size.height * f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.6f),
                        color.copy(alpha = 0.9f)
                    ),
                    startY = size.height - fillH,
                    endY = size.height
                ),
                topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - fillH),
                size = androidx.compose.ui.geometry.Size(size.width, fillH)
            )
            drawRect(
                color = Color.White.copy(alpha = 0.45f),
                topLeft = androidx.compose.ui.geometry.Offset(
                    0f,
                    (size.height - fillH).coerceAtLeast(0f)
                ),
                size = androidx.compose.ui.geometry.Size(size.width, glowHeight.toPx())
            )
        }
    }
}

fun Modifier.dimplePressWithFill(
    scaleFactor: Float = 0.94f,
    fillColor: Color = PlasmaCyan,
    fillAnimSpec: SpringSpec<Float> = spring(dampingRatio = 0.6f, stiffness = 400f),
    scaleAnimSpec: SpringSpec<Float> = spring(dampingRatio = 0.7f, stiffness = 500f),
    onTap: (() -> Unit)? = null
): Modifier = composed {
    val scale = remember { Animatable(1f) }
    val fillFraction = remember { Animatable(0f) }
    val currentOnTap = rememberUpdatedState(onTap)

    this
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown()
                launch { scale.animateTo(scaleFactor, scaleAnimSpec) }
                launch { fillFraction.animateTo(1f, fillAnimSpec) }
                do {
                    val event = awaitPointerEvent()
                    val released = event.changes.all { !it.pressed }
                    if (released) {
                        currentOnTap.value?.invoke()
                        launch { scale.animateTo(1f, scaleAnimSpec) }
                        launch { fillFraction.animateTo(0f, fillAnimSpec) }
                        break
                    }
                } while (true)
            }
        }
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .drawBehind {
            val f = fillFraction.value
            if (f > 0.001f) {
                val fillH = size.height * f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            fillColor.copy(alpha = 0.6f),
                            fillColor.copy(alpha = 0.9f)
                        ),
                        startY = size.height - fillH,
                        endY = size.height
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - fillH),
                    size = androidx.compose.ui.geometry.Size(size.width, fillH)
                )
                drawRect(
                    color = Color.White.copy(alpha = 0.45f),
                    topLeft = androidx.compose.ui.geometry.Offset(
                        0f,
                        (size.height - fillH).coerceAtLeast(0f)
                    ),
                    size = androidx.compose.ui.geometry.Size(size.width, 4.dp.toPx())
                )
            }
        }
}
