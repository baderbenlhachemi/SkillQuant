package com.badereddine.skillquant.ui.news

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.badereddine.skillquant.domain.repository.SkillRepository
import com.badereddine.skillquant.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
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
    val source: String = "Hacker News",
    val isWatchlisted: Boolean = false   // true when the skill is in the user's watchlist
)

data class NewsUiState(
    val watchlistNews: List<NewsItem> = emptyList(),  // personalised — watchlist skills
    val generalNews: List<NewsItem> = emptyList(),    // fallback / default skills
    val isLoading: Boolean = true,
    val error: String? = null
) {
    // flat list used by the LazyColumn
    val allNews: List<NewsItem> get() = watchlistNews + generalNews
}

// Skills that are substrings of other skill names — need exact matching
private val AMBIGUOUS_SKILLS = setOf("Java", "Go", "C", "R", "C#", "C++")

private fun buildSearchQuery(skill: String): String {
    if (skill.equals("Java", ignoreCase = true))
        return "%22Java%22+-JavaScript+-JavaFX"
    if (skill.equals("Go", ignoreCase = true))
        return "%22Go+language%22+OR+%22Golang%22"
    if (skill.equals("C", ignoreCase = true))
        return "%22C+programming%22+OR+%22C+language%22"
    if (skill.equals("R", ignoreCase = true))
        return "%22R+programming%22+OR+%22R+language%22+OR+%22RStudio%22"
    return skill.replace(" ", "%20")
}

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

// Default skills shown when the user has no watchlist
private val DEFAULT_SKILLS = listOf("React", "Python", "Kotlin", "TypeScript", "Kubernetes")

// Reddit subreddits to search for each skill category
private fun redditSubreddits(skill: String): List<String> {
    val lower = skill.lowercase()
    return when {
        lower in listOf("react", "vue", "angular", "typescript", "javascript", "nextjs", "nuxtjs") ->
            listOf("reactjs", "webdev", "javascript")
        lower in listOf("python", "django", "flask", "fastapi") ->
            listOf("Python", "learnpython")
        lower in listOf("kotlin", "android", "jetpack compose") ->
            listOf("androiddev", "Kotlin")
        lower in listOf("rust") -> listOf("rust")
        lower in listOf("go", "golang") -> listOf("golang")
        lower in listOf("machine learning", "ml", "tensorflow", "pytorch", "ai") ->
            listOf("MachineLearning", "artificial")
        lower in listOf("docker", "kubernetes", "devops", "terraform", "aws", "gcp", "azure") ->
            listOf("devops", "kubernetes")
        lower in listOf("java", "spring", "springboot") ->
            listOf("java", "learnjava")
        else -> listOf("programming", "softwareengineering")
    }
}

