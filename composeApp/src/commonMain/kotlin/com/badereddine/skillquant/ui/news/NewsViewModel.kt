package com.badereddine.skillquant.ui.news

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.badereddine.skillquant.domain.repository.SkillRepository
import com.badereddine.skillquant.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class NewsItem(
    val title: String,
    val url: String,
    val author: String = "",
    val points: Int = 0,
    val createdAt: String = "",
    val matchedSkill: String = "",
    val source: String = "Hacker News"
)

data class NewsUiState(
    val news: List<NewsItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

// Skills that are substrings of other skill names — need exact matching
private val AMBIGUOUS_SKILLS = setOf("Java", "Go", "C", "R", "C#", "C++")

// Build a search-safe query: for ambiguous skills, use quotes to get exact match
private fun buildSearchQuery(skill: String): String {
    // For "Java", search "Java" -JavaScript -JavaFX to avoid false matches
    if (skill.equals("Java", ignoreCase = true)) {
        return "%22Java%22+-JavaScript+-JavaFX"
    }
    if (skill.equals("Go", ignoreCase = true)) {
        return "%22Go+language%22+OR+%22Golang%22"
    }
    if (skill.equals("C", ignoreCase = true)) {
        return "%22C+programming%22+OR+%22C+language%22"
    }
    if (skill.equals("R", ignoreCase = true)) {
        return "%22R+programming%22+OR+%22R+language%22+OR+%22RStudio%22"
    }
    return skill.replace(" ", "%20")
}

// Check if a title actually matches the intended skill (post-filter)
private fun titleMatchesSkill(title: String, skill: String): Boolean {
    val lower = title.lowercase()
    val skillLower = skill.lowercase()
    return when {
        skill.equals("Java", ignoreCase = true) ->
            lower.contains("java") && !lower.contains("javascript")
        skill.equals("Go", ignoreCase = true) ->
            lower.contains("golang") || lower.contains("go language") ||
            lower.contains("go 1.") || Regex("\\bgo\\b").containsMatchIn(lower)
        skill.equals("C", ignoreCase = true) ->
            lower.contains("c programming") || lower.contains("c language") ||
            Regex("\\bc\\b").containsMatchIn(lower) && !lower.contains("c#") && !lower.contains("c++")
        skill.equals("R", ignoreCase = true) ->
            lower.contains("r programming") || lower.contains("rstudio") || lower.contains("r language")
        else -> lower.contains(skillLower)
    }
}

class NewsViewModel(
    private val userRepository: UserRepository,
    private val skillRepository: SkillRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init {
        loadNews()
    }

    private fun loadNews() {
        screenModelScope.launch {
            try {
                val userId = userRepository.getCurrentUserId()
                var skillNames = listOf("React", "Python", "Kotlin", "TypeScript", "Kubernetes")

                if (userId != null) {
                    val profile = userRepository.getUserProfile(userId).firstOrNull()
                    if (!profile?.watchlist.isNullOrEmpty()) {
                        val resolved = profile!!.watchlist.take(5).map { skillId ->
                            try { skillRepository.getSkillName(skillId) } catch (_: Exception) { skillId }
                        }
                        skillNames = resolved
                    }
                }

                val allNews = mutableListOf<NewsItem>()
                // Only fetch articles from the last 30 days
                val lastMonthSeconds = (System.currentTimeMillis() / 1000) - (30L * 24 * 60 * 60)

                for (skill in skillNames) {
                    // === Source 1: Hacker News (Algolia API) ===
                    try {
                        val query = buildSearchQuery(skill)
                        val hnUrl = "https://hn.algolia.com/api/v1/search?query=${query}&tags=story&hitsPerPage=8&numericFilters=created_at_i%3E${lastMonthSeconds}"
                        val response = withContext(Dispatchers.IO) { fetchUrl(hnUrl) }
                        if (response != null) {
                            val jsonObj = json.parseToJsonElement(response).jsonObject
                            val hits = jsonObj["hits"]?.jsonArray ?: emptyList()
                            for (hit in hits) {
                                val obj = hit.jsonObject
                                val title = obj["title"]?.jsonPrimitive?.content ?: ""
                                // Post-filter: reject false matches
                                if (title.isBlank() || !titleMatchesSkill(title, skill)) continue
                                val articleUrl = obj["url"]?.jsonPrimitive?.content
                                val hnLink = "https://news.ycombinator.com/item?id=${obj["objectID"]?.jsonPrimitive?.content ?: ""}"
                                allNews.add(
                                    NewsItem(
                                        title = title,
                                        url = if (!articleUrl.isNullOrBlank()) articleUrl else hnLink,
                                        author = obj["author"]?.jsonPrimitive?.content ?: "",
                                        points = obj["points"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                                        createdAt = obj["created_at"]?.jsonPrimitive?.content ?: "",
                                        matchedSkill = skill,
                                        source = "Hacker News"
                                    )
                                )
                            }
                        }
                    } catch (_: Exception) {}

                    // === Source 2: Dev.to API ===
                    try {
                        val devQuery = skill.replace(" ", "%20")
                        val devUrl = "https://dev.to/api/articles?tag=${devQuery.lowercase()}&top=7&per_page=5"
                        val devResponse = withContext(Dispatchers.IO) { fetchUrl(devUrl) }
                        if (devResponse != null) {
                            val articles = json.parseToJsonElement(devResponse).jsonArray
                            for (article in articles) {
                                val obj = article.jsonObject
                                val title = obj["title"]?.jsonPrimitive?.content ?: ""
                                if (title.isBlank()) continue
                                allNews.add(
                                    NewsItem(
                                        title = title,
                                        url = obj["url"]?.jsonPrimitive?.content ?: "",
                                        author = obj["user"]?.jsonObject?.get("name")?.jsonPrimitive?.content ?: "",
                                        points = obj["positive_reactions_count"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                                        createdAt = obj["published_at"]?.jsonPrimitive?.content ?: "",
                                        matchedSkill = skill,
                                        source = "Dev.to"
                                    )
                                )
                            }
                        }
                    } catch (_: Exception) {}
                }

                val deduplicated = allNews
                    .filter { it.title.isNotBlank() }
                    .distinctBy { it.title }
                    .sortedByDescending { it.points }
                _uiState.update { it.copy(news = deduplicated, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadNews()
    }
}

// Platform-agnostic URL fetch (works on Android + JVM)
internal expect fun fetchUrl(url: String): String?

