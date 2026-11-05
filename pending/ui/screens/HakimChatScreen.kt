package com.agon.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.CheckCircle
import com.agon.app.data.Show
import com.agon.app.data.Catalog
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agon.app.viewmodel.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

import com.agon.app.ui.components.CustomMediaPicker
import android.media.MediaRecorder
import android.media.MediaPlayer
import java.io.File
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.border
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode

data class HakimChatSession(
    val id: String,
    val title: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val messageCount: Int = 0
)

fun isGeneralGreetingOrConversation(msg: String): Boolean {
    return Catalog.isGeneralGreetingOrConversation(msg)
}

fun isFollowUpAboutContextShow(msg: String): Boolean {
    val clean = Catalog.normalizeTitle(msg).lowercase()
    val followUpKeywords = listOf(
        "تقييم", "تقييمه", "تقييمك", "تقييم عمل", "كم تقييم", "كم تقييمه", "كم تقييمك",
        "قصة", "قصته", "قصة عمل", "وش قصته", "ما هي قصته", "ماهي قصته", "شنو قصته",
        "ابطال", "أبطال", "ابطاله", "أبطاله", "مين الابطال", "من الأبطال", "مين أبطاله",
        "مخرج", "مخرجه", "سنة", "سنة كم", "متى نزل", "كم حلقة", "عدد الحلقات",
        "زر", "اخش", "ادخل", "اشوف", "شاهد", "مشاهدة", "رابط", "وين", "وين القاه", "وين ألقاه",
        "عطني اياه", "عطني إياه", "ابيه", "أبيه", "كيف اشوفه", "كيف ألقاه", "رايك فيه", "رأيك فيه"
    )
    return followUpKeywords.any { clean.contains(it) }
}

fun findLastDiscussedShow(messages: List<ChatMessage>, allShows: List<Show>): Show? {
    for (msg in messages.asReversed()) {
        if (msg.linkedShowId.isNotBlank()) {
            val s = allShows.firstOrNull { it.id == msg.linkedShowId }
            if (s != null) return s
        }
        if (msg.linkedShowTitle.isNotBlank()) {
            val s = allShows.firstOrNull { 
                Catalog.normalizeTitle(it.title).equals(Catalog.normalizeTitle(msg.linkedShowTitle), ignoreCase = true) ||
                it.title.contains(msg.linkedShowTitle, ignoreCase = true)
            }
            if (s != null) return s
        }
    }
    return null
}

fun findMatchingShow(query: String, allShows: List<Show>): Show? {
    return Catalog.findMatchingShow(query, allShows)
}

fun extractCandidateShowTitle(query: String, history: List<ChatMessage>): String? {
    if (query.isBlank() || isGeneralGreetingOrConversation(query)) return null

    val quoteRegex = Regex("""["'«»“]([^"'«»“”]+)["'«»”]""")
    val quoteMatch = quoteRegex.find(query)?.groupValues?.get(1)?.trim()
    if (!quoteMatch.isNullOrBlank() && quoteMatch.length in 3..40) return quoteMatch

    val boldRegex = Regex("""\*\*([^\*]+)\*\*""")
    val boldMatch = boldRegex.find(query)?.groupValues?.get(1)?.trim()
    if (!boldMatch.isNullOrBlank() && boldMatch.length in 3..40) return boldMatch

    val seriesRegex = Regex("""(?:مسلسل|فيلم|فلم|دراما)\s+([^\n\r,?.!]+)""")
    val seriesMatch = seriesRegex.find(query)?.groupValues?.get(1)?.trim()
    if (!seriesMatch.isNullOrBlank() && seriesMatch.length in 3..40 && !seriesMatch.equals("عمل", ignoreCase = true)) return seriesMatch

    return null
}

