@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.agon.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.closeFile
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.seekToEndOfFile
import platform.Foundation.writeData
import platform.Foundation.writeToFile
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIPopoverPresentationController
import platform.Foundation.lastPathComponent
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerMediaURL
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UILabel
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.UIWindowSceneGeometryPreferencesIOS
import platform.UIKit.UIInterfaceOrientationMask
import platform.UIKit.UIInterfaceOrientationMaskLandscape
import platform.UIKit.UIInterfaceOrientationMaskPortrait
import platform.UIKit.setNeedsUpdateOfSupportedInterfaceOrientations
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

fun ByteArray.toNSData(): NSData =
    if (isEmpty()) NSData() else usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }

fun NSData.toByteArray(): ByteArray {
    val n = length.toInt()
    val out = ByteArray(n)
    if (n > 0) out.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return out
}

internal fun keyWindow(): UIWindow? =
    UIApplication.sharedApplication.keyWindow ?: (UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow)

internal fun topViewController(): UIViewController? {
    var vc = keyWindow()?.rootViewController
    while (true) {
        val next = vc?.presentedViewController ?: break
        vc = next
    }
    return vc
}

actual object PlatformFiles {
    private val fm get() = NSFileManager.defaultManager

    actual fun downloadsDir(): String {
        val docs = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String ?: NSTemporaryDirectory()
        val dir = "$docs/Downloads"
        fm.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)
        return dir
    }

    actual fun exists(path: String): Boolean = fm.fileExistsAtPath(path)

    actual fun size(path: String): Long {
        val attrs = fm.attributesOfItemAtPath(path, error = null) ?: return 0L
        return (attrs[NSFileSize] as? NSNumber)?.longLongValue ?: 0L
    }

    actual fun delete(path: String) { fm.removeItemAtPath(path, error = null) }

    actual fun create(path: String) {
        fm.removeItemAtPath(path, error = null)
        fm.createFileAtPath(path, contents = null, attributes = null)
    }

    actual fun append(path: String, bytes: ByteArray) {
        try {
            val existing = NSFileManager.defaultManager.contentsAtPath(path)
            val combined = if (existing != null) {
                val old = existing.toByteArray()
                old + bytes
            } else bytes
            combined.toNSData().writeToFile(path = path, atomically = true)
        } catch (_: Throwable) { }
    }

    actual fun toFileUrl(path: String): String = NSURL.fileURLWithPath(path).absoluteString ?: "file://$path"
}

actual object PlatformUi {
    actual val isAndroid: Boolean = false
    actual fun toast(message: String) {
        dispatch_async(dispatch_get_main_queue()) {
            val window = keyWindow() ?: return@dispatch_async
            val (w, h) = window.bounds.useContents { size.width to size.height }
            val label = UILabel().apply {
                text = message
                textColor = UIColor.whiteColor
                backgroundColor = UIColor.blackColor.colorWithAlphaComponent(0.85)
                textAlignment = NSTextAlignmentCenter
                numberOfLines = 0
                font = UIFont.systemFontOfSize(14.0)
                layer.cornerRadius = 12.0
                clipsToBounds = true
                alpha = 1.0
            }
            label.setFrame(CGRectMake(24.0, h - 170.0, w - 48.0, 52.0))
            window.addSubview(label)
            UIView.animateWithDuration(
                duration = 0.4, delay = 2.2, options = 0uL,
                animations = { label.alpha = 0.0 },
                completion = { _ -> label.removeFromSuperview() }
            )
        }
    }

    actual fun shareText(text: String) {
        dispatch_async(dispatch_get_main_queue()) {
            val top = topViewController() ?: return@dispatch_async
            val vc = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
            top.presentViewController(vc, animated = true, completion = null)
        }
    }

    actual fun shareFile(path: String) {
        dispatch_async(dispatch_get_main_queue()) {
            val top = topViewController() ?: return@dispatch_async
            val vc = UIActivityViewController(activityItems = listOf(NSURL.fileURLWithPath(path)), applicationActivities = null)
            top.presentViewController(vc, animated = true, completion = null)
        }
    }

    actual fun openUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any?>(), completionHandler = null)
    }

    actual fun setLandscape(landscape: Boolean) {
        dispatch_async(dispatch_get_main_queue()) {
            val scene = keyWindow()?.windowScene ?: return@dispatch_async
            val mask: UIInterfaceOrientationMask =
                if (landscape) UIInterfaceOrientationMaskLandscape else UIInterfaceOrientationMaskPortrait
            // iOS 16+. Info.plist must allow both orientations (project.yml).
            val prefs = UIWindowSceneGeometryPreferencesIOS(interfaceOrientations = mask)
            scene.requestGeometryUpdateWithPreferences(prefs, errorHandler = { _ -> })
            keyWindow()?.rootViewController?.setNeedsUpdateOfSupportedInterfaceOrientations()
        }
    }
}

private class PickerDelegate(
    var onPicked: (PickedMedia?) -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val mediaUrl = didFinishPickingMediaWithInfo[UIImagePickerControllerMediaURL] as? NSURL
        if (mediaUrl != null) {
            val data = NSData.dataWithContentsOfURL(mediaUrl)
            onPicked(data?.let { PickedMedia(it.toByteArray(), mediaUrl.absoluteString?.substringAfterLast("/") ?: "video.mp4", true) })
            return
        }
        val img = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        val jpeg = img?.let { UIImageJPEGRepresentation(it, 0.9) }
        onPicked(jpeg?.let { PickedMedia(it.toByteArray(), "image.jpg", false) })
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onPicked(null)
    }
}

private class IosMediaPicker(private val delegate: PickerDelegate) : MediaPickerHandle {
    private fun present(video: Boolean) {
        dispatch_async(dispatch_get_main_queue()) {
            val top = topViewController() ?: return@dispatch_async
            val picker = UIImagePickerController().apply {
                sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
                mediaTypes = listOf(if (video) "public.movie" else "public.image")
                this.delegate = this@IosMediaPicker.delegate
            }
            top.presentViewController(picker, animated = true, completion = null)
        }
    }
    override fun pickImage() = present(false)
    override fun pickVideo() = present(true)
}

@Composable
actual fun rememberMediaPicker(onPicked: (PickedMedia?) -> Unit): MediaPickerHandle {
    val delegate = remember { PickerDelegate(onPicked) }
    delegate.onPicked = onPicked            // keep latest lambda, delegate must stay strongly referenced
    return remember { IosMediaPicker(delegate) }
}
