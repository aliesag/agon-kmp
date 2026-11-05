package com.agon.app.ui.screens

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.agon.app.R
import kotlinx.coroutines.delay

/**
 * Preload screen with sleek progress bar at the BOTTOM.
 * Appears right after device selection to prepare the home screen data.
 */
@Composable
fun AppHomePreloadScreen(
    onPreloadComplete: () -> Unit
) {
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
        modifier = Modifier
            .fillMaxSize()
            .clickable { onPreloadComplete() }
            .background(Color(0xFF0C0306))
    ) {
        // Center: App Logo
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
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "حكايتنا",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(75.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "حكايتنا",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "عالم من الترفيه اللامحدود",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 14.sp
            )
        }

        // Bottom: Progress bar strictly positioned at the BOTTOM
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 36.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "جاري تهيئة الواجهة الرئيسية...",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = Color(0xFFE50914),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFE50914))
                )
            }
        }
    }
}

/**
 * Professional Video Intro:
 * - Plays video at: app/src/main/res/raw/intro_video.mp4
 * - If Mobile (جوال): Crops horizontally & fills vertical screen completely without black bars.
 * - If TV (شاشة / تلفزيون): Expands in widescreen 16:9 filling the screen.
 * - Transitions smoothly upon finish or when Skip is clicked.
 * - Failsafe handling if video is missing/empty so app never hangs.
 */
@Composable
fun HekayatnaProfessionalIntro(
    deviceType: String,
    onIntroFinished: () -> Unit
) {
    val context = LocalContext.current
    var hasFinished by remember { mutableStateOf(false) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    val isTv = deviceType == "tv"

    fun completeIntro() {
        if (!hasFinished) {
            hasFinished = true
            try {
                mediaPlayerRef?.stop()
                mediaPlayerRef?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaPlayerRef = null
            onIntroFinished()
        }
    }

    // Safety timeout in case video file is empty or decoding fails
    LaunchedEffect(Unit) {
        delay(12000L) // Max 12 seconds
        completeIntro()
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayerRef?.stop()
                mediaPlayerRef?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaPlayerRef = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val textureView = TextureView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                        try {
                            val resId = ctx.resources.getIdentifier("intro_video", "raw", ctx.packageName)
                            if (resId == 0) {
                                completeIntro()
                                return
                            }

                            // Verify file is playable / non-empty
                            val afd = ctx.resources.openRawResourceFd(resId)
                            if (afd == null || afd.length <= 100) {
                                afd?.close()
                                completeIntro()
                                return
                            }
                            afd.close()

                            val uri = Uri.parse("android.resource://${ctx.packageName}/$resId")
                            val player = MediaPlayer().apply {
                                setSurface(Surface(st))
                                setDataSource(ctx, uri)
                                setAudioAttributes(
                                    AudioAttributes.Builder()
                                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                                        .setUsage(AudioAttributes.USAGE_MEDIA)
                                        .build()
                                )
                                setVolume(1.0f, 1.0f)
                                isLooping = false
                                setOnPreparedListener { mp ->
                                    val videoWidth = mp.videoWidth
                                    val videoHeight = mp.videoHeight
                                    if (videoWidth > 0 && videoHeight > 0) {
                                        adjustTextureViewCrop(textureView, w, h, videoWidth, videoHeight, isTv)
                                    }
                                    mp.start()
                                }
                                setOnCompletionListener {
                                    completeIntro()
                                }
                                setOnErrorListener { _, _, _ ->
                                    completeIntro()
                                    true
                                }
                                prepareAsync()
                            }
                            mediaPlayerRef = player
                        } catch (e: Exception) {
                            e.printStackTrace()
                            completeIntro()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                        mediaPlayerRef?.let { mp ->
                            if (mp.isPlaying) {
                                adjustTextureViewCrop(textureView, w, h, mp.videoWidth, mp.videoHeight, isTv)
                            }
                        }
                    }

                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        try {
                            mediaPlayerRef?.stop()
                            mediaPlayerRef?.release()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        mediaPlayerRef = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                }

                textureView
            }
        )

    }
}

/**
 * Calculates and applies Matrix transformation to TextureView so that the video
 * scales using ContentScale.Crop — completely filling the whole screen without black bars
 * for both phone portrait mode (طولي يملأ الشاشة) and TV widescreen mode (عرضي 16:9).
 */
private fun adjustTextureViewCrop(
    textureView: TextureView,
    viewWidth: Int,
    viewHeight: Int,
    videoWidth: Int,
    videoHeight: Int,
    isTv: Boolean
) {
    if (viewWidth <= 0 || viewHeight <= 0 || videoWidth <= 0 || videoHeight <= 0) return

    val scaleX: Float
    val scaleY: Float

    val viewRatio = viewWidth.toFloat() / viewHeight.toFloat()
    val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()

    if (viewRatio > videoRatio) {
        // Screen is wider than video -> scale based on width
        scaleX = 1f
        scaleY = (viewWidth.toFloat() / videoWidth.toFloat()) * (videoHeight.toFloat() / viewHeight.toFloat())
    } else {
        // Screen is taller than video (e.g. mobile portrait) -> scale based on height so it fills full height and crops horizontal sides
        scaleX = (viewHeight.toFloat() / videoHeight.toFloat()) * (videoWidth.toFloat() / viewWidth.toFloat())
        scaleY = 1f
    }

    val matrix = Matrix()
    matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
    textureView.setTransform(matrix)
}
