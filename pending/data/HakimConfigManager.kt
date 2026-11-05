package com.agon.app.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiConfig(
    val apiKey: String = "sk-27439376a9cf4d91bc6dc3e977ad3577",
    val baseUrl: String = "https://api.deepseek.com",
    val model: String = "deepseek-chat",
    val type: String = "openai",
    val url: String = "https://api.deepseek.com"
)

object HakimConfigManager {
    private const val TAG = "HakimConfigManager"
    private const val FIREBASE_AI_CONFIG_URL = "https://chat-516a8-default-rtdb.firebaseio.com/ai_config.json"
    private const val GPT_OCR_URL = "https://dark.ps/v2/gpt-ocr/completions"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gptOcrClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var cachedConfig: AiConfig = AiConfig()
    private var lastFetchTime: Long = 0L
    private const val CACHE_DURATION_MS = 60_000L // 1 minute

    suspend fun getAiConfig(forceRefresh: Boolean = false): AiConfig {
        val now = System.currentTimeMillis()
        if (!forceRefresh && now - lastFetchTime < CACHE_DURATION_MS && cachedConfig.apiKey.isNotBlank()) {
            return cachedConfig
        }

        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(FIREBASE_AI_CONFIG_URL)
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank() && body != "null") {
                            val json = JSONObject(body)
                            val apiKey = json.optString("api_key", json.optString("key", "sk-27439376a9cf4d91bc6dc3e977ad3577"))
                            val baseUrl = json.optString("baseUrl", json.optString("url", "https://api.deepseek.com"))
                            val model = json.optString("model", json.optString("model_name", "deepseek-chat"))
                            val type = json.optString("type", "openai")
                            val url = json.optString("url", baseUrl)

                            cachedConfig = AiConfig(
                                apiKey = apiKey,
                                baseUrl = baseUrl,
                                model = if (model.contains("v4-flash")) "deepseek-chat" else model,
                                type = type,
                                url = url
                            )
                            lastFetchTime = now
                            Log.d(TAG, "Dynamic AI config loaded successfully: model=${cachedConfig.model}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching AI config from Firebase, using fallback", e)
            }
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
                if (isEscaped) {
                    isEscaped = false
                    continue
                }
                if (c == '\\') {
                    isEscaped = true
                    continue
                }
                if (c == '"') {
                    inString = !inString
                    continue
                }
                if (!inString) {
                    if (c == '{') depth++
                    else if (c == '}') {
                        depth--
                        if (depth == 0) {
                            endBrace = i
                            break
                        }
                    }
                }
            }

            if (endBrace != -1) {
                val jsonStr = fullBody.substring(startBrace, endBrace + 1)
                try {
                    val obj = JSONObject(jsonStr)
                    if (!obj.optBoolean("done", false)) {
                        val text = obj.optString("text", "")
                        sb.append(text)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing chunk: $jsonStr", e)
                }
                idx = endBrace + 1
            } else {
                break
            }
        }
        return sb.toString().trim()
    }

    /**
     * Primary AI Engine: ChatGPT OCR from dark.ps with media/PDF vision support.
     */
    suspend fun askHakimGptOcr(
        userMessage: String,
        fileUrls: List<String> = emptyList(),
        systemPrompt: String? = null
    ): String? {
        return withContext(Dispatchers.IO) {
            val defaultSystem = "أنت حكيم ✨، المستشار والناقد الفني والذكي لتطبيق مسلسلات وأفلام 'حكايتنا'. وظيفتك التفاعل بأسلوب راقٍ، مرح، وناصح للمستخدمين في اقتراح المسلسلات والأفلام مع إدراج رموز تعبيرية (Emojis). عندما تقترح مسلسلاً أو يسألك المستخدم عن عمل، اذكر اسمه بوضوح مثل: شباب البومب، حبك نار، العشق والعرش، شارع الأعشى، خريف القلب، خمسين خمسين، قيامة عثمان."
            val effectivePrompt = systemPrompt ?: defaultSystem

            try {
                val requestJson = JSONObject().apply {
                    put("prompt", userMessage)
                    put("system_prompt", effectivePrompt)
                    val filesArray = JSONArray()
                    fileUrls.take(10).forEach { filesArray.put(it) }
                    put("file_urls", filesArray)
                }

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = requestJson.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(GPT_OCR_URL)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                gptOcrClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: ""
                        val parsed = parseGptOcrStream(bodyString)
                        if (parsed.isNotBlank()) {
                            return@withContext parsed
                        }
                    } else {
                        Log.e(TAG, "GPT-OCR API error: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calling GPT-OCR API", e)
            }
            null
        }
    }

    /**
     * Main askHakim entry point:
     * Tries dark.ps GPT-OCR first (supports images/PDF urls),
     * and gracefully falls back to DeepSeek if needed.
     */
    suspend fun askHakim(
        userMessage: String,
        fileUrls: List<String> = emptyList(),
        systemPrompt: String? = null
    ): String? {
        // 1. Primary: ChatGPT OCR completions
        val gptResult = askHakimGptOcr(userMessage, fileUrls, systemPrompt)
        if (!gptResult.isNullOrBlank()) {
            return gptResult
        }

        // 2. Fallback secondary engine
        return askHakimSecondary(userMessage, systemPrompt)
    }

    suspend fun askHakimSecondary(userMessage: String, systemPrompt: String? = null): String? {
        return withContext(Dispatchers.IO) {
            val config = getAiConfig()
            val effectivePrompt = systemPrompt ?: "أنت حكيم ✨، المستشار والناقد السينمائي والذكي في تطبيق حكايتنا. أجب بأسلوب ذكي وممتع ومساعد باللغة العربية مع Emojis واقتراحات مسلسلات مميزة."

            try {
                val endpoint = if (config.baseUrl.endsWith("/chat/completions")) {
                    config.baseUrl
                } else {
                    "${config.baseUrl.trimEnd('/')}/chat/completions"
                }

                val requestBodyJson = JSONObject().apply {
                    put("model", if (config.model.isBlank()) "deepseek-chat" else config.model)
                    val messagesArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", effectivePrompt)
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", userMessage)
                        })
                    }
                    put("messages", messagesArray)
                    put("temperature", 0.7)
                }

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

                val apiRequest = Request.Builder()
                    .url(endpoint)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer ${config.apiKey}")
                    .addHeader("Content-Type", "application/json")
                    .build()

                httpClient.newCall(apiRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val respBody = response.body?.string()
                        if (!respBody.isNullOrBlank()) {
                            val jsonResponse = JSONObject(respBody)
                            val choices = jsonResponse.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val choice = choices.getJSONObject(0)
                                val messageObj = choice.optJSONObject("message")
                                return@withContext messageObj?.optString("content", "")?.trim()
                            }
                        }
                    } else {
                        Log.e(TAG, "Secondary API call failed with code: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in askHakimSecondary", e)
            }
            null
        }
    }
}
