package com.badereddine.skillquant.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.badereddine.skillquant.ui.theme.GoldAccent
import com.badereddine.skillquant.ui.theme.NegativeRed
import com.badereddine.skillquant.ui.theme.PositiveGreen

@Composable
fun ScoreGauge(
    score: Double,
    size: Dp = 120.dp,
    strokeWidth: Dp = 10.dp,
    modifier: Modifier = Modifier
) {
    val scoreColor = when {
        score >= 75 -> GoldAccent
        score >= 50 -> PositiveGreen
        score >= 25 -> Color(0xFFFBBF24) // yellow
        else -> NegativeRed
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val sweepAngle = 270f
            val startAngle = 135f
            val strokePx = strokeWidth.toPx()

            // Background arc (track)
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Foreground arc (score)
            val scoreSweep = sweepAngle * (score.toFloat() / 100f).coerceIn(0f, 1f)
            drawArc(
                color = scoreColor,
                startAngle = startAngle,
                sweepAngle = scoreSweep,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        // Center text
        Text(
            text = "${score.toInt()}",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = scoreColor,
            fontSize = (size.value * 0.3).sp
        )
    }
}

