package com.aksapps.symphonicmix.services

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import android.widget.Toast
import androidx.annotation.Keep
import androidx.core.app.NotificationCompat
import com.aksapps.symphonicmix.R
import com.aksapps.symphonicmix.activities.MainActivity
import com.aksapps.symphonicmix.activities.PlayerActivity
import com.aksapps.symphonicmix.broadcasts.NotificationReceiver
import com.aksapps.symphonicmix.classes.ApplicationClass
import com.aksapps.symphonicmix.models.formatDuration
import com.aksapps.symphonicmix.models.getImageR

// To make a song sticky or runnable in foreground, we have to use service to play the music using service.
@Keep
class MusicService : Service() {
    private var myBinder = MyBinder() // Need instance of current class, but can not do that directly, so used an object of inner class.
    var mediaPlayer: MediaPlayer? = null
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var runnable: Runnable // Needed to update the seekbar in realtime.

    // This method is called when the service is attached to an activity.
    override fun onBind(intent: Intent?): IBinder {
        mediaSession = MediaSessionCompat(baseContext, "SymphonicMix") // Creates a session for the service.
        return myBinder
    }

    // Need inner class to create object of current class.
    inner class MyBinder : Binder() {
        fun currentService(): MusicService {
            return this@MusicService
        }
    }

    // Used to show the notification.
    fun showNotification(playPauseBtn: Int) {
        // Create intents and pending intents of all the buttons present in notification bar. These will be used to add functionality to them.
        val prevIntent = Intent(
            baseContext,
            NotificationReceiver::class.java
        ).setAction(ApplicationClass.PREVIOUS)
        val prevPendingIntent = PendingIntent.getBroadcast(
            baseContext,
            0,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playIntent =
            Intent(baseContext, NotificationReceiver::class.java).setAction(ApplicationClass.PLAY)
        val playPendingIntent = PendingIntent.getBroadcast(
            baseContext,
            0,
            playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent =
            Intent(baseContext, NotificationReceiver::class.java).setAction(ApplicationClass.NEXT)
        val nextPendingIntent = PendingIntent.getBroadcast(
            baseContext,
            0,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val exitIntent =
            Intent(baseContext, NotificationReceiver::class.java).setAction(ApplicationClass.EXIT)
        val exitPendingIntent = PendingIntent.getBroadcast(
            baseContext,
            0,
            exitIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Set the image art of currently playing one. In case there are none, use the default image of our app icon.
        val imgArt = getImageR(PlayerActivity.musicListPA[PlayerActivity.songPosition].path)
        val image = if (imgArt != null) BitmapFactory.decodeByteArray(
            imgArt,
            0,
            imgArt.size
        ) else BitmapFactory.decodeResource(
            resources,
            com.aksapps.symphonicmix.R.drawable.ic_music_splash
        )

        // Create an instance of notification and set the properties of notification.
        val notification = NotificationCompat.Builder(baseContext, ApplicationClass.CHANNEL_ID)
        notification.setContentTitle(PlayerActivity.musicListPA[PlayerActivity.songPosition].title)
            .setContentText(PlayerActivity.musicListPA[PlayerActivity.songPosition].artist)
            .setSmallIcon(com.aksapps.symphonicmix.R.drawable.ic_music_notification)
            .setLargeIcon(image)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            notification.setStyle(androidx.media.app.NotificationCompat.MediaStyle())
        else
            notification.setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
            )

        notification.priority = NotificationCompat.PRIORITY_HIGH
        notification.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .addAction(
                com.aksapps.symphonicmix.R.drawable.ic_previous_notification,
                "Previous",
                prevPendingIntent
            )
            .addAction(
                playPauseBtn,
                "Play",
                playPendingIntent
            )
            .addAction(
                com.aksapps.symphonicmix.R.drawable.ic_next_notification,
                "Next",
                nextPendingIntent
            )
        notification.addAction(
            com.aksapps.symphonicmix.R.drawable.ic_exit,
            "Exit",
            exitPendingIntent
        )
        notification.build()

        // Start foreground service.
        startForeground(7, notification.build())
    }

    // To create an instance of media player, load the song and play the song.
    fun createMediaPlayer() {
        try {
            PlayerActivity.musicListPA = ArrayList()
            PlayerActivity.musicListPA.addAll(MainActivity.musicListMA)
//            Log.d(TAG, "createMediaPlayer: ${musicListPA.toString()}")
            if (PlayerActivity.musicService?.mediaPlayer == null) PlayerActivity.musicService?.mediaPlayer = MediaPlayer()
            PlayerActivity.musicService?.mediaPlayer?.reset()
            PlayerActivity.musicService?.mediaPlayer?.setDataSource(PlayerActivity.musicListPA[PlayerActivity.songPosition].path)
            PlayerActivity.musicService?.mediaPlayer?.prepare()
            PlayerActivity.binding.playPauseBtn.setImageResource(R.drawable.pause_button)
            PlayerActivity.musicService?.showNotification(R.drawable.ic_pause_notification)

            // Set up the current song duration text and total song duration text.
            PlayerActivity.binding.tvSeekbarStart.text = mediaPlayer?.currentPosition?.let {
                formatDuration(
                    it.toLong())
            }
            PlayerActivity.binding.tvSeekbarEnd.text = mediaPlayer?.duration?.let {
                formatDuration(
                    it.toLong())
            }

            // Seekbar progress and text should restart after another song starts playing.
            PlayerActivity.binding.seekBarPa.progress = 0
            PlayerActivity.binding.seekBarPa.max = mediaPlayer?.duration!!

            // If song is currently playing and same song clicked again, fetch the id to avoid reloading of current song.
            PlayerActivity.nowPlayingId = PlayerActivity.musicListPA[PlayerActivity.songPosition].id
        } catch (e: Exception) {
//            Log.d(TAG, "createMediaPlayer: not found!")
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
            return
        }
    }

    // Handle the seekbar functionality. It should update as the song proceed.
    fun seekBarSetup() {
        runnable = Runnable {
            PlayerActivity.binding.tvSeekbarStart.text = mediaPlayer?.currentPosition?.let {
                formatDuration(
                    it.toLong())
            }
            PlayerActivity.binding.seekBarPa.progress = mediaPlayer?.currentPosition!!
            Handler(Looper.getMainLooper()).postDelayed(runnable, 500)
        }
        Handler(Looper.getMainLooper()).postDelayed(runnable, 0)
    }
}