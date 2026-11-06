package com.agon.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agon.app.BuildConfig
import com.agon.app.data.UpdateInfo
import com.agon.app.platform.PlatformUi

/**
 * iOS counterpart of the Android in-app APK updater: iOS can't self-install, so the button opens
 * info.downloadUrl (App Store / TestFlight / your IPA host) in Safari.
 */
@Composable
fun ForceUpdateScreen(info: UpdateInfo, onDismiss: (() -> Unit)?) {
    val red = Color(0xFFE50914)
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0B0B0F)).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.SystemUpdate, null, tint = red, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(20.dp))
        Text("تحديث جديد متاح لتطبيق حكايتنا", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            if (info.isMandatory) "تحديث إجباري للاستمرار في استخدام التطبيق والتمتع بأحدث الميزات والبث المباشر."
            else "يتوفر إصدار جديد مع ميزات وتحسينات.",
            color = Color(0xFFB0B0B8), fontSize = 14.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VersionBox("الإصدار الحالي", "v${BuildConfig.VERSION_NAME}", Color(0xFF1E1E24), Modifier.weight(1f))
            VersionBox("الإصدار الجديد المطلوب", "v${info.versionName}", Color(0xFF3A0B0F), Modifier.weight(1f))
        }
        if (info.features.isNotBlank()) {
            Spacer(Modifier.height(20.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF16161C)).padding(16.dp)) {
                Text("ما الجديد في هذا التحديث:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Text(info.features.replace("\\n", "\n"), color = Color(0xFFB0B0B8), fontSize = 13.sp, lineHeight = 20.sp)
            }
        }
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = { PlatformUi.openUrl(info.downloadUrl) },
            colors = ButtonDefaults.buttonColors(containerColor = red),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) { Text("تحديث الآن", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        if (onDismiss != null) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss) { Text("تخطي هذا التحديث", color = Color(0xFF8C8C94)) }
        }
    }
}

@Composable
private fun VersionBox(label: String, value: String, bg: Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(bg).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color(0xFFB0B0B8), fontSize = 11.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}
