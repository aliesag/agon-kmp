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
import androidx.compose.material.icons.filled.Movie
import androidx.compose.foundation.border
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.material.icons.filled.Star

// Message Data Class
data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val createdAt: Long = 0L,
    val userId: String = "",
    val imageUrl: String = "",
    val audioUrl: String = "",
    val videoUrl: String = "",
    val voiceDuration: Int = 0,
    val isSticker: Boolean = false,
    val linkedShowId: String = "",
    val linkedShowTitle: String = "",
    val linkedShowPoster: String = "",
    val linkedShowRating: Double = 0.0,
    val linkedShowYear: String = "",
    val userAvatarPosH: String = "50",
    val userAvatarPosV: String = "50",
    val userAvatarZoom: String = "100",
    val userTemplate: String = "none"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
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

    var attachedUri by remember { mutableStateOf<Uri?>(null) }
    var attachedUrl by remember { mutableStateOf("") }
    var selectedLinkedShowId by remember { mutableStateOf<String?>(null) }
    var showShowPicker by remember { mutableStateOf(false) }
    var isUploadingMedia by remember { mutableStateOf(false) }
    var showMediaPicker by remember { mutableStateOf(false) }

    if (showShowPicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showShowPicker = false },
            title = { Text("اختر مسلسلاً للإرفاق", color = Color.White) },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(com.agon.app.data.Catalog.all) { show ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedLinkedShowId = show.id
                                    showShowPicker = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            coil.compose.AsyncImage(
                                model = show.poster,
                                contentDescription = null,
                                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            Text(show.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showShowPicker = false }) {
                    Text("إلغاء", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF222222)
        )
    }

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

    // Constants
    val firebaseDbUrl = "https://chat-516a8-default-rtdb.firebaseio.com/chats/global_chat_v1.json"

    // Helper: Load messages from Firebase
    suspend fun loadMessages() {
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(firebaseDbUrl)
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
                        val linkedShowId = if (msgObj.has("linkedShowId")) msgObj.optString("linkedShowId") else ""
                        val linkedShowTitle = msgObj.optString("linkedShowTitle", "")
                        val linkedShowPoster = msgObj.optString("linkedShowPoster", "")
                        val linkedShowRating = msgObj.optDouble("linkedShowRating", 0.0)
                        val linkedShowYear = msgObj.optString("linkedShowYear", "")
                        val userAvatarPosH = msgObj.optString("userAvatarPosH", "50")
                        val userAvatarPosV = msgObj.optString("userAvatarPosV", "50")
                        val userAvatarZoom = msgObj.optString("userAvatarZoom", "100")
                        val userTemplate = msgObj.optString("userTemplate", "none")
                        val voiceDuration = msgObj.optInt("voiceDuration", 0)
                        
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
                .url(firebaseDbUrl)
                .post(fbRequestBody)
                .build()
            client.newCall(fbRequest).execute().close()
            loadMessages()
        } catch (e: Exception) {
            // Ignore nested errors
        }
    }

    // Helper: Call DeepSeek API for Hakim using dynamic Firebase config and post the result
    fun triggerHakimResponse(userMsg: String) {
        isHakimThinking = true
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val systemPrompt = "أنت حكيم ✨، المستشار والناقد والذكي المتواجد في غرفة دردشة تطبيق حكايتنا لمشاهدة الأفلام والمسلسلات. واجبك هو التفاعل مع المستخدمين بأسلوب حكيم، مشوق، ومرح مليء بالنصائح السينمائية والأفلام الرائعة مع إدراج رموز تعبيرية (Emojis) جذابة وبطريقة موجزة وممتعة باللغة العربية الفصحى."
                    val replyText = com.agon.app.data.HakimConfigManager.askHakim(userMsg, emptyList(), systemPrompt)
                    val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

                    if (!replyText.isNullOrBlank()) {
                        val hakimMessageJson = JSONObject().apply {
                            put("text", replyText)
                            put("userName", "حكيم ✨ (مستشارك الذكي)")
                            put("userAvatar", "https://i.ibb.co/0wvJfBH/file-00000000c1e4720a9aba88f120b35bd1.png")
                            put("createdAt", System.currentTimeMillis())
                            put("userId", "hakim_ai")
                        }

                        val fbRequestBody = hakimMessageJson.toString().toRequestBody(mediaType)
                        val fbRequest = Request.Builder()
                            .url(firebaseDbUrl)
                            .post(fbRequestBody)
                            .build()

                        client.newCall(fbRequest).execute().use { fbResponse ->
                            if (fbResponse.isSuccessful) {
                                loadMessages()
                                coroutineScope.launch {
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
        val showIdToSend = selectedLinkedShowId
        textInput = ""
        attachedUri = null
        attachedUrl = ""
        selectedLinkedShowId = null

        if (editingMessage != null) {
            val msgId = editingMessage!!.id
            editingMessage = null
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val editUrl = "https://chat-516a8-default-rtdb.firebaseio.com/chats/global_chat_v1/${msgId}.json"
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
                        if (showIdToSend != null) put("linkedShowId", showIdToSend)
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
                        .url(firebaseDbUrl)
                        .post(requestBody)
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            loadMessages()
                            // Scroll to bottom
                            coroutineScope.launch {
                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }

                            // Trigger Hakim AI if the message mentions "حكيم" or if it is a voice note and we simulate a Hakim response
                            if (textToSend.contains("حكيم", ignoreCase = true) || (voiceUrl.isNotEmpty() && (0..1).random() == 1)) {
                                val triggerMsg = if (textToSend.isNotBlank()) textToSend else "أرسل لك تسجيلاً صوتياً حكيماً"
                                triggerHakimResponse(triggerMsg)
                            }
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
                    val deleteUrl = "https://chat-516a8-default-rtdb.firebaseio.com/chats/global_chat_v1/${msgId}.json"
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

    // Polling effect: Load messages every 4 seconds when screen is visible
    LaunchedEffect(Unit) {
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
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "دردشة حكايتنا",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "الغرفة العامة للدردشة والمناقشة 💬",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Row {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            loadMessages()
                            isLoading = false
                        }
                    },
                    modifier = Modifier.testTag("refresh_chat_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "تحديث", tint = Color.White)
                }
                IconButton(
                    onClick = onBack
                ) {
                    Icon(Icons.Filled.ArrowForward, contentDescription = "رجوع", tint = Color.White)
                }
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
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Forum,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "لا توجد رسائل بعد.\nكن أول من يرسل رسالة!",
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            fontSize = 15.sp
                        )
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
                            HakimThinkingItem()
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
        if (attachedUri != null || isUploadingMedia || selectedLinkedShowId != null) {
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
                    } else if (attachedUri != null) {
                        coil.compose.AsyncImage(
                            model = attachedUrl,
                            contentDescription = "الصورة المرفقة",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else if (selectedLinkedShowId != null) {
                        val show = com.agon.app.data.Catalog.all.find { it.id == selectedLinkedShowId }
                        if (show != null) {
                            coil.compose.AsyncImage(
                                model = show.poster,
                                contentDescription = "المسلسل المرفق",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isUploadingMedia) "جاري رفع ومعالجة الصورة..." else if (attachedUri != null) "تم تجهيز الصورة للإرسال بنجاح ✨" else "تم إرفاق المسلسل ✨",
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
                            selectedLinkedShowId = null
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

        // Input bottom bar
        Surface(
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface,
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
                            .background(profileColor, CircleShape)
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
                            .background(profileColor, CircleShape)
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
                if (editingMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2B2B2B))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "تعديل الرسالة",
                                color = Color(0xFFFF9800),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = editingMessage!!.text,
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { 
                            editingMessage = null 
                            textInput = ""
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "إلغاء", tint = Color.Gray)
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showMediaPicker = true },
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(38.dp)
                            .background(Color(0xFF222222), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = "إرفاق صورة",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.RECORD_AUDIO
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                startVoiceRecording()
                            } else {
                                recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(38.dp)
                            .background(Color(0xFF222222), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "تسجيل صوتي",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { showShowPicker = true },
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(38.dp)
                            .background(Color(0xFF222222), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Movie,
                            contentDescription = "إرفاق مسلسل",
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { textInput = "@حكيم " },
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(38.dp)
                            .background(Color(0xFF222222), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoFixHigh,
                            contentDescription = "اسأل حكيم",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    TextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("اكتب تعليقك أو اسأل حكيم...", color = Color.Gray, fontSize = 13.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1E1E1E),
                            disabledContainerColor = Color(0xFF1E1E1E),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp, max = 120.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .testTag("chat_input_field"),
                        maxLines = 4,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                    )

                    Spacer(Modifier.width(6.dp))

                    IconButton(
                        onClick = { sendMessage() },
                        enabled = (textInput.isNotBlank() || attachedUrl.isNotEmpty()) && !isSending && !isUploadingMedia,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (textInput.isNotBlank() || attachedUrl.isNotEmpty()) Color(0xFFE91E63).copy(alpha = 0.3f) else Color(0xFF222222),
                                shape = CircleShape
                            )
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "إرسال",
                            tint = if (textInput.isNotBlank() || attachedUrl.isNotEmpty()) Color(0xFFE91E63) else Color.Gray,
                            modifier = Modifier.size(18.dp)
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
}

@Composable
fun HakimThinkingItem(statusText: String = "حكيم يفكر ويكتب الرد...") {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Circle avatar for Hakim
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFFF9800), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✨",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.width(8.dp))

        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "حكيم ✨ (مستشارك الذكي)",
                color = Color(0xFFFF9800),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            // Chat bubble
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .background(
                        color = Color(0xFF2C2C2C),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = statusText,
                        color = if (statusText.contains("بحث")) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = if (statusText.contains("بحث")) FontWeight.Bold else FontWeight.Normal
                    )
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = Color(0xFFFF9800)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageItem(
    onOpenShow: (String) -> Unit = {},
    message: ChatMessage,
    isMe: Boolean,
    myProfileColor: Color,
    showsList: List<com.agon.app.data.Show> = emptyList(),
    onLongClick: () -> Unit = {}
) {
    val isHakim = message.userId == "hakim_ai" || message.userName.contains("حكيم")
    
    // Generate an aesthetic color based on username hash
    val senderColor = remember(message.userName) {
        if (isHakim) {
            Color(0xFFFF9800) // Golden color for Hakim
        } else {
            val hash = message.userName.hashCode()
            val colors = listOf(
                Color(0xFFE50914), // Red
                Color(0xFF2196F3), // Blue
                Color(0xFF4CAF50), // Green
                Color(0xFFFF9800), // Orange
                Color(0xFF9C27B0), // Purple
                Color(0xFF00BCD4), // Cyan
                Color(0xFFFFC107), // Amber
                Color(0xFFE91E63)  // Pink
            )
            colors[abs(hash) % colors.size]
        }
    }

    // Format message time
    val formattedTime = remember(message.createdAt) {
        if (message.createdAt <= 0) "" else {
            try {
                val sdf = SimpleDateFormat("hh:mm a", Locale("ar"))
                sdf.format(Date(message.createdAt))
            } catch (e: Exception) {
                ""
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            // Circle avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(senderColor),
                contentAlignment = Alignment.Center
            ) {
                if (isHakim) {
                    Text(
                        text = "✨",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                } else if (message.userAvatar.startsWith("http")) {
                    coil.compose.AsyncImage(
                        model = message.userAvatar,
                        contentDescription = "صورة المرسل",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Text(
                        text = message.userName.firstOrNull()?.uppercase() ?: "U",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            if (!isMe) {
                Text(
                    text = message.userName,
                    color = senderColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            // Chat bubble using styled Box for compatibility and robustness
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .background(
                        color = if (isMe) myProfileColor else Color(0xFF2C2C2C),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp
                        )
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { onLongClick() }
                        )
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    if (message.audioUrl.isNotEmpty()) {
                        VoicePlayBubble(
                            audioUrl = message.audioUrl,
                            duration = message.voiceDuration,
                            isMe = isMe
                        )
                    } else if (message.imageUrl.isNotEmpty()) {
                        coil.compose.AsyncImage(
                            model = message.imageUrl,
                            contentDescription = "صورة مرفقة",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
                        )
                    }
                    if (message.text.isNotEmpty()) {
                        Text(
                            text = message.text.replace("\\n", "\n").replace("\\\"", "\""),
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                    val effectiveShow = remember(message.linkedShowId, message.text, showsList) {
                        if (message.linkedShowId.isNotEmpty()) {
                            (showsList + com.agon.app.data.Catalog.all).find { it.id == message.linkedShowId }
                                ?: if (message.linkedShowTitle.isNotEmpty()) {
                                    com.agon.app.data.Show(
                                        id = message.linkedShowId,
                                        title = message.linkedShowTitle,
                                        year = message.linkedShowYear.ifEmpty { "2024" },
                                        rating = "+13",
                                        imdb = if (message.linkedShowRating > 0) message.linkedShowRating else 7.5,
                                        genres = listOf("دراما"),
                                        description = "",
                                        poster = message.linkedShowPoster.ifEmpty { "https://files.catbox.moe/u0fux9.jpg" },
                                        backdrop = message.linkedShowPoster.ifEmpty { "https://files.catbox.moe/u0fux9.jpg" },
                                        duration = "45 دقيقة",
                                        isMovie = false
                                    )
                                } else null
                        } else {
                            null
                        }
                    }

                    if (effectiveShow != null) {
                        Spacer(Modifier.height(10.dp))
                        androidx.compose.material3.Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF1B1B22)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE50914).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenShow(effectiveShow.id) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Box {
                                    coil.compose.AsyncImage(
                                        model = effectiveShow.poster,
                                        contentDescription = effectiveShow.title,
                                        modifier = Modifier
                                            .size(width = 56.dp, height = 80.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Black.copy(alpha = 0.7f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (effectiveShow.isMovie) "فيلم" else "مسلسل",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = effectiveShow.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(12.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = String.format(java.util.Locale.US, "%.1f", effectiveShow.imdb),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = effectiveShow.year,
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFFE50914))
                                            .clickable { onOpenShow(effectiveShow.id) }
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "اذهب للمشاهدة",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = formattedTime,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        if (isMe) {
            Spacer(Modifier.width(8.dp))
            // Circle avatar for me
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(myProfileColor),
                contentAlignment = Alignment.Center
            ) {
                if (message.userAvatar.startsWith("http")) {
                    coil.compose.AsyncImage(
                        model = message.userAvatar,
                        contentDescription = "صورتي",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Text(
                        text = message.userName.firstOrNull()?.uppercase() ?: "U",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun VoicePlayBubble(
    audioUrl: String,
    duration: Int,
    isMe: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var isDownloading by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var localFile by remember { mutableStateOf<File?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    
    var currentProgress by remember { mutableStateOf(0f) }
    var currentPositionMs by remember { mutableStateOf(0) }
    var totalDurationMs by remember { mutableStateOf(if (duration > 0) duration * 1000 else 1000) }
    var isDragging by remember { mutableStateOf(false) }

    fun releasePlayer() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        isPlaying = false
    }

    fun initPlayer(file: File): MediaPlayer? {
        return try {
            releasePlayer()
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                val dur = this.duration
                if (dur > 0) {
                    totalDurationMs = dur
                }
                setOnCompletionListener {
                    isPlaying = false
                    currentProgress = 0f
                    currentPositionMs = 0
                    seekTo(0)
                }
            }
            mediaPlayer = player
            player
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun initPlayerWithUrl(url: String): MediaPlayer? {
        return try {
            releasePlayer()
            val player = MediaPlayer().apply {
                setDataSource(url)
                prepare()
                val dur = this.duration
                if (dur > 0) {
                    totalDurationMs = dur
                }
                setOnCompletionListener {
                    isPlaying = false
                    currentProgress = 0f
                    currentPositionMs = 0
                    seekTo(0)
                }
            }
            mediaPlayer = player
            player
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            releasePlayer()
        }
    }

    LaunchedEffect(isPlaying, isDragging) {
        if (isPlaying && !isDragging) {
            while (isPlaying) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            val pos = player.currentPosition
                            val dur = player.duration
                            if (dur > 0) {
                                totalDurationMs = dur
                                currentPositionMs = pos
                                currentProgress = pos.toFloat() / dur.toFloat()
                            }
                        } else {
                            isPlaying = false
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                delay(100)
            }
        }
    }

    val currentSec = currentPositionMs / 1000
    val totalSec = totalDurationMs / 1000
    val currentDisplayTime = "%02d:%02d".format(currentSec / 60, currentSec % 60)
    val totalDisplayTime = "%02d:%02d".format(totalSec / 60, totalSec % 60)

    val accentColor = if (isMe) Color.White else Color(0xFFFF9800)
    val trackColor = if (isMe) Color.White.copy(alpha = 0.35f) else Color.Gray.copy(alpha = 0.35f)
    val thumbColor = if (isMe) Color.White else Color(0xFFFF9800)
    val displayTextColor = if (isMe) Color.White.copy(alpha = 0.9f) else Color.LightGray

    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = accentColor,
                    strokeWidth = 2.5.dp
                )
            } else {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            var player = mediaPlayer
                            if (player == null) {
                                if (localFile == null) {
                                    if (audioUrl.startsWith("/")) {
                                        val f = File(audioUrl)
                                        if (f.exists()) {
                                            localFile = f
                                            player = initPlayer(f)
                                        } else {
                                            player = initPlayerWithUrl(audioUrl)
                                        }
                                    } else {
                                        isDownloading = true
                                        val downloaded = com.agon.app.utils.ImageUploader.downloadAudioFile(context, audioUrl)
                                        isDownloading = false
                                        if (downloaded != null && downloaded.exists()) {
                                            localFile = downloaded
                                            player = initPlayer(downloaded)
                                        } else {
                                            player = initPlayerWithUrl(audioUrl)
                                        }
                                    }
                                } else {
                                    player = initPlayer(localFile!!)
                                }
                            }
                            
                            if (player != null) {
                                if (isPlaying) {
                                    player.pause()
                                    isPlaying = false
                                } else {
                                    try {
                                        player.start()
                                        isPlaying = true
                                    } catch (e: Exception) {
                                        localFile?.let {
                                            val reinit = initPlayer(it)
                                            reinit?.start()
                                            isPlaying = true
                                        }
                                    }
                                }
                            } else {
                                isPlaying = !isPlaying
                            }
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(accentColor, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Slider(
                value = currentProgress,
                onValueChange = { newValue ->
                    isDragging = true
                    currentProgress = newValue
                    currentPositionMs = (newValue * totalDurationMs).toInt()
                },
                onValueChangeFinished = {
                    isDragging = false
                    mediaPlayer?.let { player ->
                        try {
                            val targetMs = (currentProgress * totalDurationMs).toInt()
                            player.seekTo(targetMs)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                colors = SliderDefaults.colors(
                    thumbColor = thumbColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = trackColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            )
            
            Spacer(Modifier.height(2.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentDisplayTime,
                    color = displayTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                
                if (localFile == null && !isDownloading) {
                    Text(
                        text = "اضغط للتشغيل",
                        color = displayTextColor.copy(alpha = 0.5f),
                        fontSize = 9.sp
                    )
                }
                
                Text(
                    text = totalDisplayTime,
                    color = displayTextColor.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
