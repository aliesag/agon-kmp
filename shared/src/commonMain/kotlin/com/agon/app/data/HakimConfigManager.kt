package com.agon.app.data

import com.agon.app.Http
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.time.TimeSource

data class AiConfig(
    val apiKey: String = "sk-27439376a9cf4d91bc6dc3e977ad3577",
    val baseUrl: String = "https://api.deepseek.com",
    val model: String = "deepseek-chat",
    val type: String = "openai",
    val url: String = "https://api.deepseek.com"
)

/**
 * KMP port of HakimConfigManager.
 * OkHttp -> Ktor, org.json -> kotlinx.serialization, System.currentTimeMillis -> TimeSource.Monotonic.
 * Behavior unchanged: fetch remote AI config from Firebase RTDB, 1-min cache, GPT-OCR primary,
 * DeepSeek/OpenAI-compatible secondary.
 */
object HakimConfigManager {
    private const val FIREBASE_AI_CONFIG_URL = "https://chat-516a8-default-rtdb.firebaseio.com/ai_config.json"
    private const val GPT_OCR_URL = "https://dark.ps/v2/gpt-ocr/completions"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private var cachedConfig: AiConfig = AiConfig()
    private var lastFetchMark: TimeSource.Monotonic.ValueTimeMark? = null
    private const val CACHE_DURATION_MS = 60_000L

    private fun cacheValid(): Boolean {
        val mark = lastFetchMark ?: return false
        return mark.elapsedNow().inWholeMilliseconds < CACHE_DURATION_MS &&
            cachedConfig.apiKey.isNotBlank()
    }

    suspend fun getAiConfig(forceRefresh: Boolean = false): AiConfig {
        if (!forceRefresh && cacheValid()) return cachedConfig
        return try {
            val body = Http.get(FIREBASE_AI_CONFIG_URL)
            if (body.isBlank() || body == "null") return cachedConfig
            val obj = json.parseToJsonElement(body) as? JsonObject ?: return cachedConfig
            fun s(vararg keys: String): String? {
                for (k in keys) {
                    val v = obj[k]?.jsonPrimitive?.contentOrNull
                    if (!v.isNullOrBlank()) return v
                }
                return null
            }
            val apiKey = s("api_key", "key") ?: "sk-27439376a9cf4d91bc6dc3e977ad3577"
            val baseUrl = s("baseUrl", "url") ?: "https://api.deepseek.com"
            val modelRaw = s("model", "model_name") ?: "deepseek-chat"
            val type = s("type") ?: "openai"
            val url = s("url") ?: baseUrl
            cachedConfig = AiConfig(
                apiKey = apiKey,
                baseUrl = baseUrl,
                model = if (modelRaw.contains("v4-flash")) "deepseek-chat" else modelRaw,
                type = type,
                url = url
            )
            lastFetchMark = TimeSource.Monotonic.markNow()
            cachedConfig
        } catch (e: Exception) {
            cachedConfig
        }
    }

    private fun parseGptOcrStream(fullBody: String): String {
        val sb = StringBuilder()
        var idx = 0
        while (idx < fullBody.length) {
            val dataIdx = fullBody.indexOf("data:", idx)
            if (dataIdx == -1) break
            val startBrace = fullBody.indexOf('{', dataIdx)
            if (startBrace == -1) break

            var depth = 0
            var endBrace = -1
            var inString = false
            var isEscaped = false
            for (i in startBrace until fullBody.length) {
                val c = fullBody[i]
                if (isEscaped) { isEscaped = false; continue }
                if (c == '\\') { isEscaped = true; continue }
                if (c == '"') { inString = !inString; continue }
                if (!inString) {
                    if (c == '{') depth++
                    else if (c == '}') { depth--; if (depth == 0) { endBrace = i; break } }
                }
            }
            if (endBrace != -1) {
                val chunk = fullBody.substring(startBrace, endBrace + 1)
                try {
                    val o = json.parseToJsonElement(chunk) as? JsonObject
                    val done = o?.get("done")?.jsonPrimitive?.contentOrNull == "true"
                    if (!done) {
                        val text = o?.get("text")?.jsonPrimitive?.contentOrNull ?: ""
                        sb.append(text)
                    }
                } catch (_: Exception) { /* skip malformed chunk */ }
                idx = endBrace + 1
            } else break
        }
        return sb.toString().trim()
    }

    suspend fun askHakimGptOcr(
        userMessage: String,
        fileUrls: List<String> = emptyList(),
        systemPrompt: String? = null
    ): String? {
        val defaultSystem = "أنت حكيم ✨، المستشار والناقد الفني والذكي لتطبيق مسلسلات وأفلام 'حكايتنا'. وظيفتك التفاعل بأسلوب راقٍ، مرح، وناصح للمستخدمين في اقتراح المسلسلات والأفلام مع إدراج رموز تعبيرية (Emojis). عندما تقترح مسلسلاً أو يسألك المستخدم عن عمل، اذكر اسمه بوضوح مثل: شباب البومب، حبك نار، العشق والعرش، شارع الأعشى، خريف القلب، خمسين خمسين، قيامة عثمان."
        val effectivePrompt = systemPrompt ?: defaultSystem
        return try {
            val body = buildJsonObject {
                put("prompt", userMessage)
                put("system_prompt", effectivePrompt)
                put("file_urls", buildJsonArray { fileUrls.take(10).forEach { add(JsonPrimitive(it)) } })
            }.toString()

            val resp = Http.client.post {
                url(GPT_OCR_URL)
                contentType(ContentType.Application.Json)
                header("Content-Type", "application/json")
                setBody(body)
            }.bodyAsText()

            val parsed = parseGptOcrStream(resp)
            parsed.ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun askHakim(
        userMessage: String,
        fileUrls: List<String> = emptyList(),
        systemPrompt: String? = null
    ): String? {
        val gpt = askHakimGptOcr(userMessage, fileUrls, systemPrompt)
        if (!gpt.isNullOrBlank()) return gpt
        return askHakimSecondary(userMessage, systemPrompt)
    }

    suspend fun askHakimSecondary(userMessage: String, systemPrompt: String? = null): String? {
        val config = getAiConfig()
        val effectivePrompt = systemPrompt
            ?: "أنت حكيم ✨، المستشار والناقد السينمائي والذكي في تطبيق حكايتنا. أجب بأسلوب ذكي وممتع ومساعد باللغة العربية مع Emojis واقتراحات مسلسلات مميزة."
        return try {
            val endpoint = if (config.baseUrl.endsWith("/chat/completions")) config.baseUrl
            else "${config.baseUrl.trimEnd('/')}/chat/completions"

            val body = buildJsonObject {
                put("model", if (config.model.isBlank()) "deepseek-chat" else config.model)
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "system")
                        put("content", effectivePrompt)
                    })
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", userMessage)
                    })
                })
                put("temperature", 0.7)
            }.toString()

            val resp = Http.client.post {
                url(endpoint)
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer ${config.apiKey}")
                header("Content-Type", "application/json")
                setBody(body)
            }.bodyAsText()

            val obj = json.parseToJsonElement(resp) as? JsonObject ?: return null
            val choices = obj["choices"] as? JsonArray ?: return null
            val first = choices.firstOrNull() as? JsonObject ?: return null
            val message = first["message"] as? JsonObject ?: return null
            message["content"]?.jsonPrimitive?.contentOrNull?.trim()
        } catch (e: Exception) {
            null
        }
    }
}
