package com.aksapps.symphonicmix.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.aksapps.symphonicmix.R
import com.aksapps.symphonicmix.databinding.ActivityPlayerBinding
import com.aksapps.symphonicmix.fragments.NowPlayingFragment
import com.aksapps.symphonicmix.models.Music
import com.aksapps.symphonicmix.models.exitApplication
import com.aksapps.symphonicmix.models.favoriteChecker
import com.aksapps.symphonicmix.models.formatDuration
import com.aksapps.symphonicmix.models.setSongPosition
import com.aksapps.symphonicmix.services.MusicService
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt

class PlayerActivity : AppCompatActivity(), ServiceConnection, MediaPlayer.OnCompletionListener {

    // Public Companion objects are accessible all over the module. Their members are of static type and only created once, even if created multiple instances of companion object.
    companion object {
        var musicListPA: ArrayList<Music> = ArrayList()
        var songPosition: Int = 0
        var isPlaying: Boolean = false
        var musicService: MusicService? = null

        @SuppressLint("StaticFieldLeak")
        lateinit var binding: ActivityPlayerBinding

        var repeat: Boolean = false

        var min15: Boolean = false
        var min30: Boolean = false
        var min60: Boolean = false

        var nowPlayingId: String = ""

        var isFavorite: Boolean = false
        var fIndex: Int = -1
    }

