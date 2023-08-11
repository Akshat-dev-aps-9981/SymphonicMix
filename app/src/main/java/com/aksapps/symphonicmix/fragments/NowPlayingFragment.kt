package com.aksapps.symphonicmix.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.aksapps.symphonicmix.R
import com.aksapps.symphonicmix.activities.PlayerActivity
import com.aksapps.symphonicmix.databinding.FragmentNowPlayingBinding
import com.aksapps.symphonicmix.models.setSongPosition
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions


class NowPlayingFragment : Fragment() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var binding : FragmentNowPlayingBinding
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_now_playing, container, false)
        binding = FragmentNowPlayingBinding.bind(view)
        // Now playing should by default turn off, if no song is playing.
        binding.root.visibility = View.GONE

        // Play/Pause button functionality of now playing fragment.
        binding.playPauseBtnNP.setOnClickListener { if (PlayerActivity.isPlaying) pauseMusic() else playMusic() }

        // Next button functionality of now playing fragment.
        binding.nextBtnNP.setOnClickListener {
            setSongPosition(increment = true)
            PlayerActivity.musicService?.createMediaPlayer()
            try {
                // Set the image and song title in fragment after changing song by clicking on next/previous buttons.
                Glide.with(this).load(PlayerActivity.musicListPA[PlayerActivity.songPosition].artUri)
                    .apply(RequestOptions().placeholder(R.drawable.ic_music_splash).centerCrop())
                    .apply(RequestOptions().error(R.drawable.ic_music_splash).centerCrop())
                    .into(binding.imgSongNp)
                binding.songNameNP.text = PlayerActivity.musicListPA[PlayerActivity.songPosition].title
                PlayerActivity.musicService?.showNotification(R.drawable.ic_pause_notification)
            } catch (e: Exception) {
                Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show()
            }
            playMusic()
        }

        // Previous button functionality of now playing fragment.
        binding.previousBtnNP.setOnClickListener {
            setSongPosition(increment = false)
            PlayerActivity.musicService?.createMediaPlayer()
            try {
                // Set the image and song title in fragment after changing song by clicking on next/previous buttons.
                Glide.with(this).load(PlayerActivity.musicListPA[PlayerActivity.songPosition].artUri)
                    .apply(RequestOptions().placeholder(R.drawable.ic_music_splash).centerCrop())
                    .apply(RequestOptions().error(R.drawable.ic_music_splash).centerCrop())
                    .into(binding.imgSongNp)
                binding.songNameNP.text = PlayerActivity.musicListPA[PlayerActivity.songPosition].title
                PlayerActivity.musicService?.showNotification(R.drawable.ic_pause_notification)
            } catch (e: Exception) {
                Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show()
            }
            playMusic()
        }

        // When clicking anywhere or parent layout of now playing fragment, player activity should open without reinstalling fresh.
        binding.root.setOnClickListener {
            val intent = Intent(requireContext(), PlayerActivity::class.java)
            intent.putExtra("index", PlayerActivity.songPosition)
            intent.putExtra("class", "NowPlayingFragment")
            ContextCompat.startActivity(requireContext(), intent, null)
        }
        return view
    }

    // onResume() is called after onCreate().
    override fun onResume() {
        super.onResume()
        // Check if any song playing or is paused and show fragment unless the music service is not created.
        if (PlayerActivity.musicService != null) {
            binding.root.visibility = View.VISIBLE
            binding.songNameNP.isSelected = true
            Glide.with(this).load(PlayerActivity.musicListPA[PlayerActivity.songPosition].artUri)
                .apply(RequestOptions().placeholder(R.drawable.ic_music_splash).centerCrop())
                .apply(RequestOptions().error(R.drawable.ic_music_splash).centerCrop())
                .into(binding.imgSongNp)
            binding.songNameNP.text = PlayerActivity.musicListPA[PlayerActivity.songPosition].title
            if (PlayerActivity.isPlaying)
                binding.playPauseBtnNP.setImageResource(R.drawable.pause_button)
            else
                binding.playPauseBtnNP.setImageResource(R.drawable.play_button)
        }
    }

    // Play the music.
    private fun playMusic() {
        PlayerActivity.musicService?.mediaPlayer?.start()
        binding.playPauseBtnNP.setImageResource(R.drawable.pause_button)
        PlayerActivity.musicService?.showNotification(R.drawable.ic_pause_notification)
        PlayerActivity.binding.playPauseBtn.setImageResource(R.drawable.pause_button)
        PlayerActivity.isPlaying = true
    }

    // Pause the music.
    private fun pauseMusic() {
        PlayerActivity.musicService?.mediaPlayer?.pause()
        binding.playPauseBtnNP.setImageResource(R.drawable.play_button)
        PlayerActivity.musicService?.showNotification(R.drawable.ic_play_notification)
        PlayerActivity.binding.playPauseBtn.setImageResource(R.drawable.play_button)
        PlayerActivity.isPlaying = false
    }
}