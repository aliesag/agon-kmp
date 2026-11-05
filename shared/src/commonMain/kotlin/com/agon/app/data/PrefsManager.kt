package com.agon.app.data

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * KMP port of the Android DataStore-based PrefsManager.
 * Backed by multiplatform-settings (NSUserDefaults on iOS, SharedPreferences on Android).
 * Public API mirrors the original: read via StateFlow (compatible with collectAsState),
 * write via suspend setters.
 */
class PrefsManager(private val settings: Settings = Settings()) {

    private object Keys {
        const val LAST_SHOW_ID = "last_show_id"
        const val LAST_EPISODE_ID = "last_episode_id"
        const val AUTO_UPDATE = "auto_update"
        const val SHOW_CONTINUE_WATCHING = "show_continue_watching"
        const val APP_LANGUAGE = "app_language"
        const val PLAYER_AUTO_HIDE = "player_auto_hide"
        const val PLAYER_HIDE_SECONDS = "player_hide_seconds"
        const val PROFILE_AVATAR = "profile_avatar"
        const val PROFILE_NAME = "profile_name"
        const val PROFILE_AGE = "profile_age"
        const val PROFILE_COLOR = "profile_color"
        const val ONBOARDING_COMPLETED = "onboarding_completed"
        const val INSTALLED_VERSION_CODE = "installed_version_code"
        const val DEVICE_TYPE = "device_type"
        const val USER_EMAIL = "user_email"
        const val SKIP_INTRO = "skip_intro"
        const val LAST_SEEN_SHOW_ID = "last_seen_show_id"
    }

    val skipIntro: StateFlow<Boolean> = stateOf(Keys.SKIP_INTRO, false)
    val userEmail: StateFlow<String> = stateOf(Keys.USER_EMAIL, "")
    val lastShowId: StateFlow<String?> = stateOfNullable(Keys.LAST_SHOW_ID)
    val lastEpisodeId: StateFlow<String?> = stateOfNullable(Keys.LAST_EPISODE_ID)
    val autoUpdate: StateFlow<Boolean> = stateOf(Keys.AUTO_UPDATE, true)
    val showContinueWatching: StateFlow<Boolean> = stateOf(Keys.SHOW_CONTINUE_WATCHING, true)
    val appLanguage: StateFlow<String> = stateOf(Keys.APP_LANGUAGE, "ar")
    val playerAutoHide: StateFlow<Boolean> = stateOf(Keys.PLAYER_AUTO_HIDE, true)
    val playerHideSeconds: StateFlow<Int> = stateOf(Keys.PLAYER_HIDE_SECONDS, 10)
    val profileAvatar: StateFlow<String> = stateOf(Keys.PROFILE_AVATAR, "")
    val profileName: StateFlow<String> = stateOf(Keys.PROFILE_NAME, "ضيف")
    val profileAge: StateFlow<String> = stateOf(Keys.PROFILE_AGE, "")
    val profileColor: StateFlow<Int> = stateOf(Keys.PROFILE_COLOR, 0xFFE50914.toInt())
    val onboardingCompleted: StateFlow<Boolean> = stateOf(Keys.ONBOARDING_COMPLETED, false)
    val installedVersionCode: StateFlow<Int> = stateOf(Keys.INSTALLED_VERSION_CODE, 0)
    val deviceType: StateFlow<String> = stateOf(Keys.DEVICE_TYPE, "")

    // ---------- writers ----------

