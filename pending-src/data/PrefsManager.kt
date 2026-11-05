package com.agon.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class PrefsManager(private val context: Context) {

    companion object {
        val LAST_SHOW_ID = stringPreferencesKey("last_show_id")
        val LAST_EPISODE_ID = stringPreferencesKey("last_episode_id")
        val AUTO_UPDATE = booleanPreferencesKey("auto_update")
        val SHOW_CONTINUE_WATCHING = booleanPreferencesKey("show_continue_watching")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val PLAYER_AUTO_HIDE = booleanPreferencesKey("player_auto_hide")
        val PLAYER_HIDE_SECONDS = intPreferencesKey("player_hide_seconds")
        val PROFILE_AVATAR = stringPreferencesKey("profile_avatar")
        val PROFILE_NAME = stringPreferencesKey("profile_name")
        val PROFILE_AGE = stringPreferencesKey("profile_age")
        val PROFILE_COLOR = intPreferencesKey("profile_color")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val INSTALLED_VERSION_CODE = intPreferencesKey("installed_version_code")
        val DEVICE_TYPE = stringPreferencesKey("device_type")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val SKIP_INTRO = booleanPreferencesKey("skip_intro")
    }

    val skipIntro: Flow<Boolean> = context.dataStore.data.map { it[SKIP_INTRO] ?: false }
    val userEmail: Flow<String> = context.dataStore.data.map { it[USER_EMAIL] ?: "" }
    val lastShowId: Flow<String?> = context.dataStore.data.map { it[LAST_SHOW_ID] }
    val lastEpisodeId: Flow<String?> = context.dataStore.data.map { it[LAST_EPISODE_ID] }
    val autoUpdate: Flow<Boolean> = context.dataStore.data.map { it[AUTO_UPDATE] ?: true }
    val showContinueWatching: Flow<Boolean> = context.dataStore.data.map { it[SHOW_CONTINUE_WATCHING] ?: true }
    val appLanguage: Flow<String> = context.dataStore.data.map { it[APP_LANGUAGE] ?: "ar" }
    val playerAutoHide: Flow<Boolean> = context.dataStore.data.map { it[PLAYER_AUTO_HIDE] ?: true }
    val playerHideSeconds: Flow<Int> = context.dataStore.data.map { it[PLAYER_HIDE_SECONDS] ?: 10 }
    val profileAvatar: Flow<String> = context.dataStore.data.map { it[PROFILE_AVATAR] ?: "" }
    val profileName: Flow<String> = context.dataStore.data.map { it[PROFILE_NAME] ?: "ضيف" }
    val profileAge: Flow<String> = context.dataStore.data.map { it[PROFILE_AGE] ?: "" }
    val profileColor: Flow<Int> = context.dataStore.data.map { it[PROFILE_COLOR] ?: 0xFFE50914.toInt() }
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_COMPLETED] ?: false }
    val installedVersionCode: Flow<Int> = context.dataStore.data.map { it[INSTALLED_VERSION_CODE] ?: 0 }
    val deviceType: Flow<String> = context.dataStore.data.map { it[DEVICE_TYPE] ?: "" }

    suspend fun setDeviceType(type: String) {
        context.dataStore.edit { it[DEVICE_TYPE] = type }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setProfileInfo(name: String, color: Int, avatar: String) {
        context.dataStore.edit {
            it[PROFILE_NAME] = name
            it[PROFILE_COLOR] = color
            it[PROFILE_AVATAR] = avatar
        }
    }

    suspend fun setUserAccount(username: String, email: String) {
        context.dataStore.edit {
            it[PROFILE_NAME] = username
            it[USER_EMAIL] = email
        }
    }

    suspend fun setProfileAge(age: String) {
        context.dataStore.edit { it[PROFILE_AGE] = age }
    }

    suspend fun setInstalledVersionCode(versionCode: Int) {
        context.dataStore.edit { it[INSTALLED_VERSION_CODE] = versionCode }
    }

    suspend fun saveLastWatched(showId: String, episodeId: String) {
        context.dataStore.edit {
            it[LAST_SHOW_ID] = showId
            it[LAST_EPISODE_ID] = episodeId
        }
    }

    suspend fun setPlayerAutoHide(enabled: Boolean) {
        context.dataStore.edit { it[PLAYER_AUTO_HIDE] = enabled }
    }

    suspend fun setPlayerHideSeconds(seconds: Int) {
        context.dataStore.edit { it[PLAYER_HIDE_SECONDS] = seconds }
    }

    suspend fun setAutoUpdate(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_UPDATE] = enabled }
    }

    suspend fun setShowContinueWatching(show: Boolean) {
        context.dataStore.edit { it[SHOW_CONTINUE_WATCHING] = show }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[APP_LANGUAGE] = lang }
    }

    suspend fun setSkipIntro(skip: Boolean) {
        context.dataStore.edit { it[SKIP_INTRO] = skip }
    }

    suspend fun setProfileAvatar(url: String) {
        context.dataStore.edit { it[PROFILE_AVATAR] = url }
    }

    fun getLastSeenShowIdSync(): String? {
        val prefs = context.getSharedPreferences("worker_prefs", Context.MODE_PRIVATE)
        return prefs.getString("last_seen_show_id", null)
    }

    fun saveLastSeenShowIdSync(id: String) {
        val prefs = context.getSharedPreferences("worker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("last_seen_show_id", id).apply()
    }
}
