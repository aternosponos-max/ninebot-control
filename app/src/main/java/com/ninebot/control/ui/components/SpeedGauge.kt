package com.ninebot.control.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ninebot.control.ui.theme.BrandBlue
import com.ninebot.control.ui.theme.BrandGreen
import com.ninebot.control.ui.theme.BrandOrange

/** Круговой спидометр с цветовыми зонами эко/норма/спорт. */
@Composable
fun SpeedGauge(speedKmh: Double, maxKmh: Double = 35.0, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            val strokeWidth = size.minDimension * 0.08f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2)
            val sweepTotal = 270f
            val startAngle = 135f

            drawArc(
                color = Color.DarkGray.copy(alpha = 0.3f),
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )

            val progress = (speedKmh / maxKmh).coerceIn(0.0, 1.0).toFloat()
            val color = when {
                speedKmh <= 6.0 -> BrandGreen
                speedKmh <= 20.0 -> BrandBlue
                else -> BrandOrange
            }
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepTotal * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
        }
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format("%.1f", speedKmh),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            Text(text = "км/ч", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
