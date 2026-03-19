package com.example.lyricsplayer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.lyricsplayer.R
import com.example.lyricsplayer.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var songAdapter: SongAdapter
    private lateinit var lyricsAdapter: LyricsAdapter

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.loadSongs()
        } else {
            Toast.makeText(this, "Müzik taramak için izin gerekli", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setupUI()
        observeViewModel()
        checkPermissionsAndLoad()
    }

    private fun setupUI() {
        songAdapter = SongAdapter { song ->
            viewModel.playSong(song)
        }
        binding.rvSongs.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = songAdapter
        }

        lyricsAdapter = LyricsAdapter { line ->
            viewModel.seekTo(line.timeMs)
        }
        binding.rvLyrics.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = lyricsAdapter
        }

        binding.btnPlayPause.setOnClickListener {
            viewModel.togglePlayPause()
        }

        binding.btnNext.setOnClickListener {
            viewModel.playNext()
        }

        binding.btnPrevious.setOnClickListener {
            viewModel.playPrevious()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    viewModel.seekTo(progress.toLong())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnBackToList.setOnClickListener {
            viewModel.hidePlayer()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.songs.collect { songs ->
                songAdapter.submitList(songs)
                binding.tvEmptyState.visibility = if (songs.isEmpty()) View.VISIBLE else View.GONE
                binding.rvSongs.visibility = if (songs.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        lifecycleScope.launch {
            viewModel.isLoadingSongs.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.isPlayerVisible.collect { isVisible ->
                binding.layoutSongList.visibility = if (isVisible) View.GONE else View.VISIBLE
                binding.layoutPlayer.visibility = if (isVisible) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.currentSong.collect { song ->
                song?.let {
                    binding.tvPlayerTitle.text = it.title
                    binding.tvPlayerArtist.text = it.artist
                    Glide.with(this@MainActivity)
                        .load(it.albumArtUri)
                        .placeholder(R.drawable.ic_music_note)
                        .error(R.drawable.ic_music_note)
                        .centerCrop()
                        .into(binding.ivPlayerAlbumArt)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoadingLyrics.collect { isLoading ->
                binding.progressBarLyrics.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.currentLyrics.collect { lyrics ->
                if (lyrics == null) {
                    binding.tvNoLyrics.visibility = View.VISIBLE
                    binding.rvLyrics.visibility = View.GONE
                    binding.tvPlainLyrics.visibility = View.GONE
                } else {
                    binding.tvNoLyrics.visibility = View.GONE
                    if (lyrics.syncedLyrics.isNullOrEmpty()) {
                        binding.rvLyrics.visibility = View.GONE
                        binding.tvPlainLyrics.visibility = View.VISIBLE
                        binding.tvPlainLyrics.text = lyrics.plainLyrics ?: "Şarkı sözleri bulunamadı"
                    } else {
                        binding.rvLyrics.visibility = View.VISIBLE
                        binding.tvPlainLyrics.visibility = View.GONE
                        lyricsAdapter.updateLines(lyrics.syncedLyrics)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.currentLyricsLine.collect { line ->
                line?.let {
                    val allLines = viewModel.allSyncedLines.value
                    val index = allLines.indexOfFirst { l -> l.timeMs == it.timeMs }
                    if (index >= 0) {
                        lyricsAdapter.highlightLine(index)
                        (binding.rvLyrics.layoutManager as? LinearLayoutManager)
                            ?.scrollToPositionWithOffset(index, binding.rvLyrics.height / 3)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.playbackState.collect { state ->
                binding.btnPlayPause.setImageResource(
                    if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                )
                binding.seekBar.max = state.duration.toInt()
                binding.seekBar.progress = state.currentPosition.toInt()
                binding.tvTimeCurrent.text = formatDuration(state.currentPosition)
                binding.tvTimeTotal.text = formatDuration(state.duration)
            }
        }
    }

    private fun checkPermissionsAndLoad() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            viewModel.loadSongs()
        } else {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}
