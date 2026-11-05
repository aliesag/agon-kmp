package com.agon.app.video

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun NativeVideoPlayer(
    assetName: String,
    modifier: Modifier,
    autoPlay: Boolean,
    loop: Boolean,
    onFinished: () -> Unit,
) { /* Android VideoView port pending */ }