@Composable
fun HakimChatScreen(
    vm: AppViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onOpenShow: (String) -> Unit = {}
) {
    val profileName by vm.profileName.collectAsStateWithLifecycle()
    val profileColorVal by vm.profileColor.collectAsStateWithLifecycle()
    val profileColor = Color(profileColorVal)
    val profileAvatar by vm.profileAvatar.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Locally computed unique guest ID
    val myGuestId = remember(profileName) {
        "guest_" + abs(profileName.hashCode() % 100000)
    }

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var textInput by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var isHakimThinking by remember { mutableStateOf(false) }
    var hakimThinkingStatus by remember { mutableStateOf("حكيم يفكر ويكتب الرد...") }

    var attachedUri by remember { mutableStateOf<Uri?>(null) }
    var attachedUrl by remember { mutableStateOf("") }
    var isUploadingMedia by remember { mutableStateOf(false) }
    var showMediaPicker by remember { mutableStateOf(false) }

    if (showMediaPicker) {
        CustomMediaPicker(
            onDismiss = { showMediaPicker = false },
            onMediaSelected = { uri: android.net.Uri ->
                showMediaPicker = false
                attachedUri = uri
                isUploadingMedia = true
                coroutineScope.launch {
                    val uploadedUrl = com.agon.app.utils.ImageUploader.uploadToTop4ToP(context, uri)
                    if (uploadedUrl != null) {
                        attachedUrl = uploadedUrl
                    } else {
                        errorMessage = "فشل رفع الصورة إلى مركز التحميل. يرجى المحاولة مرة أخرى."
                        attachedUri = null
                    }
                    isUploadingMedia = false
                }
            }
        )
    }

    var isRecordingVoice by remember { mutableStateOf(false) }
    var voiceRecordDuration by remember { mutableStateOf(0) }
    var isUploadingVoice by remember { mutableStateOf(false) }
    
    var draftVoiceFile by remember { mutableStateOf<File?>(null) }
    var draftVoiceDuration by remember { mutableStateOf(0) }
    
    var activeRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var activeRecordFile by remember { mutableStateOf<File?>(null) }
    
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    
    // Dialog state for message options
    var selectedMessageForOptions by remember { mutableStateOf<ChatMessage?>(null) }

    val listState = rememberLazyListState()
    val client = remember { OkHttpClient() }

    // Session Management State
    val sharedPrefs = remember { context.getSharedPreferences("hakim_prefs", android.content.Context.MODE_PRIVATE) }
    var currentSessionId by remember { mutableStateOf(sharedPrefs.getString("current_session_id", "default") ?: "default") }
    var currentSessionTitle by remember { mutableStateOf("المحادثة الرئيسية") }
    var showSessionsDialog by remember { mutableStateOf(false) }
    var sessionsList by remember { mutableStateOf<List<HakimChatSession>>(emptyList()) }

    fun getSessionDbUrl(): String {
        return if (currentSessionId == "default") {
            "https://chat-516a8-default-rtdb.firebaseio.com/chats/hakim_chat_${profileName}.json"
        } else {
            "https://chat-516a8-default-rtdb.firebaseio.com/hakim_sessions_${profileName}/${currentSessionId}/messages.json"
        }
    }

    fun getSessionInfoUrl(sessionId: String): String {
        return "https://chat-516a8-default-rtdb.firebaseio.com/hakim_sessions_${profileName}/${sessionId}/info.json"
    }

    // Helper: Load messages from Firebase
    suspend fun loadMessages() {
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(getSessionDbUrl())
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        errorMessage = "خطأ في الاتصال بالخادم: ${response.code}"
                        return@withContext
                    }
                    val bodyString = response.body?.string()
                    if (bodyString.isNullOrBlank() || bodyString == "null") {
                        messages = emptyList()
                        errorMessage = null
                        return@withContext
                    }
                    
                    val jsonObject = JSONObject(bodyString)
                    val keys = jsonObject.keys()
                    val newList = mutableListOf<ChatMessage>()
                    
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val msgObj = jsonObject.optJSONObject(key) ?: continue
                        val text = msgObj.optString("text", "")
                        val userName = msgObj.optString("userName", "")
                        val userAvatar = msgObj.optString("userAvatar", "custom")
                        val createdAt = msgObj.optLong("createdAt", 0L)
                        val userId = msgObj.optString("userId", "")
                        val imageUrl = msgObj.optString("imageUrl", "")
                        val audioUrl = msgObj.optString("audioUrl", "")
                        val videoUrl = msgObj.optString("videoUrl", "")
                        val isSticker = msgObj.optBoolean("isSticker", false)
                        val userAvatarPosH = msgObj.optString("userAvatarPosH", "50")
                        val userAvatarPosV = msgObj.optString("userAvatarPosV", "50")
                        val userAvatarZoom = msgObj.optString("userAvatarZoom", "100")
                        val userTemplate = msgObj.optString("userTemplate", "none")
                        val voiceDuration = msgObj.optInt("voiceDuration", 0)
                        val linkedShowId = msgObj.optString("linkedShowId", "")
                        val linkedShowTitle = msgObj.optString("linkedShowTitle", "")
                        val linkedShowPoster = msgObj.optString("linkedShowPoster", "")
                        val linkedShowRating = msgObj.optDouble("linkedShowRating", 0.0)
                        val linkedShowYear = msgObj.optString("linkedShowYear", "")
                        
                        newList.add(
                            ChatMessage(
                                id = key,
                                text = text,
                                userName = userName,
                                userAvatar = userAvatar,
                                createdAt = createdAt,
                                userId = userId,
                                imageUrl = imageUrl,
                                audioUrl = audioUrl,
                                videoUrl = videoUrl,
                                isSticker = isSticker,
                                linkedShowId = linkedShowId,
                                linkedShowTitle = linkedShowTitle,
                                linkedShowPoster = linkedShowPoster,
                                linkedShowRating = linkedShowRating,
                                linkedShowYear = linkedShowYear,
                                userAvatarPosH = userAvatarPosH,
                                userAvatarPosV = userAvatarPosV,
                                userAvatarZoom = userAvatarZoom,
                                userTemplate = userTemplate,
                                voiceDuration = voiceDuration
                            )
                        )
                    }
                    
                    newList.sortBy { it.createdAt }
                    messages = newList
                    errorMessage = null
                }
            } catch (e: Exception) {
                errorMessage = "تعذر تحميل الرسائل: ${e.localizedMessage}"
            }
        }
    }

    suspend fun loadSessions() {
        withContext(Dispatchers.IO) {
            try {
                val url = "https://chat-516a8-default-rtdb.firebaseio.com/hakim_sessions_${profileName}.json"
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank() && body != "null") {
                            val json = JSONObject(body)
                            val list = mutableListOf<HakimChatSession>()
                            val keys = json.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                val sessionObj = json.optJSONObject(k) ?: continue
                                val info = sessionObj.optJSONObject("info")
                                val title = info?.optString("title", "محادثة حكيم") ?: "محادثة حكيم"
                                val updatedAt = info?.optLong("updatedAt", System.currentTimeMillis()) ?: System.currentTimeMillis()
                                val count = info?.optInt("messageCount", 0) ?: 0
                                list.add(HakimChatSession(k, title, updatedAt, count))
                            }
                            list.sortByDescending { it.updatedAt }
                            withContext(Dispatchers.Main) {
                                sessionsList = list
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startNewChatSession() {
        val newId = "session_${System.currentTimeMillis()}"
        currentSessionId = newId
        currentSessionTitle = "محادثة جديدة"
        sharedPrefs.edit().putString("current_session_id", newId).apply()
        messages = emptyList()
        showSessionsDialog = false
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val meta = JSONObject().apply {
                        put("id", newId)
                        put("title", "محادثة جديدة ✨")
                        put("updatedAt", System.currentTimeMillis())
                        put("messageCount", 0)
                    }
                    val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                    val request = Request.Builder()
                        .url(getSessionInfoUrl(newId))
                        .put(meta.toString().toRequestBody(mediaType))
                        .build()
                    client.newCall(request).execute().close()
                    loadSessions()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun selectSession(session: HakimChatSession) {
        currentSessionId = session.id
        currentSessionTitle = session.title
        sharedPrefs.edit().putString("current_session_id", session.id).apply()
        showSessionsDialog = false
        messages = emptyList()
        coroutineScope.launch {
            isLoading = true
            loadMessages()
            isLoading = false
        }
    }

    fun deleteSession(sessionId: String) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val url = if (sessionId == "default") {
                        "https://chat-516a8-default-rtdb.firebaseio.com/chats/hakim_chat_${profileName}.json"
                    } else {
                        "https://chat-516a8-default-rtdb.firebaseio.com/hakim_sessions_${profileName}/${sessionId}.json"
                    }
                    val request = Request.Builder().url(url).delete().build()
                    client.newCall(request).execute().close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (currentSessionId == sessionId) {
                currentSessionId = "default"
                currentSessionTitle = "المحادثة الرئيسية"
                sharedPrefs.edit().putString("current_session_id", "default").apply()
                loadMessages()
            }
            loadSessions()
        }
    }

    // Helper: Post fallback error for Hakim
    suspend fun postDefaultErrorResponse(mediaType: okhttp3.MediaType?) {
        try {
            val errorJson = JSONObject().apply {
                put("text", "عذراً! يبدو أن هناك مشكلة مؤقتة في الاتصال بخوادم الذكاء الاصطناعي. يرجى تكرار المحاولة.")
                put("userName", "حكيم ✨ (مستشارك الذكي)")
                put("userAvatar", "https://i.ibb.co/0wvJfBH/file-00000000c1e4720a9aba88f120b35bd1.png")
                put("createdAt", System.currentTimeMillis())
                put("userId", "hakim_ai")
            }
            val fbRequestBody = errorJson.toString().toRequestBody(mediaType)
            val fbRequest = Request.Builder()
                .url(getSessionDbUrl())
                .post(fbRequestBody)
                .build()
            client.newCall(fbRequest).execute().close()
            loadMessages()
        } catch (e: Exception) {
            // Ignore nested errors
        }
    }

    // Helper: Call AI for Hakim with guaranteed exact plot and accurate show matching
    fun triggerHakimResponse(userMsg: String, fileUrl: String = "") {
        isHakimThinking = true
        hakimThinkingStatus = "حكيم يفكر ويكتب الرد..."
        coroutineScope.launch {
            val allShows = (vm.shows.value + Catalog.all).distinctBy { it.id }
            withContext(Dispatchers.IO) {
                try {
                    var foundShow: Show? = null

                    if (!isGeneralGreetingOrConversation(userMsg)) {
                        if (isFollowUpAboutContextShow(userMsg)) {
                            // User is asking a question about the previous show discussed in chat!
                            foundShow = findLastDiscussedShow(messages, allShows)
                        }

                        if (foundShow == null) {
                            foundShow = findMatchingShow(userMsg, allShows)
                        }

                        if (foundShow == null) {
                            val candidate = extractCandidateShowTitle(userMsg, messages)
                            if (candidate != null && !isFollowUpAboutContextShow(candidate)) {
                                foundShow = findMatchingShow(candidate, allShows)
                            }
                        }

                        // If not found in local catalog and query is not a greeting/question, search online remotely
                        if (foundShow == null && !isFollowUpAboutContextShow(userMsg)) {
                            val cleanSearch = userMsg
                                .replace("مسلسل", "").replace("فيلم", "").replace("فلم", "")
                                .replace(Regex("""[^\p{L}\p{N}\s]"""), " ").trim()
                            if (cleanSearch.length in 3..35 && !isGeneralGreetingOrConversation(cleanSearch)) {
                                withContext(Dispatchers.Main) {
                                    hakimThinkingStatus = "جاري البحث عن العمل..."
                                }
                                foundShow = vm.searchShowRemotely(cleanSearch)
                            }
                        }
                    }

                    if (foundShow != null) {
                        vm.addShowIfAbsent(foundShow)
                    }

                    withContext(Dispatchers.Main) {
                        hakimThinkingStatus = "حكيم يجهز لك الرد..."
                    }

                    val systemPrompt = if (foundShow != null) {
                        """
                        أنت حكيم ✨، المستشار والناقد الفني والذكي الرسمي لتطبيق 'حكايتنا' لمشاهدة الأفلام والمسلسلات.
                        تتحدث بذكاء وسلاسة بأسلوب محفز ومرح مع رموز تعبيرية (Emojis) جذابة باللغة العربية.

                        المعلومات الخاصة بالعمل المرفق مع بطاقة المشاهدة للمستخدم:
                        - اسم العمل: "${foundShow.title}"
                        - التصنيف: ${foundShow.genres.joinToString("، ")}
                        - سنة الإصدار: ${foundShow.year}
                        - التقييم الحالي: ${String.format(java.util.Locale.US, "%.1f", foundShow.imdb)} من 10 ⭐
                        - القصة: "${foundShow.description.ifEmpty { "عمل درامي وترفيهي مميز يعرض بجودة عالية على حكايتنا" }}"

                        ⚠️ تعليمات صارمة:
                        1. إذا كان سؤال المستخدم عن تقييم العمل، أو قصته، أو أبطاله، أجب بدقة وعاطفة عن "${foundShow.title}".
                        2. أذكر دائماً اسم العمل بوضوح ووجه المستخدم بحماس إلى زر (اذهب للمشاهدة) الموجود ببطاقة العمل أسفل رسالتك فوراً!
                        3. كن مختصراً، ذكياً، وواضحاً بدون أي رموز غريبة أو سطور برمجية.
                        """.trimIndent()
                    } else {
                        val sampleShows = allShows.filter { !it.isMovie }.take(10).joinToString("، ") { it.title }
                        """
                        أنت حكيم ✨، المستشار والناقد الفني والذكي لتطبيق 'حكايتنا'.
                        تحدث بأسلوب ممتع، ترحيبي، وذكي مع إدراج Emojis جذابة.
                        إذا رحب بك المستخدم (مثل: سلام عليكم، أهلاً، مرحباً)، رد عليه بتحية حارة وودودة واعرض عليه مساعدتك واقتراح أفضل المسلسلات له.
                        إذا سألك عن اقتراحات، اقترح له أعمالاً مميزة من مكتبتنا مثل: $sampleShows.
                        لا ترفق بطاقة عمل إلا عندما يتم تخصيص مسلسل محدد في المحادثة.
                        """.trimIndent()
                    }

                    val files = if (fileUrl.isNotBlank()) listOf(fileUrl) else emptyList()
                    val rawReplyText = com.agon.app.data.HakimConfigManager.askHakim(userMsg, files, systemPrompt)
                    val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

                    if (!rawReplyText.isNullOrBlank()) {
                        val cleanReply = rawReplyText
                            .replace("\\n", "\n")
                            .replace("\\\"", "\"")
                            .trim()

                        val matchedShow = foundShow

                        val hakimMessageJson = JSONObject().apply {
                            put("text", cleanReply)
                            put("userName", "حكيم ✨ (مستشارك الذكي)")
                            put("userAvatar", "https://i.ibb.co/0wvJfBH/file-00000000c1e4720a9aba88f120b35bd1.png")
                            put("createdAt", System.currentTimeMillis())
                            put("userId", "hakim_ai")
                            if (matchedShow != null) {
                                put("linkedShowId", matchedShow.id)
                                put("linkedShowTitle", matchedShow.title)
                                put("linkedShowPoster", matchedShow.poster)
                                put("linkedShowRating", matchedShow.imdb)
                                put("linkedShowYear", matchedShow.year)
                            }
                        }

                        val fbRequestBody = hakimMessageJson.toString().toRequestBody(mediaType)
                        val fbRequest = Request.Builder()
                            .url(getSessionDbUrl())
                            .post(fbRequestBody)
                            .build()

                        client.newCall(fbRequest).execute().use { fbResponse ->
                            if (fbResponse.isSuccessful) {
                                loadMessages()
                                coroutineScope.launch {
                                    delay(500)
                                    if (messages.isNotEmpty()) {
                                        listState.animateScrollToItem(messages.size - 1)
                                    }
                                }
                            }
                        }
                    } else {
                        postDefaultErrorResponse(mediaType)
                    }
                } catch (e: Exception) {
                    val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                    postDefaultErrorResponse(mediaType)
                } finally {
                    isHakimThinking = false
                    hakimThinkingStatus = "حكيم يفكر ويكتب الرد..."
                }
            }
        }
    }

    // Helper: Send a message
    fun sendMessage(voiceUrl: String = "", voiceDur: Int = 0) {
        if ((textInput.isBlank() && attachedUrl.isEmpty() && voiceUrl.isEmpty()) || isSending || isUploadingMedia) return
        isSending = true
        val textToSend = textInput.trim()
        val mediaUrlToSend = attachedUrl
        textInput = ""
        attachedUri = null
        attachedUrl = ""

        if (editingMessage != null) {
            val msgId = editingMessage!!.id
            editingMessage = null
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val editUrl = if (currentSessionId == "default") {
                            "https://chat-516a8-default-rtdb.firebaseio.com/chats/hakim_chat_${profileName}/${msgId}.json"
                        } else {
                            "https://chat-516a8-default-rtdb.firebaseio.com/hakim_sessions_${profileName}/${currentSessionId}/messages/${msgId}.json"
                        }
                        val json = JSONObject().apply { put("text", textToSend) }
                        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                        val request = Request.Builder()
                            .url(editUrl)
                            .patch(json.toString().toRequestBody(mediaType))
                            .build()
                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) loadMessages()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        isSending = false
                    }
                }
            }
            return
        }

        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val messageJson = JSONObject().apply {
                        put("text", textToSend)
                        put("userName", profileName)
                        put("userAvatar", profileAvatar.ifBlank { "custom" })
                        put("createdAt", System.currentTimeMillis())
                        put("userId", myGuestId)
                        put("userAvatarPosH", "50")
                        put("userAvatarPosV", "50")
                        put("userAvatarZoom", "100")
                        put("userTemplate", "none")
                        put("isSticker", false)
                        put("videoUrl", "")
                        put("audioUrl", "")
                        put("imageUrl", "")
                        if (voiceUrl.isNotEmpty()) {
                            put("audioUrl", voiceUrl)
                            put("voiceDuration", voiceDur)
                        } else if (mediaUrlToSend.isNotEmpty()) {
                            put("imageUrl", mediaUrlToSend)
                        }
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                    val requestBody = messageJson.toString().toRequestBody(mediaType)
                    
                    val request = Request.Builder()
                        .url(getSessionDbUrl())
                        .post(requestBody)
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            // Update session meta info
                            try {
                                val summary = if (textToSend.isNotBlank()) textToSend.take(35) else "محادثة حكيم"
                                val meta = JSONObject().apply {
                                    put("id", currentSessionId)
                                    put("title", summary)
                                    put("updatedAt", System.currentTimeMillis())
                                    put("messageCount", messages.size + 1)
                                }
                                val metaReq = Request.Builder()
                                    .url(getSessionInfoUrl(currentSessionId))
                                    .put(meta.toString().toRequestBody(mediaType))
                                    .build()
                                client.newCall(metaReq).execute().close()
                            } catch (_: Exception) {}

                            loadMessages()
                            // Scroll to bottom
                            coroutineScope.launch {
                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }

                    // Always Trigger Hakim Response in this screen
                    val triggerMsg = if (textToSend.isNotBlank()) textToSend else "أرسل لك مرفقاً أو تسجيلاً لحكيم ✨"
                    triggerHakimResponse(triggerMsg, mediaUrlToSend)
                        } else {
                            withContext(Dispatchers.Main) {
                                errorMessage = "فشل في إرسال الرسالة: ${response.code}"
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "حدث خطأ أثناء الإرسال: ${e.localizedMessage}"
                    }
                } finally {
                    isSending = false
                }
            }
        }
    }

    fun deleteMessage(msgId: String) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val deleteUrl = "https://chat-516a8-default-rtdb.firebaseio.com/chats/hakim_chat_${profileName}/${msgId}.json"
                    val request = Request.Builder()
                        .url(deleteUrl)
                        .delete()
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) loadMessages()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun startNewChat() {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder()
                        .url(getSessionDbUrl())
                        .delete()
                        .build()
                    client.newCall(request).execute().close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            messages = emptyList()
        }
    }

    fun startVoiceRecording() {
        try {
            val file = File(context.cacheDir, "voice_record_${System.currentTimeMillis()}.m4a")
            activeRecordFile = file
            
            val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            
            activeRecorder = recorder
            isRecordingVoice = true
            voiceRecordDuration = 0
        } catch (e: Exception) {
            e.printStackTrace()
            // Interactive fallback mode
            isRecordingVoice = true
            voiceRecordDuration = 0
            activeRecorder = null
        }
    }

    fun finishVoiceRecording() {
        val recorder = activeRecorder
        val file = activeRecordFile
        
        isRecordingVoice = false
        activeRecorder = null
        activeRecordFile = null
        
        if (recorder != null) {
            try {
                recorder.stop()
                recorder.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        if (file != null && file.exists() && voiceRecordDuration > 0) {
            draftVoiceFile = file
            draftVoiceDuration = voiceRecordDuration
        } else {
            file?.delete()
            draftVoiceFile = null
            draftVoiceDuration = 0
        }
    }
    
    fun cancelVoiceRecording() {
        draftVoiceFile?.delete()
        draftVoiceFile = null
        draftVoiceDuration = 0
    }
    
    fun sendDraftVoice() {
        val file = draftVoiceFile ?: return
        val duration = draftVoiceDuration
        
        isUploadingVoice = true
        draftVoiceFile = null
        draftVoiceDuration = 0
        
        coroutineScope.launch {
            try {
                val bytes = file.readBytes()
                val url = com.agon.app.utils.ImageUploader.uploadAudioToTop4ToP(context, bytes)
                if (url != null) {
                    sendMessage(voiceUrl = url, voiceDur = duration)
                } else {
                    withContext(Dispatchers.Main) {
                        errorMessage = "تعذر رفع التسجيل الصوتي. حاول مجدداً"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isUploadingVoice = false
                file.delete()
            }
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startVoiceRecording()
        } else {
            errorMessage = "يجب السماح بصلاحية المايكروفون لتسجيل الصوت"
        }
    }

    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            while (isRecordingVoice) {
                delay(1000)
                voiceRecordDuration += 1
            }
        }
    }

    // Polling effect: Load messages every 4 seconds for the current session
    LaunchedEffect(currentSessionId) {
        isLoading = true
        loadMessages()
        isLoading = false
        
        while (true) {
            delay(4000)
            loadMessages()
        }
    }

    // Auto-scroll to bottom on first load
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App bar (Custom Hakim Design with 3 Lines menu for past conversations)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F0F0F))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Hamburger Menu (3 Lines) for previous conversations + New Chat quick action
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 3 Lines Icon Button (سجل المحادثات)
                IconButton(
                    onClick = {
                        showSessionsDialog = true
                        coroutineScope.launch { loadSessions() }
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E222A))
                        .border(1.dp, Color(0xFFE50914).copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "سجل المحادثات السابقة (ثلاث خطوط)",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // New Chat quick button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1E222A))
                        .border(1.dp, Color(0xFF2E3440), RoundedCornerShape(18.dp))
                        .clickable { startNewChatSession() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "محادثة جديدة",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "محادثة جديدة",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Center: Hakim Profile
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Texts
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF4A1515))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "مستشار\nالدراما",
                                color = Color(0xFFFF2D3A),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حكيم الذكي",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.FlashOn,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "متصل الآن ومستعد لمساعدتك",
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(10.dp))
                
                // Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoFixHigh,
                        contentDescription = "Avatar",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676))
                            .border(1.5.dp, Color(0xFF0F0F0F), CircleShape)
                    )
                }
            }

            // Right: Back Button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF1E1E1E), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = "رجوع",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Main Body
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isLoading && messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = profileColor)
                }
            } else if (messages.isEmpty() && errorMessage == null) {
                // Empty state (Suggestions Design)
                Box(
                    modifier = Modifier.fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE50914).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Forum,
                                contentDescription = null,
                                tint = Color(0xFFE50914),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "دردش مع حكيم منصة حكايتنا!",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "اسأل حكيم عن تفاصيل الفيلم الذي تفكر فيه، أو ارفع لقطة شاشة من مشهد يعجبك، أو تحدث صوتياً لمعرفة العمل فوراً!",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        
                        Spacer(Modifier.height(32.dp))
                        
                        val suggestions = listOf(
                            "اقترح لي فيلم تركي رومانسي وحزين \uD83D\uDC96",
                            "عندكم فيلم المتوحش؟ عطني قصته \uD83D\uDC3A",
                            "أبي فيلم أكشن وحرب تاريخي رهيب ⚔️",
                            "دلني على فيلم درامي عائلي راقي \uD83D\uDCFA"
                        )
                        
                        suggestions.forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color(0xFF1E1E1E))
                                    .clickable { 
                                        textInput = suggestion
                                        sendMessage()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ChevronLeft,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = suggestion,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages) { message ->
                        val isMe = message.userId == myGuestId || (message.userName == profileName && message.userId.startsWith("guest_"))
                        MessageItem(
                            onOpenShow = onOpenShow,
                            message = message,
                            isMe = isMe,
                            myProfileColor = profileColor,
                            showsList = vm.shows.value,
                            onLongClick = {
                                if (isMe) {
                                    selectedMessageForOptions = message
                                }
                            }
                        )
                    }

                    if (isHakimThinking) {
                        item {
                            HakimThinkingItem(statusText = hakimThinkingStatus)
                        }
                    }
                }
            }

            // Error message overlay
            if (errorMessage != null) {
                errorMessage?.let { error ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                            .clickable { errorMessage = null }
                            .padding(12.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Attached Media Preview if any
        if (attachedUri != null || isUploadingMedia) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isUploadingMedia) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color.DarkGray, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFFFF9800))
                        }
                    } else {
                        coil.compose.AsyncImage(
                            model = attachedUrl,
                            contentDescription = "الصورة المرفقة",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isUploadingMedia) "جاري رفع ومعالجة الصورة..." else "تم تجهيز الصورة للإرسال بنجاح ✨",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isUploadingMedia) "انتظر قليلاً من فضلك" else "اضغط إرسال لمشاركتها مع الجميع",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(
                        onClick = {
                            attachedUri = null
                            attachedUrl = ""
                            isUploadingMedia = false
                        }
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "إلغاء", tint = Color.Red)
                    }
                }
            }
        }

        // Voice Uploading Progress Bar
        if (isUploadingVoice) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFFFF9800))
                    Spacer(Modifier.width(12.dp))
                    Text("جاري معالجة ورفع التسجيل الصوتي السريع...", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        // Quick Action Chat Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE50914))
                    .clickable { textInput = "@حكيم " }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "أسأل حكيم ✨",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            listOf("سلام عليكم", "😂😂😂", "حي عـلى الفلاح").forEach { chipText ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF222222))
                        .clickable {
                            textInput = chipText
                            sendMessage()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = chipText,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Input bottom bar (Custom Design)
        Surface(
            tonalElevation = 4.dp,
            color = Color(0xFF0F0F0F),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isRecordingVoice) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { 
                            finishVoiceRecording()
                            cancelVoiceRecording()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF222222), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "حذف التسجيل",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulseAlpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.Red.copy(alpha = pulseAlpha))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "جاري التسجيل... %02d:%02d".format(voiceRecordDuration / 60, voiceRecordDuration % 60),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = { finishVoiceRecording() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFE50914), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "إنهاء التسجيل",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else if (draftVoiceFile != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { cancelVoiceRecording() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF222222), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "حذف التسجيل",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(Modifier.width(8.dp))
                    
                    Box(modifier = Modifier.weight(1f)) {
                        VoicePlayBubble(
                            audioUrl = draftVoiceFile!!.absolutePath,
                            duration = draftVoiceDuration,
                            isMe = true
                        )
                    }
                    
                    Spacer(Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = { sendDraftVoice() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFE50914), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "إرسال",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Send Button on Left
                    IconButton(
                        onClick = { sendMessage() },
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color(0xFF3B0B0C), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "إرسال",
                            tint = Color(0xFFFF2D3A),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // TextField in Middle
                    TextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("اسأل حكيم عن أفلامك المفضلة...", color = Color.Gray, fontSize = 13.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1E1E1E),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    Spacer(Modifier.width(8.dp))

                    // Mic Button
                    IconButton(
                        onClick = { 
                            if (androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.RECORD_AUDIO
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                startVoiceRecording()
                            } else {
                                recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF1E1E1E), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "صوت",
                            tint = Color.Gray,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    // Image Button
                    IconButton(
                        onClick = { showMediaPicker = true },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF1E1E1E), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = "إرفاق صورة",
                            tint = Color.Gray,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }

    if (selectedMessageForOptions != null) {
        val msg = selectedMessageForOptions!!
        AlertDialog(
            onDismissRequest = { selectedMessageForOptions = null },
            title = {
                Text(
                    text = "خيارات الرسالة",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            editingMessage = msg
                            textInput = msg.text
                            selectedMessageForOptions = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تعديل الرسالة", color = Color(0xFF2196F3), fontSize = 16.sp)
                    }
                    Divider(color = Color.DarkGray)
                    TextButton(
                        onClick = {
                            deleteMessage(msg.id)
                            selectedMessageForOptions = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حذف الرسالة", color = Color.Red, fontSize = 16.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMessageForOptions = null }) {
                    Text("إلغاء", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    // Previous Conversations Dialog (سجل المحادثات السابقة عند الضغط على الثلاث خطوط)
    if (showSessionsDialog) {
        AlertDialog(
            onDismissRequest = { showSessionsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "سجل المحادثات السابقة ✨",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 17.sp
                        )
                    }
                    IconButton(
                        onClick = { showSessionsDialog = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = Color.Gray)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    // Button to start a brand new conversation
                    Button(
                        onClick = { startNewChatSession() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("بدء محادثة جديدة مع حكيم ✨", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "المحادثات السابقة:",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))

                    val displaySessions = remember(sessionsList) {
                        val list = sessionsList.toMutableList()
                        if (list.none { it.id == "default" }) {
                            list.add(0, HakimChatSession("default", "المحادثة الرئيسية", System.currentTimeMillis(), 0))
                        }
                        list
                    }

                    if (displaySessions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد محادثات سابقة محفوظة بعد.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(displaySessions) { session ->
                                val isCurrent = session.id == currentSessionId
                                val formattedDate = remember(session.updatedAt) {
                                    try {
                                        SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar")).format(Date(session.updatedAt))
                                    } catch (e: Exception) {
                                        ""
                                    }
                                }

                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrent) Color(0xFF26181A) else Color(0xFF1E2028)
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isCurrent) Color(0xFFE50914) else Color(0xFF2E3240)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectSession(session) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (isCurrent) Color(0xFFE50914) else Color(0xFF2E3240)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Forum,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = session.title.ifEmpty { "محادثة حكيم" },
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                    )
                                                    if (isCurrent) {
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(
                                                            text = "نشطة 🟢",
                                                            color = Color(0xFF4CAF50),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                if (formattedDate.isNotEmpty()) {
                                                    Text(
                                                        text = formattedDate,
                                                        color = Color.Gray,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }

                                        if (session.id != "default") {
                                            IconButton(
                                                onClick = { deleteSession(session.id) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "حذف المحادثة",
                                                    tint = Color.Red.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = Color(0xFF14151B)
        )
    }
}

