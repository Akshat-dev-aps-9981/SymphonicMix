package com.aksapps.symphonicmix.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aksapps.symphonicmix.R
import com.aksapps.symphonicmix.activities.PlayerActivity
import com.aksapps.symphonicmix.databinding.FavouriteViewBinding
import com.aksapps.symphonicmix.models.Music
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions


class FavoritesAdapter(private val context: Context, private var musicList: ArrayList<Music>) :
    RecyclerView.Adapter<FavoritesAdapter.FavouriteViewHolder>() {

    // ViewHolder class holds all the views present in the sample layout file, which are needed to modify for each item.
    class FavouriteViewHolder(binding: FavouriteViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val image = binding.imgSongFv
        val name = binding.songNameFv
        val root = binding.root
    }

    // Define which one is our sample layout file and returns it.
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FavoritesAdapter.FavouriteViewHolder {
        return FavouriteViewHolder(
            FavouriteViewBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    // Define the functionality of how each and every item should appear or behave based on their position.
    override fun onBindViewHolder(holder: FavoritesAdapter.FavouriteViewHolder, position: Int) {
        holder.name.text = musicList[position].title
        Glide.with(context).load(musicList[position].artUri)
            .apply(RequestOptions().placeholder(R.drawable.ic_music_splash).centerCrop())
            .apply(RequestOptions().error(R.drawable.ic_music_splash).centerCrop())
            .into(holder.image)

        holder.root.setOnClickListener {
            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra("index", position)
            intent.putExtra("class", "FavoriteAdapter")
            ContextCompat.startActivity(context, intent, null)
        }
    }

    // Decide how much sample layouts should be created.
    override fun getItemCount(): Int {
        return musicList.size // Returns the items present in the list.
    }

    // Send to player activity.
    private fun sendIntent(reference: String, position: Int) {

    }

}