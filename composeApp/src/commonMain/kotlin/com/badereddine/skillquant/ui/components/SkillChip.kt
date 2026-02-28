package com.badereddine.skillquant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.badereddine.skillquant.ui.theme.*

@Composable
fun SkillChip(
    text: String,
    category: String = "",
    modifier: Modifier = Modifier
) {
    val chipColor = categoryColor(category)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(chipColor.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = chipColor
        )
    }
}

private fun categoryColor(category: String): Color {
    return when (category.lowercase()) {
        "backend" -> TealPrimary
        "frontend" -> NeutralBlue
        "ai/ml", "ai", "ml" -> GoldAccent
        "devops", "cloud" -> PositiveGreen
        "mobile" -> Color(0xFFA78BFA)
        "data" -> Color(0xFFF472B6)
        else -> TealPrimary
    }
}

