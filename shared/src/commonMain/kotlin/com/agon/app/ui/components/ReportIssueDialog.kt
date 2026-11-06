package com.agon.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agon.app.ui.theme.NetflixRed
import com.agon.app.ui.tvClickable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

object SoothingSoundPlayer {
    /** AudioTrack chime is Android-only; silent on iOS (FILL: AVAudioEngine tone if you want it back). */
    fun playComfortingChime() {}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportIssueDialog(
    initialUserName: String = "",
    deviceType: String = "mobile",
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

    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(horizontal = 20.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                    .shadow(24.dp, RoundedCornerShape(24.dp)),
                color = Color(0xFF12141C),
                shape = RoundedCornerShape(24.dp)
            ) {
                if (isSubmittedSuccess) {
                    // Success View with animated checkmark and soothing chime feedback
                    SuccessAnimatedReportView(
                        onDone = onDismiss
                    )
                } else {
                    // Form View
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(NetflixRed.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.BugReport,
                                        contentDescription = null,
                                        tint = NetflixRed,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "إبلاغ عن مشكلة",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "سنقوم بمراجعتها وحلها فوراً",
                                        color = Color.LightGray.copy(alpha = 0.8f),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "إغلاق",
                                    tint = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Category Chips
                        Text(
                            text = "نوع المشكلة:",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categories.forEach { cat ->
                                val isSelected = selectedCategory == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isSelected) NetflixRed.copy(alpha = 0.25f)
                                            else Color(0xFF1E212E)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) NetflixRed else Color.White.copy(alpha = 0.08f),
                                            RoundedCornerShape(20.dp)
                                        )
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        color = if (isSelected) Color.White else Color.LightGray,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // User Name input
                        Text(
                            text = "اسمك (أو لقبك):",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
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

                        Spacer(modifier = Modifier.height(14.dp))

                        // Problem Description input
                        Text(
                            text = "تفاصيل المشكلة التي واجهتها:",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = problemDescription,
                            onValueChange = { problemDescription = it },
                            placeholder = {
                                Text(
                                    "اكتب اسم المسلسل أو الحلقة أو وصف المشكلة بالتفصيل لنساعدك بشكل أسرع...",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
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
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                if (problemDescription.trim().length < 3) {
                                    errorMessage = "يرجى كتابة وصف المشكلة بشكل أوضح"
                                    return@Button
                                }
                                errorMessage = null
                                isSubmitting = true

                                coroutineScope.launch {
                                    val success = submitReportToFirebase(
                                        userName = userName.trim().ifEmpty { "مستخدم التطبيق" },
                                        problem = problemDescription.trim(),
                                        category = selectedCategory,
                                        deviceType = deviceType
                                    )
                                    isSubmitting = false
                                    if (success) {
                                        SoothingSoundPlayer.playComfortingChime()
                                        isSubmittedSuccess = true
                                    } else {
                                        errorMessage = "حدث خطأ أثناء الإرسال، يرجى المحاولة ثانية"
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NetflixRed,
                                disabledContainerColor = NetflixRed.copy(alpha = 0.5f)
                            )
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("جاري إرسال البلاغ...", color = Color.White, fontSize = 15.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Send,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "إرسال البلاغ للمطورين",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessAnimatedReportView(
    onDone: () -> Unit
) {
    val checkProgress = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.4f) }

    LaunchedEffect(Unit) {
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        delay(200)
        checkProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated Drawing Checkmark inside glowing emerald container
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00E676).copy(alpha = 0.25f),
                            Color(0xFF00C853).copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .border(2.5.dp, Color(0xFF00E676).copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(54.dp)) {
                val path = Path().apply {
                    // Start of checkmark
                    moveTo(size.width * 0.2f, size.height * 0.52f)
                    // Bottom vertex of checkmark
                    lineTo(size.width * 0.42f, size.height * 0.74f)
                    // End tip of checkmark
                    lineTo(size.width * 0.82f, size.height * 0.28f)
                }

                val pathMeasure = PathMeasure()
                pathMeasure.setPath(path, false)
                val totalLength = pathMeasure.length

                val animatedPath = Path()
                pathMeasure.getSegment(0f, totalLength * checkProgress.value, animatedPath, true)

                drawPath(
                    path = animatedPath,
                    color = Color(0xFF00E676),
                    style = Stroke(
                        width = 5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "تم إرسال بلاغك بنجاح",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "شكراً لمساعدتنا في تحسين التطبيق.\nيعمل فريقنا التقني الآن على مراجعة المشكلة وحلها فوراً.",
            color = Color.LightGray,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E212E)
            )
        ) {
            Text(
                text = "إغلاق ومتابعة المشاهدة",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private suspend fun submitReportToFirebase(
    userName: String,
    problem: String,
    category: String,
    deviceType: String
): Boolean {
    fun esc(v: String) = v.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")
    val body = "{\"userName\":\"${esc(userName)}\",\"problem\":\"${esc(problem)}\",\"category\":\"${esc(category)}\"," +
        "\"deviceType\":\"${esc(deviceType)}\",\"timestamp\":${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}," +
        "\"status\":\"open\",\"source\":\"app_home_report\"}"
    return com.agon.app.data.FirebaseHttp.post("https://chat-516a8-default-rtdb.firebaseio.com/reports.json", body) != null
}
