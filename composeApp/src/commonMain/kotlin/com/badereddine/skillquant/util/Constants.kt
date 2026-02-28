package com.badereddine.skillquant.util

object Constants {
    // Firestore collection names
    const val COLLECTION_SKILLS = "skills"
    const val COLLECTION_SKILL_METRICS = "skillMetrics"
    const val COLLECTION_ARBITRAGE_OPPORTUNITIES = "arbitrageOpportunities"
    const val COLLECTION_TRENDING_SKILLS = "trendingSkills"
    const val COLLECTION_ALERTS = "alerts"
    const val COLLECTION_USER_PROFILES = "userProfiles"
    const val COLLECTION_APP_CONFIG = "appConfig"

    // Subcollection
    const val SUBCOLLECTION_HISTORY = "history"

    // Free tier limits
    const val FREE_WATCHLIST_LIMIT = 5
    const val FREE_HISTORY_DAYS = 90
    const val FREE_OPPORTUNITIES_LIMIT = 15
    const val FREE_LEARNING_RESOURCES_LIMIT = 3

    // Pro tier
    const val PRO_HISTORY_DAYS = 90
    const val PRO_OPPORTUNITIES_LIMIT = 20

    // Tiers
    const val TIER_FREE = "free"
    const val TIER_PRO = "pro"
}

