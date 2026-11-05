package com.agon.app.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

private class NoOpImagePicker : ImagePickerHandle {
    override fun pick() { /* wired in stage 8 */ }
}

@Composable
actual fun rememberImagePicker(onResult: (ByteArray) -> Unit): ImagePickerHandle =
    remember { NoOpImagePicker() }