class NewsViewModel(
    private val userRepository: UserRepository,
    private val skillRepository: SkillRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    private val supervisedScope = screenModelScope + SupervisorJob()
    private val json = Json { ignoreUnknownKeys = true }

    init { loadNews() }

    private fun loadNews() {
        supervisedScope.launch {
            try {
                // Resolve watchlist skill names
                val userId = userRepository.getCurrentUserId()
                var watchlistSkills: List<String> = emptyList()
                if (userId != null) {
                    val profile = userRepository.getUserProfile(userId)
                        .catch { }
                        .firstOrNull()
                    if (!profile?.watchlist.isNullOrEmpty()) {
                        watchlistSkills = profile.watchlist.take(8).mapNotNull { skillId ->
                            runCatching { skillRepository.getSkillName(skillId) }.getOrNull()
                        }
                    }
                }

                val skillsToFetch = watchlistSkills.ifEmpty { DEFAULT_SKILLS }
                val lastMonthSeconds = (System.currentTimeMillis() / 1000) - (30L * 24 * 60 * 60)

                val watchlistSet = watchlistSkills.map { it.lowercase() }.toSet()

                val allNews = mutableListOf<NewsItem>()

                for (skill in skillsToFetch) {
                    val isWatched = skill.lowercase() in watchlistSet
                    // Fetch more articles for watched skills
                    val hnHits = if (isWatched) 10 else 5
                    val devHits = if (isWatched) 6 else 3

                    // ── Source 1: Hacker News ────────────────────────────────
                    try {
                        val query = buildSearchQuery(skill)
                        val hnUrl = "https://hn.algolia.com/api/v1/search?query=${query}" +
                            "&tags=story&hitsPerPage=${hnHits}" +
                            "&numericFilters=created_at_i%3E${lastMonthSeconds}"
                        val response = withContext(Dispatchers.IO) { fetchUrl(hnUrl) }
                        if (response != null) {
                            val hits = json.parseToJsonElement(response).jsonObject["hits"]?.jsonArray ?: emptyList()
                            for (hit in hits) {
                                val obj = hit.jsonObject
                                val title = obj["title"]?.jsonPrimitive?.content ?: continue
                                if (!titleMatchesSkill(title, skill)) continue
                                val articleUrl = obj["url"]?.jsonPrimitive?.content
                                val hnLink = "https://news.ycombinator.com/item?id=${obj["objectID"]?.jsonPrimitive?.content ?: ""}"
                                allNews += NewsItem(
                                    title = title,
                                    url = if (!articleUrl.isNullOrBlank()) articleUrl else hnLink,
                                    author = obj["author"]?.jsonPrimitive?.content ?: "",
                                    points = obj["points"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                                    createdAt = obj["created_at"]?.jsonPrimitive?.content ?: "",
                                    matchedSkill = skill,
                                    source = "Hacker News",
                                    isWatchlisted = isWatched
                                )
                            }
                        }
                    } catch (_: Exception) {}

                    // ── Source 2: Dev.to ─────────────────────────────────────
                    try {
                        val tag = skill.replace(" ", "").lowercase()
                        val devUrl = "https://dev.to/api/articles?tag=${tag}&top=7&per_page=${devHits}"
                        val devResponse = withContext(Dispatchers.IO) { fetchUrl(devUrl) }
                        if (devResponse != null) {
                            val articles = json.parseToJsonElement(devResponse).jsonArray
                            for (article in articles) {
                                val obj = article.jsonObject
                                val title = obj["title"]?.jsonPrimitive?.content ?: continue
                                allNews += NewsItem(
                                    title = title,
                                    url = obj["url"]?.jsonPrimitive?.content ?: "",
                                    author = obj["user"]?.jsonObject?.get("name")?.jsonPrimitive?.content ?: "",
                                    points = obj["positive_reactions_count"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                                    createdAt = obj["published_at"]?.jsonPrimitive?.content ?: "",
                                    matchedSkill = skill,
                                    source = "Dev.to",
                                    isWatchlisted = isWatched
                                )
                            }
                        }
                    } catch (_: Exception) {}

                    // ── Source 3: Reddit (JSON API, no auth needed) ──────────
                    try {
                        val subreddits = redditSubreddits(skill)
                        val subreddit = subreddits.first()
                        val redditUrl = "https://www.reddit.com/r/${subreddit}/search.json" +
                            "?q=${buildSearchQuery(skill)}&restrict_sr=1&sort=top&t=month&limit=${devHits}"
                        val redditResponse = withContext(Dispatchers.IO) { fetchUrl(redditUrl) }
                        if (redditResponse != null) {
                            val data = json.parseToJsonElement(redditResponse)
                                .jsonObject["data"]?.jsonObject
                                ?.get("children")?.jsonArray ?: emptyList()
                            for (child in data) {
                                val post = child.jsonObject["data"]?.jsonObject ?: continue
                                val title = post["title"]?.jsonPrimitive?.content ?: continue
                                if (!titleMatchesSkill(title, skill)) continue
                                val permalink = post["permalink"]?.jsonPrimitive?.content ?: ""
                                val externalUrl = post["url"]?.jsonPrimitive?.content ?: ""
                                // Prefer external link; fall back to reddit post
                                val url = if (!externalUrl.startsWith("https://www.reddit.com")) externalUrl
                                          else "https://www.reddit.com$permalink"
                                val score = post["score"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                                if (score < 10) continue // skip low-signal posts
                                allNews += NewsItem(
                                    title = title,
                                    url = url,
                                    author = post["author"]?.jsonPrimitive?.content ?: "",
                                    points = score,
                                    createdAt = post["created_utc"]?.jsonPrimitive?.content?.toLongOrNull()
                                        ?.let { epochToIso(it) } ?: "",
                                    matchedSkill = skill,
                                    source = "Reddit r/$subreddit",
                                    isWatchlisted = isWatched
                                )
                            }
                        }
                    } catch (_: Exception) {}

                    // ── Source 4: Lobsters (JSON API) ────────────────────────
                    try {
                        val lobTag = skill.replace(" ", "-").lowercase()
                        val lobUrl = "https://lobste.rs/t/${lobTag}.json"
                        val lobResponse = withContext(Dispatchers.IO) { fetchUrl(lobUrl) }
                        if (lobResponse != null) {
                            val stories = json.parseToJsonElement(lobResponse).jsonArray
                            for (story in stories.take(if (isWatched) 5 else 2)) {
                                val obj = story.jsonObject
                                val title = obj["title"]?.jsonPrimitive?.content ?: continue
                                if (!titleMatchesSkill(title, skill)) continue
                                allNews += NewsItem(
                                    title = title,
                                    url = obj["url"]?.jsonPrimitive?.content
                                        ?: obj["short_id_url"]?.jsonPrimitive?.content ?: "",
                                    author = obj["submitter_user"]?.jsonObject
                                        ?.get("username")?.jsonPrimitive?.content ?: "",
                                    points = obj["score"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                                    createdAt = obj["created_at"]?.jsonPrimitive?.content ?: "",
                                    matchedSkill = skill,
                                    source = "Lobsters",
                                    isWatchlisted = isWatched
                                )
                            }
                        }
                    } catch (_: Exception) {}

                    // ── Source 5: GitHub Trending (unofficial scrape-free RSS) ─
                    // Uses gtrend.leven.re which provides a public JSON mirror of
                    // GitHub Trending — no auth, no scraping, CORS-friendly.
                    try {
                        val ghLang = skill.replace(" ", "-").lowercase()
                        val ghUrl = "https://gtrend.leven.re/repositories?language=${ghLang}&since=weekly&limit=5"
                        val ghResponse = withContext(Dispatchers.IO) { fetchUrl(ghUrl) }
                        if (ghResponse != null) {
                            val repos = json.parseToJsonElement(ghResponse).jsonArray
                            for (repo in repos.take(if (isWatched) 5 else 2)) {
                                val obj = repo.jsonObject
                                val name = obj["name"]?.jsonPrimitive?.content ?: continue
                                val repoUrl = obj["url"]?.jsonPrimitive?.content ?: continue
                                val desc = obj["description"]?.jsonPrimitive?.content ?: ""
                                val stars = obj["stargazers"]?.jsonPrimitive?.content?.toIntOrNull()
                                    ?: obj["stars"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                                val title = if (desc.isNotBlank()) "⭐ $name — $desc" else "⭐ $name"
                                allNews += NewsItem(
                                    title = title.take(120),
                                    url = repoUrl,
                                    author = obj["author"]?.jsonPrimitive?.content ?: "",
                                    points = stars,
                                    createdAt = "",
                                    matchedSkill = skill,
                                    source = "GitHub Trending",
                                    isWatchlisted = isWatched
                                )
                            }
                        }
                    } catch (_: Exception) {}
                }

                val deduplicated = allNews
                    .filter { it.title.isNotBlank() && it.url.isNotBlank() }
                    .distinctBy { it.title.lowercase().take(60) }

                // Split into personalised (watchlist) and general sections
                // Within each section sort by points desc
                val watchlistNews = deduplicated
                    .filter { it.isWatchlisted }
                    .sortedByDescending { it.points }
                val generalNews = deduplicated
                    .filter { !it.isWatchlisted }
                    .sortedByDescending { it.points }

                _uiState.update {
                    it.copy(
                        watchlistNews = watchlistNews,
                        generalNews = generalNews,
                        isLoading = false
                    )
                }
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

// Convert Unix epoch (Long) to approximate ISO-8601 string
private fun epochToIso(epoch: Long): String {
    // Simple approximation — good enough for relative display
    val ms = epoch * 1000L
    val secs = ms / 1000
    val days = secs / 86400
    val year = 1970 + (days / 365).toInt()
    val dayOfYear = (days % 365).toInt()
    val month = (dayOfYear / 30).coerceIn(0, 11) + 1
    val day = (dayOfYear % 30) + 1
    return "${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}T00:00:00Z"
}

// Platform-agnostic URL fetch (works on Android + JVM)
internal expect fun fetchUrl(url: String): String?

