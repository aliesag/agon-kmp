package com.agon.app.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
private class IosPicker(
    private val onResult: (ByteArray) -> Unit
) : ImagePickerHandle {

    private var delegate: NSObject? = null

    override fun pick() {
        val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        val config = PHPickerConfiguration().apply { selectionLimit = 1 }
        val picker = PHPickerViewController(configuration = config)
        val cb = object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                val first = didFinishPicking.firstOrNull() as? PHPickerResult
                if (first != null) {
                    first.itemProvider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ ->
                        if (data != null) {
                            val bytes = data.toByteArray()
                            onResult(bytes)
                        }
                    }
                }
                picker.dismissViewControllerAnimated(true, null)
            }
        }
        delegate = cb
        picker.delegate = cb
        root.presentViewController(picker, animated = true, completion = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val len = length.toInt()
    val bytes = ByteArray(len)
    if (len > 0) {
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, length)
        }
    }
    return bytes
}

@Composable
actual fun rememberImagePicker(onResult: (ByteArray) -> Unit): ImagePickerHandle =
    remember(onResult) { IosPicker(onResult) }
