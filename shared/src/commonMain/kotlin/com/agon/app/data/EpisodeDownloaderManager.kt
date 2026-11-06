package com.agon.app.data

import com.agon.app.Http
import com.agon.app.platform.PlatformFiles
import com.agon.app.platform.PlatformUi
import com.russhwolf.settings.Settings
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class DownloadedEpisode(
    val id: String,
    val showId: String,
    val showTitle: String,
    val episodeTitle: String,
    val episodeNum: String,
    val poster: String,
    val localFilePath: String,
    val progress: Int = 100,
    val isDownloading: Boolean = false,
    val fileSizeFormatted: String = ""
)

/** Persisted form. Only the FILE NAME is stored: the iOS app container path changes on update/reinstall. */
@Serializable
private data class StoredEpisode(
    val id: String, val showId: String, val showTitle: String, val episodeTitle: String,
    val episodeNum: String, val poster: String, val fileName: String
)

/** KMP port of the Android HttpURLConnection downloader: Ktor stream -> Documents/Downloads/<name>.mp4 */
object EpisodeDownloaderManager {
    private const val KEY = "offline_downloads_items"
    private val settings: Settings by lazy { Settings() }
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isLoaded = false

    private val _downloadedEpisodes = MutableStateFlow<List<DownloadedEpisode>>(emptyList())
    val downloadedEpisodes: StateFlow<List<DownloadedEpisode>> = _downloadedEpisodes.asStateFlow()

    private val _downloadProgresses = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgresses: StateFlow<Map<String, Int>> = _downloadProgresses.asStateFlow()

    fun init() {
        if (isLoaded) return
        isLoaded = true
        try {
            val items = json.decodeFromString<List<StoredEpisode>>(settings.getString(KEY, "[]"))
            val dir = PlatformFiles.downloadsDir()
            _downloadedEpisodes.value = items.mapNotNull { s ->
                val path = "$dir/${s.fileName}"
                if (PlatformFiles.exists(path) && PlatformFiles.size(path) > 0)
                    DownloadedEpisode(
                        s.id, s.showId, s.showTitle, s.episodeTitle, s.episodeNum, s.poster, path,
                        progress = 100, isDownloading = false, fileSizeFormatted = formatSize(PlatformFiles.size(path))
                    ) else null
            }
        } catch (_: Throwable) { }
    }

    private fun saveToDisk() {
        val list = _downloadedEpisodes.value.filter { !it.isDownloading }.map {
            StoredEpisode(it.id, it.showId, it.showTitle, it.episodeTitle, it.episodeNum, it.poster,
                it.localFilePath.substringAfterLast('/'))
        }
        settings.putString(KEY, json.encodeToString(list))
    }

    fun isEpisodeDownloaded(episodeId: String, showId: String? = null): Boolean =
        _downloadedEpisodes.value.any { ep ->
            ep.id == episodeId && (showId.isNullOrEmpty() || ep.showId == showId) &&
                !ep.isDownloading && PlatformFiles.exists(ep.localFilePath)
        }

    fun getDownloadedEpisode(episodeId: String, showId: String? = null): DownloadedEpisode? =
        _downloadedEpisodes.value.firstOrNull { ep ->
            ep.id == episodeId && (showId.isNullOrEmpty() || ep.showId == showId) && PlatformFiles.exists(ep.localFilePath)
        }

    private fun needsAlooyReferer(u: String): Boolean {
        val l = u.lowercase()
        return listOf("ylkqwsfghrt", "zvde", "ujeklsj", "work456", "work789", "workers.dev", "alooy", ".site", ".click").any { l.contains(it) }
    }

