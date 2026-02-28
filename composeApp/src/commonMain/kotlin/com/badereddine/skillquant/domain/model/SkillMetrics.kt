package com.badereddine.skillquant.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SkillMetrics(
    val skillId: String = "",
    val skillName: String = "",
    val category: String = "",
    val demandScore: Double = 0.0,          // 0-100
    val supplyScore: Double = 0.0,          // 0-100
    val arbitrageScore: Double = 0.0,       // 0-100 (high = underserved + high demand)
    val avgSalary: Long = 0L,
    val medianSalary: Long = 0L,
    val freelanceHourlyRate: Double = 0.0,
    val jobPostCount: Int = 0,
    val freelanceGigCount: Int = 0,
    val demandTrend: List<TrendPoint> = emptyList(),
    val salaryTrend: List<TrendPoint> = emptyList(),
    val topEmployers: List<String> = emptyList(),
    val learningResources: List<LearningResource> = emptyList(),
    val jobListings: List<JobListing> = emptyList(),
    val location: String = "",
    val updatedAt: Long = 0L
)

