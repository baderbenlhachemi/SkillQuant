package com.badereddine.skillquant.data.repository

import com.badereddine.skillquant.domain.model.Alert
import com.badereddine.skillquant.domain.repository.AlertRepository
import com.badereddine.skillquant.util.Constants
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private inline fun <reified T : Any> DocumentSnapshot.safeGet(field: String): T? =
    runCatching { get<T>(field) }.getOrNull()

class FirestoreAlertRepository(
    private val firestore: FirebaseFirestore
) : AlertRepository {

    override fun getAlerts(userId: String): Flow<List<Alert>> {
        return firestore
            .collection(Constants.COLLECTION_ALERTS)
            .where { "userId" equalTo userId }
            .limit(50)
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        Alert(
                            id = doc.id,
                            userId = doc.safeGet<String>("userId") ?: "",
                            skillId = doc.safeGet<String>("skillId") ?: "",
                            skillName = doc.safeGet<String>("skillName") ?: "",
                            type = doc.safeGet<String>("type") ?: "info",
                            title = doc.safeGet<String>("title") ?: "",
                            message = doc.safeGet<String>("message") ?: "",
                            read = doc.safeGet<Boolean>("read") ?: false,
                            createdAt = doc.safeGet<Long>("createdAt") ?: 0L
                        )
                    }.getOrNull()
                }.sortedByDescending { it.createdAt } // sort client-side, no composite index needed
            }
    }

    override fun getUnreadCount(userId: String): Flow<Int> {
        // Reuse getAlerts and filter client-side — avoids composite index requirement
        return getAlerts(userId).map { alerts -> alerts.count { !it.read } }
    }

    override suspend fun markAsRead(alertId: String) {
        firestore
            .collection(Constants.COLLECTION_ALERTS)
            .document(alertId)
            .update("read" to true)
    }

    override suspend fun markAllAsRead(userId: String) {
        val snapshot = firestore
            .collection(Constants.COLLECTION_ALERTS)
            .where { "userId" equalTo userId }
            .where { "read" equalTo false }
            .get()

        if (snapshot.documents.isEmpty()) return

        val batch = firestore.batch()
        snapshot.documents.forEach { doc ->
            batch.update(
                firestore.collection(Constants.COLLECTION_ALERTS).document(doc.id),
                "read" to true
            )
        }
        batch.commit()
    }
}


