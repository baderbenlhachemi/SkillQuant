package com.badereddine.skillquant.ui.news

import java.net.HttpURLConnection
import java.net.URL

internal actual fun fetchUrl(url: String): String? {
    return try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.requestMethod = "GET"
        conn.inputStream.bufferedReader().use { it.readText() }
    } catch (_: Exception) {
        null
    }
}

