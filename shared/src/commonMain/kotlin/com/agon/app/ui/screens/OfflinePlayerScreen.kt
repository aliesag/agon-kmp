package com.agon.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agon.app.player.NativeStreamPlayer
import com.agon.app.ui.theme.NetflixRed

@Composable
fun OfflinePlayerScreen(
    filePath: String,
    title: String,
    onBack: () -> Unit
) {
    // file:// URL for the local file — AVPlayer plays both mp4 and m3u8 from disk.
    val url = if (filePath.startsWith("file://")) filePath else "file://$filePath"

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Player
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            NativeStreamPlayer(
                url = url,
                referrer = null,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF16161C))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
            }
            Spacer(Modifier.width(4.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                modifier = Modifier.weight(1f))
        }

        // Info card
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NetflixRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("تشغيل من التحميلات", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("بدون اتصال بالإنترنت", color = Color(0xFF8C8C94), fontSize = 11.sp)
                }
            }
        }
    }
}
