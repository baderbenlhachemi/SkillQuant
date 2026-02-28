package com.badereddine.skillquant.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ArbitrageOpportunity(
    val id: String = "",
    val skillId: String = "",
    val skillName: String = "",
    val arbitrageScore: Double = 0.0,
    val demandScore: Double = 0.0,
    val supplyScore: Double = 0.0,
    val avgSalary: Long = 0L,
    val changePercent: Double = 0.0,
    val direction: String = "up",   // "up" or "down"
    val summary: String = "",
    val location: String = "Global",
    val updatedAt: Long = 0L
)

