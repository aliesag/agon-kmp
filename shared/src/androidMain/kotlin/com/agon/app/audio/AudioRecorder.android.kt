package com.agon.app.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Android: real MediaRecorder wiring was in ChatScreen; stubbed here for the KMP shared module. */
private class AndroidAudioRecorder : AudioRecorderHandle {
    override fun start(): Boolean = false
    override fun stop(): ByteArray? = null
    override fun cancel() {}
    override fun isRecording(): Boolean = false
}

@Composable
actual fun rememberAudioRecorder(): AudioRecorderHandle = remember { AndroidAudioRecorder() }
