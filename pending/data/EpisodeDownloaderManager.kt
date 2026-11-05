package com.agon.app.data

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

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

object EpisodeDownloaderManager {

    private val _downloadedEpisodes = MutableStateFlow<List<DownloadedEpisode>>(emptyList())
    val downloadedEpisodes: StateFlow<List<DownloadedEpisode>> = _downloadedEpisodes.asStateFlow()

    private val _downloadProgresses = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgresses: StateFlow<Map<String, Int>> = _downloadProgresses.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var isLoaded = false

    fun init(context: Context) {
        if (isLoaded) return
        isLoaded = true
        loadFromDisk(context)
    }

    private fun loadFromDisk(context: Context) {
        try {
            val prefs = context.getSharedPreferences("offline_downloads", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("items", "[]") ?: "[]"
            val array = JSONArray(jsonStr)
            val list = mutableListOf<DownloadedEpisode>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val localPath = obj.optString("localFilePath")
                val file = File(localPath)
                if (file.exists() && file.length() > 0) {
                    list.add(
                        DownloadedEpisode(
                            id = obj.optString("id"),
                            showId = obj.optString("showId"),
                            showTitle = obj.optString("showTitle"),
                            episodeTitle = obj.optString("episodeTitle"),
                            episodeNum = obj.optString("episodeNum"),
                            poster = obj.optString("poster"),
                            localFilePath = localPath,
                            progress = 100,
                            isDownloading = false,
                            fileSizeFormatted = formatSize(file.length())
                        )
                    )
                }
            }
            _downloadedEpisodes.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveToDisk(context: Context) {
        try {
            val prefs = context.getSharedPreferences("offline_downloads", Context.MODE_PRIVATE)
            val array = JSONArray()
            _downloadedEpisodes.value.filter { !it.isDownloading && File(it.localFilePath).exists() }.forEach { ep ->
                val obj = JSONObject()
                obj.put("id", ep.id)
                obj.put("showId", ep.showId)
                obj.put("showTitle", ep.showTitle)
                obj.put("episodeTitle", ep.episodeTitle)
                obj.put("episodeNum", ep.episodeNum)
                obj.put("poster", ep.poster)
                obj.put("localFilePath", ep.localFilePath)
                array.put(obj)
            }
            prefs.edit().putString("items", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isEpisodeDownloaded(episodeId: String, showId: String? = null): Boolean {
        return _downloadedEpisodes.value.any { ep ->
            val matchesId = ep.id == episodeId
            val matchesShow = showId.isNullOrEmpty() || ep.showId == showId
            matchesId && matchesShow && !ep.isDownloading && File(ep.localFilePath).exists()
        }
    }

    fun getDownloadedEpisode(episodeId: String, showId: String? = null): DownloadedEpisode? {
        return _downloadedEpisodes.value.firstOrNull { ep ->
            val matchesId = ep.id == episodeId
            val matchesShow = showId.isNullOrEmpty() || ep.showId == showId
            matchesId && matchesShow && File(ep.localFilePath).exists()
        }
    }

    fun startDownload(
        context: Context,
        showId: String,
        showTitle: String,
        episodeId: String,
        episodeTitle: String,
        episodeNum: String,
        poster: String,
        videoUrl: String
    ) {
        val appContext = context.applicationContext
        if (isEpisodeDownloaded(episodeId)) return

        scope.launch {
            try {
                var directUrl = videoUrl
                if (videoUrl.startsWith("http") && (videoUrl.contains("/watch/") || videoUrl.contains(".html") || (!videoUrl.endsWith(".mp4") && !videoUrl.endsWith(".m3u8")))) {
                    val parsedServers = EpisodeScraper.getServersForEpisode(videoUrl)
                    val bestServer = parsedServers.find { 
                        it.name.contains("مباشر") || it.name.contains("HD") || it.url.endsWith(".mp4") || it.url.endsWith(".m3u8")
                    } ?: parsedServers.firstOrNull()
                    
                    if (bestServer != null) {
                        directUrl = EpisodeScraper.resolveServerDirectUrl(bestServer.url, bestServer.url) ?: bestServer.url
                    }
                } else {
                    directUrl = EpisodeScraper.resolveServerDirectUrl(videoUrl, videoUrl) ?: videoUrl
                }

                if (directUrl.isBlank() || !directUrl.startsWith("http")) {
                    throw Exception("Could not resolve direct download URL")
                }

                val downloadsDir = appContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: appContext.filesDir
                val sanitizeName = "${showTitle}_ep_${episodeNum}_${episodeId.takeLast(6)}".replace(Regex("[^a-zA-Z0-9_]"), "_")
                val targetFile = File(downloadsDir, "$sanitizeName.mp4")

                val tempItem = DownloadedEpisode(
                    id = episodeId,
                    showId = showId,
                    showTitle = showTitle,
                    episodeTitle = episodeTitle,
                    episodeNum = episodeNum,
                    poster = poster,
                    localFilePath = targetFile.absolutePath,
                    progress = 0,
                    isDownloading = true
                )

                _downloadedEpisodes.value = _downloadedEpisodes.value.filter { it.id != episodeId } + tempItem
                _downloadProgresses.value = _downloadProgresses.value + (episodeId to 0)

                val connection = URL(directUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                if (LocalStreamProxy.isAlooyCdnUrl(directUrl) || directUrl.contains("zvde") || directUrl.contains("ylkqwsfghrt") || directUrl.contains("ujeklsj") || directUrl.contains("alooy") || directUrl.contains("workers.dev") || directUrl.contains(".site") || directUrl.contains(".click")) {
                    connection.setRequestProperty("Referer", "https://ep.alooytv16.xyz/")
                    connection.setRequestProperty("Origin", "https://ep.alooytv16.xyz")
                }
                connection.connect()

                val fileLength = connection.contentLengthLong
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(targetFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead: Long = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (fileLength > 0) {
                        val pct = ((totalBytesRead * 100) / fileLength).toInt()
                        _downloadProgresses.value = _downloadProgresses.value + (episodeId to pct)
                        _downloadedEpisodes.value = _downloadedEpisodes.value.map {
                            if (it.id == episodeId) it.copy(progress = pct) else it
                        }
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()
                connection.disconnect()

                val completedItem = tempItem.copy(
                    progress = 100,
                    isDownloading = false,
                    fileSizeFormatted = formatSize(targetFile.length())
                )

                _downloadedEpisodes.value = _downloadedEpisodes.value.map {
                    if (it.id == episodeId) completedItem else it
                }
                _downloadProgresses.value = _downloadProgresses.value - episodeId
                saveToDisk(appContext)

            } catch (e: Exception) {
                e.printStackTrace()
                _downloadedEpisodes.value = _downloadedEpisodes.value.filter { it.id != episodeId }
                _downloadProgresses.value = _downloadProgresses.value - episodeId
                try {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(appContext, "فشل تحميل الحلقة، الرجاء استخدام سيرفر آخر", android.widget.Toast.LENGTH_LONG).show()
                    }
                } catch (t: Throwable) {
                    // Ignore context exceptions in background
                }
            }
        }
    }

    fun deleteDownloadedEpisode(context: Context, episodeId: String) {
        val item = _downloadedEpisodes.value.firstOrNull { it.id == episodeId } ?: return
        try {
            val file = File(item.localFilePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _downloadedEpisodes.value = _downloadedEpisodes.value.filter { it.id != episodeId }
        saveToDisk(context.applicationContext)
    }

    private fun formatSize(bytes: Long): String {
        val mb = bytes / (1024 * 1024)
        return if (mb > 0) "$mb ميغابايت" else "${bytes / 1024} كيلوبايت"
    }
}
