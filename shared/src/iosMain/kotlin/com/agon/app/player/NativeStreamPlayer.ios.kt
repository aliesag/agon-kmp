package com.agon.app.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSURL
import platform.UIKit.UIColor

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun NativeStreamPlayer(url: String, referrer: String?, modifier: Modifier) {
    val effectiveReferer = when {
        referrer?.contains("alooytv", ignoreCase = true) == true -> "https://ep.alooytv16.xyz/"
        referrer?.contains("3cktv", ignoreCase = true) == true -> "https://e.3cktv.com/"
        !referrer.isNullOrBlank() -> referrer
        else -> null
    }

    val controller = remember(url) {
        try {
            val nsUrl = NSURL.URLWithString(url) ?: return@remember null
            val options: Map<Any?, Any?>? = if (effectiveReferer != null)
                mapOf("AVURLAssetHTTPHeaderFieldsKey" to mapOf("Referer" to effectiveReferer))
            else null
            val asset = AVURLAsset(uRL = nsUrl, options = options)
            val item = AVPlayerItem(asset = asset)
            val player = AVPlayer(playerItem = item)
            AVPlayerViewController().apply {
                this.player = player
                showsPlaybackControls = true
                allowsPictureInPicturePlayback = true
                view.backgroundColor = UIColor.blackColor
            }
        } catch (t: Throwable) {
            null
        }
    }

    if (controller == null) {
        Box(modifier.background(Color.Black))
        return
    }

    UIKitView(
        factory = { controller.view },
        modifier = modifier,
        update = { _ -> }
    )

    DisposableEffect(url) {
        controller.player?.play()
        onDispose { controller.player?.pause() }
    }
}
