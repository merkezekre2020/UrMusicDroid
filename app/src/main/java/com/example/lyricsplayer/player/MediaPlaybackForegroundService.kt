package com.example.lyricsplayer.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.lyricsplayer.R
import com.example.lyricsplayer.ui.MainActivity

class MediaPlaybackForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "music_playback_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.lyricsplayer.ACTION_START"
        const val ACTION_STOP = "com.example.lyricsplayer.ACTION_STOP"
        const val ACTION_PLAY_PAUSE = "com.example.lyricsplayer.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.lyricsplayer.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.lyricsplayer.ACTION_PREVIOUS"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_ALBUM = "extra_album"
        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_ALBUM_URI = "extra_album_uri"
        const val EXTRA_IS_PLAYING = "extra_is_playing"

        var musicService: MusicPlaybackService? = null
    }

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        initMediaSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                musicService?.stop()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PLAY_PAUSE -> {
                musicService?.togglePlayPause()
                refreshNotification()
            }
            ACTION_NEXT -> {
                musicService?.onCompletion?.invoke()
            }
            ACTION_PREVIOUS -> {
                musicService?.onPrevRequested?.invoke()
            }
            else -> {
                val isPlaying = intent?.getBooleanExtra(EXTRA_IS_PLAYING, true) ?: true
                val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Bilinmeyen"
                val artist = intent?.getStringExtra(EXTRA_ARTIST) ?: "Bilinmeyen Sanatçı"
                val album = intent?.getStringExtra(EXTRA_ALBUM) ?: ""
                val duration = intent?.getLongExtra(EXTRA_DURATION, 0) ?: 0
                val albumUri = intent?.getStringExtra(EXTRA_ALBUM_URI)

                updateMediaSession(isPlaying, title, artist, album, duration, albumUri)
                val notification = buildNotification(title, artist, album, albumUri, isPlaying)
                startForeground(NOTIFICATION_ID, notification)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mediaSession.isActive = false
        mediaSession.release()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Müzik çalma bildirimleri"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "LyricsPlayerSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    musicService?.togglePlayPause()
                    refreshNotification()
                }

                override fun onPause() {
                    musicService?.togglePlayPause()
                    refreshNotification()
                }

                override fun onSkipToNext() {
                    musicService?.onCompletion?.invoke()
                }

                override fun onSkipToPrevious() {
                    musicService?.onPrevRequested?.invoke()
                }

                override fun onSeekTo(pos: Long) {
                    musicService?.seekTo(pos)
                }

                override fun onStop() {
                    musicService?.stop()
                    stopSelf()
                }
            })
            isActive = true
        }
    }

    private fun updateMediaSession(
        isPlaying: Boolean,
        title: String,
        artist: String,
        album: String,
        duration: Long,
        albumUri: String?
    ) {
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_STOP
            )
            .setState(state, musicService?.getCurrentPosition() ?: 0, 1f)
            .build()
        mediaSession.setPlaybackState(playbackState)

        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
            .apply {
                val albumArt = try {
                    albumUri?.let {
                        contentResolver.openInputStream(Uri.parse(it))?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }
                } catch (_: Exception) { null }
                albumArt?.let {
                    putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
                }
            }
            .build()
        mediaSession.setMetadata(metadata)
    }

    private fun buildNotification(
        title: String,
        artist: String,
        album: String,
        albumUri: String?,
        isPlaying: Boolean
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val playPauseTitle = if (isPlaying) "Duraklat" else "Oynat"

        val albumArt = try {
            albumUri?.let {
                contentResolver.openInputStream(Uri.parse(it))?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        } catch (_: Exception) { null }

        val prevPendingIntent = PendingIntent.getService(
            this, 100,
            Intent(this, MediaPlaybackForegroundService::class.java).apply {
                action = ACTION_PREVIOUS
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPausePendingIntent = PendingIntent.getService(
            this, 101,
            Intent(this, MediaPlaybackForegroundService::class.java).apply {
                action = ACTION_PLAY_PAUSE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextPendingIntent = PendingIntent.getService(
            this, 102,
            Intent(this, MediaPlaybackForegroundService::class.java).apply {
                action = ACTION_NEXT
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText(album)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_skip_previous, "Önceki", prevPendingIntent)
            .addAction(playPauseIcon, playPauseTitle, playPausePendingIntent)
            .addAction(R.drawable.ic_skip_next, "Sonraki", nextPendingIntent)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        albumArt?.let { builder.setLargeIcon(it) }

        return builder.build()
    }

    private fun refreshNotification() {
        val service = musicService ?: return
        val state = service.playbackState.value
        val song = state.song ?: return

        val isPlaying = state.isPlaying
        updateMediaSession(isPlaying, song.title, song.artist, song.album, song.duration, song.albumArtUri)
        val notification = buildNotification(song.title, song.artist, song.album, song.albumArtUri, isPlaying)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
