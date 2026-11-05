package com.agon.app

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

// Replaces okhttp3 usage in EpisodeScraper / MatchesRepository / TmdbHelper.
// Engine is picked per platform automatically (OkHttp on Android, Darwin on iOS).
object Http {
    val client = HttpClient()
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String =
        client.get(url) { headers.forEach { (k, v) -> header(k, v) } }.bodyAsText()
}
