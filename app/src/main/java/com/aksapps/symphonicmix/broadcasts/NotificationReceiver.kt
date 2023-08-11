package com.aksapps.symphonicmix.broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.Keep
import com.aksapps.symphonicmix.R
import com.aksapps.symphonicmix.activities.PlayerActivity
import com.aksapps.symphonicmix.classes.ApplicationClass
import com.aksapps.symphonicmix.fragments.NowPlayingFragment
import com.aksapps.symphonicmix.models.exitApplication
import com.aksapps.symphonicmix.models.favoriteChecker
import com.aksapps.symphonicmix.models.setSongPosition
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

// Broadcast Receiver class is needed to handle the button pressed event from notification bar.
@Keep
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // Check which button is clicked.
        when (intent?.action) {
            ApplicationClass.PREVIOUS -> prevNextSong(increment = false, context = context!!)
            ApplicationClass.PLAY -> if (PlayerActivity.isPlaying) pauseMusic() else playMusic()
            ApplicationClass.NEXT -> prevNextSong(increment = true, context = context!!)
            ApplicationClass.EXIT -> {
                exitApplication()
            }
        }
    }

    // Play music.
    private fun playMusic() {
        PlayerActivity.isPlaying = true
        PlayerActivity.musicService?.mediaPlayer?.start()
        PlayerActivity.musicService?.showNotification(R.drawable.ic_pause_notification)
        PlayerActivity.binding.playPauseBtn.setImageResource(R.drawable.pause_button)
        NowPlayingFragment.binding.playPauseBtnNP.setImageResource(R.drawable.pause_button)
    }

    // Pause music.
    private fun pauseMusic() {
        PlayerActivity.isPlaying = false
        PlayerActivity.musicService?.mediaPlayer?.pause()
        PlayerActivity.musicService?.showNotification(R.drawable.ic_play_notification)
        PlayerActivity.binding.playPauseBtn.setImageResource(R.drawable.play_button)
        NowPlayingFragment.binding.playPauseBtnNP.setImageResource(R.drawable.play_button)
    }

    // Play next/previous song from notification.
    private fun prevNextSong(increment : Boolean, context: Context) {
        setSongPosition(increment = increment)
        PlayerActivity.musicService?.createMediaPlayer()
        try {
            // Set the image and song title in notification after changing song by clicking on next/previous buttons.
            Glide.with(context).load(PlayerActivity.musicListPA[PlayerActivity.songPosition].artUri)
                .apply(RequestOptions().placeholder(R.drawable.ic_music_splash).centerCrop())
                .apply(RequestOptions().error(R.drawable.ic_music_splash).centerCrop())
                .into(PlayerActivity.binding.imgAlbum)
            PlayerActivity.binding.songNamePa.text = PlayerActivity.musicListPA[PlayerActivity.songPosition].title

            // Set the image and song title in fragment after changing song by clicking on next/previous buttons.
            Glide.with(context).load(PlayerActivity.musicListPA[PlayerActivity.songPosition].artUri)
                .apply(RequestOptions().placeholder(R.drawable.ic_music_splash).centerCrop())
                .apply(RequestOptions().error(R.drawable.ic_music_splash).centerCrop())
                .into(NowPlayingFragment.binding.imgSongNp)
            NowPlayingFragment.binding.songNameNP.text = PlayerActivity.musicListPA[PlayerActivity.songPosition].title

            PlayerActivity.fIndex = favoriteChecker(PlayerActivity.musicListPA[PlayerActivity.songPosition].id)
            if (PlayerActivity.isFavorite)
                PlayerActivity.binding.favBtnPa.setImageResource(R.drawable.ic_favorite)
            else
                PlayerActivity.binding.favBtnPa.setImageResource(R.drawable.ic_favorite_border)
        } catch (e: Exception) {
            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show()
        }
        playMusic()
    }
}