package com.agon.app.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.seekToTime
import platform.AVFoundation.setVolume
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectZero
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSBundle
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIView

/** UIView subclass whose AVPlayerLayer always matches its bounds. */
@OptIn(ExperimentalForeignApi::class)
private class PlayerContainerView(private val player: AVPlayer) : UIView(frame = CGRectZero.readValue()) {
    private val playerLayer: AVPlayerLayer = AVPlayerLayer.playerLayerWithPlayer(player).apply {
        videoGravity = "AVLayerVideoGravityResizeAspectFill"
    }

    init {
        layer.addSublayer(playerLayer)
        backgroundColor = platform.UIKit.UIColor.blackColor
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        CATransaction.begin()
        CATransaction.setValue(true, kCATransactionDisableActions)
        playerLayer.frame = bounds
        CATransaction.commit()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun NativeVideoPlayer(
    assetName: String,
    modifier: Modifier,
    autoPlay: Boolean,
    loop: Boolean,
    onFinished: () -> Unit,
) {
    val holder = remember(assetName) {
        val url = NSBundle.mainBundle.URLForResource(assetName, withExtension = "mp4")
            ?: NSBundle.mainBundle.URLForResource(assetName, withExtension = "mov")
        if (url == null) null
        else {
            val player = AVPlayer(uRL = url)
            player to PlayerContainerView(player)
        }
    }

    if (holder == null) {
        Box(modifier.background(Color.Black))
        return
    }

    UIKitView(
        factory = { holder.second },
        modifier = modifier,
        update = { _ -> }
    )

    DisposableEffect(assetName) {
        val player = holder.first
        val token = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { _ ->
                if (loop) {
                    player.seekToTime(CMTimeMake(value = 0, timescale = 1))
                    player.play()
                } else {
                    onFinished()
                }
            }
        )
        player.setVolume(1.0f)
        if (autoPlay) player.play()
        onDispose {
            player.pause()
            NSNotificationCenter.defaultCenter.removeObserver(token)
        }
    }
}
