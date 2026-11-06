package com.agon.app.video

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Programmatic control over a native video surface (custom Compose controls sit on top of it). */
interface VideoController {
    val positionMs: Long
    val durationMs: Long
    val isPlaying: Boolean
    val isEnded: Boolean
    val videoWidth: Int
    val videoHeight: Int
    fun play()
    fun pause()
    fun seekTo(ms: Long)
    fun setSpeed(speed: Float)
}

/**
 * Video surface with NO native controls (iOS: AVPlayer in AVPlayerViewController, Android: VideoView).
 * [url] may be http(s)://, file:// or an absolute path. [onController] fires once the player exists.
 */
@Composable
expect fun NativeVideoSurface(
    url: String,
    modifier: Modifier = Modifier,
    referer: String? = null,
    autoPlay: Boolean = true,
    loop: Boolean = false,
    onController: (VideoController) -> Unit
)
