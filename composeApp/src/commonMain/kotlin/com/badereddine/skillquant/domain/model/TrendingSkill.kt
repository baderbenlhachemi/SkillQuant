package com.badereddine.skillquant.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TrendingSkill(
    val id: String = "",
    val skillId: String = "",
    val skillName: String = "",
    val trendDirection: String = "up",  // "up" or "down"
    val changePercent: Double = 0.0,
    val period: String = "7d",          // "7d", "30d"
    val location: String = "Global",
    val updatedAt: Long = 0L
)

