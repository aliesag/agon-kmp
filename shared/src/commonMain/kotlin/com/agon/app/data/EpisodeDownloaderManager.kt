package com.agon.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

/**
 * KMP stub. Full URLSession-backed downloader is wired in stage 7.
 * Public surface mirrors the Android original (in pending-src/).
 */
object EpisodeDownloaderManager {
    private val _downloadedEpisodes = MutableStateFlow<List<DownloadedEpisode>>(emptyList())
    val downloadedEpisodes: StateFlow<List<DownloadedEpisode>> = _downloadedEpisodes.asStateFlow()

    private val _downloadProgresses = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgresses: StateFlow<Map<String, Int>> = _downloadProgresses.asStateFlow()

    fun isEpisodeDownloaded(episodeId: String, showId: String = ""): Boolean =
        _downloadedEpisodes.value.any { it.id == episodeId }

    fun getDownloadedEpisode(episodeId: String): DownloadedEpisode? =
        _downloadedEpisodes.value.find { it.id == episodeId }

    fun deleteDownloadedEpisode(episodeId: String) {
        _downloadedEpisodes.value = _downloadedEpisodes.value.filterNot { it.id == episodeId }
    }

    // FILL: real download via URLSession is wired in stage 7.
    fun startDownload(
        showId: String, showTitle: String, episodeId: String, episodeTitle: String,
        episodeNum: String, poster: String, videoUrl: String
    ): Boolean = false
}
