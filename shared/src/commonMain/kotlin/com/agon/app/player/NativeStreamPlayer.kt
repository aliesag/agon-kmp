package com.agon.app.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Plays a direct media URL (.mp4/.m3u8/.webm) with the platform's native player. */
@Composable
expect fun NativeStreamPlayer(
    url: String,
    referrer: String?,
    modifier: Modifier,
)
