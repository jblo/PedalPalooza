package com.example.pedalpalooza.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.pedalpalooza.MainActivity
import com.example.pedalpalooza.MyFirebaseMessagingService
import com.example.pedalpalooza.R

class RideReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val reminderKey = inputData.getString(KEY_REMINDER_KEY).orEmpty()
        val title = inputData.getString(KEY_TITLE)?.takeIf { it.isNotBlank() } ?: "Upcoming Ride"
        val time = inputData.getString(KEY_TIME).orEmpty()
        val venue = inputData.getString(KEY_VENUE).orEmpty()
        val url = inputData.getString(KEY_URL)

        showNotification(
            reminderKey = reminderKey,
            title = title,
            body = buildNotificationBody(time, venue),
            url = url
        )

        if (reminderKey.isNotEmpty()) {
            AndroidRideReminderManager.removePersistedKey(applicationContext, reminderKey)
        }

        return Result.success()
    }

    private fun buildNotificationBody(time: String, venue: String): String {
        val parts = listOfNotNull(
            time.takeIf { it.isNotBlank() }?.let { "Starts at $it" },
            venue.takeIf { it.isNotBlank() }
        )
        return if (parts.isEmpty()) {
            "Your saved PedalPalooza ride is starting soon!"
        } else {
            parts.joinToString(" @ ")
        }
    }

    private fun showNotification(
        reminderKey: String,
        title: String,
        body: String,
        url: String?
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationId = (reminderKey.hashCode() and 0x7FFFFFFF).coerceAtLeast(1000)
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (!url.isNullOrBlank()) {
                putExtra(MyFirebaseMessagingService.EXTRA_NOTIFICATION_URL, url)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ride Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts before your saved PedalPalooza rides begin"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bike_logo)
            .setContentTitle("Ride soon: $title")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_ID = "pedalpalooza_ride_reminders"
        const val KEY_REMINDER_KEY = "reminder_key"
        const val KEY_TITLE = "title"
        const val KEY_TIME = "time"
        const val KEY_VENUE = "venue"
        const val KEY_URL = "url"
    }
}
