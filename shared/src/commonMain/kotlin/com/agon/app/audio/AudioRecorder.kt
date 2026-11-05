package com.agon.app.audio

import androidx.compose.runtime.Composable

interface AudioRecorderHandle {
    fun start(): Boolean
    fun stop(): ByteArray?
    fun cancel()
    fun isRecording(): Boolean
}

@Composable
expect fun rememberAudioRecorder(): AudioRecorderHandle