    suspend fun setDeviceType(type: String) { write(Keys.DEVICE_TYPE, type); deviceTypeFlow.value = type }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        write(Keys.ONBOARDING_COMPLETED, completed); onboardingFlow.value = completed
    }

    suspend fun setProfileInfo(name: String, color: Int, avatar: String) {
        settings.putString(Keys.PROFILE_NAME, name)
        settings.putInt(Keys.PROFILE_COLOR, color)
        settings.putString(Keys.PROFILE_AVATAR, avatar)
        nameFlow.value = name; colorFlow.value = color; avatarFlow.value = avatar
    }

    suspend fun setUserAccount(username: String, email: String) {
        settings.putString(Keys.PROFILE_NAME, username)
        settings.putString(Keys.USER_EMAIL, email)
        nameFlow.value = username; emailFlow.value = email
    }

    suspend fun setProfileAge(age: String) {
        write(Keys.PROFILE_AGE, age); ageFlow.value = age
    }

    suspend fun setInstalledVersionCode(versionCode: Int) {
        write(Keys.INSTALLED_VERSION_CODE, versionCode); installedFlow.value = versionCode
    }

    suspend fun saveLastWatched(showId: String, episodeId: String) {
        settings.putString(Keys.LAST_SHOW_ID, showId)
        settings.putString(Keys.LAST_EPISODE_ID, episodeId)
        lastShowFlow.value = showId; lastEpisodeFlow.value = episodeId
    }

    suspend fun setPlayerAutoHide(enabled: Boolean) {
        write(Keys.PLAYER_AUTO_HIDE, enabled); autoHideFlow.value = enabled
    }

    suspend fun setPlayerHideSeconds(seconds: Int) {
        write(Keys.PLAYER_HIDE_SECONDS, seconds); hideSecondsFlow.value = seconds
    }

    suspend fun setAutoUpdate(enabled: Boolean) {
        write(Keys.AUTO_UPDATE, enabled); autoUpdateFlow.value = enabled
    }

    suspend fun setShowContinueWatching(show: Boolean) {
        write(Keys.SHOW_CONTINUE_WATCHING, show); continueWatchingFlow.value = show
    }

    suspend fun setLanguage(lang: String) {
        write(Keys.APP_LANGUAGE, lang); languageFlow.value = lang
    }

    suspend fun setSkipIntro(skip: Boolean) {
        write(Keys.SKIP_INTRO, skip); skipIntroFlow.value = skip
    }

    suspend fun setProfileAvatar(url: String) {
        write(Keys.PROFILE_AVATAR, url); avatarFlow.value = url
    }

    // ---------- sync (background worker) ----------

    fun getLastSeenShowIdSync(): String? = settings.getStringOrNull(Keys.LAST_SEEN_SHOW_ID)

    fun saveLastSeenShowIdSync(id: String) { settings.putString(Keys.LAST_SEEN_SHOW_ID, id) }

    // ---------- plumbing ----------

    private fun write(key: String, v: Boolean) = settings.putBoolean(key, v)
    private fun write(key: String, v: Int) = settings.putInt(key, v)
    private fun write(key: String, v: String) = settings.putString(key, v)

    private fun stateOf(key: String, default: Boolean): StateFlow<Boolean> {
        val f = MutableStateFlow(settings.getBoolean(key, default))
        when (key) {
            Keys.SKIP_INTRO -> skipIntroFlow = f
            Keys.AUTO_UPDATE -> autoUpdateFlow = f
            Keys.SHOW_CONTINUE_WATCHING -> continueWatchingFlow = f
            Keys.PLAYER_AUTO_HIDE -> autoHideFlow = f
            Keys.ONBOARDING_COMPLETED -> onboardingFlow = f
        }
        return f.asStateFlow()
    }

    private fun stateOf(key: String, default: Int): StateFlow<Int> {
        val f = MutableStateFlow(settings.getInt(key, default))
        when (key) {
            Keys.PLAYER_HIDE_SECONDS -> hideSecondsFlow = f
            Keys.PROFILE_COLOR -> colorFlow = f
            Keys.INSTALLED_VERSION_CODE -> installedFlow = f
        }
        return f.asStateFlow()
    }

    private fun stateOf(key: String, default: String): StateFlow<String> {
        val f = MutableStateFlow(settings.getString(key, default))
        when (key) {
            Keys.USER_EMAIL -> emailFlow = f
            Keys.APP_LANGUAGE -> languageFlow = f
            Keys.PROFILE_AVATAR -> avatarFlow = f
            Keys.PROFILE_NAME -> nameFlow = f
            Keys.PROFILE_AGE -> ageFlow = f
            Keys.DEVICE_TYPE -> deviceTypeFlow = f
        }
        return f.asStateFlow()
    }

    private fun stateOfNullable(key: String): StateFlow<String?> {
        val f = MutableStateFlow(settings.getStringOrNull(key))
        when (key) {
            Keys.LAST_SHOW_ID -> lastShowFlow = f
            Keys.LAST_EPISODE_ID -> lastEpisodeFlow = f
        }
        return f.asStateFlow()
    }

    private lateinit var skipIntroFlow: MutableStateFlow<Boolean>
    private lateinit var autoUpdateFlow: MutableStateFlow<Boolean>
    private lateinit var continueWatchingFlow: MutableStateFlow<Boolean>
    private lateinit var autoHideFlow: MutableStateFlow<Boolean>
    private lateinit var onboardingFlow: MutableStateFlow<Boolean>
    private lateinit var hideSecondsFlow: MutableStateFlow<Int>
    private lateinit var colorFlow: MutableStateFlow<Int>
    private lateinit var installedFlow: MutableStateFlow<Int>
    private lateinit var emailFlow: MutableStateFlow<String>
    private lateinit var languageFlow: MutableStateFlow<String>
    private lateinit var avatarFlow: MutableStateFlow<String>
    private lateinit var nameFlow: MutableStateFlow<String>
    private lateinit var ageFlow: MutableStateFlow<String>
    private lateinit var deviceTypeFlow: MutableStateFlow<String>
    private lateinit var lastShowFlow: MutableStateFlow<String?>
    private lateinit var lastEpisodeFlow: MutableStateFlow<String?>
}
