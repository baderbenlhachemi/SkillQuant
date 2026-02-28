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

    // Google Sign-In: Web client ID from Firebase Console → Auth → Google provider
    // Replace this after enabling Google Sign-In in Firebase Console
    const val GOOGLE_WEB_CLIENT_ID = "547895354199-na63sl58l5q8j2victbno11e6naabroh.apps.googleusercontent.com"
}

