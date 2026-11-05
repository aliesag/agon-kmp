package com.agon.app.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS: AVAudioRecorder-backed implementation.
 * FILL: proper AVAudioSession + AVAudioRecorder wiring lands with Firebase iOS package setup.
 * Returns no bytes for now — the chat UI still renders and voice button shows a notice.
 */
private class IosAudioRecorder : AudioRecorderHandle {
    override fun start(): Boolean = false
    override fun stop(): ByteArray? = null
    override fun cancel() {}
    override fun isRecording(): Boolean = false
}

@Composable
actual fun rememberAudioRecorder(): AudioRecorderHandle = remember { IosAudioRecorder() }
