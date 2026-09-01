package com.t4kash.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4BrandDark
import com.t4kash.app.ui.theme.T4Mint
import com.t4kash.app.ui.theme.T4Primary

@Composable
fun ChatBackground(
    theme: ChatBackgroundTheme,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawRect(T4Background)
        when (theme) {
            ChatBackgroundTheme.T4KASH -> drawT4KashPattern()
            ChatBackgroundTheme.GRID -> drawGridPattern()
            ChatBackgroundTheme.WAVES -> drawWavePattern()
            ChatBackgroundTheme.CLEAN -> Unit
        }
    }
}

private fun DrawScope.drawT4KashPattern() {
    val step = 108.dp.toPx()
    val shapeSize = 40.dp.toPx()
    var row = 0
    var y = 24.dp.toPx()
    while (y < size.height) {
        var column = 0
        var x = 18.dp.toPx()
        while (x < size.width) {
            val shiftedX = x + if (row % 2 == 0) 0f else step / 2f
            when ((row + column) % 3) {
                0 -> drawCircle(
                    color = T4Mint.copy(alpha = 0.08f),
                    radius = shapeSize / 2f,
                    center = Offset(shiftedX, y)
                )
                1 -> drawRoundRect(
                    color = T4Primary.copy(alpha = 0.05f),
                    topLeft = Offset(shiftedX - shapeSize / 2f, y - shapeSize / 2f),
                    size = Size(shapeSize, shapeSize),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
                else -> drawLine(
                    color = T4BrandDark.copy(alpha = 0.045f),
                    start = Offset(shiftedX - shapeSize / 2f, y + shapeSize / 2f),
                    end = Offset(shiftedX + shapeSize / 2f, y - shapeSize / 2f),
                    strokeWidth = 5.dp.toPx()
                )
            }
            column++
            x += step
        }
        row++
        y += step
    }
}

private fun DrawScope.drawGridPattern() {
    val step = 34.dp.toPx()
    var x = 0f
    while (x <= size.width) {
        drawLine(
            color = T4Primary.copy(alpha = 0.045f),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1.dp.toPx()
        )
        x += step
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(
            color = T4Mint.copy(alpha = 0.07f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx()
        )
        y += step
    }
}

private fun DrawScope.drawWavePattern() {
    val radius = 86.dp.toPx()
    val spacing = 92.dp.toPx()
    var row = 0
    var y = 12.dp.toPx()
    while (y < size.height + radius) {
        val offset = if (row % 2 == 0) 0f else radius
        var x = -radius + offset
        while (x < size.width + radius) {
            drawCircle(
                color = if (row % 2 == 0) {
                    T4Primary.copy(alpha = 0.05f)
                } else {
                    T4Mint.copy(alpha = 0.08f)
                },
                radius = radius,
                center = Offset(x, y),
                style = Stroke(width = 4.dp.toPx())
            )
            x += radius * 2f
        }
        row++
        y += spacing
    }
}
