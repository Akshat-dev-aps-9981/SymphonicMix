package com.aksapps.symphonicmix.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aksapps.symphonicmix.databinding.ActivityPlaylistsBinding

class PlaylistsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlaylistsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBtnPl.setOnClickListener { finish() }
    }
}