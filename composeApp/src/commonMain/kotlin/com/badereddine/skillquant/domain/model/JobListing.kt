package com.badereddine.skillquant.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class JobListing(
    val title: String = "",
    val company: String = "",
    val location: String = "",
    val salaryRange: String = "",
    val type: String = "Full-time",      // "Full-time", "Contract", "Freelance"
    val url: String = "",
    val source: String = "",             // "LinkedIn", "Indeed"
    val postedDaysAgo: Int = 0
)

