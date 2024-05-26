package com.barryzeha.ktmusicplayer.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Notification.MediaStyle
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.barryzeha.core.common.MUSIC_PLAYER_SESSION
import com.barryzeha.core.common.createTime
import com.barryzeha.core.common.getSongCover
import com.barryzeha.core.model.SongAction
import com.barryzeha.core.model.SongController
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.ktmusicplayer.common.foregroundNotification

import com.barryzeha.ktmusicplayer.common.notificationMediaPlayer

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
    private lateinit var exoPlayer:ExoPlayer

    private var _songController: SongController? = null
    val songController: SongController get() = _songController!!
    private var isForegroundService = false
    private var currentMusicState = MusicState()

    private var songRunnable: Runnable = Runnable {}
    private var songHandler: Handler = Handler(Looper.getMainLooper())

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
                            KeyEvent.KEYCODE_MEDIA_PLAY ->{ _songController?.play()}
                            KeyEvent.KEYCODE_MEDIA_PAUSE -> {_songController?.pause()}
                            KeyEvent.KEYCODE_MEDIA_NEXT -> {_songController?.next()}
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {_songController?.previous()}
                            else -> {}
                        }
                    }
                }
                return true
            }
        })
        initExoplayer()
        startForeground(1, foregroundNotification(this)).also {
            isForegroundService=true
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        var musicState = intent?.getParcelableExtra<MusicState>("musicState")

        when (SongAction.values()[intent?.action?.toInt() ?: SongAction.Nothing.ordinal]) {
            SongAction.Pause -> {
                _songController?.pause()
              }
            SongAction.Resume -> {
                _songController?.play()
             }
            SongAction.Stop -> {
                _songController?.stop()
            }
            SongAction.Next -> {
                _songController?.next()
            }
            SongAction.Previous -> {
                _songController?.previous()
            }
            SongAction.Nothing -> {}
        }

        musicState?.let { newState ->

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
    private fun initExoplayer(){
        exoPlayer = ExoPlayer.Builder(applicationContext)
            .build()

            songRunnable = Runnable {
                if(exoPlayer.currentPosition>0) {
                    currentMusicState = currentMusicState.copy(
                        isPlaying = exoPlayer.isPlaying,
                        currentDuration = exoPlayer.currentPosition,
                        duration = exoPlayer.duration

                    )
                }
                //if(exoPlayer.isPlaying) {
                    _songController?.musicState(currentMusicState)
                //}
                songHandler.postDelayed(songRunnable, 500)
            }
            songHandler.post(songRunnable)

    }
    private fun setUpExoPlayer(songPath:String){
        if(exoPlayer.isPlaying){
            exoPlayer.stop()
            exoPlayer= ExoPlayer.Builder(applicationContext)
                .build()

        }else{
            exoPlayer= ExoPlayer.Builder(applicationContext)
                .build()
        }
        exoPlayer.addListener(object: Player.Listener{
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_READY && exoPlayer.duration > 0) {
                    val durationInMillis = exoPlayer.duration
                    val formattedDuration = createTime(durationInMillis).third

                    val songMetadata= getSongCover(applicationContext!!,songPath)

                    // Set info currentSongEntity
                    currentMusicState = MusicState(
                        isPlaying=exoPlayer.isPlaying,
                        title = songPath.substringAfterLast("/","No named")!!,
                        artist = songMetadata!!.artist,
                        album = songMetadata!!.album,
                        albumArt = songMetadata!!.albumArt,
                        duration =(exoPlayer.duration).toLong(),
                        songPath = songPath
                    )
                    _songController?.currentTrack(currentMusicState)
                }
                if(playbackState == Player.STATE_ENDED ){
                    currentMusicState = currentMusicState.copy(
                        isPlaying = false
                    )
                    _songController?.currentTrack(currentMusicState)
                }

            }
        })

        exoPlayer.addMediaItem(MediaItem.fromUri(songPath))
        exoPlayer.prepare()
        exoPlayer.play()

    }
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    fun setActivity(activity:AppCompatActivity){
        this._activity=activity
    }
    fun setSongController(controller:SongController){
        _songController=controller
    }
    fun startPlayer(songPath:String){
        songPath?.let {
            setUpExoPlayer(songPath)
        }
    }
    fun pauseExoPlayer(){
        if(exoPlayer.isPlaying){
            exoPlayer.pause()

        }
    }
    fun playingExoPlayer(){
        if(!exoPlayer.isPlaying){
            exoPlayer.play()
        }
    }
    fun setExoPlayerProgress(progress:Long){
        exoPlayer.seekTo(progress)
    }
    fun stopStartLoop(state:Boolean){
        if(state)songHandler?.removeCallbacks(songRunnable)
        else songHandler?.post(songRunnable)
    }
    fun unregisterController(){
        _songController=null
    }
    override fun onDestroy() {
        isForegroundService = false
        _songController?.stop()
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