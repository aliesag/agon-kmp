package com.agon.app.platform

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream

/** Call once from MainActivity.onCreate: AndroidPlatform.init(this). */
object AndroidPlatform {
    lateinit var app: Context
    var activity: Activity? = null
    fun init(a: Activity) { app = a.applicationContext; activity = a }
}

actual object PlatformFiles {
    actual fun downloadsDir(): String =
        (AndroidPlatform.app.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: AndroidPlatform.app.filesDir).absolutePath
    actual fun exists(path: String) = File(path).exists()
    actual fun size(path: String) = File(path).length()
    actual fun delete(path: String) { File(path).delete() }
    actual fun create(path: String) { File(path).apply { delete(); createNewFile() } }
    actual fun append(path: String, bytes: ByteArray) { FileOutputStream(path, true).use { it.write(bytes) } }
    actual fun toFileUrl(path: String) = Uri.fromFile(File(path)).toString()
}

actual object PlatformUi {
    actual val isAndroid: Boolean = true
    actual fun toast(message: String) {
        Toast.makeText(AndroidPlatform.app, message, Toast.LENGTH_LONG).show()
    }
    actual fun shareText(text: String) {
        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
        AndroidPlatform.app.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    actual fun shareFile(path: String) { toast(path) }
    actual fun openUrl(url: String) {
        AndroidPlatform.app.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    actual fun setLandscape(landscape: Boolean) {
        AndroidPlatform.activity?.requestedOrientation =
            if (landscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}

@Composable
actual fun rememberMediaPicker(onPicked: (PickedMedia?) -> Unit): MediaPickerHandle {
    val ctx = LocalContext.current
    var isVideo = false
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) { onPicked(null); return@rememberLauncherForActivityResult }
        val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        onPicked(bytes?.let { PickedMedia(it, if (isVideo) "video.mp4" else "image.jpg", isVideo) })
    }
    return remember {
        object : MediaPickerHandle {
            override fun pickImage() { isVideo = false; launcher.launch("image/*") }
            override fun pickVideo() { isVideo = true; launcher.launch("video/*") }
        }
    }
}
