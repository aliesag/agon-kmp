package com.agon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agon.app.Http
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.json.JSONObject

private val NetflixRed = Color(0xFFE50914)
private const val REPORTS_URL = "https://chat-516a8-default-rtdb.firebaseio.com/reports_v1.json"

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReportIssueDialog(
    initialUserName: String = "",
    deviceType: String = "mobile",
    showId: String = "",
    onDismiss: () -> Unit
) {
    var userName by remember { mutableStateOf(initialUserName) }
    var problemDescription by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("مشكلة في تشغيل سيرفر") }
    var isSubmitting by remember { mutableStateOf(false) }
    var isSubmittedSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        "مشكلة في تشغيل سيرفر",
        "حلقة لا تعمل",
        "جودة الفيديو أو الترجمة",
        "مشكلة في شاشة التلفزيون",
        "بطء أو تقطيع",
        "اقتراح وملاحظة عامة"
    )

    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(horizontal = 20.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                    .shadow(24.dp, RoundedCornerShape(24.dp)),
                color = Color(0xFF12141C),
                shape = RoundedCornerShape(24.dp)
            ) {
                if (isSubmittedSuccess) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(72.dp).clip(CircleShape)
                                .background(Color(0xFF00E676).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(40.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("تم استلام بلاغك", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("شكراً لك! سنراجع البلاغ ونعمل على حلّه في أسرع وقت.", color = Color.LightGray, fontSize = 14.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("تم", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth().padding(22.dp).verticalScroll(rememberScrollState())) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape)
                                        .background(NetflixRed.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.BugReport, contentDescription = null, tint = NetflixRed, modifier = Modifier.size(22.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("إبلاغ عن مشكلة", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text("سنقوم بمراجعتها وحلها فوراً", color = Color.LightGray.copy(alpha = 0.8f), fontSize = 12.sp)
                                }
                            }
                            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = Color.Gray)
                            }
                        }

                        Spacer(Modifier.height(18.dp))
                        Text("نوع المشكلة:", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categories.forEach { cat ->
                                val sel = selectedCategory == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (sel) NetflixRed.copy(alpha = 0.25f) else Color(0xFF1E212E))
                                        .border(1.dp, if (sel) NetflixRed else Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(cat, color = if (sel) Color.White else Color.LightGray, fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("اسمك (أو لقبك):", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = userName,
                            onValueChange = { userName = it },
                            placeholder = { Text("مثال: عبد الله", color = Color.Gray, fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1A1D29),
                                unfocusedContainerColor = Color(0xFF1A1D29),
                                focusedBorderColor = NetflixRed,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(Modifier.height(14.dp))
                        Text("تفاصيل المشكلة التي واجهتها:", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = problemDescription,
                            onValueChange = { problemDescription = it },
                            placeholder = {
                                Text("اكتب اسم المسلسل أو الحلقة أو وصف المشكلة بالتفصيل...", color = Color.Gray, fontSize = 13.sp)
                            },
                            minLines = 4,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1A1D29),
                                unfocusedContainerColor = Color(0xFF1A1D29),
                                focusedBorderColor = NetflixRed,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        if (errorMessage != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(errorMessage ?: "", color = Color(0xFFFF5252), fontSize = 12.sp)
                        }

                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = {
                                if (problemDescription.trim().length < 3) {
                                    errorMessage = "يرجى كتابة وصف المشكلة بشكل أوضح"
                                    return@Button
                                }
                                errorMessage = null
                                isSubmitting = true
                                scope.launch {
                                    val payload = JSONObject().apply {
                                        put("userName", userName.trim().ifEmpty { "مستخدم التطبيق" })
                                        put("problem", problemDescription.trim())
                                        put("category", selectedCategory)
                                        put("deviceType", deviceType)
                                        put("timestamp", Clock.System.now().toEpochMilliseconds())
                                    }.toString()
                                    val ok = try {
                                        Http.client.post {
                                            url(REPORTS_URL)
                                            setBody(payload)
                                            header("Content-Type", "application/json")
                                        }
                                        true
                                    } catch (_: Exception) { false }
                                    isSubmitting = false
                                    if (ok) isSubmittedSuccess = true
                                    else errorMessage = "حدث خطأ أثناء الإرسال، يرجى المحاولة ثانية"
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NetflixRed,
                                disabledContainerColor = NetflixRed.copy(alpha = 0.5f)
                            )
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                                Spacer(Modifier.width(10.dp))
                                Text("جاري إرسال البلاغ...", color = Color.White, fontSize = 15.sp)
                            } else {
                                Icon(Icons.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("إرسال البلاغ للمطورين", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
