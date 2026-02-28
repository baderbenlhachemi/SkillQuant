package com.badereddine.skillquant.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Skill(
    val id: String = "",
    val name: String = "",
    val category: String = "",   // "Backend", "Frontend", "AI/ML", "DevOps", "Mobile", "Data", "Cloud"
    val tags: List<String> = emptyList(),
    val location: String = "Global"
)

