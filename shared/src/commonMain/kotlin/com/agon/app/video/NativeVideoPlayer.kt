package com.agon.app.video

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Plays a bundled mp4 (by asset name, without extension) full-screen.
 * iOS: AVPlayer + AVPlayerLayer via UIKitView.
 * Android: ExoPlayer / VideoView (wired in stage 8 when Android build resumes).
 */
@Composable
expect fun NativeVideoPlayer(
    assetName: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    loop: Boolean = false,
    onFinished: () -> Unit = {},
)