    private lateinit var eqLauncher: ActivityResultLauncher<Intent>

//    private val TAG = "PlayerActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Used for equalize button functionality.
        eqLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Handle the result here if needed
//                val data: Intent? = result.data
                // ...
                return@registerForActivityResult
            }
        }

        // Get the song position from incoming intent.
        songPosition = intent.getIntExtra("index", 0)

        // Get the class name from which intent is coming from and act accordingly.
        when (intent.getStringExtra("class")) {
            "FavoriteAdapter" -> {
                // For starting service.
                val intent1 = Intent(this, MusicService::class.java)
                bindService(intent1, this, BIND_AUTO_CREATE)
                startService(intent1)

                musicListPA = ArrayList()
                musicListPA.addAll(FavoritesActivity.favoriteSongs)
                setLayout()
            }

            "NowPlayingFragment" -> {
                setLayout()
                binding.tvSeekbarStart.text = musicService?.mediaPlayer?.currentPosition?.toLong()
                    ?.let { formatDuration(it) }
                binding.tvSeekbarEnd.text = musicService?.mediaPlayer?.duration?.toLong()
                    ?.let { formatDuration(it) }
                binding.seekBarPa.progress = musicService?.mediaPlayer?.currentPosition!!
                binding.seekBarPa.max = musicService?.mediaPlayer?.duration!!
                if (isPlaying)
                    binding.playPauseBtn.setImageResource(R.drawable.pause_button)
                else
                    binding.playPauseBtn.setImageResource(R.drawable.play_button)
            }

            "MusicAdapterSearch" -> {
                // For starting service.
                val intent1 = Intent(this, MusicService::class.java)
                bindService(intent1, this, BIND_AUTO_CREATE)
                startService(intent1)

                musicListPA = ArrayList()
                musicListPA.addAll(MainActivity.musicListSearch)
                setLayout()
            }

            "MusicAdapter" -> {
                // For starting service.
                val intent1 = Intent(this, MusicService::class.java)
                bindService(intent1, this, BIND_AUTO_CREATE)
                startService(intent1)

                musicListPA = ArrayList()
                musicListPA.addAll(MainActivity.musicListMA)
                setLayout()
            }

            "MainActivity" -> {
                // For starting service.
                val intent1 = Intent(this, MusicService::class.java)
                bindService(intent1, this, BIND_AUTO_CREATE)
                startService(intent1)

                musicListPA = ArrayList()
                musicListPA.addAll(MainActivity.musicListMA)
                musicListPA.shuffle()
                setLayout()
            }

            "FavoritesShuffle" -> {
                // For starting service.
                val intent1 = Intent(this, MusicService::class.java)
                bindService(intent1, this, BIND_AUTO_CREATE)
                startService(intent1)

                musicListPA = ArrayList()
                musicListPA.addAll(FavoritesActivity.favoriteSongs)
                musicListPA.shuffle()
                setLayout()
            }
        }

        // Exit the activity when clicking on back button.
        binding.backBtnPa.setOnClickListener { finish() }

        binding.favBtnPa.setOnClickListener {
            if (isFavorite) {
                isFavorite = false
                binding.favBtnPa.setImageResource(R.drawable.ic_favorite_border)
                try {
                    FavoritesActivity.favoriteSongs.removeAt(fIndex)
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Unable to remove at this moment. Error: $e.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                isFavorite = true
                binding.favBtnPa.setImageResource(R.drawable.ic_favorite)
                FavoritesActivity.favoriteSongs.add(musicListPA[songPosition])
            }
            FavoritesActivity.favoritesChanged = true
        }

        // Play / Pause the music depending on whether the song is playing already or not.
        binding.playPauseBtn.setOnClickListener {
            if (isPlaying) pauseMusic()
            else playMusic()
        }

        // Set the functionality of previous button.
        binding.previousBtn.setOnClickListener { prevNextSong(increment = false) }

        // Set the functionality of next button.
        binding.nextBtn.setOnClickListener { prevNextSong(increment = true) }

        // Seek the music to the progress where our current seekbar is moved by the user.
        binding.seekBarPa.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekbar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) musicService?.mediaPlayer?.seekTo(progress)
            }

            override fun onStartTrackingTouch(seekbar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekbar: SeekBar?) = Unit

        })

        // Song should repeat after clicking on repeat button or should not repeat if clicked even times.
        binding.repeatBtnPa.setOnClickListener {
            if (!repeat) {
                repeat = true
                binding.repeatBtnPa.setColorFilter(ContextCompat.getColor(this, R.color.blue))
                Snackbar.make(it, "Repeat is ON!", Snackbar.LENGTH_SHORT)
                    .setAction("Turn OFF") {
                        repeat = false
                        binding.repeatBtnPa.setColorFilter(
                            ContextCompat.getColor(
                                this,
                                R.color.pink
                            )
                        )
                        return@setAction
                    }.show()
            } else {
                repeat = false
                binding.repeatBtnPa.setColorFilter(ContextCompat.getColor(this, R.color.pink))
                Snackbar.make(it, "Repeat is OFF!", Snackbar.LENGTH_SHORT)
                    .setAction("Turn ON") {
                        repeat = true
                        binding.repeatBtnPa.setColorFilter(
                            ContextCompat.getColor(
                                this,
                                R.color.blue
                            )
                        )
                        return@setAction
                    }.show()
            }
        }

        // Open an android built-in library when the user clicks on equalize button.
        binding.equalizerBtnPa.setOnClickListener {
            try {
                val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                    putExtra(
                        AudioEffect.EXTRA_AUDIO_SESSION,
                        musicService?.mediaPlayer?.audioSessionId
                    )
                    putExtra(AudioEffect.EXTRA_PACKAGE_NAME, baseContext.packageName)
                    putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                }
                eqLauncher.launch(eqIntent)
            } catch (e: Exception) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
            }
        }

        // Show bottom  sheet if timer is not currently installed or display a cancel dialog otherwise when clicking on timer button.
        binding.timerBtnPa.setOnClickListener {
            val timer = min15 || min30 || min60
            if (!timer)
                showBottomSheetDialog()
            else {
                val builder = MaterialAlertDialogBuilder(this)
                builder.setTitle("Stop Timer")
                    .setMessage("Do you want to stop the timer?")
                    .setPositiveButton("Yes") { _, _ ->
                        min15 = false
                        min30 = false
                        min60 = false
                        binding.timerBtnPa.setColorFilter(
                            ContextCompat.getColor(
                                this,
                                R.color.pink
                            )
                        )
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                val customDialog = builder.create()
                customDialog.show()
                customDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
                customDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED)
            }
        }

        // Share the music file by clicking on share button.
        binding.shareBtnPa.setOnClickListener {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type = "audio/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(musicListPA[songPosition].path))
            startActivity(
                Intent.createChooser(
                    shareIntent,
                    "Share SymphonicMix Music File With...!"
                )
            )
        }
    }

    // To set the album art and title of the song.
    private fun setLayout() {
        fIndex = favoriteChecker(musicListPA[songPosition].id)
        try {
            Glide.with(this).load(musicListPA[songPosition].artUri)
                .apply(RequestOptions().placeholder(R.drawable.ic_music_splash).centerCrop())
                .apply(RequestOptions().error(R.drawable.ic_music_splash).centerCrop())
                .into(binding.imgAlbum)
            binding.songNamePa.text = musicListPA[songPosition].title

            if (repeat)
                binding.repeatBtnPa.setColorFilter(ContextCompat.getColor(this, R.color.blue))

            if (min15 || min30 || min60)
                binding.timerBtnPa.setColorFilter(ContextCompat.getColor(this, R.color.blue))

            if (isFavorite)
                binding.favBtnPa.setImageResource(R.drawable.ic_favorite)
            else
                binding.favBtnPa.setImageResource(R.drawable.ic_favorite_border)

            val ed = getSharedPreferences("FirstTimePA", MODE_PRIVATE)
            val isFirstTime = ed.getBoolean("isFirstTime", true)
            if (isFirstTime)
                showTapTargets()

        } catch (e: Exception) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    // To create an instance of media player, load the song and play the song.
    private fun createMediaPlayer() {
        try {
//            Log.d(TAG, "createMediaPlayer: ${musicListPA.toString()}")
            if (musicService?.mediaPlayer == null) musicService?.mediaPlayer = MediaPlayer()
            musicService?.mediaPlayer?.reset()
            musicService?.mediaPlayer?.setDataSource(musicListPA[songPosition].path)
            musicService?.mediaPlayer?.prepare()
            musicService?.mediaPlayer?.start()
            isPlaying = true
            binding.playPauseBtn.setImageResource(R.drawable.pause_button)
            musicService?.showNotification(R.drawable.ic_pause_notification)

            // Set up the current song duration text and total song duration text.
            binding.tvSeekbarStart.text = musicService?.mediaPlayer?.currentPosition?.let {
                formatDuration(
                    it.toLong()
                )
            }
            binding.tvSeekbarEnd.text = musicService?.mediaPlayer?.duration?.let {
                formatDuration(
                    it.toLong()
                )
            }

            // Seekbar progress and text should restart after another song starts playing.
            binding.seekBarPa.progress = 0
            binding.seekBarPa.max = musicService?.mediaPlayer?.duration!!

            // Set complete listener to media player, this will be called automatically when current music finished.
            musicService?.mediaPlayer?.setOnCompletionListener(this)

            // If song is currently playing and same song clicked again, fetch the id to avoid reloading of current song.
            nowPlayingId = musicListPA[songPosition].id
        } catch (e: Exception) {
//            Log.d(TAG, "createMediaPlayer: not found!")
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
            return
        }
    }

    // Guide users to use the app.
    private fun showTapTargets() {
        val tapTargets = mutableListOf<Pair<Int, Pair<String, String>>>().apply {
            add(
                R.id.play_pause_btn to Pair("Play the song", "You can play or pause the song.")
            )
            add(
                R.id.previous_btn to Pair("Previous", "Play the previous song.")
            )
            add(
                R.id.next_btn to Pair("Next", "Play the next song.")
            )
            add(
                R.id.repeat_btn_pa to Pair(
                    "Repeat",
                    "If you like to hear this song multiple times, turn on the repeat mode."
                )
            )
            add(
                R.id.equalizer_btn_pa to Pair(
                    "Equalizer",
                    "You can equalize the beats or explore more audio effects for this song."
                )
            )
            add(
                R.id.timer_btn_pa to Pair(
                    "Timer",
                    "Stop the song automatically after a fix period of time."
                )
            )
            add(
                R.id.share_btn_pa to Pair("Share", "Share this song with your loved ones.")
            )
            add(
                R.id.back_btn_pa to Pair("Back", "Go back to see the list of more songs.")
            )
            add(
                R.id.fav_btn_pa to Pair(
                    "Add to Favourites",
                    "Add this song to favourites to listen to it later quickly."
                )
            )
        }

        showTapTargetRecursive(tapTargets, 0)
    }

    private fun showTapTargetRecursive(
        tapTargets: List<Pair<Int, Pair<String, String>>>,
        index: Int
    ) {
        if (index >= tapTargets.size) {
            // All tap targets have been shown, perform any final actions here.
            val editor = getSharedPreferences("FirstTimePA", MODE_PRIVATE).edit()
            editor.putBoolean("isFirstTime", false)
            editor.apply()
            return
        }

        val target = tapTargets[index]
        MaterialTapTargetPrompt.Builder(this@PlayerActivity)
            .setTarget(target.first)
            .setPrimaryText(target.second.first)
            .setSecondaryText(target.second.second)
            .setPromptStateChangeListener { _, state ->
                if (state == MaterialTapTargetPrompt.STATE_DISMISSED) {
                    showTapTargetRecursive(tapTargets, index + 1)
                }
            }
            .show()
    }


    // To play the song. The icon of Play should also change to Pause.
    private fun playMusic() {
        binding.playPauseBtn.setImageResource(R.drawable.pause_button)
        musicService?.showNotification(R.drawable.ic_pause_notification)
        isPlaying = true
        musicService?.mediaPlayer?.start()
    }

    // To pause the song. The icon of Pause should also change to Play.
    private fun pauseMusic() {
        binding.playPauseBtn.setImageResource(R.drawable.play_button)
        musicService?.showNotification(R.drawable.ic_play_notification)
        isPlaying = false
        musicService?.mediaPlayer?.pause()
    }

    // Play next / previous song depending on user's selection and set layout and create media player instance every time for each music played.
    private fun prevNextSong(increment: Boolean) {
        if (increment) {
            setSongPosition(increment = true)
            createMediaPlayer()
            setLayout()
        } else {
            setSongPosition(increment = false)
            createMediaPlayer()
            setLayout()
        }
    }

    // Called when service is successfully connected to this activity.
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as MusicService.MyBinder
        musicService = binder.currentService()
        createMediaPlayer()
        setLayout()
        musicService?.seekBarSetup()
    }

    // Called when service is stopped.
    override fun onServiceDisconnected(name: ComponentName?) {
        musicService = null
    }

    // Play the next song after completing the current song.
    override fun onCompletion(mp: MediaPlayer?) {
        setSongPosition(true)
        createMediaPlayer()
        try {
            setLayout()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        // For refreshing now playing image & text on song completion.
        NowPlayingFragment.binding.songNameNP.isSelected = true
        Glide.with(applicationContext)
            .load(musicListPA[songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.ic_music_splash).centerCrop())
            .apply(RequestOptions().error(R.drawable.ic_music_splash).centerCrop())
            .into(NowPlayingFragment.binding.imgSongNp)
        NowPlayingFragment.binding.songNameNP.text = musicListPA[songPosition].title
    }

    // Show bottom sheet dialog for timer, also add functionality to respective buttons.
    private fun showBottomSheetDialog() {
        val dialog = BottomSheetDialog(this@PlayerActivity)
        dialog.setContentView(R.layout.bottom_sheet_dialog)
        dialog.show()
        dialog.findViewById<TextView>(R.id.min_15)?.setOnClickListener {
            Toast.makeText(baseContext, "Music will stop after 15 minutes.", Toast.LENGTH_SHORT)
                .show()
            binding.timerBtnPa.setColorFilter(ContextCompat.getColor(this, R.color.blue))
            min15 = true
            Thread {
                Thread.sleep((15 * 60_000).toLong())
                if (min15) exitApplication()
            }.start()
            dialog.dismiss()
        }
        dialog.findViewById<TextView>(R.id.min_30)?.setOnClickListener {
            Toast.makeText(baseContext, "Music will stop after 30 minutes.", Toast.LENGTH_SHORT)
                .show()
            binding.timerBtnPa.setColorFilter(ContextCompat.getColor(this, R.color.blue))
            min30 = true
            Thread {
                Thread.sleep((30 * 60_000).toLong())
                if (min30) exitApplication()
            }.start()
            dialog.dismiss()
        }
        dialog.findViewById<TextView>(R.id.min_60)?.setOnClickListener {
            Toast.makeText(baseContext, "Music will stop after 60 minutes.", Toast.LENGTH_SHORT)
                .show()
            binding.timerBtnPa.setColorFilter(ContextCompat.getColor(this, R.color.blue))
            min60 = true
            Thread {
                Thread.sleep((60 * 60_000).toLong())
                if (min60) exitApplication()
            }.start()
            dialog.dismiss()
        }
    }
}