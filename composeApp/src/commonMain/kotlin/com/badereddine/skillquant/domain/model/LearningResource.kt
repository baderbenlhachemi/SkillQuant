package com.badereddine.skillquant.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LearningResource(
    val title: String = "",
    val url: String = "",
    val type: String = "",       // "course", "book", "tutorial", "certification"
    val platform: String = ""    // "Udemy", "Coursera", "YouTube", etc.
)

