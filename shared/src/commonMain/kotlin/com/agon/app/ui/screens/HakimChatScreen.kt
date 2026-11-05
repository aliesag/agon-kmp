package com.agon.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agon.app.data.HakimConfigManager
import com.agon.app.viewmodel.AppViewModel
import kotlinx.coroutines.launch

private val NetflixRed = Color(0xFFE50914)
private val Gold = Color(0xFFF5C518)

data class HakimMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HakimChatScreen(
    vm: AppViewModel,
    onBack: () -> Unit = {},
    onOpenShow: (String) -> Unit = {}
) {
    var messages by remember { mutableStateOf<List<HakimMessage>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            messages = listOf(
                HakimMessage(
                    id = 0,
                    text = "أهلاً بك 👋 أنا حكيم، مستشارك السينمائي في حكايتنا. اسألني عن أي مسلسل أو فيلم، أو قل لي ماذا تحب وأقترح لك ما يناسبك.",
                    isUser = false
                )
            )
        }
    }

    fun send() {
        val text = draft.trim()
        if (text.isEmpty() || isSending) return
        draft = ""
        val userMsg = HakimMessage(
            id = (messages.maxOfOrNull { it.id } ?: 0) + 1,
            text = text,
            isUser = true
        )
        messages = messages + userMsg
        isSending = true

        scope.launch {
            val reply = try {
                HakimConfigManager.askHakim(text) ?: "تعذر الاتصال بحكيم حالياً، حاول مجدداً بعد قليل."
            } catch (e: Exception) {
                "تعذر الاتصال بحكيم: ${e.message ?: "خطأ غير معروف"}"
            }
            val aiMsg = HakimMessage(
                id = (messages.maxOfOrNull { it.id } ?: 0) + 1,
                text = reply,
                isUser = false
            )
            messages = messages + aiMsg
            isSending = false
            try { listState.animateScrollToItem(messages.size - 1) } catch (_: Exception) {}
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0B0F))) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF16161C))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "رجوع", tint = Color.White)
            }
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(50))
                    .background(Brush.linearGradient(listOf(Gold, NetflixRed))),
                contentAlignment = Alignment.Center
            ) {
                Text("ح", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("حكيم", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("مستشارك السينمائي", color = Color(0xFF8C8C94), fontSize = 11.sp)
            }
            IconButton(onClick = {
                messages = emptyList()
            }) {
                Icon(Icons.Filled.Delete, contentDescription = "مسح", tint = Color(0xFF8C8C94))
            }
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!msg.isUser) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(50))
                                .background(Brush.linearGradient(listOf(Gold, NetflixRed))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("ح", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (msg.isUser) NetflixRed else Color(0xFF23232B))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(msg.text, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
            if (isSending) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        CircularProgressIndicator(
                            color = Gold, strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("حكيم يكتب…", color = Color(0xFF8C8C94), fontSize = 12.sp)
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
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("اسأل حكيم…", color = Color(0xFF8C8C94)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF333333),
                    unfocusedBorderColor = Color(0xFF333333),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                enabled = !isSending
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = { send() }, enabled = draft.isNotBlank() && !isSending) {
                Icon(
                    Icons.Filled.Send,
                    contentDescription = "إرسال",
                    tint = if (draft.isNotBlank() && !isSending) Gold else Color(0xFF44444C)
                )
            }
        }
    }
}
