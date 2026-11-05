package com.agon.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.agon.app.MainActivity
import com.agon.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object NotificationHelper {

    const val CHANNEL_ID = "hekayatna_updates_channel"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.notification_sound}")

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "إشعارات حكايتنا - حلقات وأفلام جديدة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات فورية عند إضافة مسلسلات أو حلقات أو أفلام جديدة"
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    suspend fun showNewContentNotification(
        context: Context,
        show: Show,
        episodeTitle: String? = null,
        episodeId: String? = null
    ) {
        withContext(Dispatchers.IO) {
            try {
                createNotificationChannel(context)

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.notification_sound}")

                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("OPEN_SHOW_ID", show.id)
                    if (!episodeId.isNullOrEmpty()) {
                        putExtra("OPEN_EPISODE_ID", episodeId)
                    }
                }

                val requestCode = (show.id + (episodeId ?: "")).hashCode()
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val isMovie = show.rating == "فيلم" || show.title.contains("فيلم") || show.genres.any { it.contains("افلام") || it.contains("فيلم") || it == "أفلام" }
                val titleText = if (episodeTitle != null) "حلقة جديدة: ${show.title} 📺" else if (isMovie) "فيلم جديد: ${show.title} 🎬" else "حلقة جديدة: ${show.title} ✨"
                val bodyText = if (isMovie) "فيلم ${show.title} متاح الآن للمشاهدة بجودة عالية على حكايتنا. اضغط للمشاهدة!" else if (episodeTitle != null) "تم توفير $episodeTitle من مسلسل ${show.title} الآن. اضغط لمشاهدة الحلقة فوراً!" else "تم إضافة حلقة جديدة من مسلسل ${show.title} الآن على حكايتنا. اضغط للمشاهدة فوراً!"

                var posterBitmap: Bitmap? = null
                val imageUrlStr = show.poster.ifEmpty { show.backdrop }
                if (imageUrlStr.isNotEmpty()) {
                    try {
                        val conn = URL(imageUrlStr).openConnection() as? java.net.HttpURLConnection
                        if (conn != null) {
                            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10)")
                            conn.connectTimeout = 8000
                            conn.readTimeout = 8000
                            posterBitmap = BitmapFactory.decodeStream(conn.inputStream)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(titleText)
                    .setContentText(bodyText)
                    .setSubText("حلقة جديدة")
                    .setColor(android.graphics.Color.parseColor("#E50914"))
                    .setColorized(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSound(soundUri)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setVibrate(longArrayOf(0, 250, 150, 250))
                    .addAction(
                        android.R.drawable.ic_media_play,
                        "▶️ مشاهدة الحلقة الآن",
                        pendingIntent
                    )

                if (posterBitmap != null) {
                    builder.setLargeIcon(posterBitmap)
                    builder.setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(posterBitmap)
                            .bigLargeIcon(null as Bitmap?)
                            .setSummaryText(bodyText)
                    )
                } else {
                    builder.setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
                }

                notificationManager.notify(requestCode, builder.build())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
