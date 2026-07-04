package com.folio.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.folio.core.ui.theme.PlasmaCyan
import com.folio.core.ui.theme.PlasmaViolet
import com.folio.core.ui.theme.TextSecondary

private val GlassGradientStart = 0.08f
private val GlassGradientEnd = 0.02f

fun Modifier.glassSurface(
    tint: Color = Color.White.copy(alpha = GlassGradientStart),
    borderAlpha: Float = 0.12f,
    shadowAlpha: Float = 0.35f,
    shape: Shape = RoundedCornerShape(16.dp)
): Modifier = this
    .graphicsLayer {
        shadowElevation = 8f
        ambientShadowColor = Color.Black.copy(alpha = shadowAlpha)
        spotShadowColor = Color.Black.copy(alpha = shadowAlpha)
    }
    .drawBehind {
        drawOutline(
            outline = shape.createOutline(size, layoutDirection, this),
            brush = Brush.linearGradient(
                colors = listOf(tint, tint.copy(alpha = GlassGradientEnd)),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
        )
        drawOutline(
            outline = shape.createOutline(size, layoutDirection, this),
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                start = Offset.Zero,
                end = Offset(0f, size.height)
            ),
            alpha = 0.5f
        )
    }
    .clip(shape)
    .border(
        BorderStroke(1.dp, Color.White.copy(alpha = borderAlpha)),
        shape
    )

@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = GlassGradientStart),
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.glassSurface(tint = tint, shape = shape),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun GlassPill(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = GlassGradientStart),
    textColor: Color = TextSecondary,
    borderRadius: Int = 999
) {
    val shape = RoundedCornerShape(borderRadius.dp)
    Box(
        modifier = modifier
            .glassSurface(tint = tint, shape = shape)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = GlassGradientStart),
    contentTint: Color = TextSecondary,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .glassSurface(tint = tint, shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = icon
    )
}

fun Modifier.glassCyan(): Modifier = this
    .graphicsLayer {
        shadowElevation = 12f
        ambientShadowColor = Color.Black.copy(alpha = 0.45f)
        spotShadowColor = Color.Black.copy(alpha = 0.45f)
    }
    .drawBehind {
        val shape = RoundedCornerShape(999.dp)
        drawOutline(
            outline = shape.createOutline(size, layoutDirection, this),
            brush = Brush.linearGradient(
                colors = listOf(
                    PlasmaCyan.copy(alpha = 0.32f),
                    PlasmaCyan.copy(alpha = 0.08f)
                )
            )
        )
        drawOutline(
            outline = shape.createOutline(size, layoutDirection, this),
            color = PlasmaCyan.copy(alpha = 0.55f),
            style = Stroke(width = 1.dp.toPx())
        )
        drawOutline(
            outline = shape.createOutline(size, layoutDirection, this),
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                start = Offset(size.width * 0.2f, 0f),
                end = Offset(size.width * 0.8f, 0f)
            ),
            alpha = 0.4f
        )
    }
    .clip(RoundedCornerShape(999.dp))

fun Modifier.glassViolet(): Modifier = this
    .graphicsLayer {
        shadowElevation = 12f
        ambientShadowColor = Color.Black.copy(alpha = 0.45f)
        spotShadowColor = Color.Black.copy(alpha = 0.45f)
    }
    .drawBehind {
        val shape = RoundedCornerShape(999.dp)
        drawOutline(
            outline = shape.createOutline(size, layoutDirection, this),
            brush = Brush.linearGradient(
                colors = listOf(
                    PlasmaViolet.copy(alpha = 0.34f),
                    PlasmaViolet.copy(alpha = 0.08f)
                )
            )
        )
        drawOutline(
            outline = shape.createOutline(size, layoutDirection, this),
            color = PlasmaViolet.copy(alpha = 0.55f),
            style = Stroke(width = 1.dp.toPx())
        )
        drawOutline(
            outline = shape.createOutline(size, layoutDirection, this),
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                start = Offset(size.width * 0.2f, 0f),
                end = Offset(size.width * 0.8f, 0f)
            ),
            alpha = 0.4f
        )
    }
    .clip(RoundedCornerShape(999.dp))

fun Modifier.glassCube(): Modifier = this.drawBehind {
    val shape = RoundedCornerShape(14.dp)
    drawOutline(
        outline = shape.createOutline(size, layoutDirection, this),
        brush = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
            start = Offset.Zero,
            end = Offset(size.width * 0.6f, size.height * 0.4f)
        )
    )
}
