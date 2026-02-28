package com.badereddine.skillquant.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.badereddine.skillquant.domain.model.TrendPoint
import com.badereddine.skillquant.util.toFormattedDate

@Composable
fun TrendChart(
    data: List<TrendPoint>,
    lineColor: Color,
    modifier: Modifier = Modifier,
    fillAlpha: Float = 0.1f,
    showLabels: Boolean = true
) {
    if (data.size < 2) {
        Box(modifier = modifier) {
            Text(
                text = "Not enough data",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val sortedData = data.sortedBy { it.timestamp }
    val minValue = sortedData.minOf { it.value }
    val maxValue = sortedData.maxOf { it.value }
    val valueRange = (maxValue - minValue).let { if (it == 0.0) 1.0 else it }
    val gridColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val width = size.width
            val height = size.height
            val padding = 8f

            val chartWidth = width - padding * 2
            val chartHeight = height - padding * 2

            // Draw grid lines
            for (i in 0..4) {
                val y = padding + chartHeight * (1 - i / 4f)
                drawLine(
                    color = gridColor,
                    start = Offset(padding, y),
                    end = Offset(width - padding, y),
                    strokeWidth = 0.5f
                )
            }

            // Build points
            val points = sortedData.mapIndexed { index, point ->
                val x = padding + chartWidth * index / (sortedData.size - 1)
                val y = padding + chartHeight * (1 - ((point.value - minValue) / valueRange)).toFloat()
                Offset(x, y)
            }

            // Draw filled area
            val fillPath = Path().apply {
                moveTo(points.first().x, height - padding)
                lineTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
                lineTo(points.last().x, height - padding)
                close()
            }
            drawPath(fillPath, lineColor.copy(alpha = fillAlpha))

            // Draw line
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(linePath, lineColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))

            // Draw dots at endpoints
            drawCircle(lineColor, radius = 4f, center = points.first())
            drawCircle(lineColor, radius = 4f, center = points.last())
        }

        if (showLabels && sortedData.size >= 2) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = sortedData.first().timestamp.toFormattedDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Text(
                    text = sortedData.last().timestamp.toFormattedDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

