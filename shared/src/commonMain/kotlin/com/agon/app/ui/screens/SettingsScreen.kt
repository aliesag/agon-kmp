package com.agon.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.agon.app.picker.rememberImagePicker
import com.agon.app.ui.AuthState
import com.agon.app.ui.AuthViewModel
import com.agon.app.utils.ImageUploader
import com.agon.app.viewmodel.AppViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private val NetflixRed = Color(0xFFE50914)
private val BgColor = Color(0xFF0B0B0F)
private val SurfaceColor = Color(0xFF16161C)
private val Grey = Color(0xFF8C8C94)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: AppViewModel,
    authViewModel: AuthViewModel? = null,
    onLogout: (() -> Unit)? = null
) {
    val profileName by vm.profileName.collectAsStateWithLifecycle()
    val profileAvatar by vm.profileAvatar.collectAsStateWithLifecycle()
    val profileColor by vm.profileColor.collectAsStateWithLifecycle()
    val profileAge by vm.profileAge.collectAsStateWithLifecycle()
    val autoUpdate by vm.autoUpdate.collectAsStateWithLifecycle()
    val showContinueWatching by vm.showContinueWatching.collectAsStateWithLifecycle()
    val deviceType by vm.deviceType.collectAsStateWithLifecycle()
    val skipIntro by vm.skipIntro.collectAsStateWithLifecycle()
    val playerAutoHide by vm.playerAutoHide.collectAsStateWithLifecycle()
    val playerHideSeconds by vm.playerHideSeconds.collectAsStateWithLifecycle()
    val userEmail by vm.userEmail.collectAsStateWithLifecycle()
    val wifiOnly by vm.wifiOnly.collectAsStateWithLifecycle()
    val notifications by vm.notifications.collectAsStateWithLifecycle()
    val autoplay by vm.autoplay.collectAsStateWithLifecycle()
    val appLanguage by vm.appLanguage.collectAsStateWithLifecycle()

    val authState by (authViewModel?.authState ?: MutableStateFlow<AuthState>(AuthState.Idle)).collectAsStateWithLifecycle()
    val user = (authState as? AuthState.Success)?.user
    val displayName = user?.displayName?.takeIf { it.isNotBlank() } ?: profileName
    val displayEmail = user?.email?.takeIf { it.isNotBlank() } ?: userEmail

    val scope = rememberCoroutineScope()
    var isUploadingAvatar by remember { mutableStateOf(false) }

    val picker = rememberImagePicker { bytes ->
        isUploadingAvatar = true
        scope.launch {
            val url = ImageUploader.uploadImageBytes(bytes)
            if (url != null) vm.setProfileAvatar(url)
            isUploadingAvatar = false
        }
    }

    var showProfileDialog by remember { mutableStateOf(false) }
    var showDownloadsDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showSecondsDialog by remember { mutableStateOf(false) }
    var showDeviceDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgColor),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Profile card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceColor)
                    .clickable { showProfileDialog = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape)
                        .background(Color(profileColor)),
                    contentAlignment = Alignment.Center
                ) {
                    if (profileAvatar.isNotBlank()) {
                        AsyncImage(
                            model = profileAvatar,
                            contentDescription = "avatar",
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Text(
                            displayName.take(1).ifEmpty { "؟" },
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    if (isUploadingAvatar) {
                        Box(
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(color = NetflixRed, strokeWidth = 2.dp, modifier = Modifier.size(24.dp)) }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(displayName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (displayEmail.isNotBlank())
                        Text("@$displayEmail", color = Grey, fontSize = 12.sp)
                    if (profileAge.isNotBlank())
                        Text("العمر: $profileAge سنة", color = Grey, fontSize = 12.sp)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Grey)
            }
        }

        item { SettingSectionTitle("المحتوى") }

        item {
            NavRow(Icons.Filled.Download, "الحلقات المحملة") { showDownloadsDialog = true }
        }
        item {
            NavRow(Icons.Filled.History, "سجل المشاهدة") { showHistoryDialog = true }
        }

        item { SettingSectionTitle("التشغيل") }

        item {
            NavRow(Icons.Filled.Timer, "مدة اختفاء أدوات المشغل (${playerHideSeconds} ثانية)") { showSecondsDialog = true }
        }
        item {
            ToggleRow(Icons.Filled.AccessTime, "إخفاء تلقائي للأدوات", playerAutoHide) { vm.setPlayerAutoHide(it) }
        }
        item {
            ToggleRow(Icons.Filled.Star, "التشغيل التلقائي للحلقة التالية", autoplay) { vm.setAutoplay(it) }
        }
        item {
            ToggleRow(Icons.Filled.Check, "تخطي الإنترو", skipIntro) { vm.setSkipIntro(it) }
        }
        item {
            ToggleRow(Icons.Filled.History, "متابعة المشاهدة", showContinueWatching) { vm.setShowContinueWatching(it) }
        }

        item { SettingSectionTitle("البيانات والإشعارات") }

        item {
            ToggleRow(Icons.Filled.Wifi, "التنزيل على Wi-Fi فقط", wifiOnly) { vm.setWifiOnly(it) }
        }
        item {
            ToggleRow(Icons.Filled.Notifications, "الإشعارات", notifications) { vm.setNotifications(it) }
        }
        item {
            ToggleRow(Icons.Filled.Check, "التحديث التلقائي", autoUpdate) { vm.setAutoUpdate(it) }
        }

        item { SettingSectionTitle("التخصيص") }

        item {
            NavRow(Icons.Filled.Language, "لغة التطبيق: ${languageLabel(appLanguage)}") { showLanguageDialog = true }
        }
        item {
            NavRow(Icons.Filled.Devices, "نوع الجهاز: ${deviceLabel(deviceType)}") { showDeviceDialog = true }
        }

        item { SettingSectionTitle("الحساب") }

        if (user != null || profileName != "ضيف") {
            item {
                NavRow(Icons.Filled.Close, "تسجيل الخروج") { showLogoutDialog = true }
            }
        }

        item { SettingSectionTitle("حول") }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("الإصدار", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(com.agon.app.BuildConfig.VERSION_NAME, color = Grey, fontSize = 13.sp)
            }
        }
    }

    // ---- Dialogs ----
    if (showProfileDialog) {
        var name by remember { mutableStateOf(profileName) }
        var age by remember { mutableStateOf(profileAge) }
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            containerColor = SurfaceColor,
            title = { Text("تعديل الملف الشخصي", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("اسم المستخدم") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = age, onValueChange = { age = it },
                        label = { Text("العمر") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { picker.pick() },
                        colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("تغيير الصورة")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        vm.setProfileInfo(name, profileColor, profileAvatar)
                        vm.setProfileAge(age)
                    }
                    showProfileDialog = false
                }) { Text("حفظ", color = NetflixRed) }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) { Text("إلغاء", color = Grey) }
            }
        )
    }

    if (showDownloadsDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadsDialog = false },
            containerColor = SurfaceColor,
            title = { Text("الحلقات المحملة", color = Color.White) },
            text = { Text("التنزيل الفعلي عبر URLSession سيُفعّل قريباً.", color = Grey) },
            confirmButton = { TextButton(onClick = { showDownloadsDialog = false }) { Text("حسناً", color = NetflixRed) } }
        )
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            containerColor = SurfaceColor,
            title = { Text("سجل المشاهدة", color = Color.White) },
            text = { Text("سيتم تفعيل سجل المشاهدة الكامل قريباً.", color = Grey) },
            confirmButton = { TextButton(onClick = { showHistoryDialog = false }) { Text("حسناً", color = NetflixRed) } }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = SurfaceColor,
            title = { Text("لغة التطبيق", color = Color.White) },
            text = {
                Column {
                    listOf("ar" to "العربية", "en" to "English").forEach { (code, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                scope.launch { vm.setLanguage(code) }
                                showLanguageDialog = false
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = appLanguage == code, onClick = {
                                scope.launch { vm.setLanguage(code) }
                                showLanguageDialog = false
                            })
                            Text(label, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text("إغلاق", color = NetflixRed) } }
        )
    }

    if (showSecondsDialog) {
        AlertDialog(
            onDismissRequest = { showSecondsDialog = false },
            containerColor = SurfaceColor,
            title = { Text("مدة الاختفاء", color = Color.White) },
            text = {
                Column {
                    listOf(3, 5, 10, 15, 20, 30).forEach { sec ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                scope.launch { vm.setPlayerHideSeconds(sec) }
                                showSecondsDialog = false
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = playerHideSeconds == sec, onClick = {
                                scope.launch { vm.setPlayerHideSeconds(sec) }
                                showSecondsDialog = false
                            })
                            Text("$sec ثانية", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSecondsDialog = false }) { Text("إغلاق", color = NetflixRed) } }
        )
    }

    if (showDeviceDialog) {
        AlertDialog(
            onDismissRequest = { showDeviceDialog = false },
            containerColor = SurfaceColor,
            title = { Text("نوع الجهاز", color = Color.White) },
            text = {
                Column {
                    listOf(
                        "mobile" to "جوال — واجهة عمودية",
                        "tv" to "شاشة TV — تنقل بالريموت",
                        "tablet" to "تابلت / آيباد — عرض عريض"
                    ).forEach { (code, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                scope.launch { vm.setDeviceType(code) }
                                showDeviceDialog = false
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = deviceType == code, onClick = {
                                scope.launch { vm.setDeviceType(code) }
                                showDeviceDialog = false
                            })
                            Text(label, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDeviceDialog = false }) { Text("إغلاق", color = NetflixRed) } }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = SurfaceColor,
            title = { Text("تسجيل الخروج", color = Color.White) },
            text = { Text("هل تريد تسجيل الخروج؟", color = Grey) },
            confirmButton = {
                TextButton(onClick = {
                    authViewModel?.logout()
                    showLogoutDialog = false
                    onLogout?.invoke()
                }) { Text("تسجيل الخروج", color = NetflixRed) }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("إلغاء", color = Grey) } }
        )
    }
}

private fun languageLabel(code: String) = when (code) {
    "ar" -> "العربية"
    "en" -> "English"
    else -> "العربية"
}

private fun deviceLabel(code: String) = when (code) {
    "tv" -> "شاشة TV"
    "tablet" -> "تابلت / آيباد"
    else -> "جوال"
}

@Composable
private fun SettingSectionTitle(title: String) {
    Text(
        title,
        color = NetflixRed,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun ToggleRow(icon: ImageVector, label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceColor)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Grey, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NetflixRed)
        )
    }
}

@Composable
private fun NavRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Grey, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Grey)
    }
}
