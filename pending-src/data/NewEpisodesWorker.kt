package com.agon.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.agon.app.MainActivity
import com.agon.app.R
import java.net.URL

class NewEpisodesWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val shows = EpisodeScraper.scrapeHomePageLatest()
            if (shows.isNotEmpty()) {
                val topShow = shows.first()
                val lastSeenId = PrefsManager(context).getLastSeenShowIdSync()

                if (topShow.id != lastSeenId) {
                    PrefsManager(context).saveLastSeenShowIdSync(topShow.id)
                    NotificationHelper.showNewContentNotification(context, topShow)
                }
            }
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}
