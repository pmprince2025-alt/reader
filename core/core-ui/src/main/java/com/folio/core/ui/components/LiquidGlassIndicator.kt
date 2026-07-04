package com.folio.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.folio.core.ui.theme.PlasmaCyan
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun LiquidGlassIndicator(
    targetRect: Rect?,
    modifier: Modifier = Modifier,
    color: Color = PlasmaCyan,
    shape: Shape = CircleShape,
    stretchDurationMs: Int = 150,
    settleSpec: SpringSpec<Float> = spring(dampingRatio = 0.65f, stiffness = 400f),
    zIndex: Float = 0f
) {
    val density = LocalDensity.current
    val indicatorLeft = remember { Animatable(0f) }
    val indicatorTop = remember { Animatable(0f) }
    val indicatorWidth = remember { Animatable(0f) }
    val indicatorHeight = remember { Animatable(0f) }
    var prevRect by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(targetRect) {
        val target = targetRect ?: return@LaunchedEffect
        val prev = prevRect
        prevRect = target

        if (prev != null) {
            val unionLeft = min(prev.left, target.left)
            val unionTop = min(prev.top, target.top)
            val unionRight = max(prev.right, target.right)
            val unionBottom = max(prev.bottom, target.bottom)
            val unionHeight = max(prev.height, target.height)

            indicatorLeft.animateTo(unionLeft, tween(stretchDurationMs))
            indicatorTop.animateTo(unionTop, tween(stretchDurationMs))
            indicatorWidth.animateTo(unionRight - unionLeft, tween(stretchDurationMs))
            indicatorHeight.animateTo(unionHeight, tween(stretchDurationMs))
        }

        indicatorLeft.animateTo(target.left, settleSpec)
        indicatorTop.animateTo(target.top, settleSpec)
        indicatorWidth.animateTo(target.width, settleSpec)
        indicatorHeight.animateTo(target.height, settleSpec)
    }

    val xPx = indicatorLeft.value
    val yPx = indicatorTop.value
    val wPx = indicatorWidth.value
    val hPx = indicatorHeight.value

    Box(
        modifier = modifier
            .zIndex(zIndex)
            .offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
            .size(
                width = with(density) { wPx.toDp() },
                height = with(density) { hPx.toDp() }
            )
            .drawBehind {
                val outline = shape.createOutline(size, layoutDirection, this)
                drawOutline(
                    outline = outline,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            color.copy(alpha = 0.32f),
                            color.copy(alpha = 0.08f)
                        )
                    )
                )
                drawOutline(
                    outline = outline,
                    color = color.copy(alpha = 0.55f),
                    style = Stroke(width = 1.dp.toPx())
                )
                drawOutline(
                    outline = outline,
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                        start = Offset(size.width * 0.2f, 0f),
                        end = Offset(size.width * 0.8f, 0f)
                    ),
                    alpha = 0.4f
                )
            }
    )
}

@Composable
fun SelectableChipRow(
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    indicatorColor: Color = PlasmaCyan,
    indicatorShape: Shape = CircleShape,
    chipSpacing: androidx.compose.ui.unit.Dp = 8.dp,
    content: @Composable (selectedIndex: Int, chipPositions: List<Rect?>) -> Unit
) {
    val chipRects = remember { mutableStateOf<List<Rect?>>(emptyList()) }

    Box(modifier = modifier) {
        if (chipRects.value.isNotEmpty()) {
            val target = chipRects.value.getOrNull(selectedIndex)
            if (target != null) {
                LiquidGlassIndicator(
                    targetRect = target,
                    color = indicatorColor,
                    shape = indicatorShape,
                    modifier = Modifier.matchParentSize()
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(chipSpacing),
            modifier = Modifier.matchParentSize()
        ) {
            content(selectedIndex, chipRects.value)
        }
    }
}

fun Modifier.reportChipPosition(
    index: Int,
    onPosition: (Int, Rect) -> Unit
): Modifier = this.onGloballyPositioned { coordinates ->
    val parentBounds = coordinates.parentCoordinates?.let { parent ->
        val childPos = coordinates.positionInParent()
        Rect(childPos.x, childPos.y, childPos.x + coordinates.size.width, childPos.y + coordinates.size.height)
    }
    if (parentBounds != null) {
        onPosition(index, parentBounds)
    }
}
