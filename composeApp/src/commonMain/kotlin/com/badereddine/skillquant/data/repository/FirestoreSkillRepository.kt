package com.badereddine.skillquant.data.repository

import com.badereddine.skillquant.domain.model.*
import com.badereddine.skillquant.domain.repository.SkillRepository
import com.badereddine.skillquant.util.Constants
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Safe field accessors - return null if field missing or wrong type
private inline fun <reified T : Any> DocumentSnapshot.safeGet(field: String): T? =
    runCatching { get<T>(field) }.getOrNull()

private fun DocumentSnapshot.getString(field: String): String =
    safeGet<String>(field) ?: ""

private fun DocumentSnapshot.getDouble(field: String): Double =
    safeGet<Double>(field) ?: (safeGet<Long>(field)?.toDouble()) ?: 0.0

private fun DocumentSnapshot.getLong(field: String): Long =
    safeGet<Long>(field) ?: (safeGet<Double>(field)?.toLong()) ?: 0L

private fun DocumentSnapshot.getStringList(field: String): List<String> =
    runCatching { get<List<String>>(field) }.getOrNull() ?: emptyList()

private fun DocumentSnapshot.getTrendPoints(field: String): List<TrendPoint> =
    runCatching { get<List<TrendPoint>>(field) }.getOrNull() ?: emptyList()

private fun DocumentSnapshot.getLearningResources(): List<LearningResource> =
    runCatching { get<List<LearningResource>>("learningResources") }.getOrNull() ?: emptyList()

private fun DocumentSnapshot.getJobListings(): List<JobListing> =
    runCatching { get<List<JobListing>>("jobListings") }.getOrNull() ?: emptyList()

