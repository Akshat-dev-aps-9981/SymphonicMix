package com.aksapps.symphonicmix.classes

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.annotation.Keep

// Application class will be called when user first install the app, even when the app is not currently running.
@Keep
class ApplicationClass : Application() {
    // These objects are needed for notification functionality.
    companion object {
        const val CHANNEL_ID = "Channel1"
        const val PLAY = "Play"
        const val NEXT = "Next"
        const val PREVIOUS = "Previous"
        const val EXIT = "Exit"
    }
    override fun onCreate() {
        super.onCreate()
        // We need to create notification channel if the user is using a device where the OS version is greater than or equal to Oreo (8).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(CHANNEL_ID, "Now Playing Song", NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.description = "This is an important channel for showing song!!"
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}