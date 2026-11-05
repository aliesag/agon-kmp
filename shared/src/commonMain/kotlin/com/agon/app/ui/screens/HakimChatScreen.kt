package com.agon.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.agon.app.data.Catalog
import com.agon.app.data.HakimConfigManager
import com.agon.app.data.Show
import com.agon.app.viewmodel.AppViewModel
import kotlinx.coroutines.launch

private val NetflixRed = Color(0xFFE50914)
private val Gold = Color(0xFFF5C518)

data class HakimMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean,
    val linkedShow: Show? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HakimChatScreen(
    vm: AppViewModel,
    onBack: () -> Unit = {},
    onOpenShow: (String) -> Unit = {}
) {
    val shows by vm.shows.collectAsStateWithLifecycle()
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

    fun buildSystemPrompt(matched: Show?): String {
        return if (matched != null) {
            val imdbFormatted = formatImdb(matched.imdb)
            """
            أنت حكيم ✨، المستشار والناقد الفني والذكي الرسمي لتطبيق 'حكايتنا' لمشاهدة الأفلام والمسلسلات.
            تتحدث بذكاء وسلاسة بأسلوب محفز ومرح مع رموز تعبيرية (Emojis) جذابة باللغة العربية.

            المعلومات الخاصة بالعمل المرفق مع بطاقة المشاهدة للمستخدم:
            - اسم العمل: "${matched.title}"
            - التصنيف: ${matched.genres.joinToString("، ")}
            - سنة الإصدار: ${matched.year}
            - التقييم الحالي: $imdbFormatted من 10 ⭐
            - القصة: "${matched.description.ifEmpty { "عمل درامي وترفيهي مميز يعرض بجودة عالية على حكايتنا" }}"

            ⚠️ تعليمات صارمة:
            1. إذا كان سؤال المستخدم عن تقييم العمل، أو قصته، أو أبطاله، أجب بدقة وعاطفة عن "${matched.title}".
            2. أذكر دائماً اسم العمل بوضوح ووجه المستخدم بحماس إلى زر (اذهب للمشاهدة) الموجود ببطاقة العمل أسفل رسالتك فوراً!
            3. كن مختصراً، ذكياً، وواضحاً بدون أي رموز غريبة أو سطور برمجية.
            """.trimIndent()
        } else {
            val sampleShows = shows.filter { !it.isMovie }.take(10).joinToString("، ") { it.title }
            """
            أنت حكيم ✨، المستشار والناقد الفني والذكي لتطبيق 'حكايتنا'.
            تحدث بأسلوب ممتع، ترحيبي، وذكي مع إدراج Emojis جذابة.
            إذا رحب بك المستخدم (مثل: سلام عليكم، أهلاً، مرحباً)، رد عليه بتحية حارة وودودة واعرض عليه مساعدتك واقتراح أفضل المسلسلات له.
            إذا سألك عن اقتراحات، اقترح له أعمالاً مميزة من مكتبتنا مثل: $sampleShows.
            لا ترفق بطاقة عمل إلا عندما يتم تخصيص مسلسل محدد في المحادثة.
            """.trimIndent()
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

        // Show a live status message while we fetch
        val statusId = (messages.maxOfOrNull { it.id } ?: 0) + 1
        messages = messages + HakimMessage(
            id = statusId,
            text = "🔍 جاري البحث عن العمل...",
            isUser = false
        )

        scope.launch {
            try {
                vm.performRemoteSearch(text)
                kotlinx.coroutines.delay(4000)
            } catch (_: Throwable) { }

            val currentShows = vm.shows.value
            val matched = try { Catalog.findMatchingShow(text, currentShows) } catch (_: Exception) { null }

            // Remove the status bubble
            messages = messages.filterNot { it.id == statusId }

            val prompt = buildSystemPrompt(matched)

            val reply = try {
                HakimConfigManager.askHakim(text, systemPrompt = prompt)
                    ?: "تعذر الاتصال بحكيم حالياً، حاول مجدداً بعد قليل."
            } catch (e: Exception) {
                "تعذر الاتصال بحكيم: ${e.message ?: "خطأ غير معروف"}"
            }

            val cleaned = reply
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .trim()

            // Fallback: if direct match failed, try finding any show title mentioned in the reply
            val finalMatched = matched ?: try {
                shows.firstOrNull { s ->
                    s.title.length >= 4 && cleaned.contains(s.title.substring(0, minOf(8, s.title.length)), ignoreCase = true)
                }
            } catch (_: Throwable) { null }

            val aiMsg = HakimMessage(
                id = (messages.maxOfOrNull { it.id } ?: 0) + 1,
                text = cleaned,
                isUser = false,
                linkedShow = finalMatched
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
            IconButton(onClick = { messages = emptyList() }) {
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
                Column(modifier = Modifier.fillMaxWidth()) {
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

                    // Linked show card (like Android)
                    val linked = msg.linkedShow
                    if (linked != null) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .padding(start = 34.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF16161C))
                                .clickable { onOpenShow(linked.id) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(width = 46.dp, height = 66.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF23232B))
                            ) {
                                AsyncImage(
                                    model = linked.poster,
                                    contentDescription = linked.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(linked.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(
                                    "${linked.year} • ${linked.rating}",
                                    color = Color(0xFF8C8C94), fontSize = 11.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = Gold, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text(formatImdb(linked.imdb), color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NetflixRed)
                                    .clickable { onOpenShow(linked.id) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("اذهب للمشاهدة", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            if (isSending) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        CircularProgressIndicator(color = Gold, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
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

private fun formatImdb(v: Double): String {
    val scaled = (v * 10.0 + 0.5).toInt()
    return "${scaled / 10}.${scaled % 10}"
}
