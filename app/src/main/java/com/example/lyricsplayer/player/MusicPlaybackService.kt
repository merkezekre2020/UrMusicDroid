package com.example.lyricsplayer.player

import android.media.MediaPlayer
import android.util.Log
import com.example.lyricsplayer.data.model.Lyrics
import com.example.lyricsplayer.data.model.LyricsLine
import com.example.lyricsplayer.data.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MusicPlaybackService {

    private var mediaPlayer: MediaPlayer? = null
    private var currentSong: Song? = null
    private var currentLyrics: Lyrics? = null
    private var lyricsJob: Job? = null
    var onCompletion: (() -> Unit)? = null
    var onPrevRequested: (() -> Unit)? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState

    private val _currentLyricsLine = MutableStateFlow<LyricsLine?>(null)
    val currentLyricsLine: StateFlow<LyricsLine?> = _currentLyricsLine

    private val _allSyncedLines = MutableStateFlow<List<LyricsLine>>(emptyList())
    val allSyncedLines: StateFlow<List<LyricsLine>> = _allSyncedLines

    data class PlaybackState(
        val isPlaying: Boolean = false,
        val currentPosition: Long = 0,
        val duration: Long = 0,
        val song: Song? = null
    )

    fun playSong(song: Song, lyrics: Lyrics? = null) {
        stop()
        currentSong = song
        currentLyrics = lyrics

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(song.data)
                prepare()
                start()
                setOnCompletionListener {
                    updatePlaybackState()
                    onCompletion?.invoke()
                }
            }

            _allSyncedLines.value = lyrics?.syncedLyrics ?: emptyList()

            updatePlaybackState()
            startLyricsSync()
        } catch (e: Exception) {
            Log.e("MusicPlaybackService", "Error playing song", e)
        }
    }

    fun setLyrics(lyrics: Lyrics) {
        currentLyrics = lyrics
        _allSyncedLines.value = lyrics.syncedLyrics ?: emptyList()
        startLyricsSync()
    }

    fun togglePlayPause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.start()
            }
            updatePlaybackState()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        updatePlaybackState()
    }

    fun playNext(songs: List<Song>) {
        val currentIndex = songs.indexOfFirst { it.id == currentSong?.id }
        if (currentIndex >= 0 && currentIndex < songs.size - 1) {
            playSong(songs[currentIndex + 1])
        }
    }

    fun playPrevious(songs: List<Song>) {
        val currentIndex = songs.indexOfFirst { it.id == currentSong?.id }
        if (currentIndex > 0) {
            playSong(songs[currentIndex - 1])
        }
    }

    fun getCurrentPosition(): Long {
        return mediaPlayer?.currentPosition?.toLong() ?: 0
    }

    fun getDuration(): Long {
        return mediaPlayer?.duration?.toLong() ?: 0
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    fun stop() {
        lyricsJob?.cancel()
        mediaPlayer?.apply {
            try {
                if (isPlaying) stop()
            } catch (_: IllegalStateException) {}
            release()
        }
        mediaPlayer = null
        _currentLyricsLine.value = null
        _allSyncedLines.value = emptyList()
        _playbackState.value = PlaybackState()
    }

    fun release() {
        stop()
    }

    private fun updatePlaybackState() {
        _playbackState.value = PlaybackState(
            isPlaying = mediaPlayer?.isPlaying ?: false,
            currentPosition = mediaPlayer?.currentPosition?.toLong() ?: 0,
            duration = mediaPlayer?.duration?.toLong() ?: 0,
            song = currentSong
        )
    }

    private fun startLyricsSync() {
        lyricsJob?.cancel()
        val syncedLyrics = currentLyrics?.syncedLyrics
        if (syncedLyrics.isNullOrEmpty()) return

        lyricsJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                val position = mediaPlayer?.currentPosition?.toLong() ?: 0

                val currentLine = syncedLyrics
                    .filter { it.timeMs <= position }
                    .maxByOrNull { it.timeMs }

                _currentLyricsLine.value = currentLine
                updatePlaybackState()
                delay(50)
            }
        }
    }
}
