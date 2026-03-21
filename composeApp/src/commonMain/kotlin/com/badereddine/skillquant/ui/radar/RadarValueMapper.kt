package com.badereddine.skillquant.ui.radar

import com.badereddine.skillquant.domain.model.SkillMetrics

internal data class RadarNormalizedMetrics(
    val demand: Double,
    val supplyGap: Double,
    val salary: Double,
    val arbitrage: Double,
    val jobs: Double
)

internal fun SkillMetrics.toRadarNormalizedMetrics(maxSalary: Long, maxJobs: Int): RadarNormalizedMetrics {
    val safeMaxSalary = maxSalary.coerceAtLeast(1L)
    val safeMaxJobs = maxJobs.coerceAtLeast(1)

    val demandScore = normalizeScore(demandScore)
    val supplyScore = normalizeScore(supplyScore)
    val arbitrageScore = normalizeScore(arbitrageScore)

    return RadarNormalizedMetrics(
        demand = demandScore,
        // Low market supply should visually expand outward as opportunity on the radar.
        supplyGap = toSupplyGapScore(supplyScore),
        salary = (avgSalary.toDouble() / safeMaxSalary.toDouble() * 100.0).coerceIn(0.0, 100.0),
        arbitrage = arbitrageScore,
        jobs = (jobPostCount.toDouble() / safeMaxJobs.toDouble() * 100.0).coerceIn(0.0, 100.0)
    )
}

internal fun toSupplyGapScore(supplyScore: Double): Double =
    (100.0 - normalizeScore(supplyScore)).coerceIn(0.0, 100.0)

internal fun normalizeScore(value: Double): Double {
    // Some datasets may store score ratios as 0..1; map them to 0..100 for chart consistency.
    return if (value in 0.0..1.0) value * 100.0 else value.coerceIn(0.0, 100.0)
}

