package com.aksapps.symphonicmix.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.aksapps.symphonicmix.R
import com.aksapps.symphonicmix.adapters.MusicAdapter
import com.aksapps.symphonicmix.databinding.ActivityMainBinding
import com.aksapps.symphonicmix.models.Music
import com.aksapps.symphonicmix.models.exitApplication
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import java.io.File

/*const val DENIED = "Denied"
const val EXPLAINED = "Explained"*/

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val REQUEST_CODE = 13
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var toolbar: Toolbar

    private lateinit var musicAdapter: MusicAdapter

    companion object {
        lateinit var musicListMA: ArrayList<Music>
        lateinit var musicListSearch: ArrayList<Music>
        var search: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // First initialize the toolbar.
        toolbar = findViewById(R.id.custom_toolbar)
        setSupportActionBar(toolbar)

        // Then set up the navigation drawer.
        toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, toolbar,
            R.string.open, R.string.close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        // Check if permission is already granted, if so, then initialize recycler layout, skip the code otherwise.
        if (requestRuntimePermissions()) {
            initializeLayout()
            val ed = getSharedPreferences("FirstTime", MODE_PRIVATE)
            val isFirstTime = ed.getBoolean("isFirstTime", true)
            if (isFirstTime)
                showTapTarget()
            // For retrieve favorites data using shared preferences.
            FavoritesActivity.favoriteSongs = ArrayList()
            val editor = getSharedPreferences("FAVORITE", MODE_PRIVATE)
            val jsonString = editor.getString("FavoriteSongs", null)
            val typeToken = object : TypeToken<ArrayList<Music>>() {}.type
            if (jsonString != null) {
                val data: ArrayList<Music> = GsonBuilder().create().fromJson(jsonString, typeToken)
                FavoritesActivity.favoriteSongs.addAll(data)
            }
        }

        // Click Listeners for the three buttons on the top - shuffle, favorite & playlist.
        binding.shuffleBtn.setOnClickListener {
            val intent = Intent(this@MainActivity, PlayerActivity::class.java)
            intent.putExtra("index", 0)
            intent.putExtra("class", "MainActivity")
            startActivity(intent)
        }
        binding.favBtn.setOnClickListener {
            val intent = Intent(this@MainActivity, FavoritesActivity::class.java)
            startActivity(intent)
        }
        binding.playlistBtn.setOnClickListener {
            val intent = Intent(this@MainActivity, PlaylistsActivity::class.java)
            startActivity(intent)
        }

        // Handle the clicks for the items in navigation drawer.
        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_feedback -> Toast.makeText(this, "Feedback.", Toast.LENGTH_SHORT).show()
                R.id.nav_settings -> Toast.makeText(this, "Settings.", Toast.LENGTH_SHORT).show()
                R.id.nav_about -> Toast.makeText(this, "About.", Toast.LENGTH_SHORT).show()
                R.id.nav_exit -> {
                    val builder = MaterialAlertDialogBuilder(this)
                    builder.setTitle("Exit")
                        .setMessage("Do you want to close the App?")
                        .setPositiveButton("Yes") { _, _ ->
                            exitApplication()
                        }
                        .setNegativeButton("No") { dialog, _ ->
                            dialog.dismiss()
                        }
                    builder.setIcon(R.drawable.ic_music_splash)
                    val customDialog = builder.create()
                    customDialog.show()
                    customDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
                    customDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED)
                }

                else -> {}
            }
            true
        }
    }

    // Initialize recycler view and corresponding layouts.
    @SuppressLint("SetTextI18n")
    private fun initializeLayout() {
        search = false
        musicListMA = getAllAudio()
        binding.musicRv.setHasFixedSize(true)
        binding.musicRv.setItemViewCacheSize(10)
        binding.musicRv.layoutManager = LinearLayoutManager(this)
        musicAdapter = MusicAdapter(this@MainActivity, musicListMA)
        binding.musicRv.adapter = musicAdapter
        binding.totalSongs.text = "Total Songs: ${musicAdapter.itemCount}"
    }

    // Sync the toggle state after onRestoreInstanceState has occurred.
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
    }

    // Guide users to use the app.
    private fun showTapTarget() {
        MaterialTapTargetPrompt.Builder(this@MainActivity)
            .setTarget(R.id.shuffle_btn)
            .setPrimaryText("Shuffle Songs")
            .setSecondaryText("Obtain a random song from the list of songs.")
            .setBackgroundColour(Color.GREEN)
            .setPromptStateChangeListener { _, state ->
                if (state == MaterialTapTargetPrompt.STATE_DISMISSED) {
                    MaterialTapTargetPrompt.Builder(this@MainActivity)
                        .setTarget(R.id.fav_btn)
                        .setPrimaryText("Favourite Songs")
                        .setSecondaryText("See the list of favourite songs marked by you.")
                        .setBackgroundColour(Color.RED)
                        .setPromptStateChangeListener { prompt, stt ->
                            if (stt == MaterialTapTargetPrompt.STATE_DISMISSED) {
                                prompt.dismiss()
                                val editor = getSharedPreferences("FirstTime", MODE_PRIVATE).edit()
                                editor.putBoolean("isFirstTime", false)
                                editor.apply()
                            }
                        }
                        .show()
                }
            }
            .show()
    }

    // To request accessing audio file at runtime.
    private fun requestRuntimePermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) android.Manifest.permission.READ_EXTERNAL_STORAGE else android.Manifest.permission.READ_MEDIA_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showDialog()
            return false
        }
        return true
    }

    // Handle the result of permission request: i.e. check whether user has granted permission or not.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show()
                initializeLayout()
                showTapTarget()
            } else
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE) else arrayOf(
                        android.Manifest.permission.READ_MEDIA_AUDIO
                    ),
                    REQUEST_CODE
                )
        }
    }

    // Search button on top toolbar.
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search_view_menu, menu)
        val searchView = menu?.findItem(R.id.search_view)?.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true

            // Show relevant results when user types something in search box.
            override fun onQueryTextChange(newText: String?): Boolean {
                musicListSearch = ArrayList()
                if (newText != null) {
                    val userInput = newText.lowercase()
                    for (song in musicListMA) {
                        if (song.title.lowercase().contains(userInput))
                            musicListSearch.add(song)
                        search = true
                        musicAdapter.updateMusicList(searchList = musicListSearch)
                    }
                }
                return true
            }

        })
        return super.onCreateOptionsMenu(menu)
    }

    // Handle the item clicks on menu navigation.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item))
            return true
        return super.onOptionsItemSelected(item)

    }

    // This function is used to get all the songs or audio files from user's device. Call this after granting appropriate permissions.
    @SuppressLint("Range")
    private fun getAllAudio(): ArrayList<Music> {
        val tempList = arrayListOf<Music>()
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val cursor = this.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            MediaStore.Audio.Media.DATE_ADDED + " DESC",
            null
        )
        if (cursor != null) {
            if (cursor.moveToFirst())
                do {
                    val titleC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
                    val idC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID))
                    val albumC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM))
                    val artistC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                    val pathC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                    val durationC =
                        cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                    val albumIdC =
                        cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
                            .toString()
                    val uri = Uri.parse("content://media/external/audio/albumart")
                    val artUriC = Uri.withAppendedPath(uri, albumIdC).toString()
                    val music = Music(
                        id = idC,
                        title = titleC,
                        album = albumC,
                        artist = artistC,
                        path = pathC,
                        duration = durationC,
                        artUri = artUriC
                    )
                    val file = File(music.path)
                    if (file.exists())
                        tempList.add(music)
                } while (cursor.moveToNext())
            cursor.close()
        }
        return tempList
    }

    // If music is not currently playing, i.e. paused or not selected any music to play then stop service and exit application when it is destroyed.
    override fun onDestroy() {
        super.onDestroy()
        if (!PlayerActivity.isPlaying && PlayerActivity.musicService != null) {
            exitApplication()
        }
    }

    override fun onResume() {
        super.onResume()
        // For storing favorites data using shared preferences.
        val editor = getSharedPreferences("FAVORITE", MODE_PRIVATE).edit()
        val jsonString = GsonBuilder().create().toJson(FavoritesActivity.favoriteSongs)
        editor.putString("FavoriteSongs", jsonString)
        editor.apply()
    }

    // If the drawer is open, close the drawer first when clicking on back button, exit otherwise.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START))
            binding.drawerLayout.closeDrawers()
        else
            super.onBackPressed()
    }

    // User consent before asking for permission.
    private fun showDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("Welcome to SymphonicMix.")
            .setMessage(
                "Hi there, we are so happy to see you here! Before we move on further, we would like to access the audio files present in your device in order to display and play music/songs. This app doesn't require internet connection and is fully functional in offline mode. Your data is always protected, we don't collect and/or share any user data. For more details you can refer to our Privacy Policy.\n" +
                        "Thank-you so much for using the app and we hope you will have great time here!"
            )
            .setPositiveButton("Allow Permission") { dialog, _ ->
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE) else arrayOf(
                        android.Manifest.permission.READ_MEDIA_AUDIO
                    ),
                    REQUEST_CODE
                )
                dialog.dismiss()
            }
            .setNegativeButton("Exit") { dialog, _ ->
                dialog.dismiss()
                exitApplication()
            }
            .setNeutralButton("Privacy Policy") { dialog, _ ->
                dialog.dismiss()
            }
        builder.setCancelable(false)
        builder.setIcon(R.drawable.ic_music_splash)
        val customDialog = builder.create()
        customDialog.show()
        customDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
        customDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED)
    }
}