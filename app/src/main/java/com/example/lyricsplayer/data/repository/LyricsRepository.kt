package com.example.lyricsplayer.data.repository

import android.util.Log
import com.example.lyricsplayer.data.api.RetrofitClient
import com.example.lyricsplayer.data.model.Lyrics
import com.example.lyricsplayer.data.model.Song

class LyricsRepository {
    
    private val apiService = RetrofitClient.lrcLibApiService
    
    suspend fun getLyrics(song: Song): Lyrics? {
        return try {
            val response = apiService.getLyrics(
                trackName = song.title,
                artistName = song.artist,
                albumName = song.album,
                duration = song.duration / 1000
            )
            
            if (response != null) {
                // First try synced lyrics
                if (!response.syncedLyrics.isNullOrBlank()) {
                    return Lyrics.parseSyncedLyrics(response.syncedLyrics)
                }
                // Fall back to plain lyrics
                if (!response.plainLyrics.isNullOrBlank()) {
                    return Lyrics(response.plainLyrics, null)
                }
            }
            null
        } catch (e: Exception) {
            Log.e("LyricsRepository", "Error fetching lyrics", e)
            null
        }
    }
    
    suspend fun searchLyrics(trackName: String, artistName: String): List<Lyrics> {
        return try {
            val responses = apiService.search(
                trackName = trackName,
                artistName = artistName
            )
            
            responses.mapNotNull { response ->
                val synced = if (!response.syncedLyrics.isNullOrBlank()) {
                    Lyrics.parseSyncedLyrics(response.syncedLyrics)?.syncedLyrics
                } else null
                
                val plain = response.plainLyrics
                
                if (plain != null || synced != null) {
                    Lyrics(plain, synced)
                } else null
            }
        } catch (e: Exception) {
            Log.e("LyricsRepository", "Error searching lyrics", e)
            emptyList()
        }
    }
}
