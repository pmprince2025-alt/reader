package com.folio.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun EmptyLibraryIllustration(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF6B4226).copy(alpha = 0.4f)
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val bookW = size.width * 0.55f
        val bookH = size.height * 0.7f
        val spineX = cx
        val topY = cy - bookH / 2f
        val bottomY = cy + bookH / 2f

        // Left page
        drawPages(
            spineX = spineX,
            topY = topY,
            bottomY = bottomY,
            bookW = bookW,
            isLeft = true,
            color = tint
        )

        // Right page
        drawPages(
            spineX = spineX,
            topY = topY,
            bottomY = bottomY,
            bookW = bookW,
            isLeft = false,
            color = tint
        )

        // Pages (offset white rectangles inside the cover)
        val pageInset = bookW * 0.06f
        val pageTopInset = bookH * 0.06f
        val pageBottomInset = bookH * 0.08f
        val pageColor = tint.copy(alpha = 0.15f)

        drawRect(
            color = pageColor,
            topLeft = Offset(spineX - bookW + pageInset, topY + pageTopInset),
            size = Size(bookW - pageInset * 2f, bookH - pageTopInset - pageBottomInset)
        )
        drawRect(
            color = pageColor,
            topLeft = Offset(spineX + pageInset, topY + pageTopInset),
            size = Size(bookW - pageInset * 2f, bookH - pageTopInset - pageBottomInset)
        )

        // Spine line
        drawLine(
            color = tint.copy(alpha = 0.6f),
            start = Offset(spineX, topY + bookH * 0.1f),
            end = Offset(spineX, bottomY - bookH * 0.1f),
            strokeWidth = 2f
        )

        // Text lines on left page
        val lineColor = tint.copy(alpha = 0.2f)
        val lineStartX = spineX - bookW + pageInset * 2f
        val lineWidth = (bookW - pageInset * 4f) * 0.85f
        for (i in 0 until 4) {
            val y = topY + bookH * 0.3f + i * bookH * 0.14f
            drawLine(
                color = lineColor,
                start = Offset(lineStartX, y),
                end = Offset(lineStartX + lineWidth, y),
                strokeWidth = 2.5f
            )
        }

        // Floating particles (small circles) for visual interest
        val particleColor = tint.copy(alpha = 0.15f)
        val angles = floatArrayOf(0.3f, 1.8f, 3.9f, 5.1f)
        val radii = floatArrayOf(bookW * 0.8f, bookW * 1.1f, bookW * 0.9f, bookW * 1.0f)
        val sizes = floatArrayOf(6f, 4f, 8f, 5f)
        for (i in angles.indices) {
            val px = cx + cos(angles[i]) * radii[i]
            val py = cy + sin(angles[i]) * radii[i] * 0.5f
            drawCircle(color = particleColor, radius = sizes[i], center = Offset(px, py))
        }
    }
}

private fun DrawScope.drawPages(
    spineX: Float,
    topY: Float,
    bottomY: Float,
    bookW: Float,
    isLeft: Boolean,
    color: Color
) {
    val sign = if (isLeft) -1f else 1f
    val coverPath = Path().apply {
        moveTo(spineX, topY)
        // Top edge with slight arch
        cubicTo(
            spineX + sign * bookW * 0.3f, topY - bookW * 0.08f,
            spineX + sign * bookW * 0.7f, topY - bookW * 0.02f,
            spineX + sign * bookW, topY + bookW * 0.04f
        )
        // Right edge (outer)
        lineTo(spineX + sign * bookW, bottomY - bookW * 0.04f)
        // Bottom edge
        cubicTo(
            spineX + sign * bookW * 0.7f, bottomY + bookW * 0.02f,
            spineX + sign * bookW * 0.3f, bottomY + bookW * 0.08f,
            spineX, bottomY
        )
        close()
    }
    drawPath(path = coverPath, color = color)
    drawPath(path = coverPath, color = color.copy(alpha = 0.3f), style = Stroke(width = 1.5f))
}
