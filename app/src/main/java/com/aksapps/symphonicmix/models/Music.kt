package com.aksapps.symphonicmix.models

import android.media.MediaMetadataRetriever
import androidx.annotation.Keep
import com.aksapps.symphonicmix.activities.FavoritesActivity
import com.aksapps.symphonicmix.activities.PlayerActivity
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

// These are the variables we need from a single music file.
@Keep
data class Music(
    val id: String,
    val title: String,
    val album: String,
    val artist: String,
    val duration: Long = 0,
    val path: String,
    val artUri: String
)

// Current format is in LONG type, we have to transform it into minutes and seconds.
fun formatDuration(duration: Long) : String {
    val minutes = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS)
    val seconds = (TimeUnit.SECONDS.convert(
        duration,
        TimeUnit.MILLISECONDS
    ) - minutes * TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES))
    return String.format("%02d:%02d", minutes, seconds)
}

// Returns a byte array of currently playing song's artwork image.
fun getImageR(path: String): ByteArray? {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(path)
    return retriever.embeddedPicture
}

// If user has reached the end of song list and clicks next, then return to the starting point, similarly, if user is on the very first song and clicks previous, throw them to the last song on the list.
fun setSongPosition(increment: Boolean) {
   if (!PlayerActivity.repeat) {
       if (increment) {
           if (PlayerActivity.songPosition == PlayerActivity.musicListPA.size - 1)
               PlayerActivity.songPosition = 0
           else ++PlayerActivity.songPosition
       } else {
           if (PlayerActivity.songPosition == 0)
               PlayerActivity.songPosition = PlayerActivity.musicListPA.size - 1
           else --PlayerActivity.songPosition
       }
   }
}

// Exit the application after closing the currently running media. If media is not running, exit directly.
fun exitApplication() {
    if (PlayerActivity.musicService != null) {
        PlayerActivity.musicService?.stopForeground(true)
        PlayerActivity.musicService?.mediaPlayer?.release()
        PlayerActivity.musicService = null
    }
    exitProcess(1)
}

// Check if the song exist, make it favorite and return the index of that song. Otherwise return -1.
fun favoriteChecker(id: String): Int {
    PlayerActivity.isFavorite = false
    FavoritesActivity.favoriteSongs.forEachIndexed { index, music ->
        if (id == music.id) {
            PlayerActivity.isFavorite = true
            return index
        }
    }
    return -1
}