package com.agon.app.video

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
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.seekToTime
import platform.AVFoundation.setVolume
import platform.AVKit.AVPlayerViewController
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSBundle
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIColor

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun NativeVideoPlayer(
    assetName: String,
    modifier: Modifier,
    autoPlay: Boolean,
    loop: Boolean,
    onFinished: () -> Unit,
) {
    val controller = remember(assetName) {
        val url = NSBundle.mainBundle.URLForResource(assetName, withExtension = "mp4")
            ?: NSBundle.mainBundle.URLForResource(assetName, withExtension = "mov")
        if (url == null) null
        else {
            val player = AVPlayer(uRL = url)
            val ctrl = AVPlayerViewController().apply {
                this.player = player
                this.showsPlaybackControls = false
                this.view.backgroundColor = UIColor.blackColor
            }
            ctrl
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

    DisposableEffect(assetName) {
        val player = controller.player
        val token = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { _ ->
                if (loop) {
                    player?.seekToTime(CMTimeMake(value = 0, timescale = 1))
                    player?.play()
                } else {
                    onFinished()
                }
            }
        )
        player?.setVolume(1.0f)
        if (autoPlay) player?.play()
        onDispose {
            player?.pause()
            NSNotificationCenter.defaultCenter.removeObserver(token)
        }
    }
}
