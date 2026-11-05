package com.agon.app.data

import com.agon.app.Http
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)
@Serializable data class Content(val parts: List<Part>)
@Serializable data class Part(val text: String? = null)
@Serializable data class GenerationConfig(
    val responseFormat: ResponseFormat? = null,
    val temperature: Float? = null
)
@Serializable data class ResponseFormat(val type: String, val responseMimeType: String? = null)
@Serializable data class GenerateContentResponse(val candidates: List<Candidate>)
@Serializable data class Candidate(val content: Content)

/**
 * KMP port of GeminiApi (Retrofit + OkHttp -> Ktor).
 * Wired but secondary per §11.2 of the spec — the API key is intentionally
 * left blank; pull it from build secrets (.env → generated constants) when
 * this path is actually enabled.
 */
object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com"
    const val API_KEY = "" // FILL: from .env GEMINI_API_KEY

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun generateContent(request: GenerateContentRequest): GenerateContentResponse? {
        val url = "$BASE_URL/v1beta/models/gemini-3.5-flash:generateContent"
        return try {
            val body = json.encodeToString(request)
            val resp = Http.client.post {
                url(url)
                parameter("key", API_KEY)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.bodyAsText()
            json.decodeFromString<GenerateContentResponse>(resp)
        } catch (e: Exception) {
            null
        }
    }
}
