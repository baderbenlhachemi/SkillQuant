package com.badereddine.skillquant.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TrendPoint(
    val timestamp: Long = 0L,
    val value: Double = 0.0
)

