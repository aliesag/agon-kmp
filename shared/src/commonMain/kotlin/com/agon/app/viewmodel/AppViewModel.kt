package com.agon.app.viewmodel

import androidx.lifecycle.ViewModel
import com.agon.app.data.FirebaseHttp
import androidx.lifecycle.viewModelScope
import com.agon.app.data.*
import kotlinx.datetime.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Progress(val showId: String, val fraction: Float, val label: String)

class AppViewModel(private val prefsManager: PrefsManager) : ViewModel() {

    private val _autoUpdate = MutableStateFlow(true)
    val autoUpdate = _autoUpdate.asStateFlow()

    private val _deviceType = MutableStateFlow("mobile")
    val deviceType = _deviceType.asStateFlow()

    private val _showContinueWatching = MutableStateFlow(true)
    val showContinueWatching = _showContinueWatching.asStateFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo = _updateInfo.asStateFlow()

    private val _remoteLogoUrl = MutableStateFlow<String?>(null)
    val remoteLogoUrl = _remoteLogoUrl.asStateFlow()

    private val _lastWatchedShow = MutableStateFlow<Show?>(null)
    val lastWatchedShow = _lastWatchedShow.asStateFlow()

    private val _lastWatchedEpisode = MutableStateFlow<Episode?>(null)
    val lastWatchedEpisode = _lastWatchedEpisode.asStateFlow()

    private val _shows = MutableStateFlow<List<Show>>(Catalog.all.filterNot { Catalog.isTashShow(it.title, it.link, it.id) })
    val shows: StateFlow<List<Show>> = _shows.asStateFlow()

    private val _hasPlayedIntroSession = MutableStateFlow(false)
    val hasPlayedIntroSession: StateFlow<Boolean> = _hasPlayedIntroSession.asStateFlow()

    val skipIntro: StateFlow<Boolean> = prefsManager.skipIntro.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    fun setSkipIntro(skip: Boolean) {
        viewModelScope.launch {
            prefsManager.setSkipIntro(skip)
        }
    }

    fun markIntroCompleted() {
        _hasPlayedIntroSession.value = true
    }

    private val _categories = MutableStateFlow<List<Category>>(Catalog.categories)
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _showEpisodes = MutableStateFlow<Map<String, List<Episode>>>(emptyMap())
    val showEpisodes: StateFlow<Map<String, List<Episode>>> = _showEpisodes.asStateFlow()

    private val _isLoadingEpisodes = MutableStateFlow(false)
    val isLoadingEpisodes: StateFlow<Boolean> = _isLoadingEpisodes.asStateFlow()

    private val _videoUrl = MutableStateFlow<String?>(null)
    val videoUrl: StateFlow<String?> = _videoUrl.asStateFlow()

    private val _isLoadingVideo = MutableStateFlow(false)
    val isLoadingVideo: StateFlow<Boolean> = _isLoadingVideo.asStateFlow()

    private val _servers = MutableStateFlow<List<VideoServer>>(emptyList())
    val servers: StateFlow<List<VideoServer>> = _servers.asStateFlow()

    private val _selectedServer = MutableStateFlow<VideoServer?>(null)
    val selectedServer: StateFlow<VideoServer?> = _selectedServer.asStateFlow()

    private val _myList = MutableStateFlow<Set<String>>(emptySet())
    val myList: StateFlow<Set<String>> = _myList.asStateFlow()

    private val _continueWatching = MutableStateFlow(
        listOf(
            Progress("qrmzi_2", 0.12f, "48 دقيقة متبقية"),
        )
    )
    val continueWatching: StateFlow<List<Progress>> = _continueWatching.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _wifiOnly = MutableStateFlow(true)
    val wifiOnly: StateFlow<Boolean> = _wifiOnly.asStateFlow()

    private val _notifications = MutableStateFlow(true)
    val notifications: StateFlow<Boolean> = _notifications.asStateFlow()

    private val _autoplay = MutableStateFlow(false)
    val autoplay: StateFlow<Boolean> = _autoplay.asStateFlow()

    private val _shortsList = MutableStateFlow<List<ShortItem>>(emptyList())
    val shortsList: StateFlow<List<ShortItem>> = _shortsList.asStateFlow()

    private val _profileName = MutableStateFlow("ضيف")
    val profileName: StateFlow<String> = _profileName.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _profileColor = MutableStateFlow(0xFFE50914.toInt())
    val profileColor: StateFlow<Int> = _profileColor.asStateFlow()

    private val _profileAvatar = MutableStateFlow("")
    val profileAvatar: StateFlow<String> = _profileAvatar.asStateFlow()

    private val _profileAge = MutableStateFlow("")
    val profileAge: StateFlow<String> = _profileAge.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(true)
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _isInitialized = MutableStateFlow(true)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _downloads = MutableStateFlow<Set<String>>(setOf("qrmzi_0"))
    val downloads: StateFlow<Set<String>> = _downloads.asStateFlow()

    private val _watchHistory = MutableStateFlow<List<String>>(listOf("qrmzi_0", "qrmzi_2"))
    val watchHistory: StateFlow<List<String>> = _watchHistory.asStateFlow()

    private val _appLanguage = MutableStateFlow("العربية")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _playerAutoHide = MutableStateFlow(false)
    val playerAutoHide: StateFlow<Boolean> = _playerAutoHide.asStateFlow()

    private val _playerHideSeconds = MutableStateFlow(10)
    val playerHideSeconds: StateFlow<Int> = _playerHideSeconds.asStateFlow()

    private val _isSearchingRemote = MutableStateFlow(false)
    val isSearchingRemote: StateFlow<Boolean> = _isSearchingRemote.asStateFlow()

    init {
        loadHomeScrapedData()
        observePrefs()
        checkForUpdates()
        fetchShortsFromRemote()
    }

    private fun getPosterUrl(coverUrl: String): String {
        if (coverUrl.contains("poster", ignoreCase = true)) return coverUrl
        val specificMap = mapOf(
            "Doganin-Kanunu-Cover-470x264.jpg" to "Doganin-Kanunu-Poster-374x520.jpg",
            "Alti-Ustu-Istanbul-Cover-470x255.jpg" to "Alti-Ustu-Istanbul-Poster-349x520.jpg",
            "Daha-17-Cover-470x255.jpg" to "Daha-17-Poster-349x520.jpg",
            "Abi-Cover-470x264.jpg" to "Abi-Poster-416x520.jpg",
            "Teskilat-Cover-470x265.jpg" to "Teskilat-Poster-416x520.jpg",
            "Tasacak-Bu-Deniz-cover-470x272.jpg" to "Tasacak-Bu-Deniz-poster-390x520.jpg",
            "Sevdigim-Sensin-cover-470x255.jpg" to "Sevdigim-Sensin-poster-364x520.jpg",
            "Cirkin-Cover-470x265.jpg" to "Cirkin-Poster-390x520.jpg",
            "Kurulus-Orhan-Cover-470x265.jpg" to "Kurulus-Orhan-poster-347x520.jpg",
            "Delikanli-Cover-470x234.jpg" to "Delikanli-Poster-368x520.jpg"
        )
        for ((cover, poster) in specificMap) {
            if (coverUrl.contains(cover, ignoreCase = true)) {
                return coverUrl.replace(cover, poster, ignoreCase = true)
            }
        }
        var url = coverUrl
        url = url.replace("cover-470x255", "poster-349x520", ignoreCase = true)
        url = url.replace("Cover-470x255", "Poster-349x520", ignoreCase = true)
        return url
    }

    private fun getGenresForShow(id: String, baseGenres: List<String>): List<String> {
        val seed = kotlin.math.abs(id.hashCode())
        val extra = when (seed % 7) {
            0 -> listOf("رومانسي")
            1 -> listOf("أكشن", "إثارة")
            2 -> listOf("جريمة", "إثارة")
            3 -> listOf("خيال علمي")
            4 -> listOf("رعب", "إثارة")
            5 -> listOf("مغامرة")
            else -> listOf("دراما")
        }
        return (baseGenres + extra).distinct()
    }

