package com.agon.app.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.play
import platform.AVFoundation.pause
import platform.AVFoundation.seekToTime
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSBundle
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIView
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun NativeVideoPlayer(
    assetName: String,
    modifier: Modifier,
    autoPlay: Boolean,
    loop: Boolean,
    onFinished: () -> Unit,
) {
    val playerAndLayer = remember(assetName) {
        val url = NSBundle.mainBundle.URLForResource(assetName, withExtension = "mp4")
            ?: NSBundle.mainBundle.URLForResource(assetName, withExtension = "mov")
        if (url == null) null
        else {
            val player = AVPlayer(uRL = url)
            val layer = AVPlayerLayer.playerLayerWithPlayer(player).apply {
                videoGravity = "AVLayerVideoGravityResizeAspectFill"
            }
            Triple(player, layer, url)
        }
    }

    // observe end
    DisposableEffect(assetName) {
        val (player, _, _) = playerAndLayer ?: return@DisposableEffect onDispose { }
        val observer = object : NSObject() {
            @Suppress("unused")
            fun onEnd(note: platform.Foundation.NSNotification) {
                if (loop) {
                    player.seekToTime(CMTimeMake(value = 0, timescale = 1))
                    player.play()
                } else {
                    onFinished()
                }
            }
        }.let { it }
        val token = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = player.currentItem,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { _ -> if (loop) { player.seekToTime(CMTimeMake(value = 0, timescale = 1)); player.play() } else onFinished() }
        )
        if (autoPlay) player.play()
        onDispose {
            player.pause()
            NSNotificationCenter.defaultCenter.removeObserver(token)
        }
    }

    UIKitView(
        factory = {
            val container = UIView()
            playerAndLayer?.let { (_, layer, _) ->
                container.layer.addSublayer(layer)
                layer.frame = container.bounds
            }
            container
        },
        modifier = modifier,
        update = { container ->
            playerAndLayer?.let { (_, layer, _) ->
                CATransaction.begin()
                CATransaction.setValue(true, kCATransactionDisableActions)
                layer.frame = container.bounds
                CATransaction.commit()
            }
        }
    )
}
