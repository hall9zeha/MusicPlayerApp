package com.barryzeha.ktmusicplayer.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Notification.MediaStyle
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Binder
import android.os.IBinder
import android.provider.SyncStateContract.Helpers
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.barryzeha.core.common.MUSIC_PLAYER_SESSION


import com.barryzeha.core.model.SongAction
import com.barryzeha.core.model.SongController
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.ktmusicplayer.common.foregroundNotification

import com.barryzeha.ktmusicplayer.common.notificationMediaPlayer
import kotlin.math.log
import kotlin.system.exitProcess


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 16/5/24.
 * Copyright (c)  All rights reserved.
 **/

class MusicPlayerService : Service() {
    private lateinit var mediaSession: MediaSession
    private lateinit var mediaStyle: MediaStyle
    private lateinit var notificationManager: NotificationManager
    private val binder: Binder = MusicPlayerServiceBinder()
    private var _activity:AppCompatActivity?= null

    private var songController: SongController? = null
    private var isForegroundService = false

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mediaSession = MediaSession(this, MUSIC_PLAYER_SESSION)
        mediaStyle = Notification.MediaStyle().setMediaSession(mediaSession.sessionToken)

        mediaSession.setCallback(object : MediaSession.Callback(){
            override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                if(Intent.ACTION_MEDIA_BUTTON == mediaButtonIntent.action){
                    val event = mediaButtonIntent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                    event?.let{
                        when(it.keyCode){
                            KeyEvent.KEYCODE_MEDIA_PLAY -> songController?.play()
                            KeyEvent.KEYCODE_MEDIA_PAUSE -> songController?.pause()
                            KeyEvent.KEYCODE_MEDIA_NEXT -> songController?.next()
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> songController?.previous()
                            else -> {}
                        }
                    }
                }
                return true
            }
        })
        startForeground(1, foregroundNotification(this)).also {
            isForegroundService=true
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (SongAction.values()[intent?.action?.toInt() ?: SongAction.Nothing.ordinal]) {
            SongAction.Pause -> songController?.pause()
            SongAction.Resume -> songController?.play()
            SongAction.Stop -> songController?.stop()
            SongAction.Next -> songController?.next()
            SongAction.Previous -> songController?.previous()
            SongAction.Nothing -> {}
        }
        val musicState = intent?.getParcelableExtra<MusicState>("musicState")

        musicState?.let { newState ->
            Log.e("MUSIC-ENTITY", newState.toString() )
            if (isForegroundService) {
                mediaSession.setPlaybackState(
                    PlaybackState.Builder()
                        .setState(
                            if (newState.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                            newState.currentDuration,
                            1f
                        )
                        .setActions(PlaybackState.ACTION_PLAY_PAUSE)
                        .build()
                )

                // Update state to media session
                mediaSession.setMetadata(
                    MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_TITLE, newState.title)
                        .putString(MediaMetadata.METADATA_KEY_ALBUM, newState.album)
                        .putString(MediaMetadata.METADATA_KEY_ARTIST, newState.artist)
                        .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, newState.albumArt)
                        .putLong(MediaMetadata.METADATA_KEY_DURATION, newState.duration)
                        .build()
                )


                // Update notification
                notificationManager.notify(
                    0,
                    notificationMediaPlayer(
                        this,
                        Notification.MediaStyle().setMediaSession(mediaSession.sessionToken),
                        newState
                    )
                )
            }
        }

        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    fun setActivity(activity:AppCompatActivity){
        this._activity=activity
    }
    fun setSongController(controller:SongController){
        songController=controller
    }


    override fun onDestroy() {
        isForegroundService = false
        songController?.stop()
        mediaSession.release()
        stopSelf()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exitProcess(0)
    }
    inner class MusicPlayerServiceBinder : Binder() {
        fun getService(): MusicPlayerService {
            return this@MusicPlayerService
        }
    }
}