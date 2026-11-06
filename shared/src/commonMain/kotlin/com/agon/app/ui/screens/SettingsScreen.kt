package com.agon.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.TabletMac
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import com.agon.app.ui.tvClickable
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.agon.app.utils.ImageUploader
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import com.agon.app.data.Catalog
import com.agon.app.ui.components.PosterArt
import com.agon.app.viewmodel.AppViewModel
import com.agon.app.ui.AuthViewModel
import com.agon.app.ui.AuthState
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun SettingsScreen(vm: AppViewModel, authViewModel: AuthViewModel? = null, onLogout: (() -> Unit)? = null) {
    val wifiOnly by vm.wifiOnly.collectAsState()
    val notifications by vm.notifications.collectAsState()
    val autoplay by vm.autoplay.collectAsState()
    val profileName by vm.profileName.collectAsState()
    val profileColorVal by vm.profileColor.collectAsState()
    val downloads by vm.downloads.collectAsState()
    val watchHistory by vm.watchHistory.collectAsState()
    val appLanguage by vm.appLanguage.collectAsState()
    val profileAvatar by vm.profileAvatar.collectAsState()
    val profileAge by vm.profileAge.collectAsState()
    val autoUpdate by vm.autoUpdate.collectAsState()
    val showContinueWatching by vm.showContinueWatching.collectAsState()
    val deviceType by vm.deviceType.collectAsState()
    val skipIntro by vm.skipIntro.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var tempAvatarUrl by remember { mutableStateOf("") }
    val avatarPicker = com.agon.app.platform.rememberMediaPicker { media ->
        if (media != null) {
            isUploadingAvatar = true
            coroutineScope.launch {
                val uploadedUrl: String? = ImageUploader.uploadImageBytes(media.bytes)
                if (uploadedUrl != null) {
                    tempAvatarUrl = uploadedUrl
                    vm.setProfileAvatar(uploadedUrl)
                }
                isUploadingAvatar = false
            }
        }
    }

    var showProfileDialog by remember { mutableStateOf(false) }
    var showDownloadsDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var showSecondsDialog by remember { mutableStateOf(false) }
    var showDeviceDialog by remember { mutableStateOf(false) }
    var selectedLang by remember { mutableStateOf("") }

    val playerAutoHide by vm.playerAutoHide.collectAsState()
    val playerHideSeconds by vm.playerHideSeconds.collectAsState()

    val userEmail by vm.userEmail.collectAsState()
    val authState by (authViewModel?.authState ?: MutableStateFlow(AuthState.Idle)).collectAsState()
    val user = (authState as? AuthState.Success)?.user
    
    val displayName = user?.displayName?.takeIf { it.isNotBlank() } ?: profileName
    val displayEmail = user?.email?.takeIf { it.isNotBlank() } ?: userEmail
    val displayUsername = com.agon.app.platform.UserPrefs.getString("username", "")
    val displayAge = com.agon.app.platform.UserPrefs.getString("age", profileAge)

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        tempAvatarUrl = profileAvatar
                        showProfileDialog = true 
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(profileColorVal), Color(profileColorVal).copy(alpha = 0.6f))
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (profileAvatar.isNotEmpty()) {
                        coil3.compose.AsyncImage(
                            model = profileAvatar,
                            contentDescription = "الصورة الشخصية",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }
                Spacer(Modifier.size(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(displayName, color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (displayUsername.isNotEmpty()) {
                        Text("@$displayUsername", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    }
                    if (displayEmail.isNotEmpty()) {
                        Text(displayEmail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                    if (displayAge.isNotEmpty()) {
                        Text("العمر: $displayAge سنة", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "تعديل",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }

        if (displayEmail.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "التطبيق مجاني بالكامل 🍿",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "سجل اسمك وبريدك الإلكتروني فقط لتتمكن من التفاعل في الشات",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { 
                                tempAvatarUrl = profileAvatar
                                showProfileDialog = true 
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("تسجيل للشات", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { SettingSectionTitle("التفضيلات") }
        item {
            val deviceLabel = when (deviceType) {
                "tv" -> "شاشة TV"
                "tablet" -> "تابلت / آيباد"
                "mobile" -> "جوال"
                else -> "تلقائي"
            }
            NavRow(
                Icons.Filled.Devices,
                "نوع واجهة الجهاز ($deviceLabel)"
            ) {
                showDeviceDialog = true
            }
        }
        item {
            ToggleRow(Icons.Filled.Wifi, "التنزيل عبر الواي فاي فقط", wifiOnly) { vm.setWifiOnly(it) }
        }
        item {
            ToggleRow(Icons.Filled.Notifications, "الإشعارات", notifications) { vm.setNotifications(it) }
        }
        item {
            ToggleRow(Icons.Filled.HighQuality, "التشغيل التلقائي للفيلم التالي", autoplay) { vm.setAutoplay(it) }
        }
        item {
            ToggleRow(Icons.Filled.History, "عرض متابعة المشاهدة", showContinueWatching) { vm.setShowContinueWatching(it) }
        }
        item {
            ToggleRow(Icons.Filled.Star, "التحديث التلقائي", autoUpdate) { vm.setAutoUpdate(it) }
        }
        item {
            ToggleRow(Icons.Filled.Timer, "تخطي مقدمة التطبيق (الانترو)", skipIntro) { vm.setSkipIntro(it) }
        }

        item { SettingSectionTitle("إعدادات المشغل") }
        item {
            ToggleRow(
                Icons.Filled.Timer, 
                "إخفاء تلقائي للتحكم والقائمة", 
                playerAutoHide
            ) { vm.setPlayerAutoHide(it) }
        }
        item {
            NavRow(
                Icons.Filled.AccessTime, 
                "مدة الاختفاء ($playerHideSeconds ثواني)"
            ) {
                showSecondsDialog = true
            }
        }

        item { SettingSectionTitle("الحساب") }
        item {
            NavRow(Icons.Filled.Person, "إدارة الملف الشخصي") {
                tempAvatarUrl = profileAvatar
                showProfileDialog = true
            }
        }
        item {
            NavRow(
                Icons.Filled.Star,
                "المشاهدة لاحقاً (${downloads.size})"
            ) {
                showDownloadsDialog = true
            }
        }
        item {
            NavRow(
                Icons.Filled.History,
                "سجل المشاهدة (${watchHistory.size})"
            ) {
                showHistoryDialog = true
            }
        }
        item {
            NavRow(
                Icons.Filled.Language,
                "اللغة ($appLanguage)"
            ) {
                showLanguageDialog = true
            }
        }
        
        if (onLogout != null) {
            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE50914)
                    )
                ) {
                    Text("تسجيل الخروج", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            Text(
                "حكايتنا · الإصدار ${com.agon.app.BuildConfig.VERSION_NAME}",
                color = MaterialTheme.colorScheme.outline,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
    }

    // --- DIALOGS FOR FUNCTIONAL SETTINGS ---

    // 1. Profile Editing Dialog
    if (showProfileDialog) {
        var tempName by remember { mutableStateOf(if (profileName != "ضيف") profileName else "") }
        var tempEmail by remember { mutableStateOf(userEmail) }
        var tempAge by remember { mutableStateOf(profileAge) }
        var tempColor by remember { mutableStateOf(profileColorVal) }
        val colorsList = listOf(
            0xFFE50914.toInt(), // Netflix Red
            0xFF1E88E5.toInt(), // Material Blue
            0xFF43A047.toInt(), // Material Green
            0xFFFFB300.toInt(), // Material Gold
            0xFF8E24AA.toInt(), // Material Purple
            0xFF00ACC1.toInt()  // Material Cyan
        )
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("تعديل الملف الشخصي", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile picture preview with camera overlay
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(2.dp, Color(tempColor), CircleShape)
                            .clickable {
                                if (!isUploadingAvatar) {
                                    avatarPicker.pickImage()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploadingAvatar) {
                            CircularProgressIndicator(color = Color.Red, modifier = Modifier.size(24.dp))
                        } else if (tempAvatarUrl.isNotEmpty() || profileAvatar.isNotEmpty()) {
                            coil3.compose.AsyncImage(
                                model = if (tempAvatarUrl.isNotEmpty()) tempAvatarUrl else profileAvatar,
                                contentDescription = "الصورة الشخصية",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(56.dp))
                        }
                        
                        // Edit Icon Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    
                    if (tempAvatarUrl.isNotEmpty() || profileAvatar.isNotEmpty()) {
                        TextButton(onClick = { 
                            vm.setProfileAvatar("") 
                            tempAvatarUrl = ""
                        }) {
                            Text("إزالة الصورة", color = Color.Red.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("أو اختر صورة رمزية جاهزة:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))

                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(com.agon.app.data.RealAvatars.list.size) { index ->
                            val url = com.agon.app.data.RealAvatars.list[index]
                            val isSelected = tempAvatarUrl == url
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        tempAvatarUrl = url
                                    }
                            ) {
                                coil3.compose.AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("اسم المستخدم") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempEmail,
                        onValueChange = { tempEmail = it },
                        label = { Text("البريد الإلكتروني") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempAge,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() }) {
                                tempAge = it
                            }
                        },
                        label = { Text("العمر") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("اختر لون الرمز التعبيري", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        colorsList.forEach { colorVal ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorVal))
                                    .border(
                                        width = if (tempColor == colorVal) 3.dp else 0.dp,
                                        color = if (tempColor == colorVal) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { tempColor = colorVal }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            vm.setUserAccount(tempName.trim(), tempEmail.trim())
                            vm.setProfileInfo(tempName.trim(), tempColor, tempAvatarUrl)
                            vm.setProfileAge(tempAge)
                        }
                        showProfileDialog = false
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 2. Downloads Management Dialog
    if (showDownloadsDialog) {
        val downloadedShows = Catalog.all.filter { it.id in downloads }

        AlertDialog(
            onDismissRequest = { showDownloadsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المشاهدة لاحقاً", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                    if (downloadedShows.isNotEmpty()) {
                        TextButton(
                            onClick = { vm.clearDownloads() }
                        ) {
                            Text("حذف الكل", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            text = {
                if (downloadedShows.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("قائمتك فارغة", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(downloadedShows) { show ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(modifier = Modifier.size(45.dp, 60.dp).clip(RoundedCornerShape(4.dp))) {
                                        PosterArt(showId = show.id, posterUrl = show.poster, title = show.title, showTitle = false)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(show.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                                        Text(if (show.title.contains("فيلم")) "فيلم" else "مسلسل", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                IconButton(onClick = { vm.toggleDownload(show.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "حذف التنزيل", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDownloadsDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // 3. Watch History Dialog
    if (showHistoryDialog) {
        val historyShows = watchHistory.mapNotNull { id -> Catalog.byId(id) }

        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("سجل المشاهدة", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                    if (historyShows.isNotEmpty()) {
                        TextButton(
                            onClick = { vm.clearHistory() }
                        ) {
                            Text("مسح السجل", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            text = {
                if (historyShows.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("سجل المشاهدة فارغ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(historyShows) { show ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(modifier = Modifier.size(45.dp, 60.dp).clip(RoundedCornerShape(4.dp))) {
                                        PosterArt(showId = show.id, posterUrl = show.poster, title = show.title, showTitle = false)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(show.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                                        Text(if (show.title.contains("فيلم")) "فيلم" else "مسلسل", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                IconButton(onClick = { vm.removeFromHistory(show.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "حذف من السجل", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showHistoryDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // 4. Duration Selection Dialog
    if (showSecondsDialog) {
        val durations = listOf(4, 5, 6, 10, 15)
        AlertDialog(
            onDismissRequest = { showSecondsDialog = false },
            title = { Text("اختر مدة الاختفاء", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    durations.forEach { sec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    vm.setPlayerHideSeconds(sec)
                                    showSecondsDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = playerHideSeconds == sec,
                                onClick = { 
                                    vm.setPlayerHideSeconds(sec)
                                    showSecondsDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("$sec ثواني", color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSecondsDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // 5. Language Selector Dialog
    if (showLanguageDialog) {
        val languages = listOf("العربية", "English", "Français", "Español")
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("اختر لغة التطبيق", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    languages.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (appLanguage == lang) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    vm.setLanguage(lang)
                                    selectedLang = lang
                                    showLanguageDialog = false
                                    showRestartDialog = true
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(lang, color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp)
                            if (appLanguage == lang) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showRestartDialog) {
        val titleText = when (selectedLang) {
            "English" -> "Language Changed"
            "Français" -> "Langue changée"
            "Español" -> "Idioma cambiado"
            else -> "تم تغيير لغة التطبيق"
        }
        val messageText = when (selectedLang) {
            "English" -> "The application language has been successfully changed to English. Please close and restart the app for all changes to take full effect."
            "Français" -> "La langue de l'application a été changée en Français avec succès. Veuillez fermer et redémarrer l'application pour que les changements prennent pleinement effet."
            "Español" -> "El idioma de la aplicación se ha cambiado a Español correctamente. Por favor, cierre y reinicie la aplicación para que todos los cambios surtan efecto."
            else -> "تم تغيير لغة التطبيق بنجاح إلى العربية. يرجى إغلاق التطبيق وإعادة تشغيله لتطبيق كافة التغييرات والترجمات بالكامل."
        }
        val confirmButtonText = when (selectedLang) {
            "English" -> "OK"
            "Français" -> "OK"
            "Español" -> "Aceptar"
            else -> "حسناً"
        }
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text(titleText, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
            text = { Text(messageText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { showRestartDialog = false }) {
                    Text(confirmButtonText, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 7. Device Selection Dialog
    if (showDeviceDialog) {
        AlertDialog(
            onDismissRequest = { showDeviceDialog = false },
            title = {
                Text(
                    "اختر نوع الجهاز",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        "تغيير نوع الجهاز يخصص واجهة وسلوك التطبيق لتلائم طريقة الاستخدام:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // TV Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                vm.setDeviceType("tv")
                                showDeviceDialog = false
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = deviceType == "tv",
                            onClick = {
                                vm.setDeviceType("tv")
                                showDeviceDialog = false
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Filled.DesktopWindows,
                            contentDescription = null,
                            tint = if (deviceType == "tv") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("شاشة TV", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Text("تنقل بالريموت كنترول وشريط جانبي", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Mobile Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                vm.setDeviceType("mobile")
                                showDeviceDialog = false
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = deviceType == "mobile",
                            onClick = {
                                vm.setDeviceType("mobile")
                                showDeviceDialog = false
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Filled.PhoneAndroid,
                            contentDescription = null,
                            tint = if (deviceType == "mobile") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("جوال", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Text("واجهة عمودية وأزرار لمس مريحة", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Tablet Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                vm.setDeviceType("tablet")
                                showDeviceDialog = false
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = deviceType == "tablet",
                            onClick = {
                                vm.setDeviceType("tablet")
                                showDeviceDialog = false
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Filled.TabletMac,
                            contentDescription = null,
                            tint = if (deviceType == "tablet") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("تابلت / آيباد", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Text("عرض عريض ومحتوى متعدد الأسطر", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeviceDialog = false }) {
                    Text("إلغاء", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun SettingSectionTitle(title: String) {
    Text(
        title,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun ToggleRow(icon: ImageVector, label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .tvClickable(shape = RoundedCornerShape(10.dp)) { onChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Spacer(Modifier.size(14.dp))
            Text(label, color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun NavRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .tvClickable(shape = RoundedCornerShape(10.dp)) { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Spacer(Modifier.size(14.dp))
            Text(label, color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}
