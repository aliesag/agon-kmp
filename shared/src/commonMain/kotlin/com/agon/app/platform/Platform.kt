package com.agon.app.platform

import androidx.compose.runtime.Composable
import com.russhwolf.settings.Settings

/** File access that replaces java.io.File / Context.filesDir in the Android code. */
expect object PlatformFiles {
    /** Writable, persistent dir for offline episodes (iOS: Documents/Downloads). */
    fun downloadsDir(): String
    fun exists(path: String): Boolean
    fun size(path: String): Long
    fun delete(path: String)
    fun create(path: String)
    fun append(path: String, bytes: ByteArray)
    /** Absolute path -> URL string a native player accepts (file://...). */
    fun toFileUrl(path: String): String
}

/** Toast / share sheet / open link / orientation / quit — everything that needed Context or Activity. */
expect object PlatformUi {
    val isAndroid: Boolean
    fun toast(message: String)
    fun shareText(text: String)
    fun openUrl(url: String)
    /** Share/save a local file (iOS: share sheet -> Save Video / Save to Files). */
    fun shareFile(path: String)
    /** true = landscape (player screens), false = portrait. */
    fun setLandscape(landscape: Boolean)
}

class PickedMedia(val bytes: ByteArray, val fileName: String, val isVideo: Boolean)

interface MediaPickerHandle {
    fun pickImage()
    fun pickVideo()
}

/** Replaces ActivityResultContracts.GetContent / MediaStore picker. Callback gets null on cancel. */
@Composable
expect fun rememberMediaPicker(onPicked: (PickedMedia?) -> Unit): MediaPickerHandle

/** Replaces context.getSharedPreferences("user_prefs", ...). Same keys as Android: avatar, username, age. */
object UserPrefs {
    private val s: Settings by lazy { Settings() }
    fun getString(key: String, default: String = ""): String = s.getString(key, default)
    fun putString(key: String, value: String) = s.putString(key, value)
}
