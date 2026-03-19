package com.example.lyricsplayer.data.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val data: String,
    val albumArtUri: String?
)