    /** returns false when already downloaded / downloading (caller may show its own message). */
    fun startDownload(
        showId: String, showTitle: String, episodeId: String, episodeTitle: String,
        episodeNum: String, poster: String, videoUrl: String
    ): Boolean {
        init()
        if (isEpisodeDownloaded(episodeId)) return false
        if (_downloadProgresses.value.containsKey(episodeId)) return false

        scope.launch {
            try {
                var directUrl = videoUrl
                if (videoUrl.startsWith("http") && (videoUrl.contains("/watch/") || videoUrl.contains(".html") ||
                            (!videoUrl.endsWith(".mp4") && !videoUrl.endsWith(".m3u8")))) {
                    val servers = EpisodeScraper.getServersForEpisode(videoUrl)
                    val best = servers.find {
                        it.name.contains("مباشر") || it.name.contains("HD") || it.url.endsWith(".mp4") || it.url.endsWith(".m3u8")
                    } ?: servers.firstOrNull()
                    if (best != null) directUrl = EpisodeScraper.resolveServerDirectUrl(best.url, best.url) ?: best.url
                } else {
                    directUrl = EpisodeScraper.resolveServerDirectUrl(videoUrl, videoUrl) ?: videoUrl
                }
                if (directUrl.isBlank() || !directUrl.startsWith("http")) throw Exception("Could not resolve direct download URL")
                // AVPlayer cannot play a raw HLS playlist saved as .mp4 — progressive files only.
                if (directUrl.substringBefore('?').endsWith(".m3u8")) throw Exception("HLS stream is not downloadable")

                val safe = "${showTitle}_ep_${episodeNum}_${episodeId.takeLast(6)}".replace(Regex("[^a-zA-Z0-9_]"), "_")
                val target = "${PlatformFiles.downloadsDir()}/$safe.mp4"
                PlatformFiles.create(target)

                val temp = DownloadedEpisode(episodeId, showId, showTitle, episodeTitle, episodeNum, poster, target, 0, true)
                _downloadedEpisodes.value = _downloadedEpisodes.value.filter { it.id != episodeId } + temp
                _downloadProgresses.value = _downloadProgresses.value + (episodeId to 0)

                Http.client.prepareGet(directUrl) {
                    header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1")
                    if (needsAlooyReferer(directUrl)) {
                        header("Referer", "https://ep.alooytv16.xyz/")
                        header("Origin", "https://ep.alooytv16.xyz")
                    }
                }.execute { resp ->
                    if (!resp.status.isSuccess()) throw Exception("HTTP ${resp.status.value}")
                    val total = resp.contentLength() ?: -1L
                    val ch = resp.bodyAsChannel()
                    val buf = ByteArray(256 * 1024)
                    var read = 0L
                    var lastPct = -1
                    while (true) {
                        val n = ch.readAvailable(buf, 0, buf.size)
                        if (n < 0) break
                        if (n == 0) continue
                        PlatformFiles.append(target, if (n == buf.size) buf else buf.copyOf(n))
                        read += n
                        if (total > 0) {
                            val pct = ((read * 100) / total).toInt()
                            if (pct != lastPct) {
                                lastPct = pct
                                _downloadProgresses.value = _downloadProgresses.value + (episodeId to pct)
                                _downloadedEpisodes.value = _downloadedEpisodes.value.map {
                                    if (it.id == episodeId) it.copy(progress = pct) else it
                                }
                            }
                        }
                    }
                }

                val done = temp.copy(progress = 100, isDownloading = false, fileSizeFormatted = formatSize(PlatformFiles.size(target)))
                _downloadedEpisodes.value = _downloadedEpisodes.value.map { if (it.id == episodeId) done else it }
                _downloadProgresses.value = _downloadProgresses.value - episodeId
                saveToDisk()
            } catch (e: Throwable) {
                _downloadedEpisodes.value.firstOrNull { it.id == episodeId }?.let { PlatformFiles.delete(it.localFilePath) }
                _downloadedEpisodes.value = _downloadedEpisodes.value.filter { it.id != episodeId }
                _downloadProgresses.value = _downloadProgresses.value - episodeId
                PlatformUi.toast("فشل تحميل الحلقة، الرجاء استخدام سيرفر آخر")
            }
        }
        return true
    }

    fun deleteDownloadedEpisode(episodeId: String) {
        val item = _downloadedEpisodes.value.firstOrNull { it.id == episodeId } ?: return
        PlatformFiles.delete(item.localFilePath)
        _downloadedEpisodes.value = _downloadedEpisodes.value.filter { it.id != episodeId }
        saveToDisk()
    }

    private fun formatSize(bytes: Long): String {
        val mb = bytes / (1024 * 1024)
        return if (mb > 0) "$mb ميغابايت" else "${bytes / 1024} كيلوبايت"
    }
}
