package com.example.lyricsplayer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lyricsplayer.data.model.Lyrics
import com.example.lyricsplayer.data.model.Song
import com.example.lyricsplayer.data.repository.LyricsRepository
import com.example.lyricsplayer.player.MusicPlaybackService
import com.example.lyricsplayer.scanner.MusicScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val musicScanner = MusicScanner(application.contentResolver)
    private val lyricsRepository = LyricsRepository()
    val playbackService = MusicPlaybackService()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _currentLyrics = MutableStateFlow<Lyrics?>(null)
    val currentLyrics: StateFlow<Lyrics?> = _currentLyrics

    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics

    private val _isLoadingSongs = MutableStateFlow(false)
    val isLoadingSongs: StateFlow<Boolean> = _isLoadingSongs

    private val _isPlayerVisible = MutableStateFlow(false)
    val isPlayerVisible: StateFlow<Boolean> = _isPlayerVisible

    val playbackState = playbackService.playbackState
    val currentLyricsLine = playbackService.currentLyricsLine
    val allSyncedLines = playbackService.allSyncedLines

    fun loadSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingSongs.value = true
            val scannedSongs = musicScanner.scanSongs()
            _songs.value = scannedSongs
            _isLoadingSongs.value = false
        }
    }

    fun playSong(song: Song) {
        _currentSong.value = song
        _isPlayerVisible.value = true
        _isLoadingLyrics.value = true
        _currentLyrics.value = null

        viewModelScope.launch(Dispatchers.IO) {
            val lyrics = lyricsRepository.getLyrics(song)
            _currentLyrics.value = lyrics
            _isLoadingLyrics.value = false

            playbackService.playSong(song, lyrics)
        }
    }

    fun togglePlayPause() {
        playbackService.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playbackService.seekTo(positionMs)
    }

    fun playNext() {
        val songs = _songs.value
        playbackService.playNext(songs)
        val currentIndex = songs.indexOfFirst { it.id == _currentSong.value?.id }
        if (currentIndex >= 0 && currentIndex < songs.size - 1) {
            playSong(songs[currentIndex + 1])
        }
    }

    fun playPrevious() {
        val songs = _songs.value
        val currentIndex = songs.indexOfFirst { it.id == _currentSong.value?.id }
        if (currentIndex > 0) {
            playSong(songs[currentIndex - 1])
        }
    }

    fun hidePlayer() {
        _isPlayerVisible.value = false
    }

    override fun onCleared() {
        super.onCleared()
        playbackService.release()
    }
}
