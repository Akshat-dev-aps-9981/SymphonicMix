package com.aksapps.symphonicmix.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aksapps.symphonicmix.R
import com.aksapps.symphonicmix.activities.MainActivity
import com.aksapps.symphonicmix.activities.PlayerActivity
import com.aksapps.symphonicmix.databinding.MusicViewBinding
import com.aksapps.symphonicmix.models.Music
import com.aksapps.symphonicmix.models.formatDuration
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions


class MusicAdapter(private val context: Context, private var musicList: ArrayList<Music>) :
    RecyclerView.Adapter<MusicAdapter.MusicViewHolder>() {

    // ViewHolder class holds all the views present in the sample layout file, which are needed to modify for each item.
    class MusicViewHolder(binding: MusicViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.songName
        val album = binding.songAlbum
        val image = binding.imgMusic
        val duration = binding.songDuration
        val root = binding.root
    }

    // Define which one is our sample layout file and returns it.
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MusicAdapter.MusicViewHolder {
        return MusicViewHolder(
            MusicViewBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    // Define the functionality of how each and every item should appear or behave based on their position.
    override fun onBindViewHolder(holder: MusicAdapter.MusicViewHolder, position: Int) {
        holder.title.text = musicList[position].title
        holder.album.text = musicList[position].album
        holder.duration.text = formatDuration(musicList[position].duration)
        Glide.with(context).load(musicList[position].artUri)
            .apply(RequestOptions().placeholder(R.drawable.ic_music_splash).centerCrop())
            .apply(RequestOptions().error(R.drawable.ic_music_splash).centerCrop())
            .into(holder.image)

        holder.root.setOnClickListener {
            when {
                MainActivity.search -> sendIntent(reference = "MusicAdapterSearch", position = position)
                musicList[position].id == PlayerActivity.nowPlayingId -> sendIntent(reference = "NowPlayingFragment", position = PlayerActivity.songPosition)
                else -> sendIntent(reference = "MusicAdapter", position = position)
            }
        }
    }

    // Decide how much sample layouts should be created.
    override fun getItemCount(): Int {
        return musicList.size // Returns the items present in the list.
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateMusicList (searchList : ArrayList<Music>) {
        musicList = ArrayList()
        musicList.addAll(searchList)
        notifyDataSetChanged()
    }

    private fun sendIntent(reference : String, position : Int) {
        val intent = Intent(context, PlayerActivity::class.java)
        intent.putExtra("index", position)
        intent.putExtra("class", reference)
        ContextCompat.startActivity(context, intent, null)
    }

}