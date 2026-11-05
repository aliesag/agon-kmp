package com.agon.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import com.agon.app.Http
import com.agon.app.audio.rememberAudioRecorder
import com.agon.app.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.json.JSONObject

private val NetflixRed = Color(0xFFE50914)
private const val CHAT_URL = "https://chat-516a8-default-rtdb.firebaseio.com/chats/global_chat_v1.json"

data class ChatMessage(
    val id: String,
    val text: String,
    val user: String,
    val avatar: String,
    val color: Long,
    val timestamp: Long,
    val imageUrl: String = "",
    val voiceUrl: String = "",
    val voiceDur: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    vm: AppViewModel,
    onBack: () -> Unit = {},
    onOpenShow: (String) -> Unit = {}
) {
    val profileName by vm.profileName.collectAsStateWithLifecycle()
    val profileAvatar by vm.profileAvatar.collectAsStateWithLifecycle()
    val profileColor by vm.profileColor.collectAsStateWithLifecycle()

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showVoicePanel by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val recorder = rememberAudioRecorder()

    suspend fun load() {
        try {
            val raw = Http.get(CHAT_URL)
            if (raw.isNullOrBlank() || raw == "null") { messages = emptyList(); return }
            val obj = JSONObject(raw)
            val out = mutableListOf<ChatMessage>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val m = obj.optJSONObject(k) ?: continue
                out.add(
                    ChatMessage(
                        id = k,
                        text = m.optString("text", ""),
                        user = m.optString("user", m.optString("name", "مستخدم")),
                        avatar = m.optString("avatar", ""),
                        color = m.optLong("color", 0xFFE50914),
                        timestamp = m.optLong("timestamp", Clock.System.now().toEpochMilliseconds()),
                        imageUrl = m.optString("imageUrl", ""),
                        voiceUrl = m.optString("voiceUrl", ""),
                        voiceDur = m.optInt("voiceDur", 0)
                    )
                )
            }
            messages = out.sortedBy { it.timestamp }
            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
        } catch (_: Exception) { /* keep old list */ }
    }

    LaunchedEffect(Unit) {
        while (true) {
            load()
            isLoading = false
            delay(5_000)
        }
    }

    fun send() {
        val text = draft.trim()
        if (text.isEmpty()) return
        draft = ""
        val payload = JSONObject().apply {
            put("text", text)
            put("user", profileName)
            put("avatar", profileAvatar)
            put("color", profileColor.toLong() and 0xFFFFFFFFL)
            put("timestamp", Clock.System.now().toEpochMilliseconds())
        }.toString()
        scope.launch {
            try {
                Http.client.post {
                    url(CHAT_URL)
                    setBody(payload)
                    header("Content-Type", "application/json")
                }
                load()
            } catch (_: Exception) { }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0B0F))) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF16161C))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "رجوع", tint = Color.White)
            }
            Icon(Icons.Filled.Forum, contentDescription = null, tint = NetflixRed)
            Spacer(Modifier.width(8.dp))
            Text("الدردشة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                modifier = Modifier.weight(1f))
            IconButton(onClick = { scope.launch { load() } }) {
                Icon(Icons.Filled.Refresh, contentDescription = "تحديث", tint = Color.White)
            }
        }

        // Messages
        Box(modifier = Modifier.weight(1f)) {
            if (isLoading && messages.isEmpty()) {
                CircularProgressIndicator(color = NetflixRed,
                    modifier = Modifier.align(Alignment.Center))
            } else if (messages.isEmpty()) {
                Text("لا رسائل بعد — كن أول من يكتب!",
                    color = Color(0xFF8C8C94),
                    modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isOwn = msg.user == profileName
                        MessageBubble(msg = msg, isOwn = isOwn)
                    }
                }
            }
        }

        // Input
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF16161C))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showVoicePanel = !showVoicePanel }) {
                Icon(
                    if (showVoicePanel) Icons.Filled.Close else Icons.Filled.Mic,
                    contentDescription = "صوت",
                    tint = if (showVoicePanel) NetflixRed else Color.White
                )
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("اكتب رسالة…", color = Color(0xFF8C8C94)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF333333),
                    unfocusedBorderColor = Color(0xFF333333),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp)
            )
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = { send() },
                enabled = draft.isNotBlank()
            ) {
                Icon(
                    Icons.Filled.Send,
                    contentDescription = "إرسال",
                    tint = if (draft.isNotBlank()) NetflixRed else Color(0xFF44444C)
                )
            }
        }

        if (showVoicePanel) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF16161C))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ملاحظة صوتية (قيد التفعيل)", color = Color(0xFF8C8C94), fontSize = 12.sp,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = { showVoicePanel = false }) {
                    Text("إغلاق", color = NetflixRed)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage, isOwn: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isOwn && msg.avatar.isNotBlank()) {
            coil3.compose.AsyncImage(
                model = msg.avatar,
                contentDescription = null,
                modifier = Modifier.size(28.dp).clip(CircleShape)
            )
            Spacer(Modifier.width(6.dp))
        }
        Column(
            horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (!isOwn) {
                Text(msg.user, color = Color(0xFFB3B3B3), fontSize = 11.sp)
                Spacer(Modifier.height(2.dp))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isOwn) NetflixRed else Color(0xFF23232B))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(msg.text, color = Color.White, fontSize = 14.sp)
            }
        }
    }
}
