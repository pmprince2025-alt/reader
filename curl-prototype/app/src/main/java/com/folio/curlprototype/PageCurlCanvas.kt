package com.folio.curlprototype

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun PageCurlView() {
    var currentPage by remember { mutableIntStateOf(1) }
    val curlProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2C2C2C))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        scope.launch { curlProgress.snapTo(0f) }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newProgress = (curlProgress.value - dragAmount.x / size.width).coerceIn(0f, 1f)
                        scope.launch { curlProgress.snapTo(newProgress) }
                    },
                    onDragEnd = {
                        scope.launch {
                            if (curlProgress.value > 0.3f) {
                                currentPage++
                            }
                            curlProgress.animateTo(
                                0f,
                                spring(stiffness = 300f, dampingRatio = 0.7f)
                            )
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            curlProgress.animateTo(0f)
                        }
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val progress = curlProgress.value
        val pageWidth = w / 2f

        // Left page (current page)
        drawPage(
            textMeasurer = textMeasurer,
            pageNumber = currentPage,
            backgroundColor = Color(0xFFF5EFE6),
            textColor = Color(0xFF1A1614),
            area = Offset.Zero,
            pageWidth = pageWidth,
            height = h
        )

        // Right page (next page) clipped by curl fold
        val rightVisibleWidth = pageWidth * (1f - progress)
        if (rightVisibleWidth > 0f) {
            clipRect(
                left = pageWidth,
                top = 0f,
                right = pageWidth + rightVisibleWidth,
                bottom = h
            ) {
                drawPage(
                    textMeasurer = textMeasurer,
                    pageNumber = currentPage + 1,
                    backgroundColor = Color(0xFFFAF7F2),
                    textColor = Color(0xFF1A1614),
                    area = Offset(pageWidth, 0f),
                    pageWidth = pageWidth,
                    height = h
                )
            }
        }

        if (progress > 0.01f) {
            drawCurlFold(w, h, progress, pageWidth)
        }
    }
}

private fun DrawScope.drawPage(
    textMeasurer: TextMeasurer,
    pageNumber: Int,
    backgroundColor: Color,
    textColor: Color,
    area: Offset,
    pageWidth: Float,
    height: Float
) {
    drawRect(color = backgroundColor, topLeft = area, size = Size(pageWidth, height))
    drawRect(color = Color(0x08000000), topLeft = area, size = Size(pageWidth, height))

    val textResult = textMeasurer.measure(
        AnnotatedString("Page $pageNumber"),
        style = TextStyle(
            color = textColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    )
    drawText(
        textLayoutResult = textResult,
        topLeft = Offset(
            area.x + (pageWidth - textResult.size.width) / 2f,
            (height - textResult.size.height) / 2f
        )
    )

    val lineColor = textColor.copy(alpha = 0.2f)
    for (i in 0 until 5) {
        val y = height * 0.3f + i * height * 0.1f
        val lineWidth = pageWidth * (0.5f + i * 0.08f)
        drawLine(
            color = lineColor,
            start = Offset(area.x + (pageWidth - lineWidth) / 2f, y),
            end = Offset(area.x + (pageWidth + lineWidth) / 2f, y),
            strokeWidth = 2f
        )
    }
}

private fun DrawScope.drawCurlFold(w: Float, h: Float, progress: Float, pageWidth: Float) {
    val foldX = pageWidth + pageWidth * (1f - progress)
    if (foldX >= w || foldX <= pageWidth) return

    val curlWidth = (w - foldX) * 0.6f
    val curlHeight = curlWidth * 0.7f

    // Drop shadow along the fold line spanning the full height
    val shadowFeather = 20f * progress + 4f
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color(0x00000000),
                Color(0x44000000),
                Color(0x22000000),
                Color(0x00000000)
            ),
            startX = foldX - shadowFeather,
            endX = foldX + shadowFeather * 2f
        ),
        topLeft = Offset(foldX - shadowFeather, 0f),
        size = Size(shadowFeather * 3f, h)
    )

    // Curled corner shape (verso — back of page)
    val curlPath = Path().apply {
        moveTo(foldX, 0f)
        cubicTo(
            foldX + curlWidth * 0.3f, -curlHeight * 0.5f,
            foldX + curlWidth * 0.7f, -curlHeight * 0.9f,
            w, 0f
        )
        close()
    }
    drawPath(path = curlPath, color = Color(0xFFE0D0B8).copy(alpha = 0.85f * progress))

    // Shadow cast by curled corner onto the page beneath
    val dropShadowPath = Path().apply {
        moveTo(foldX, 0f)
        lineTo(foldX + curlWidth * 0.4f, curlHeight * 0.25f)
        lineTo(w + 10f, curlHeight * 0.55f)
        lineTo(w, 0f)
        close()
    }
    drawPath(path = dropShadowPath, color = Color(0x28000000))

    // Edge highlight
    val edgePath = Path().apply {
        moveTo(foldX, 0f)
        cubicTo(
            foldX + curlWidth * 0.3f, -curlHeight * 0.5f,
            foldX + curlWidth * 0.7f, -curlHeight * 0.9f,
            w, 0f
        )
    }
    drawPath(
        path = edgePath,
        color = Color(0x55FFFFFF).copy(alpha = progress),
        style = Stroke(width = 2f)
    )

    // Secondary shadow on the back side (top edge of curled portion)
    val topShadow = Path().apply {
        moveTo(foldX + curlWidth * 0.2f, -curlHeight * 0.3f)
        lineTo(w, 0f)
        lineTo(w, curlHeight * 0.1f)
        lineTo(foldX + curlWidth * 0.3f, -curlHeight * 0.2f)
        close()
    }
    drawPath(path = topShadow, color = Color(0x18000000))
}
