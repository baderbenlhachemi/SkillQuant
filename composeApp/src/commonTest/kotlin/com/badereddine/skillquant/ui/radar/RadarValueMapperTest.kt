package com.badereddine.skillquant.ui.radar

import com.badereddine.skillquant.domain.model.SkillMetrics
import kotlin.test.Test
import kotlin.test.assertEquals

class RadarValueMapperTest {

    @Test
    fun normalizeScore_convertsRatioToPercent() {
        assertEquals(40.0, normalizeScore(0.4))
    }

    @Test
    fun toSupplyGapScore_invertsNormalizedSupplyScore() {
        assertEquals(60.0, toSupplyGapScore(40.0))
        assertEquals(60.0, toSupplyGapScore(0.4))
    }

    @Test
    fun toRadarNormalizedMetrics_mapsAllAxesTo0to100() {
        val metrics = SkillMetrics(
            demandScore = 0.75,
            supplyScore = 0.2,
            arbitrageScore = 88.0,
            avgSalary = 100_000,
            jobPostCount = 50
        )

        val normalized = metrics.toRadarNormalizedMetrics(maxSalary = 200_000, maxJobs = 100)

        assertEquals(75.0, normalized.demand)
        assertEquals(80.0, normalized.supplyGap)
        assertEquals(50.0, normalized.salary)
        assertEquals(88.0, normalized.arbitrage)
        assertEquals(50.0, normalized.jobs)
    }
}

