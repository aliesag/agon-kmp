package com.agon.app.video

import android.net.Uri
import android.widget.VideoView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private class AndroidVideoController(val vv: VideoView) : VideoController {
    var ended = false
    override val positionMs get() = vv.currentPosition.toLong()
    override val durationMs get() = vv.duration.toLong().coerceAtLeast(0L)
    override val isPlaying get() = vv.isPlaying
    override val isEnded get() = ended
    override fun play() { ended = false; vv.start() }
    override fun pause() { vv.pause() }
    override fun seekTo(ms: Long) { vv.seekTo(ms.toInt()) }
    override val videoWidth get() = vv.width
    override val videoHeight get() = vv.height
    override fun setSpeed(speed: Float) {}
}

@Composable
actual fun NativeVideoSurface(
    url: String, modifier: Modifier, referer: String?, autoPlay: Boolean, loop: Boolean, onController: (VideoController) -> Unit
) {
    val holder = remember { arrayOfNulls<AndroidVideoController>(1) }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            VideoView(ctx).also { vv ->
                val c = AndroidVideoController(vv)
                holder[0] = c
                vv.setVideoURI(if (url.startsWith("/")) Uri.fromFile(java.io.File(url)) else Uri.parse(url))
                vv.setOnPreparedListener { if (autoPlay) vv.start() }
                vv.setOnCompletionListener { if (loop) { vv.start() } else c.ended = true }
                onController(c)
            }
        }
    )
}
