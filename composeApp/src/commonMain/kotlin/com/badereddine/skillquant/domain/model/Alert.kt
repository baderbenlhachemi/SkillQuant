package com.badereddine.skillquant.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Alert(
    val id: String = "",
    val userId: String = "",
    val skillId: String = "",
    val skillName: String = "",
    val type: String = "",          // "spike", "new_opportunity", "price_change"
    val title: String = "",
    val message: String = "",
    val read: Boolean = false,
    val createdAt: Long = 0L
)