    private fun observePrefs() {
        viewModelScope.launch {
            prefsManager.deviceType.collectLatest { _deviceType.value = it }
        }
        viewModelScope.launch {
            prefsManager.autoUpdate.collectLatest { _autoUpdate.value = it }
        }
        viewModelScope.launch {
            prefsManager.showContinueWatching.collectLatest { _showContinueWatching.value = it }
        }
        viewModelScope.launch {
            prefsManager.appLanguage.collectLatest { _appLanguage.value = if (it == "ar") "العربية" else "English" }
        }
        viewModelScope.launch {
            prefsManager.playerAutoHide.collectLatest { _playerAutoHide.value = it }
        }
        viewModelScope.launch {
            prefsManager.playerHideSeconds.collectLatest { _playerHideSeconds.value = it }
        }
        viewModelScope.launch {
            prefsManager.profileAvatar.collectLatest { _profileAvatar.value = it }
        }
        viewModelScope.launch {
            prefsManager.profileName.collectLatest { _profileName.value = it }
        }
        viewModelScope.launch {
            prefsManager.userEmail.collectLatest { _userEmail.value = it }
        }
        viewModelScope.launch {
            prefsManager.profileAge.collectLatest { _profileAge.value = it }
        }
        viewModelScope.launch {
            prefsManager.profileColor.collectLatest { _profileColor.value = it }
        }
        viewModelScope.launch {
            prefsManager.onboardingCompleted.collectLatest { 
                _onboardingCompleted.value = it
                _isInitialized.value = true
            }
        }
        
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                prefsManager.lastShowId,
                prefsManager.lastEpisodeId,
                _shows,
                _showEpisodes
            ) { showId: String?, epId: String?, shows: List<Show>, epMap: Map<String, List<Episode>> ->
                val show = shows.find { it.id == showId }
                val episodes = epMap[showId ?: ""]
                val episode = episodes?.find { it.id == epId }
                Triple(show, episode, epId)
            }.collectLatest { (show, episode, epId) ->
                _lastWatchedShow.value = show
                _lastWatchedEpisode.value = episode ?: if (epId != null) Episode(id = epId, title = "جاري التحميل...", episodeNum = "?", poster = "", link = "") else null
            }
        }
    }

    fun saveProgress(showId: String, episodeId: String) {
        viewModelScope.launch {
            prefsManager.saveLastWatched(showId, episodeId)
        }
    }

    private fun checkForUpdates() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            try {
                val url = "https://raw.githubusercontent.com/mrpubg252-cmd/esp-config/refs/heads/main/update.json"
                val jsonStr = (com.agon.app.Http.get(url) ?: "")
                val json = org.json.JSONObject(jsonStr)
                val info = UpdateInfo(
                    versionCode = json.getInt("versionCode"),
                    versionName = json.getString("versionName"),
                    features = json.getString("features"),
                    downloadUrl = json.getString("downloadUrl"),
                    isMandatory = json.optBoolean("isMandatory", true)
                )
                
                _remoteLogoUrl.value = json.optString("logoUrl", null)

                val currentVersionName = com.agon.app.BuildConfig.VERSION_NAME.trim().removePrefix("v").removePrefix("V")
                val targetVersionName = info.versionName.trim().removePrefix("v").removePrefix("V")
                
                val currentVersionCode = com.agon.app.BuildConfig.VERSION_CODE
                val isCurrentOrNewer = info.versionCode <= currentVersionCode || currentVersionName == targetVersionName

                if (!isCurrentOrNewer && info.versionCode > currentVersionCode) {
                    _updateInfo.value = info
                } else {
                    _updateInfo.value = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun dismissUpdate(versionCode: Int) {
        viewModelScope.launch {
            prefsManager.setInstalledVersionCode(versionCode)
            _updateInfo.value = null
        }
    }

    fun setDeviceType(type: String) {
        viewModelScope.launch { prefsManager.setDeviceType(type) }
    }

    fun setAutoUpdate(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setAutoUpdate(enabled) }
    }

    fun setShowContinueWatching(show: Boolean) {
        viewModelScope.launch { prefsManager.setShowContinueWatching(show) }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { 
            prefsManager.setLanguage(if (lang == "العربية") "ar" else "en")
        }
    }

    fun setPlayerAutoHide(enabled: Boolean) {
        viewModelScope.launch {
            prefsManager.setPlayerAutoHide(enabled)
        }
    }

    fun setPlayerHideSeconds(seconds: Int) {
        viewModelScope.launch {
            prefsManager.setPlayerHideSeconds(seconds)
        }
    }

    fun toggleLikeShort(shortId: String) {
        val target = _shortsList.value.find { it.id == shortId } ?: return
        val newLiked = !target.isLiked
        val newLikesCount = if (newLiked) target.likesCount + 1 else (target.likesCount - 1).coerceAtLeast(0)

        _shortsList.value = _shortsList.value.map { item ->
            if (item.id == shortId) {
                item.copy(isLiked = newLiked, likesCount = newLikesCount)
            } else item
        }

        // Sync like count to Firebase Realtime Database
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            try {
                val patchUrl = "https://chat-516a8-default-rtdb.firebaseio.com/shorts_v1/$shortId.json"
                val json = org.json.JSONObject().apply {
                    put("likesCount", newLikesCount)
                }
                FirebaseHttp.patch(patchUrl, json.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleSaveShort(shortId: String) {
        _shortsList.value = _shortsList.value.map { item ->
            if (item.id == shortId) {
                item.copy(isSaved = !item.isSaved)
            } else item
        }
    }

    fun incrementShareShort(shortId: String) {
        var updatedSharesCount = 0
        _shortsList.value = _shortsList.value.map { item ->
            if (item.id == shortId) {
                updatedSharesCount = item.sharesCount + 1
                item.copy(sharesCount = updatedSharesCount)
            } else item
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            try {
                val patchUrl = "https://chat-516a8-default-rtdb.firebaseio.com/shorts_v1/$shortId.json"
                val patchJson = org.json.JSONObject().apply {
                    put("sharesCount", updatedSharesCount)
                }
                FirebaseHttp.patch(patchUrl, patchJson.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addCommentToShort(shortId: String, text: String, linkedShowId: String? = null) {
        if (text.isBlank() && linkedShowId.isNullOrEmpty()) return
        val currentProfile = _profileName.value.ifEmpty { "مستخدم" }
        val commentId = "comment_${Clock.System.now().toEpochMilliseconds()}"
        val newComment = ShortComment(
            id = commentId,
            user = currentProfile,
            avatar = _profileAvatar.value,
            text = text.trim(),
            linkedShowId = linkedShowId
        )

        var updatedCommentsCount = 0
        _shortsList.value = _shortsList.value.map { item ->
            if (item.id == shortId) {
                val updatedList = listOf(newComment) + item.comments
                updatedCommentsCount = item.commentsCount + 1
                item.copy(
                    comments = updatedList,
                    commentsCount = updatedCommentsCount
                )
            } else item
        }

        // Sync comment to Firebase Realtime Database
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            try {
                val commentUrl = "https://chat-516a8-default-rtdb.firebaseio.com/shorts_v1/$shortId/comments/$commentId.json"
                val commentJson = org.json.JSONObject().apply {
                    put("id", commentId)
                    put("user", currentProfile)
                    put("avatar", _profileAvatar.value)
                    put("text", text.trim())
                    if (linkedShowId != null) put("linkedShowId", linkedShowId)
                    put("timeAgo", "الآن")
                    put("createdAt", Clock.System.now().toEpochMilliseconds())
                }
                FirebaseHttp.put(commentUrl, commentJson.toString())

                val patchUrl = "https://chat-516a8-default-rtdb.firebaseio.com/shorts_v1/$shortId.json"
                val patchJson = org.json.JSONObject().apply {
                    put("commentsCount", updatedCommentsCount)
                }
                FirebaseHttp.patch(patchUrl, patchJson.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun uploadShortToFirebase(shortItem: ShortItem) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            try {
                val firebaseDbUrl = "https://chat-516a8-default-rtdb.firebaseio.com/shorts_v1/${shortItem.id}.json"
                val jsonObj = org.json.JSONObject().apply {
                    put("id", shortItem.id)
                    put("title", shortItem.title)
                    put("description", shortItem.description)
                    put("videoUrl", shortItem.videoUrl)
                    put("posterUrl", shortItem.posterUrl)
                    put("uploaderName", shortItem.uploaderName)
                    put("uploaderAvatar", shortItem.uploaderAvatar)
                    put("audioTitle", shortItem.audioTitle)
                    shortItem.linkedShowId?.let { put("linkedShowId", it) }
                    put("likesCount", shortItem.likesCount)
                    put("commentsCount", shortItem.commentsCount)
                    put("sharesCount", shortItem.sharesCount)
                    put("createdAt", Clock.System.now().toEpochMilliseconds())
                }
                FirebaseHttp.put(firebaseDbUrl, jsonObj.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addShort(title: String, description: String, videoUrl: String, linkedShowId: String? = null) {
        if (videoUrl.isBlank()) return
        val newShort = ShortItem(
            id = "short_${Clock.System.now().toEpochMilliseconds()}",
            title = title.ifEmpty { "شورتس جديد" },
            description = description,
            videoUrl = videoUrl,
            uploaderName = _profileName.value.ifEmpty { "حكاياتنا شورتس" },
            uploaderAvatar = _profileAvatar.value,
            audioTitle = "الصوت الأصلي - " + _profileName.value.ifEmpty { "حكاياتنا" },
            linkedShowId = linkedShowId
        )
        _shortsList.value = listOf(newShort) + _shortsList.value
        uploadShortToFirebase(newShort)
    }

    fun addShowIfAbsent(show: Show) {
        if (Catalog.isTashShow(show.title, show.link, show.id)) return
        val current = _shows.value.toMutableList()
        val cleanShow = Catalog.sanitizeShowToSeries(show)
        if (current.none { it.id == cleanShow.id || (it.link.isNotEmpty() && it.link == cleanShow.link) || it.title.equals(cleanShow.title, ignoreCase = true) }) {
            current.add(0, cleanShow)
            val updated = deduplicateByTitle(current)
            _shows.value = updated
            rebuildCategories(updated)
        }
    }

    suspend fun searchShowRemotely(title: String): Show? {
        if (title.isBlank() || Catalog.isTashShow(title)) return null

        // 1. Check existing shows in memory
        val allCurrent = _shows.value.ifEmpty { Catalog.all }
        val localMatch = Catalog.findMatchingShow(title, allCurrent)
        if (localMatch != null) return localMatch

        // 2. Perform live search via 3cktv and Fosta
        return withContext(Dispatchers.Default) {
            try {
                val cleanQuery = title
                    .replace("مسلسل", "")
                    .replace("فيلم", "")
                    .replace("فلم", "")
                    .replace("مشاهدة", "")
                    .replace("حلقات", "")
                    .replace("حلقة", "")
                    .replace(Regex("[*\"'«»]"), "")
                    .trim()

                if (cleanQuery.isBlank() || Catalog.isTashShow(cleanQuery)) return@withContext null

                val job3ck = async {
                    try { EpisodeScraper.search3cktv(cleanQuery) } catch (_: Exception) { emptyList() }
                }
                val jobFosta = async {
                    try { EpisodeScraper.searchFosta(cleanQuery) } catch (_: Exception) { emptyList() }
                }

                val results = (job3ck.await() + jobFosta.await())
                    .filterNot { isAlooytvSeries(it) || Catalog.isTashShow(it.title, it.link, it.id) }

                if (results.isNotEmpty()) {
                    val candidate = results.firstOrNull {
                        val normShow = Catalog.normalizeTitle(it.title)
                        val normQ = Catalog.normalizeTitle(cleanQuery)
                        normShow.contains(normQ) || normQ.contains(normShow)
                    } ?: results.first()

                    val cleanShow = Catalog.sanitizeShowToSeries(candidate)
                    withContext(Dispatchers.Main) {
                        addShowIfAbsent(cleanShow)
                    }
                    cleanShow
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun fetchShortsFromRemote() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            try {
                val firebaseDbUrl = "https://chat-516a8-default-rtdb.firebaseio.com/shorts_v1.json"
                val bodyString = com.agon.app.Http.get(firebaseDbUrl)
                if (!bodyString.isNullOrBlank() && bodyString != "null") {
                    val jsonObject = org.json.JSONObject(bodyString)
                    val keys = jsonObject.keys()
                    val firebaseShorts = mutableListOf<ShortItem>()

                    while (keys.hasNext()) {
                        val key = keys.next()
                        val itemObj = jsonObject.optJSONObject(key) ?: continue
                        val title = itemObj.optString("title", "شورتس مميز")
                        val description = itemObj.optString("description", "")
                        val videoUrl = itemObj.optString("videoUrl", "")
                        if (videoUrl.isBlank()) continue
                        val posterUrl = itemObj.optString("posterUrl", "")
                        val uploaderName = itemObj.optString("uploaderName", "حكاياتنا شورتس")
                        val uploaderAvatar = itemObj.optString("uploaderAvatar", "")
                        val audioTitle = itemObj.optString("audioTitle", "الصوت الأصلي")
                        val linkedShowId = if (itemObj.has("linkedShowId")) itemObj.optString("linkedShowId") else null
                        val likesCount = itemObj.optInt("likesCount", 0)
                        val commentsCount = itemObj.optInt("commentsCount", 0)
                        val sharesCount = itemObj.optInt("sharesCount", 0)

                        val commentsList = mutableListOf<ShortComment>()
                        val commentsObj = itemObj.opt("comments")
                        if (commentsObj is org.json.JSONObject) {
                            val cKeys = commentsObj.keys()
                            while (cKeys.hasNext()) {
                                val ck = cKeys.next()
                                val cObj = commentsObj.optJSONObject(ck) ?: continue
                                commentsList.add(
                                    ShortComment(
                                        id = ck,
                                        user = cObj.optString("user", "مستخدم"),
                                        avatar = cObj.optString("avatar", ""),
                                        text = cObj.optString("text", ""),
                                        timeAgo = cObj.optString("timeAgo", "الآن"),
                                        linkedShowId = if (cObj.has("linkedShowId")) cObj.optString("linkedShowId") else null
                                    )
                                )
                            }
                        } else if (commentsObj is org.json.JSONArray) {
                            for (ci in 0 until commentsObj.length()) {
                                val cObj = commentsObj.optJSONObject(ci) ?: continue
                                commentsList.add(
                                    ShortComment(
                                        id = cObj.optString("id", "c_$ci"),
                                        user = cObj.optString("user", "مستخدم"),
                                        avatar = cObj.optString("avatar", ""),
                                        text = cObj.optString("text", ""),
                                        timeAgo = cObj.optString("timeAgo", "الآن"),
                                        linkedShowId = if (cObj.has("linkedShowId")) cObj.optString("linkedShowId") else null
                                    )
                                )
                            }
                        }

                        firebaseShorts.add(
                            ShortItem(
                                id = key,
                                title = title,
                                description = description,
                                videoUrl = videoUrl,
                                posterUrl = posterUrl,
                                uploaderName = uploaderName,
                                uploaderAvatar = uploaderAvatar,
                                audioTitle = audioTitle,
                                linkedShowId = linkedShowId,
                                likesCount = likesCount,
                                commentsCount = commentsList.size.coerceAtLeast(commentsCount),
                                sharesCount = sharesCount,
                                comments = commentsList
                            )
                        )
                    }

                    if (firebaseShorts.isNotEmpty()) {
                        val currentMap = _shortsList.value.associateBy { it.id }
                        val mergedList = firebaseShorts.map { fbItem ->
                            val local = currentMap[fbItem.id]
                            if (local != null) {
                                fbItem.copy(isLiked = local.isLiked, isSaved = local.isSaved)
                            } else {
                                fbItem
                            }
                        }
                        _shortsList.value = mergedList
                    } else {
                        _shortsList.value = getDefaultSampleShorts()
                    }
                } else {
                    _shortsList.value = getDefaultSampleShorts()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (_shortsList.value.isEmpty()) {
                    _shortsList.value = getDefaultSampleShorts()
                }
            }
        }
    }

    private fun getDefaultSampleShorts(): List<ShortItem> {
        return listOf(
            ShortItem(
                id = "sample_1",
                title = "شباب البومب - لقطة كوميدية رهيبة 😂",
                description = "أروع مقاطع الكوميديا السعودية والخليجية الحصرية على تطبيقنا #شباب_البومب #كوميديا",
                videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4",
                posterUrl = "https://image.tmdb.org/t/p/w500/5E2tW6WMUDux911r6m7haRef0WH.jpg",
                uploaderName = "حكاياتنا كوميدي",
                uploaderAvatar = "https://i.ibb.co/0wvJfBH/file-00000000c1e4720a9aba88f120b35bd1.png",
                audioTitle = "الصوت الأصلي - شباب البومب",
                likesCount = 1420,
                commentsCount = 85,
                sharesCount = 320
            ),
            ShortItem(
                id = "sample_2",
                title = "ولاد رزق 3 - حماس وأكشن لا ينتهي 🔥",
                description = "مغامرة حماسية ومعركة حامية الوطيس في شوارع الرياض #ولاد_رزق #أكشن",
                videoUrl = "https://www.w3schools.com/html/movie.mp4",
                posterUrl = "https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg",
                uploaderName = "عشاق السينما",
                uploaderAvatar = "https://i.ibb.co/0wvJfBH/file-00000000c1e4720a9aba88f120b35bd1.png",
                audioTitle = "موسيقى الأكشن الحماسية - ولاد رزق",
                likesCount = 3890,
                commentsCount = 210,
                sharesCount = 890
            )
        )
    }

    private fun isAlooytvSeries(show: Show): Boolean {
        return (show.link.contains("alooytv", ignoreCase = true) || show.id.contains("alooy", ignoreCase = true)) &&
                (show.title.contains("مسلسل") || show.genres.any { it.contains("مسلسل") || it.contains("رمضان") || it.contains("خليجي") } ||
                        show.id.startsWith("ramadan_") || show.id.startsWith("kleeji_") || show.id.startsWith("arabic_") || show.id.startsWith("asia_"))
    }

    private fun loadHomeScrapedData() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            try {
                val currentShows = mutableListOf<Show>()
                currentShows.addAll(Catalog.all.filterNot { isAlooytvSeries(it) || Catalog.isTashShow(it.title, it.link, it.id) })
                val initialList = deduplicateByTitle(currentShows)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _shows.value = initialList
                    rebuildCategories(initialList)
                }

                // 1. Fetch live updates directly from https://e.3cktv.com/ root homepage
                val rootHomeShows = try {
                    EpisodeScraper.scrape3cktvRootHome()
                } catch (e: Exception) {
                    emptyList<Show>()
                }

                if (rootHomeShows.isNotEmpty()) {
                    rootHomeShows.reversed().forEach { newShow ->
                        if (!isAlooytvSeries(newShow) && !Catalog.isTashShow(newShow.title, newShow.link, newShow.id) && currentShows.none { it.id == newShow.id || (it.link.isNotEmpty() && it.link == newShow.link) }) {
                            currentShows.add(0, newShow)
                        }
                    }
                    val updated = deduplicateByTitle(currentShows)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        _shows.value = updated
                        rebuildCategories(updated)
                    }
                }

                // 2. Fetch series page updates
                val latestFromHome = try {
                    EpisodeScraper.scrapeHomePageLatest()
                } catch (e: Exception) {
                    emptyList<Show>()
                }

                if (latestFromHome.isNotEmpty()) {
                    latestFromHome.forEach { newShow ->
                        if (!isAlooytvSeries(newShow) && !Catalog.isTashShow(newShow.title, newShow.link, newShow.id) && currentShows.none { it.id == newShow.id || (it.link.isNotEmpty() && it.link == newShow.link) }) {
                            currentShows.add(0, newShow)
                        }
                    }
                    val updated = deduplicateByTitle(currentShows)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        _shows.value = updated
                        rebuildCategories(updated)
                    }
                }

                // 3. Fetch ALL 50 pages of 3cktv series in parallel chunks
                try {
                    for (batchStart in 1..50 step 5) {
                        val batchEnd = (batchStart + 4).coerceAtMost(50)
                        val jobs = (batchStart..batchEnd).map { page ->
                            async {
                                try {
                                    EpisodeScraper.scrapeSeriesPage(page, "e.3cktv.com")
                                } catch (e: Exception) {
                                    emptyList()
                                }
                            }
                        }
                        val results = jobs.awaitAll().flatten()
                        if (results.isNotEmpty()) {
                            var addedAny = false
                            results.forEach { newShow ->
                                if (!Catalog.isTashShow(newShow.title, newShow.link, newShow.id) && currentShows.none { 
                                    it.title.equals(newShow.title, ignoreCase = true) || 
                                    (it.link.isNotEmpty() && it.link == newShow.link) 
                                }) {
                                    currentShows.add(newShow)
                                    addedAny = true
                                }
                            }
                            if (addedAny) {
                                val updated = deduplicateByTitle(currentShows)
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    _shows.value = updated
                                    rebuildCategories(updated)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 4. Fetch live featured series and movies from home
                try {
                    val fostaHome = EpisodeScraper.scrapeFostaHomePage()
                    if (fostaHome.isNotEmpty()) {
                        var added = false
                        fostaHome.forEach { show ->
                            if (!isAlooytvSeries(show) && !Catalog.isTashShow(show.title, show.link, show.id) && currentShows.none { it.id == show.id || (it.link.isNotEmpty() && it.link == show.link) }) {
                                currentShows.add(show)
                                added = true
                            }
                        }
                        if (added) {
                            val updated = deduplicateByTitle(currentShows)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                _shows.value = updated
                                rebuildCategories(updated)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 5. Ingest series catalog (Arabic & popular series)
                try {
                    val seriesJobs = (1..3).map { page ->
                        async {
                            try {
                                EpisodeScraper.scrapeFostaSeries(page)
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }
                    val seriesResults = seriesJobs.awaitAll().flatten()
                    if (seriesResults.isNotEmpty()) {
                        var added = false
                        seriesResults.forEach { show ->
                            if (!isAlooytvSeries(show) && !Catalog.isTashShow(show.title, show.link, show.id) && currentShows.none { 
                                it.title.equals(show.title, ignoreCase = true) ||
                                (it.link.isNotEmpty() && it.link == show.link)
                            }) {
                                currentShows.add(show)
                                added = true
                            }
                        }
                        if (added) {
                            val updated = deduplicateByTitle(currentShows)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                _shows.value = updated
                                rebuildCategories(updated)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 6. Ingest movies catalog (Arabic & foreign movies)
                try {
                    val arabicMoviesJob = async {
                        try {
                            EpisodeScraper.scrapeFostaMovies("1aflam-3araby", "افلام عربية", 1)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                    val foreignMoviesJob = async {
                        try {
                            EpisodeScraper.scrapeFostaMovies("english-movies", "افلام اجنبية", 1)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                    val movieResults = (arabicMoviesJob.await() + foreignMoviesJob.await())
                    if (movieResults.isNotEmpty()) {
                        var added = false
                        movieResults.forEach { show ->
                            if (!isAlooytvSeries(show) && !Catalog.isTashShow(show.title, show.link, show.id) && currentShows.none { 
                                it.title.equals(show.title, ignoreCase = true) ||
                                (it.link.isNotEmpty() && it.link == show.link)
                            }) {
                                currentShows.add(show)
                                added = true
                            }
                        }
                        if (added) {
                            val updated = deduplicateByTitle(currentShows)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                _shows.value = updated
                                rebuildCategories(updated)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun deduplicateByTitle(list: List<Show>): List<Show> {
        val map = LinkedHashMap<String, Show>()
        for (rawShow in list) {
            if (Catalog.isTashShow(rawShow.title, rawShow.link, rawShow.id)) continue
            val show = Catalog.sanitizeShowToSeries(rawShow)
            if (Catalog.isTashShow(show.title, show.link, show.id)) continue
            val baseTitle = show.title
            
            val existing = map[baseTitle]
            if (existing == null) {
                map[baseTitle] = show
            } else {
                val updatedPoster = when {
                    existing.poster.contains("image.tmdb.org") -> existing.poster
                    show.poster.contains("image.tmdb.org") -> show.poster
                    existing.poster.isNotBlank() && !existing.poster.contains("grey.gif") -> existing.poster
                    show.poster.isNotBlank() && !show.poster.contains("grey.gif") -> show.poster
                    else -> ""
                }
                val updatedBackdrop = when {
                    existing.backdrop.contains("image.tmdb.org") -> existing.backdrop
                    show.backdrop.contains("image.tmdb.org") -> show.backdrop
                    existing.backdrop.isNotBlank() && !existing.backdrop.contains("grey.gif") -> existing.backdrop
                    show.backdrop.isNotBlank() && !show.backdrop.contains("grey.gif") -> show.backdrop
                    else -> ""
                }
                val updatedLink = if (existing.link.contains("/series/") || existing.link.contains("view-serie")) existing.link else show.link
                val updatedId = if (existing.id.isNotEmpty() && !existing.id.contains("-ep-")) existing.id else show.id
                map[baseTitle] = existing.copy(
                    id = updatedId,
                    title = baseTitle,
                    poster = updatedPoster,
                    backdrop = updatedBackdrop,
                    link = updatedLink
                )
            }
        }
        return map.values.toList()
    }

    private fun rebuildCategories(showsList: List<Show>) {
        val cleanList = showsList.filterNot { isAlooytvSeries(it) || Catalog.isTashShow(it.title, it.link, it.id) }
        val turkishShows = cleanList.filter { it.genres.contains("مسلسلات تركية") || it.genres.contains("تركي") || it.link.contains("3cktv") }
        val dubbedTurkish = turkishShows.filter { it.title.contains("مدبلج") || it.rating.contains("مدبلج") || it.genres.contains("مدبلج") }
        val subbedTurkish = turkishShows.filter { !it.title.contains("مدبلج") && !it.rating.contains("مدبلج") && !it.genres.contains("مدبلج") }
        val arabicShows = cleanList.filter { it.genres.contains("مسلسلات عربية") || (!it.genres.contains("مسلسلات تركية") && !it.genres.contains("تركي") && !it.link.contains("3cktv") && !it.genres.contains("أفلام") && it.rating != "فيلم") }
        val movies = cleanList.filter { it.genres.contains("أفلام") || it.genres.contains("افلام عربية") || it.genres.contains("افلام اجنبية") || it.rating == "فيلم" || it.title.contains("فيلم") }
        val newReleases = cleanList.filter { it.isNew || it.id.startsWith("3ck_home_") || it.id.startsWith("fosta_h_") || it.ribbon == "حلقة جديدة" || it.ribbon == "مضاف حديثاً" || it.ribbon == "أحدث حلقة" }.ifEmpty { cleanList.take(20) }
        val topShows = cleanList.filter { it.isTop10 }.ifEmpty { cleanList.take(10) }

        _categories.value = listOf(
            Category("حلقة جديدة", newReleases),
            Category("أحدث الأفلام الحصرية", movies),
            Category("مسلسلات عربية ومصرية", arabicShows),
            Category("مسلسلات تركية مترجمة", subbedTurkish),
            Category("مسلسلات تركية مدبلجة", dubbedTurkish),
            Category("مسلسلات تركية حصرية", turkishShows),
            Category("الأكثر مشاهدة رائج الآن", topShows),
            Category("جميع المسلسلات والأفلام", cleanList)
        ).filter { it.shows.isNotEmpty() }
    }

    fun setVideoUrl(url: String?) {
        _videoUrl.value = url
    }

    fun clearEpisodeVideo() {
        videoLoadJob?.cancel()
        _videoUrl.value = null
        _servers.value = emptyList()
        _selectedServer.value = null
        _isLoadingVideo.value = false
    }

    private var videoLoadJob: kotlinx.coroutines.Job? = null

    fun loadEpisodeVideo(episode: Episode) {
        videoLoadJob?.cancel()
        _videoUrl.value = null
        _servers.value = emptyList()
        _selectedServer.value = null
        _isLoadingVideo.value = true
        videoLoadJob = viewModelScope.launch {
            val parsedServers = EpisodeScraper.getServersForEpisode(episode.link)
            if (parsedServers.isNotEmpty()) {
                _servers.value = parsedServers
                
                val defaultServer = if (episode.link.contains("alooytv") || episode.link.contains("alooytv16.xyz")) {
                    parsedServers.firstOrNull { it.name.contains("Cloudflare") || it.name.contains("1") || it.name.contains("سيرفر ١") || it.name.contains("Server 1") || it.name.contains("أساسي") }
                        ?: parsedServers.first()
                } else {
                    parsedServers.first()
                }
                
                _selectedServer.value = defaultServer

                val resolved = EpisodeScraper.resolveServerDirectUrl(defaultServer.url, episode.link) ?: defaultServer.url
                _videoUrl.value = resolved
            } else {
                val url = EpisodeScraper.getVideoUrlForEpisode(episode.link)
                val finalUrl = if (url != null) EpisodeScraper.resolveServerDirectUrl(url, episode.link) else null
                val effective = finalUrl ?: url
                _videoUrl.value = effective
            }
            _isLoadingVideo.value = false
        }
    }

    fun selectServer(server: VideoServer, episodeLink: String) {
        videoLoadJob?.cancel()
        _videoUrl.value = null
        _selectedServer.value = server
        _isLoadingVideo.value = true
        videoLoadJob = viewModelScope.launch {
            val resolved = EpisodeScraper.resolveServerDirectUrl(server.url, episodeLink) ?: server.url
            _videoUrl.value = resolved
            _isLoadingVideo.value = false
        }
    }

    fun selectNextServer(episodeLink: String) {
        val currentList = _servers.value
        val current = _selectedServer.value
        if (currentList.size > 1) {
            val currentIndex = currentList.indexOfFirst { it.url == current?.url }
            val nextIndex = if (currentIndex >= 0 && currentIndex + 1 < currentList.size) currentIndex + 1 else 0
            selectServer(currentList[nextIndex], episodeLink)
        }
    }

    fun loadEpisodes(show: Show) {
        if (Catalog.isTashShow(show.title, show.link, show.id)) return
        val existing = _showEpisodes.value[show.id]
        val hasFallback = existing?.any { it.id.startsWith("fallback_") } ?: false
        if (existing != null && !hasFallback) return 
        
        viewModelScope.launch {
            _isLoadingEpisodes.value = true
            var list = EpisodeScraper.getEpisodesForShow(show.link)
            if (list.isEmpty()) {
                // Try auto-recovering by searching remote sources
                val cleanTitle = show.title
                    .replace("فيلم", "")
                    .replace("مسلسل", "")
                    .replace("مدبلج", "")
                    .replace("مترجم", "")
                    .trim()
                if (cleanTitle.length >= 2) {
                    val s1 = EpisodeScraper.search3cktv(cleanTitle)
                    val s2 = EpisodeScraper.searchFosta(cleanTitle)
                    val found = (s1 + s2).firstOrNull { it.link.isNotBlank() && it.link != show.link }
                    if (found != null) {
                        val remoteEps = EpisodeScraper.getEpisodesForShow(found.link)
                        if (remoteEps.isNotEmpty()) {
                            list = remoteEps
                        }
                    }
                }
            }

            if (list.isEmpty()) {
                val isMovie = show.title.contains("فيلم") || show.genres.any { it.contains("افلام") || it.contains("فيلم") }
                val count = if (isMovie) 1 else 1
                list = (1..count).map { num ->
                    Episode(
                        id = "fallback_${show.id}_$num",
                        title = if (isMovie) "مشاهدة الفيلم كامل" else "${show.title} - العرض المباشر",
                        episodeNum = num.toString(),
                        poster = show.poster,
                        link = show.link
                    )
                }
            } else {
                list = list.distinctBy { it.episodeNum }.sortedBy { it.episodeNum.toIntOrNull() ?: 0 }
                // Use show.poster as fallback for any empty episode posters
                list = list.map { ep ->
                    if (ep.poster.isBlank()) {
                        ep.copy(poster = show.poster)
                    } else {
                        ep
                    }
                }
                if (show.ribbon == "تعرض قريباً" && list.none { ep -> ep.id == "coming_soon_next" }) {
                    val numbersList = list.mapNotNull { ep -> ep.episodeNum.toIntOrNull() }
                    val maxNum = if (numbersList.isNotEmpty()) numbersList.maxOrNull() ?: list.size else list.size
                    val nextNum = maxNum + 1
                    val comingSoonEp = Episode(
                        id = "coming_soon_next",
                        title = "تعرض قريباً على تطبيقنا",
                        episodeNum = nextNum.toString(),
                        poster = show.poster,
                        link = ""
                    )
                    list = list + listOf(comingSoonEp)
                }
            }
            _showEpisodes.value = _showEpisodes.value + (show.id to list)
            _isLoadingEpisodes.value = false
        }
    }

    private fun fetchEpisodeImages(showId: String, tmdbId: Int, mediaType: String) {
        if (mediaType != "tv") return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            try {
                val apiKey = if (com.agon.app.BuildConfig.TMDB_API_KEY.isEmpty() || com.agon.app.BuildConfig.TMDB_API_KEY == "YOUR_TMDB_API_KEY") {
                    "5afaeea7216a76d8c0600ecf217f6427"
                } else {
                    com.agon.app.BuildConfig.TMDB_API_KEY
                }
                val accessToken = if (com.agon.app.BuildConfig.TMDB_ACCESS_TOKEN.isEmpty() || com.agon.app.BuildConfig.TMDB_ACCESS_TOKEN == "YOUR_TMDB_ACCESS_TOKEN") {
                    "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI1YWZhZWVhNzIxNmE3NmQ4YzA2MDBlY2YyMTdmNjQyNyIsIm5iZiI6MTc2NTg5OTk5OC43NDg5OTk4LCJzdWIiOiI2OTQxN2VkZTc4OThlZTVlY2YzYzU0OTgiLCJzY29wZXMiOlsiYXBpX3JlYWQiXSwidmVyc2lvbiI6MX0.28BkynkM5qL_s8muk8Evl4aCLY2zXKATfiiikgs_Nww"
                } else {
                    com.agon.app.BuildConfig.TMDB_ACCESS_TOKEN
                }
                val episodes = _showEpisodes.value[showId] ?: return@launch
                
                // Fetch season 1 details
                val seasonUrl = "https://api.themoviedb.org/3/tv/$tmdbId/season/1?api_key=$apiKey&language=ar"
                val seasonHeaders = if (accessToken.isNotEmpty() && accessToken != "YOUR_TMDB_ACCESS_TOKEN")
                    mapOf("Authorization" to "Bearer $accessToken") else emptyMap()
                val seasonJsonStr = com.agon.app.Http.get(seasonUrl, seasonHeaders)
                val tmdbEpisodes = org.json.JSONObject(seasonJsonStr).optJSONArray("episodes")
                
                if (tmdbEpisodes != null) {
                    val tmdbEpMap = mutableMapOf<Int, String>()
                    for (i in 0 until tmdbEpisodes.length()) {
                        val ep = tmdbEpisodes.getJSONObject(i)
                        val epNum = ep.optInt("episode_number")
                        val stillPath = ep.optString("still_path", null)
                        if (stillPath != null && stillPath != "null") {
                            tmdbEpMap[epNum] = "https://image.tmdb.org/t/p/w300$stillPath"
                        }
                    }
                    
                    if (tmdbEpMap.isNotEmpty()) {
                        val updatedList = episodes.map { localEp ->
                            val num = localEp.episodeNum.toIntOrNull()
                            if (num != null && tmdbEpMap.containsKey(num)) {
                                localEp.copy(poster = tmdbEpMap[num]!!)
                            } else {
                                localEp
                            }
                        }
                        _showEpisodes.value = _showEpisodes.value + (showId to updatedList)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val pendingTmdbFetch = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun prefetchTmdbPostersForTopShows() {
        // Disabled: using original site images only
    }

    fun loadShowDetails(showId: String) {
        val show = _shows.value.firstOrNull { it.id == showId } ?: return
        if (show.castMembers.isNotEmpty()) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val updated = fetchTmdbDetailsForShow(show) ?: return@launch
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                val targetTitle = Catalog.cleanSeriesTitle(show.title)
                val updatedShows = _shows.value.map { 
                    val isSameShow = it.id == showId || 
                        (targetTitle.isNotBlank() && Catalog.cleanSeriesTitle(it.title) == targetTitle) ||
                        (show.link.isNotBlank() && it.link.isNotBlank() && it.link == show.link)
                    if (isSameShow) updated else it
                }
                _shows.value = updatedShows
                rebuildCategories(updatedShows)
            }
        }
    }

    private suspend fun fetchTmdbDetailsForShow(show: Show): Show? {
        try {
            val apiKey = if (com.agon.app.BuildConfig.TMDB_API_KEY.isEmpty() || com.agon.app.BuildConfig.TMDB_API_KEY == "YOUR_TMDB_API_KEY") {
                "5afaeea7216a76d8c0600ecf217f6427"
            } else {
                com.agon.app.BuildConfig.TMDB_API_KEY
            }
            val accessToken = if (com.agon.app.BuildConfig.TMDB_ACCESS_TOKEN.isEmpty() || com.agon.app.BuildConfig.TMDB_ACCESS_TOKEN == "YOUR_TMDB_ACCESS_TOKEN") {
                "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI1YWZhZWVhNzIxNmE3NmQ4YzA2MDBlY2YyMTdmNjQyNyIsIm5iZiI6MTc2NTg5OTk5OC43NDg5OTk4LCJzdWIiOiI2OTQxN2VkZTc4OThlZTVlY2YzYzU0OTgiLCJzY29wZXMiOlsiYXBpX3JlYWQiXSwidmVyc2lvbiI6MX0.28BkynkM5qL_s8muk8Evl4aCLY2zXKATfiiikgs_Nww"
            } else {
                com.agon.app.BuildConfig.TMDB_ACCESS_TOKEN
            }
            var cleanTitle = show.title
                .replace(Regex("(?i)\\b(مسلسل|فيلم|برنامج|انمي|مترجم|مدبلج|كامل|موقع|قصة عشق|شاهد|اون لاين|اونلاين|بجودة|عالية|العربية|عربي|شاشة|كاملة)\\b"), "")
                .replace(Regex("الحلقة\\s+\\d+"), "")
                .replace(Regex("حلقة\\s+\\d+"), "")
                .replace(Regex("والأخيرة|والاخيرة"), "")
                .replace(Regex("الموسم\\s+(الأول|الاول|الثاني|الثالث|الرابع|الخامس|السادس|السابع|الثامن|التاسع|العاشر|\\d+)"), "")
                .replace(Regex("الجزء\\s+(الأول|الاول|الثاني|الثالث|الرابع|الخامس|السادس|السابع|الثامن|التاسع|العاشر|\\d+)"), "")
                .replace(Regex("مترجمة للعربية|مدبلجة للعربية|مترجم للعربية|مدبلج للعربية|للعربية|مترجمة|مدبلجة"), "")
                .replace(Regex("(?i)\\b(hd|sd|fhd|4k|1080p|720p|وب|ويب|تحميل|شاهد|بجودة|عالية|مدبلج|مترجم)\\b"), "")
                .replace(Regex("\\(.*?\\)"), "")
                .replace(Regex("\\[.*?\\]"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
            
            cleanTitle = cleanTitle.replace(Regex("\\s+\\d+$"), "").trim()
            if (cleanTitle.isEmpty()) {
                cleanTitle = show.title
            }

            val candidateQueries = mutableListOf<String>()
            if (cleanTitle.isNotBlank()) candidateQueries.add(cleanTitle)

            TmdbHelper.KNOWN_ALIASES.forEach { (key, aliases) ->
                if (cleanTitle.contains(key) || show.title.contains(key)) {
                    candidateQueries.addAll(aliases)
                }
            }

            val latinFromTitle = Regex("""[a-zA-Z0-9'\s-]{3,30}""").find(show.title)?.value?.trim()
            if (!latinFromTitle.isNullOrBlank() && latinFromTitle.length > 2) {
                candidateQueries.add(latinFromTitle)
            }
            val latinFromDesc = Regex("""[a-zA-Z0-9'\s-]{3,30}""").find(show.description)?.value?.trim()
            if (!latinFromDesc.isNullOrBlank() && latinFromDesc.length > 2) {
                candidateQueries.add(latinFromDesc)
            }

            val rawSlug = (show.link.ifEmpty { show.id }).trimEnd('/').substringAfterLast('/')
            val slug = rawSlug
                .replace(Regex("(?i)^(musalsal|film|program)-"), "")
                .replace(Regex("(?i)^(modablaj|motarjam)-"), "")
                .replace(Regex("(?i)-s0\\d+$"), "")
                .replace("-", " ")
                .trim()
            if (slug.isNotBlank()) {
                candidateQueries.add(slug)
            }

            var bestMatchOverall: org.json.JSONObject? = null
            var highestScoreOverall = -1

            for (candidate in candidateQueries.distinct()) {
                if (candidate.isBlank()) continue
                try {
                    val query = encodeQueryParam(candidate)
                    val searchUrl = "https://api.themoviedb.org/3/search/multi?api_key=$apiKey&language=ar&query=$query"
                    val searchHeaders = if (accessToken.isNotEmpty() && accessToken != "YOUR_TMDB_ACCESS_TOKEN")
                        mapOf("Authorization" to "Bearer $accessToken") else emptyMap()
                    val searchJsonStr = com.agon.app.Http.get(searchUrl, searchHeaders)
                    val resArr = org.json.JSONObject(searchJsonStr).optJSONArray("results") ?: continue

                    for (i in 0 until resArr.length()) {
                        val res = resArr.getJSONObject(i)
                        val score = TmdbHelper.scoreResult(
                            res = res,
                            targetTitle = cleanTitle.ifEmpty { show.title },
                            candidateQuery = candidate,
                            targetYear = show.year
                        )
                        if (score > highestScoreOverall) {
                            highestScoreOverall = score
                            bestMatchOverall = res
                        }
                    }

                    if (highestScoreOverall >= 1800) {
                        break
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (bestMatchOverall != null && highestScoreOverall > 0) {
                val bestMatch = bestMatchOverall

                val id = bestMatch.optInt("id")
                val mediaType = bestMatch.optString("media_type", "tv")
                val overview = bestMatch.optString("overview")
                val posterPath = bestMatch.optString("poster_path", "")
                val backdropPath = bestMatch.optString("backdrop_path", "")

                val castMembers = mutableListOf<CastMember>()
                try {
                    val creditsUrl = if (mediaType == "movie") {
                        "https://api.themoviedb.org/3/movie/$id/credits?api_key=$apiKey&language=ar"
                    } else {
                        "https://api.themoviedb.org/3/tv/$id/credits?api_key=$apiKey&language=ar"
                    }
                    val creditsHeaders = if (accessToken.isNotEmpty() && accessToken != "YOUR_TMDB_ACCESS_TOKEN")
                        mapOf("Authorization" to "Bearer $accessToken") else emptyMap()
                    val creditsJsonStr = com.agon.app.Http.get(creditsUrl, creditsHeaders)
                    val castArray = org.json.JSONObject(creditsJsonStr).optJSONArray("cast")
                    if (castArray != null) {
                        for (i in 0 until minOf(castArray.length(), 10)) {
                            val actor = castArray.getJSONObject(i)
                            val name = actor.getString("name")
                            val profile = actor.optString("profile_path", null)
                            val fullProfile = if (profile != null && profile != "null") "https://image.tmdb.org/t/p/w185$profile" else null
                            castMembers.add(CastMember(name, fullProfile))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return show.copy(
                    description = if (overview.isNotBlank()) overview else show.description,
                    poster = show.poster,
                    backdrop = show.backdrop,
                    castMembers = if (castMembers.isNotEmpty()) castMembers else show.castMembers,
                    tmdbId = id
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun setWifiOnly(v: Boolean) { _wifiOnly.value = v }
    fun setNotifications(v: Boolean) { _notifications.value = v }
    fun setAutoplay(v: Boolean) { _autoplay.value = v }
    
    fun setProfileInfo(name: String, color: Int, avatar: String) {
        viewModelScope.launch {
            prefsManager.setProfileInfo(name, color, avatar)
        }
    }

    fun setUserAccount(username: String, email: String) {
        viewModelScope.launch {
            prefsManager.setUserAccount(username.trim(), email.trim())
        }
    }

    fun setFullUserProfile(username: String, email: String, avatar: String) {
        viewModelScope.launch {
            prefsManager.setUserAccount(username.trim(), email.trim())
            prefsManager.setProfileInfo(username.trim(), _profileColor.value, avatar)
            _profileName.value = username.trim()
            _userEmail.value = email.trim()
            _profileAvatar.value = avatar
        }
    }

    fun setProfileAge(age: String) {
        viewModelScope.launch {
            prefsManager.setProfileAge(age)
        }
    }

    fun completeOnboarding(name: String, age: String, avatar: String) {
        val finalName = if (name.isNotBlank()) name else "ضيف"
        viewModelScope.launch {
            prefsManager.setProfileInfo(finalName, _profileColor.value, avatar)
            prefsManager.setProfileAge(age)
            prefsManager.setOnboardingCompleted(true)
        }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch {
            prefsManager.setOnboardingCompleted(completed)
        }
        _onboardingCompleted.value = completed
    }

    fun setProfileAvatar(avatar: String) {
        viewModelScope.launch {
            prefsManager.setProfileAvatar(avatar)
        }
    }

    fun toggleDownload(id: String) {
        _downloads.value = if (id in _downloads.value) _downloads.value - id else _downloads.value + id
    }

    fun isDownloaded(id: String): Boolean = id in _downloads.value

    fun clearDownloads() { _downloads.value = emptySet() }

    fun addToHistory(id: String) {
        _watchHistory.value = listOf(id) + _watchHistory.value.filter { it != id }
    }

    fun removeFromHistory(id: String) {
        _watchHistory.value = _watchHistory.value.filter { it != id }
    }

    fun clearHistory() { _watchHistory.value = emptyList() }

    fun setQuery(q: String) { _query.value = q }

    fun toggleMyList(id: String) {
        _myList.value = if (id in _myList.value) _myList.value - id else _myList.value + id
    }

    fun isInMyList(id: String): Boolean = id in _myList.value

    fun myListShows(): List<Show> = shows.value.filter { it.id in _myList.value }

    private var searchJob: Job? = null

    fun formatRating(rating: Double): String {
        return "%.1f".format(java.util.Locale.US, rating)
    }

    fun performRemoteSearch(query: String) {
        if (query.isBlank() || Catalog.isTashShow(query)) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearchingRemote.value = true
            try {
                delay(400) // debounce
                
                val queryVariants = mutableSetOf(query.trim())
                val normalized = Catalog.normalizeTitle(query)
                val withHamza = query.replace("ه", "ة").replace("ا", "أ").replace("ي", "ى").replace("ي", "ئ").trim()
                queryVariants.add(normalized)
                queryVariants.add(withHamza)
                
                // Add English variation if it looks like a known title or just as a fallback
                if (query.lowercase().contains("titanic")) {
                    queryVariants.add("تايتانك")
                }
                if (query.contains("تايتانك")) {
                    queryVariants.add("titanic")
                }
                
                val jobs3ck = queryVariants.filter { it.length >= 2 }.map { q ->
                    async { com.agon.app.data.EpisodeScraper.search3cktv(q) }
                }
                val jobsFosta = queryVariants.filter { it.length >= 2 }.map { q ->
                    async { com.agon.app.data.EpisodeScraper.searchFosta(q) }
                }
                
                val res3ck = kotlinx.coroutines.awaitAll(*jobs3ck.toTypedArray()).flatten()
                val resFosta = kotlinx.coroutines.awaitAll(*jobsFosta.toTypedArray()).flatten()
                
                // Also search AlooyTV for Gulf/Ramadan content if query matches
                val resAlooy = if (normalized.contains("خليجي") || normalized.contains("كويت") || normalized.contains("سعودي") || normalized.contains("رمضان")) {
                    com.agon.app.data.EpisodeScraper.scrapeAlooyGenreMultiPage("ramadan", "رمضان", 1)
                } else emptyList()

                val remoteShows = (res3ck + resFosta + resAlooy).filterNot { isAlooytvSeries(it) || Catalog.isTashShow(it.title, it.link, it.id) }
                
                if (remoteShows.isNotEmpty()) {
                    var added = false
                    val currentList = _shows.value.toMutableList()
                    remoteShows.forEach { newShow ->
                        val cleanNew = Catalog.sanitizeShowToSeries(newShow)
                        if (currentList.none {
                            it.title.equals(cleanNew.title, ignoreCase = true) ||
                            (it.link.isNotEmpty() && it.link == cleanNew.link)
                        }) {
                            currentList.add(cleanNew)
                            added = true
                        }
                    }
                    if (added) {
                        val updated = deduplicateByTitle(currentList)
                        withContext(Dispatchers.Main) {
                            _shows.value = updated
                            rebuildCategories(updated)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSearchingRemote.value = false
            }
        }
    }

    fun search(q: String, genre: String): List<Show> {
        if (Catalog.isTashShow(q)) return emptyList()
        val normQ = Catalog.normalizeTitle(q)
        val normGenre = Catalog.normalizeTitle(genre)
        
        fun matchesGenre(show: Show): Boolean {
            val isDubbed = show.genres.contains("مدبلج") || show.rating == "مدبلج" || show.title.contains("مدبلج") || show.description.contains("مدبلج")
            val isArabic = show.genres.any { it.contains("عرب") } || show.rating == "عربي" || show.id.startsWith("fosta_ser_") || show.id.startsWith("arabic_")
            val isTurkish = show.genres.any { it.contains("تركي") || it.contains("تركية") } || show.title.contains("تركي") || show.link.contains("3cktv") || show.id.startsWith("3ck")
            val isSubbed = show.genres.contains("مترجم") || show.rating == "مترجم" || (isTurkish && !isDubbed) || show.genres.contains("افلام اجنبية")
            val isMovie = show.isMovie

            return when (genre) {
                "الكل" -> true
                "أفلام" -> isMovie
                "مسلسلات عربية", "عربي" -> !isMovie && isArabic
                "مسلسلات تركية", "تركي", "قصة عشق (تركي)" -> !isMovie && isTurkish
                "مدبلج" -> isDubbed
                "مترجم" -> isSubbed && !isArabic
                "خليجي", "مسلسلات خليجية" -> show.genres.any { it.contains("خليج") } || show.rating == "خليجي" || show.id.startsWith("kleeji_")
                "رمضان", "مسلسلات رمضانية" -> show.genres.any { it.contains("رمضان") } || show.title.contains("رمضان") || show.id.contains("ramadan")
                "دراما" -> show.genres.any { it.contains("دراما") }
                "كوميدي" -> show.genres.any { it.contains("كوميدي") }
                "أكشن" -> show.genres.any { it.contains("أكشن") || it.contains("اكشن") }
                "رومانسي" -> show.genres.any { it.contains("رومانسي") }
                else -> show.genres.any { Catalog.normalizeTitle(it).contains(normGenre, true) }
            }
        }

        if (normQ.isBlank()) {
            return shows.value.filter { !Catalog.isTashShow(it.title, it.link, it.id) && matchesGenre(it) }
        }
        
        val qTokens = normQ.split(" ").map { it.trim() }.filter { it.length >= 1 }
        
        val scoredList = shows.value.mapNotNull { show ->
            if (Catalog.isTashShow(show.title, show.link, show.id)) return@mapNotNull null
            if (!matchesGenre(show)) return@mapNotNull null
            
            val normTitle = show.title.replace("ة", "ه")
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ى", "ي")
                .trim()
                
            var score = 0.0
            
            // 1. Exact match (normalized)
            if (normTitle.equals(normQ, true)) {
                score += 1000.0
            }
            
            // 2. Starts with query
            if (normTitle.startsWith(normQ, true)) {
                score += 500.0
            }
            
            // 3. Substring match
            if (normTitle.contains(normQ, true)) {
                score += 200.0 + (normQ.length.toDouble() / normTitle.length.toDouble()) * 50.0
            }
            
            // 4. Token-based matching with importance
            var matchedTokens = 0
            qTokens.forEach { token ->
                if (normTitle.contains(token, true)) {
                    score += 50.0
                    matchedTokens++
                    
                    // Bonus for matching start of a word in title
                    if (normTitle.startsWith(token, true) || normTitle.contains(" $token", true)) {
                        score += 30.0
                    }
                } else {
                    val genreMatch = show.genres.any { 
                        it.replace("ة", "ه")
                            .replace("أ", "ا")
                            .replace("إ", "ا")
                            .replace("آ", "ا")
                            .replace("ى", "ي")
                            .contains(token, true) 
                    }
                    if (genreMatch) {
                        score += 10.0
                        matchedTokens++
                    }
                }
            }
            
            // If query is long but we matched zero tokens, exclude it
            if (qTokens.size >= 2 && matchedTokens == 0) {
                return@mapNotNull null
            }
            
            // Handle the specific case like "مازلت في سابعه عشر" matching "في السابعه عشر"
            // If the query contains the title or vice versa
            if (normQ.contains(normTitle, true)) {
                score += 150.0
            }
            
            if (score <= 0 && matchedTokens == 0) return@mapNotNull null
            
            Pair(show, score)
        }
        
        return scoredList.sortedByDescending { it.second }.map { it.first }
    }

    fun removeProgress(showId: String) {
        _continueWatching.value = _continueWatching.value.filterNot { it.showId == showId }
    }

    private fun encodeQueryParam(s: String): String {
        val hex = "0123456789ABCDEF"
        val sb = StringBuilder()
        for (b in s.encodeToByteArray()) {
            val v = b.toInt() and 0xFF
            when {
                (v in 'A'.code..'Z'.code) || (v in 'a'.code..'z'.code) || (v in '0'.code..'9'.code) ||
                    v == '-'.code || v == '_'.code || v == '.'.code || v == '~'.code -> sb.append(v.toChar())
                v == ' '.code -> sb.append('+')
                else -> sb.append('%').append(hex[v shr 4]).append(hex[v and 0xF])
            }
        }
        return sb.toString()
    }

}
