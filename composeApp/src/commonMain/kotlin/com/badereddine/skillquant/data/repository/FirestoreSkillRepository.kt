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

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.getLearningResources(): List<LearningResource> {
    // Try direct deserialization first (works on some platforms)
    runCatching { get<List<LearningResource>>("learningResources") }.getOrNull()
        ?.takeIf { it.isNotEmpty() }
        ?.let { return it }
    // Fallback: manually map List<Map<String, Any>> (Firestore native representation)
    return runCatching {
        val raw = safeGet<List<Map<String, Any>>>("learningResources") ?: return emptyList()
        raw.map { m ->
            LearningResource(
                title    = (m["title"]    as? String) ?: "",
                url      = (m["url"]      as? String) ?: "",
                type     = (m["type"]     as? String) ?: "",
                platform = (m["platform"] as? String) ?: ""
            )
        }
    }.getOrElse { emptyList() }
}

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.getJobListings(): List<JobListing> {
    // Try direct deserialization first
    runCatching { get<List<JobListing>>("jobListings") }.getOrNull()
        ?.takeIf { it.isNotEmpty() }
        ?.let { return it }
    // Fallback: manually map List<Map<String, Any>>
    return runCatching {
        val raw = safeGet<List<Map<String, Any>>>("jobListings") ?: return emptyList()
        raw.map { m ->
            val storedUrl = (m["url"] as? String) ?: ""
            val title     = (m["title"]    as? String) ?: ""
            val company   = (m["company"]  as? String) ?: ""
            val location  = (m["location"] as? String) ?: ""
            val source    = (m["source"]   as? String) ?: ""
            // If the stored URL is blank, generate a live LinkedIn/Indeed search URL
            val resolvedUrl = when {
                storedUrl.isNotBlank() -> storedUrl
                source.contains("Indeed", ignoreCase = true) ->
                    "https://www.indeed.com/jobs?q=${title.encodeUrl()}&l=${location.encodeUrl()}"
                else ->
                    "https://www.linkedin.com/jobs/search/?keywords=${title.encodeUrl()}&location=${location.encodeUrl()}"
            }
            JobListing(
                title         = title,
                company       = company,
                location      = location,
                salaryRange   = (m["salaryRange"]   as? String) ?: "",
                type          = (m["type"]          as? String) ?: "Full-time",
                url           = resolvedUrl,
                source        = source,
                postedDaysAgo = (m["postedDaysAgo"] as? Number)?.toInt() ?: 0
            )
        }
    }.getOrElse { emptyList() }
}

private fun String.encodeUrl(): String =
    this.replace(" ", "%20").replace(",", "%2C")

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
        // Doc ID is always "{skillId}_{location.lowercase()}" — use a direct fetch
        // instead of a compound query (which requires a composite index and can
        // return the wrong document if the index hasn't been created yet).
        val docId = "${skillId}_${location.lowercase()}"
        return firestore
            .collection(Constants.COLLECTION_SKILL_METRICS)
            .document(docId)
            .snapshots
            .map { doc ->
                if (!doc.exists) return@map null
                runCatching {
                    SkillMetrics(
                        skillId = doc.getString("skillId").ifEmpty { skillId },
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
                        location = doc.getString("location").ifEmpty { location },
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
            // Use Morocco doc as canonical name source — same name across all countries
            firestore
                .collection(Constants.COLLECTION_SKILL_METRICS)
                .document("${skillId}_morocco")
                .snapshots
                .first()
                .getString("skillName")
                .ifEmpty { skillId }
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
        // Fetch each doc directly by its known ID — no collection scan, no index needed
        val flows = skillIds.map { skillId ->
            getSkillMetrics(skillId, location)
        }
        return kotlinx.coroutines.flow.combine(flows) { array ->
            array.filterNotNull()
        }
    }
}
