package com.badereddine.skillquant.domain.repository

import com.badereddine.skillquant.domain.model.Alert
import kotlinx.coroutines.flow.Flow

interface AlertRepository {
    fun getAlerts(userId: String): Flow<List<Alert>>
    fun getUnreadCount(userId: String): Flow<Int>
    suspend fun markAsRead(alertId: String)
    suspend fun markAllAsRead(userId: String)
}

