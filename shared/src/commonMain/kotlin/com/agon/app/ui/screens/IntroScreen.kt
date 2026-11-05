package com.agon.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agon.app.video.NativeVideoPlayer
import org.jetbrains.compose.resources.painterResource
import agon_kmp.shared.generated.resources.Res
import agon_kmp.shared.generated.resources.logo
import kotlinx.coroutines.delay

@Composable
fun AppHomePreloadScreen(onPreloadComplete: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0.1f) }

    LaunchedEffect(Unit) {
        val totalSteps = 25
        for (i in 1..totalSteps) {
            delay(30)
            progress = (i.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
        }
        delay(50)
        onPreloadComplete()
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .clickable { onPreloadComplete() }
            .background(Color(0xFF0C0306))
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(105.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFFE50914))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.logo),
                    contentDescription = "حكايتنا",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(75.dp).clip(RoundedCornerShape(20.dp))
                )
            }
            Spacer(Modifier.height(18.dp))
            Text("حكايتنا", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("عالم من الترفيه اللامحدود", color = Color.White.copy(alpha = 0.65f), fontSize = 14.sp)
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = Color(0xFFE50914),
                trackColor = Color.White.copy(alpha = 0.12f)
            )
            Spacer(Modifier.height(8.dp))
            Text("جارٍ التحضير…", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
        }
    }
}

@Composable
fun HekayatnaProfessionalIntro(deviceType: String, onIntroFinished: () -> Unit) {
    var hasFinished by remember { mutableStateOf(false) }

    fun finish() {
        if (!hasFinished) {
            hasFinished = true
            onIntroFinished()
        }
    }

    // failsafe: 20s max
    LaunchedEffect(Unit) {
        delay(20_000)
        finish()
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .windowInsetsPadding(WindowInsets(0, 0, 0, 0))
    ) {
        NativeVideoPlayer(
            assetName = "intro_video",
            modifier = Modifier.fillMaxSize(),
            autoPlay = true,
            loop = false,
            onFinished = { finish() }
        )

        // Skip button top-right
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { finish() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("تخطي", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
