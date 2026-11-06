@file:OptIn(ExperimentalForeignApi::class)

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
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.presentationSize
import kotlinx.cinterop.useContents
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.rate
import platform.AVFoundation.seekToTime
import platform.AVKit.AVPlayerViewController
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSURL
import platform.UIKit.UIColor

private class IosVideoController(val player: AVPlayer) : VideoController {
    private fun secs(ms: Double): Long = if (ms.isNaN() || ms.isInfinite()) 0L else ms.toLong()
    override val positionMs: Long get() = secs(CMTimeGetSeconds(player.currentTime()) * 1000.0)
    override val durationMs: Long get() {
        val item = player.currentItem ?: return 0L
        return secs(CMTimeGetSeconds(item.duration) * 1000.0)
    }
    override val isPlaying: Boolean get() = player.rate > 0f
    override val isEnded: Boolean get() = durationMs > 0 && positionMs >= durationMs - 300
    private var speed = 1f
    override val videoWidth: Int get() = player.currentItem?.presentationSize?.useContents { width.toInt() } ?: 0
    override val videoHeight: Int get() = player.currentItem?.presentationSize?.useContents { height.toInt() } ?: 0
    override fun play() { player.play(); if (speed != 1f) player.rate = speed }
    override fun setSpeed(speed: Float) { this.speed = speed; if (isPlaying) player.rate = speed }
    override fun pause() { player.pause() }
    override fun seekTo(ms: Long) { player.seekToTime(CMTimeMakeWithSeconds(ms / 1000.0, 600)) }
}

@Composable
actual fun NativeVideoSurface(
    url: String,
    modifier: Modifier,
    referer: String?,
    autoPlay: Boolean,
    loop: Boolean,
    onController: (VideoController) -> Unit
) {
    val holder = remember(url) {
        val nsUrl = if (url.startsWith("http") || url.startsWith("file:")) NSURL.URLWithString(url)
        else NSURL.fileURLWithPath(url)
        if (nsUrl == null) null
        else {
            // without this the silent switch mutes the video
            AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, error = null)
            val options: Map<Any?, Any?>? =
                if (referer != null) mapOf("AVURLAssetHTTPHeaderFieldsKey" to mapOf("Referer" to referer)) else null
            val item = AVPlayerItem(asset = AVURLAsset(uRL = nsUrl, options = options))
            val player = AVPlayer(playerItem = item)
            val vc = AVPlayerViewController().apply {
                this.player = player
                showsPlaybackControls = false
                view.backgroundColor = UIColor.blackColor
            }
            vc to IosVideoController(player)
        }
    }
    if (holder == null) { Box(modifier.background(Color.Black)); return }
    val (vc, controller) = holder

    UIKitView(factory = { vc.view }, modifier = modifier, update = { _ -> })

    DisposableEffect(url) {
        val token = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { _ -> if (loop) { controller.seekTo(0); controller.play() } }
        )
        onController(controller)
        if (autoPlay) controller.play()
        onDispose {
            controller.pause()
            NSNotificationCenter.defaultCenter.removeObserver(token)
        }
    }
}
