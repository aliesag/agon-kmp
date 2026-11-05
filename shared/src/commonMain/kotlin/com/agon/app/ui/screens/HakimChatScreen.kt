package com.agon.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.agon.app.data.Catalog
import com.agon.app.data.HakimConfigManager
import com.agon.app.data.Show
import com.agon.app.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val HakimRed = Color(0xFFE50914)
private val HakimGold = Color(0xFFFFC107)

data class HakimMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean,
    val linkedShow: Show? = null,
)

private fun findLastDiscussedShowInKmp(messages: List<HakimMessage>, allShows: List<Show>): Show? {
    for (msg in messages.asReversed()) {
        val ls = msg.linkedShow ?: continue
        return allShows.firstOrNull { it.id == ls.id || it.title.equals(ls.title, ignoreCase = true) } ?: ls
    }
    return null
}

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
    var thinkingStatus by remember { mutableStateOf("حكيم يكتب...") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun buildSystemPrompt(matched: Show?): String = if (matched != null) {
        """
أنت حكيم ✨، المستشار والناقد الفني والذكي الرسمي لتطبيق 'حكايتنا' لمشاهدة الأفلام والمسلسلات.
تتحدث بذكاء وسلاسة بأسلوب محفز ومرح مع رموز تعبيرية (Emojis) جذابة باللغة العربية.

المعلومات الخاصة بالعمل المرفق مع بطاقة المشاهدة للمستخدم:
- اسم العمل: "${matched.title}"
- التصنيف: ${matched.genres.joinToString("، ")}
- سنة الإصدار: ${matched.year}
- التقييم الحالي: ${formatImdb(matched.imdb)} من 10 ⭐
- القصة: "${matched.description.ifEmpty { "عمل درامي وترفيهي مميز يعرض بجودة عالية على حكايتنا" }}"

⚠️ تعليمات صارمة للمستشار حكيم:
1. إذا كان سؤال المستخدم عن تقييم العمل، أو قصته، أو أبطاله، أجب بدقة وعاطفة وإيجاز عن "${matched.title}".
2. لا تخلط أبداً بين هذا العمل وأي عمل آخر، واستند فقط إلى البيانات الرسمية المرفقة أعلاه.
3. اذكر دائماً اسم العمل بوضوح ووجه المستخدم بحماس للضغط على زر (اذهب للمشاهدة) الموجود في بطاقة العمل أسفل ردك مباشرة.
4. كن مختصراً، ذكياً، وواضحاً وخالياً تماماً من أي رموز برمجية أو نصوص شاذة.
        """.trimIndent()
    } else {
        val sampleShows = shows.filter { !it.isMovie }.take(10).joinToString("، ") { it.title }
        """
أنت حكيم ✨، المستشار والناقد الفني والذكي لتطبيق 'حكايتنا'.
تحدث بأسلوب ممتع، ترحيبي، وذكي مع إدراج Emojis جذابة.
إذا رحب بك المستخدم (مثل: سلام عليكم، أهلاً، مرحباً)، رد عليه بتحية حارة وودودة واعرض عليه مساعدتك واقتراح أفضل المسلسلات له.
إذا سألك عن اقتراحات عامة، اقترح له أعمالاً مميزة وحصرية من مكتبتنا مثل: $sampleShows.
لا ترفق بطاقة عمل إلا عندما يتم تخصيص مسلسل أو فيلم محدد في المحادثة.
        """.trimIndent()
    }

    fun startNewChat() {
        messages = emptyList()
        draft = ""
    }

    fun send(textOverride: String? = null) {
        val text = (textOverride ?: draft).trim()
        if (text.isEmpty() || isSending) return
        draft = ""

        val userMsg = HakimMessage(
            id = (messages.maxOfOrNull { it.id } ?: 0) + 1,
            text = text,
            isUser = true
        )
        messages = messages + userMsg
        isSending = true
        thinkingStatus = "حكيم يفكر..."

        scope.launch {
            try { listState.animateScrollToItem(messages.lastIndex) } catch (_: Exception) { }

            val isGreeting = try { Catalog.isGeneralGreetingOrConversation(text) } catch (_: Throwable) { false }
            val isFollowUp = try { Catalog.isFollowUpAboutContextShow(text) } catch (_: Throwable) { false }

            val contextShow = if (isFollowUp && !isGreeting) {
                try { findLastDiscussedShowInKmp(messages, vm.shows.value) } catch (_: Throwable) { null }
            } else null

            val localMatch = if (contextShow == null && !isGreeting) {
                try { Catalog.findMatchingShow(text, vm.shows.value) } catch (_: Throwable) { null }
            } else contextShow

            // Remote fallback
            if (localMatch == null && !isGreeting && !isFollowUp) {
                thinkingStatus = "🔍 جاري البحث عن العمل..."
                try { vm.performRemoteSearch(text) } catch (_: Throwable) { }
                delay(4000)
            }

            val finalMatch = localMatch ?: if (!isGreeting) {
                try { Catalog.findMatchingShow(text, vm.shows.value) } catch (_: Throwable) { null }
            } else null

            thinkingStatus = "حكيم يجهز لك الرد..."

            val prompt = buildSystemPrompt(finalMatch)
            val reply = try {
                HakimConfigManager.askHakim(text, systemPrompt = prompt)
                    ?: "تعذر الاتصال بحكيم حالياً، حاول مجدداً بعد قليل."
            } catch (e: Exception) {
                "تعذر الاتصال بحكيم: ${e.message ?: "خطأ غير معروف"}"
            }

            val cleaned = reply.replace("\\n", "\n").replace("\\\"", "\"").trim()

            messages = messages + HakimMessage(
                id = (messages.maxOfOrNull { it.id } ?: 0) + 1,
                text = cleaned,
                isUser = false,
                linkedShow = finalMatch
            )
            isSending = false
            try { listState.animateScrollToItem(messages.lastIndex) } catch (_: Exception) { }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0B0F))) {

        // ═══════ Top Bar (matched Android) ═══════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131620))
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "رجوع", tint = Color.White)
            }

            // New chat button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF1E222A))
                    .border(1.dp, Color(0xFF2E3440), RoundedCornerShape(18.dp))
                    .clickable { startNewChat() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, contentDescription = "محادثة جديدة", tint = Color(0xFFFF5252), modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("محادثة جديدة", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.weight(1f))

            // Profile block
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF4A1515))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("مستشار\nالدراما", color = Color(0xFFFF2D3A), fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 10.sp)
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("حكيم الذكي", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.FlashOn, contentDescription = null, tint = HakimGold, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("متصل الآن ومستعد لمساعدتك", color = Color.LightGray, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFE91E63), Color(0xFF9C27B0)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AutoFixHigh, contentDescription = "Avatar", tint = Color.White, modifier = Modifier.size(24.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676))
                            .border(1.5.dp, Color(0xFF131620), CircleShape)
                    )
                }
            }
        }

        // ═══════ Body ═══════
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty() && !isSending) {
                // Welcome + Suggestion chips
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(HakimRed.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Forum, contentDescription = null, tint = HakimRed, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "دردش مع حكيم منصة حكايتنا!",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "اسأل حكيم عن تفاصيل الفيلم الذي تفكر فيه، أو ارفع لقطة شاشة من مشهد يعجبك، أو تحدث صوتياً لمعرفة العمل فوراً!",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(32.dp))

                        val suggestions = listOf(
                            "اقترح لي فيلم تركي رومانسي وحزين 💖",
                            "عندكم فيلم المتوحش؟ عطني قصته 🐺",
                            "أبي فيلم أكشن وحرب تاريخي رهيب ⚔️",
                            "دلني على فيلم درامي عائلي راقي 📺"
                        )

                        suggestions.forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color(0xFF1E1E1E))
                                    .clickable { send(suggestion) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                Text(suggestion, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        MessageItem(msg = msg, onOpenShow = onOpenShow)
                    }
                    if (isSending) {
                        item { HakimThinkingItem(statusText = thinkingStatus) }
                    }
                }
            }
        }

        // ═══════ Input Bar ═══════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131620))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("اسأل حكيم…", color = Color(0xFF8C8C94), fontSize = 14.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2E3440),
                    unfocusedBorderColor = Color(0xFF2E3440),
                    focusedContainerColor = Color(0xFF1E222A),
                    unfocusedContainerColor = Color(0xFF1E222A),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                enabled = !isSending,
                maxLines = 4
            )
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (draft.isNotBlank() && !isSending) HakimRed else Color(0xFF2E3440))
                    .clickable(enabled = draft.isNotBlank() && !isSending) { send() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Send,
                    contentDescription = "إرسال",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageItem(msg: HakimMessage, onOpenShow: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!msg.isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFE91E63), Color(0xFF9C27B0)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AutoFixHigh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (msg.isUser) HakimRed else Color(0xFF1E222A))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(msg.text, color = Color.White, fontSize = 14.sp, lineHeight = 21.sp)
            }
        }

        val linked = msg.linkedShow
        if (linked != null && !msg.isUser) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .padding(start = 40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1A1D29))
                    .border(1.dp, Color(0xFF2E3440), RoundedCornerShape(14.dp))
                    .clickable { onOpenShow(linked.id) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 80.dp)
                        .clip(RoundedCornerShape(8.dp))
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
                    Text(linked.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text("${linked.year} • ${linked.rating}", color = Color(0xFF8C8C94), fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = HakimGold, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(formatImdb(linked.imdb), color = HakimGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(HakimRed)
                        .clickable { onOpenShow(linked.id) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("اذهب للمشاهدة", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HakimThinkingItem(statusText: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFE91E63), Color(0xFF9C27B0)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.AutoFixHigh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E222A))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(color = HakimGold, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(statusText, color = Color.LightGray, fontSize = 12.sp)
        }
    }
}

private fun formatImdb(v: Double): String {
    val scaled = (v * 10.0 + 0.5).toInt()
    return "${scaled / 10}.${scaled % 10}"
}
