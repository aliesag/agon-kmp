package com.agon.app.picker

import androidx.compose.runtime.Composable

/**
 * Platform image picker. Returns bytes via [onResult] callback.
 * iOS: PHPickerViewController. Android: ActivityResultContracts.GetContent.
 */
interface ImagePickerHandle {
    fun pick()
}

@Composable
expect fun rememberImagePicker(onResult: (ByteArray) -> Unit): ImagePickerHandle
