package com.aksapps.symphonicmix.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.aksapps.symphonicmix.adapters.FavoritesAdapter
import com.aksapps.symphonicmix.databinding.ActivityFavoritesBinding
import com.aksapps.symphonicmix.models.Music

class FavoritesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFavoritesBinding
    private lateinit var favoritesAdapter: FavoritesAdapter

    companion object {
        var favoriteSongs: ArrayList<Music> = ArrayList()
        var favoritesChanged: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Close the favorite activity when the user clicks on back button shown in toolbar.
        binding.backBtnFv.setOnClickListener { finish() }

        // Set recycler view functionality to display favorite songs.
        binding.favoriteRv.setHasFixedSize(true)
        binding.favoriteRv.setItemViewCacheSize(10)
        binding.favoriteRv.layoutManager = GridLayoutManager(this, 4)
        favoritesAdapter = FavoritesAdapter(this@FavoritesActivity, favoriteSongs)
        binding.favoriteRv.adapter = favoritesAdapter

        favoritesChanged = false

        if (favoriteSongs.size < 1)
            binding.shuffleBtnFa.visibility = View.GONE

        /*if (favoriteSongs.isNotEmpty())
            binding.instructionFv.visibility = View.GONE*/

        binding.shuffleBtnFa.setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("index", 0)
            intent.putExtra("class", "FavoritesShuffle")
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        if (favoritesChanged) {
//            favoritesAdapter.updateFavorites(favoriteSongs)
            favoritesChanged = false
        }
    }
}