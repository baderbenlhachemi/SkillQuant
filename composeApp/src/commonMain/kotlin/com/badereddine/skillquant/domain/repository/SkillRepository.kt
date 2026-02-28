package com.badereddine.skillquant.domain.repository

import com.badereddine.skillquant.domain.model.ArbitrageOpportunity
import com.badereddine.skillquant.domain.model.Skill
import com.badereddine.skillquant.domain.model.SkillMetrics
import com.badereddine.skillquant.domain.model.TrendingSkill
import kotlinx.coroutines.flow.Flow

interface SkillRepository {
    fun getTopArbitrageOpportunities(limit: Int, location: String): Flow<List<ArbitrageOpportunity>>
    fun getTrendingSkills(limit: Int, location: String): Flow<List<TrendingSkill>>
    fun getSkillMetrics(skillId: String, location: String): Flow<SkillMetrics?>
    fun searchSkills(query: String, location: String): Flow<List<Skill>>
    fun getAllSkillsForLocation(location: String): Flow<List<Skill>>
    fun getAllSkills(): Flow<List<Skill>>
    fun getSkillMetricsList(skillIds: List<String>, location: String): Flow<List<SkillMetrics>>
    suspend fun getSkillName(skillId: String): String
}
