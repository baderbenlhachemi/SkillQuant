package com.badereddine.skillquant.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class RadarEntry(
    val skillName: String,
    val values: List<Double>, // values 0-100 for each axis
    val color: Color
)

@Composable
fun RadarChart(
    entries: List<RadarEntry>,
    axisLabels: List<String>,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty() || axisLabels.isEmpty()) return
    val numAxes = axisLabels.size
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)

    Canvas(modifier = modifier.fillMaxWidth().height(320.dp).padding(8.dp)) {
        val cx = size.width / 2
        val cy = size.height / 2
        // Leave space for labels outside the pentagon
        val radius = minOf(cx, cy) * 0.60f

        // Draw grid pentagons + scale labels
        val gridLevels = listOf(0.25f, 0.5f, 0.75f, 1f)
        val scaleStyle = TextStyle(fontSize = 9.sp, color = Color.Gray.copy(alpha = 0.6f))
        for (level in gridLevels) {
            val gridPath = Path()
            for (i in 0 until numAxes) {
                val angle = -PI / 2 + 2 * PI * i / numAxes
                val x = cx + (radius * level * cos(angle)).toFloat()
                val y = cy + (radius * level * sin(angle)).toFloat()
                if (i == 0) gridPath.moveTo(x, y) else gridPath.lineTo(x, y)
            }
            gridPath.close()
            drawPath(gridPath, Color.Gray.copy(alpha = 0.2f), style = Stroke(width = 1f))

            // Draw scale label along the top axis (index 0, straight up)
            val scaleLabel = "${(level * 100).toInt()}"
            val scaleMeasured = textMeasurer.measure(AnnotatedString(scaleLabel), style = scaleStyle)
            val sy = cy - radius * level - scaleMeasured.size.height / 2f
            drawText(scaleMeasured, topLeft = Offset(cx + 4f, sy))
        }

        // Draw axis lines
        for (i in 0 until numAxes) {
            val angle = -PI / 2 + 2 * PI * i / numAxes
            val x = cx + (radius * cos(angle)).toFloat()
            val y = cy + (radius * sin(angle)).toFloat()
            drawLine(Color.Gray.copy(alpha = 0.3f), Offset(cx, cy), Offset(x, y), strokeWidth = 1f)
        }

        // Draw data polygons
        for (entry in entries) {
            val dataPath = Path()
            for (i in 0 until numAxes) {
                val value = (entry.values.getOrElse(i) { 0.0 } / 100.0).coerceIn(0.0, 1.0)
                val angle = -PI / 2 + 2 * PI * i / numAxes
                val x = cx + (radius * value * cos(angle)).toFloat()
                val y = cy + (radius * value * sin(angle)).toFloat()
                if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
            }
            dataPath.close()
            drawPath(dataPath, entry.color.copy(alpha = 0.15f))
            drawPath(dataPath, entry.color, style = Stroke(width = 2.5f))

            // Draw dots
            for (i in 0 until numAxes) {
                val value = (entry.values.getOrElse(i) { 0.0 } / 100.0).coerceIn(0.0, 1.0)
                val angle = -PI / 2 + 2 * PI * i / numAxes
                val x = cx + (radius * value * cos(angle)).toFloat()
                val y = cy + (radius * value * sin(angle)).toFloat()
                drawCircle(entry.color, radius = 4f, center = Offset(x, y))
            }
        }

        // Draw axis labels at pentagon vertices
        val labelOffset = radius * 1.18f // place labels slightly beyond the edge
        for (i in 0 until numAxes) {
            val angle = -PI / 2 + 2 * PI * i / numAxes
            val lx = cx + (labelOffset * cos(angle)).toFloat()
            val ly = cy + (labelOffset * sin(angle)).toFloat()

            val measured = textMeasurer.measure(
                AnnotatedString(axisLabels[i]),
                style = labelStyle.copy(color = labelColor)
            )
            val tw = measured.size.width.toFloat()
            val th = measured.size.height.toFloat()

            // Center the label on the vertex point
            val dx = lx - tw / 2
            val dy = ly - th / 2

            drawText(measured, topLeft = Offset(dx, dy))
        }
    }
}