class FirestoreSkillRepository(
    private val firestore: FirebaseFirestore
) : SkillRepository {

    override fun getTopArbitrageOpportunities(limit: Int, location: String): Flow<List<ArbitrageOpportunity>> {
        return firestore
            .collection(Constants.COLLECTION_ARBITRAGE_OPPORTUNITIES)
            .where { "location" equalTo location }
            .orderBy("arbitrageScore", Direction.DESCENDING)
            .limit(limit)
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        ArbitrageOpportunity(
                            id = doc.id,
                            skillId = doc.getString("skillId"),
                            skillName = doc.getString("skillName"),
                            arbitrageScore = doc.getDouble("arbitrageScore"),
                            demandScore = doc.getDouble("demandScore"),
                            supplyScore = doc.getDouble("supplyScore"),
                            avgSalary = doc.getLong("avgSalary"),
                            changePercent = doc.getDouble("changePercent"),
                            direction = doc.getString("direction"),
                            summary = doc.getString("summary"),
                            location = doc.getString("location"),
                            updatedAt = doc.getLong("updatedAt")
                        )
                    }.getOrNull()
                }.distinctBy { it.skillId }
            }
    }

    override fun getTrendingSkills(limit: Int, location: String): Flow<List<TrendingSkill>> {
        return firestore
            .collection(Constants.COLLECTION_TRENDING_SKILLS)
            .where { "location" equalTo location }
            .orderBy("changePercent", Direction.DESCENDING)
            .limit(limit)
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        TrendingSkill(
                            id = doc.id,
                            skillId = doc.getString("skillId"),
                            skillName = doc.getString("skillName"),
                            trendDirection = doc.safeGet<String>("trendDirection")
                                ?: doc.safeGet<String>("direction") ?: "up",
                            changePercent = doc.getDouble("changePercent"),
                            period = doc.getString("period").ifEmpty { "7d" },
                            location = doc.getString("location"),
                            updatedAt = doc.getLong("updatedAt")
                        )
                    }.getOrNull()
                }.distinctBy { it.skillId }
            }
    }

    override fun getSkillMetrics(skillId: String, location: String): Flow<SkillMetrics?> {
        return firestore
            .collection(Constants.COLLECTION_SKILL_METRICS)
            .where { "skillId" equalTo skillId }
            .where { "location" equalTo location }
            .limit(1)
            .snapshots
            .map { snapshot ->
                val doc = snapshot.documents.firstOrNull() ?: return@map null
                runCatching {
                    SkillMetrics(
                        skillId = doc.getString("skillId").ifEmpty { doc.id },
                        skillName = doc.getString("skillName"),
                        category = doc.getString("category"),
                        demandScore = doc.getDouble("demandScore"),
                        supplyScore = doc.getDouble("supplyScore"),
                        arbitrageScore = doc.getDouble("arbitrageScore"),
                        avgSalary = doc.getLong("avgSalary"),
                        medianSalary = doc.getLong("medianSalary"),
                        freelanceHourlyRate = doc.getDouble("freelanceHourlyRate"),
                        jobPostCount = doc.getLong("jobPostCount").toInt(),
                        freelanceGigCount = doc.getLong("freelanceGigCount").toInt(),
                        demandTrend = doc.getTrendPoints("demandTrend"),
                        salaryTrend = doc.getTrendPoints("salaryTrend"),
                        topEmployers = doc.getStringList("topEmployers"),
                        learningResources = doc.getLearningResources(),
                        jobListings = doc.getJobListings(),
                        location = doc.getString("location"),
                        updatedAt = doc.getLong("updatedAt")
                    )
                }.getOrNull()
            }
    }

    override fun searchSkills(query: String, location: String): Flow<List<Skill>> {
        val queryUpper = query.lowercase().replaceFirstChar { it.uppercase() }
        return firestore
            .collection(Constants.COLLECTION_SKILL_METRICS)
            .where { "location" equalTo location }
            .orderBy("skillName")
            .startAt(queryUpper)
            .endAt(queryUpper + "\uf8ff")
            .limit(20)
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        Skill(
                            id = doc.getString("skillId").ifEmpty { doc.id },
                            name = doc.getString("skillName"),
                            category = doc.getString("category"),
                            tags = doc.getStringList("tags"),
                            location = doc.getString("location")
                        )
                    }.getOrNull()
                }
            }
    }

    override suspend fun getSkillName(skillId: String): String {
        return runCatching {
            firestore
                .collection(Constants.COLLECTION_SKILL_METRICS)
                .where { "skillId" equalTo skillId }
                .limit(1)
                .snapshots
                .first()
                .documents
                .firstOrNull()
                ?.let { doc -> doc.getString("skillName") }
                ?: skillId
        }.getOrElse { skillId }
    }

    override fun getAllSkillsForLocation(location: String): Flow<List<Skill>> {
        return firestore
            .collection(Constants.COLLECTION_SKILL_METRICS)
            .where { "location" equalTo location }
            .orderBy("skillName")
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        Skill(
                            id = doc.getString("skillId").ifEmpty { doc.id },
                            name = doc.getString("skillName"),
                            category = doc.getString("category"),
                            location = doc.getString("location")
                        )
                    }.getOrNull()
                }.distinctBy { it.id }
            }
    }

    override fun getAllSkills(): Flow<List<Skill>> {
        return firestore
            .collection(Constants.COLLECTION_SKILLS)
            .orderBy("name")
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        Skill(
                            id = doc.id,
                            name = doc.getString("name"),
                            category = doc.getString("category"),
                            location = "Global"
                        )
                    }.getOrNull()
                }.distinctBy { it.id }
            }
    }

    override fun getSkillMetricsList(skillIds: List<String>, location: String): Flow<List<SkillMetrics>> {
        if (skillIds.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return firestore
            .collection(Constants.COLLECTION_SKILL_METRICS)
            .where { "location" equalTo location }
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val skillId = doc.getString("skillId").ifEmpty { doc.id }
                    if (skillId !in skillIds) return@mapNotNull null
                    runCatching {
                        SkillMetrics(
                            skillId = skillId,
                            skillName = doc.getString("skillName"),
                            category = doc.getString("category"),
                            demandScore = doc.getDouble("demandScore"),
                            supplyScore = doc.getDouble("supplyScore"),
                            arbitrageScore = doc.getDouble("arbitrageScore"),
                            avgSalary = doc.getLong("avgSalary"),
                            medianSalary = doc.getLong("medianSalary"),
                            freelanceHourlyRate = doc.getDouble("freelanceHourlyRate"),
                            jobPostCount = doc.getLong("jobPostCount").toInt(),
                            freelanceGigCount = doc.getLong("freelanceGigCount").toInt(),
                            demandTrend = doc.getTrendPoints("demandTrend"),
                            salaryTrend = doc.getTrendPoints("salaryTrend"),
                            topEmployers = doc.getStringList("topEmployers"),
                            learningResources = doc.getLearningResources(),
                            jobListings = doc.getJobListings(),
                            location = doc.getString("location"),
                            updatedAt = doc.getLong("updatedAt")
                        )
                    }.getOrNull()
                }
            }
    }
}
