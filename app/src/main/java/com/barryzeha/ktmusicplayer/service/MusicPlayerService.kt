package com.barryzeha.ktmusicplayer.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Notification.MediaStyle
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.barryzeha.core.common.MUSIC_PLAYER_SESSION
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.common.getSongCover
import com.barryzeha.core.common.toJson
import com.barryzeha.core.model.SongAction
import com.barryzeha.core.model.SongController
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.core.model.entities.SongState
import com.barryzeha.core.model.entities.SongStateWithDetail
import com.barryzeha.data.repository.MainRepository
import com.barryzeha.ktmusicplayer.MyApp
import com.barryzeha.ktmusicplayer.common.NOTIFICATION_ID
import com.barryzeha.ktmusicplayer.common.cancelPersistentNotify
import com.barryzeha.ktmusicplayer.common.notificationMediaPlayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.system.exitProcess


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 16/5/24.
 * Copyright (c)  All rights reserved.
 **/

@AndroidEntryPoint
class MusicPlayerService : Service() {

    @Inject
    lateinit var repository: MainRepository
    private var songsList: MutableList<SongEntity> = mutableListOf()

    private lateinit var mediaSession: MediaSession
    private lateinit var mediaStyle: MediaStyle
    private lateinit var notificationManager: NotificationManager
    private lateinit var mediaPlayerNotify:Notification
    private val binder: Binder = MusicPlayerServiceBinder()
    private var _activity:AppCompatActivity?= null
    private lateinit var exoPlayer:ExoPlayer

    private var _songController: SongController? = null
    val songController: SongController get() = _songController!!
    private var isForegroundService = false
    private var currentMusicState = MusicState()
    private var songRunnable: Runnable = Runnable {}
    private var songHandler: Handler = Handler(Looper.getMainLooper())
    private var executeOnceTime:Boolean=false
    private var musicState:MusicState?=null
    private lateinit var mPrefs:MyPreferences
    private var songEntity:SongEntity=SongEntity()
    private lateinit var songMetaData:MusicState

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        mPrefs = MyApp.mPrefs

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mediaSession = MediaSession(this, MUSIC_PLAYER_SESSION)
        mediaStyle = MediaStyle().setMediaSession(mediaSession.sessionToken)
        currentMusicState = MusicState(albumArt = getSongCover(applicationContext,null)!!.albumArt)
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
        setUpRepository()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        musicState = intent?.getParcelableExtra<MusicState>("musicState")

        when (SongAction.values()[intent?.action?.toInt() ?: SongAction.Nothing.ordinal]) {
            SongAction.Pause -> {
               _songController?.pause()
                exoPlayer.pause()
            }
            SongAction.Resume -> {
                _songController?.play()
                exoPlayer.play()
            }
            SongAction.Stop -> {
                _songController?.stop()

            }
            SongAction.Next -> {
                _songController?.next()
                nextOrPrevTRack(mPrefs.currentPosition.toInt() +1)
            }
            SongAction.Previous -> {
                _songController?.previous()
               nextOrPrevTRack(mPrefs.currentPosition.toInt() -1)
            }
            SongAction.Close -> {
                exoPlayer.stop()
                exoPlayer.release()
                songHandler.removeCallbacks(songRunnable)

                // Remove notification of foreground service process
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                _songController?.stop()
                // Close application
                _activity?.finish()

            }
            SongAction.Nothing -> {}
        }
        musicState?.let { newState ->
            if (isForegroundService) {
                //updateNotify()
            }
        }
        return START_NOT_STICKY
    }

    private fun setUpRepository(){
        CoroutineScope(Dispatchers.Main).launch {
            val songs=repository.fetchAllSongs()
            songs.forEach { s->
                if(!songsList.contains(s)){
                    songsList.add(s)
                }
            }
            val songState=repository.fetchSongState()
            if(!songState.isNullOrEmpty())setMusicStateSaved(songState[0])
        }
    }
    private fun nextOrPrevTRack(position:Int){
        if(_songController==null){
            if(songsList.isNotEmpty()) {
                startPlayer(songsList[position])
                mPrefs.currentPosition = position.toLong()
            }
            mPrefs.nextOrPrevFromNotify=true
        }
    }
    // Usando la actualización de la notificación con info de la pista en reproducción desde el servicio mismo
    // nos ayuda a controlar el estado de la notificación cuando el móvil esta en modo de bloqueo
    // y ya no es necesario llamarlo cada vez desde onstartCommand, porque se estará actualizando en el bucle
    // dentro de la función initExoplayer()
    @SuppressLint("ForegroundServiceType")
    private fun updateNotify(){

        currentMusicState?.let { newState ->
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
                mediaSession.setMetadata(
                    MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_TITLE, newState.title)
                        .putString(MediaMetadata.METADATA_KEY_ALBUM, newState.album)
                        .putString(MediaMetadata.METADATA_KEY_ARTIST, newState.artist)
                        .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, newState.albumArt)
                        .putLong(MediaMetadata.METADATA_KEY_DURATION, newState.duration)
                        .build()
                )
                mediaPlayerNotify = notificationMediaPlayer(
                    this,
                    MediaStyle()
                        .setMediaSession(mediaSession.sessionToken)
                        .setShowActionsInCompactView(0, 1, 2),
                   currentMusicState
                )
                startForeground(NOTIFICATION_ID, mediaPlayerNotify).also {
                    isForegroundService = true
                }
                notificationManager.notify(
                    NOTIFICATION_ID,
                    mediaPlayerNotify
                )
            }

    }
    private fun initExoplayer(){
        exoPlayer = ExoPlayer.Builder(applicationContext)
            .build()

            songRunnable = Runnable {
                if(exoPlayer.currentPosition>0) {
                    currentMusicState = currentMusicState.copy(
                        isPlaying = exoPlayer.isPlaying,
                        currentDuration = exoPlayer.currentPosition,
                        duration = exoPlayer.duration,

                    )
                }
                //if(exoPlayer.isPlaying) {
                    _songController?.musicState(currentMusicState)
                    updateNotify()
                //}
                songHandler.postDelayed(songRunnable, 500)
            }
            songHandler.post(songRunnable)

    }
    private fun setUpExoPlayer(song:SongEntity){
        songEntity = song
        val songPath = song.pathLocation.toString()

        // Obtenemos el archivo bitmap de la carátula del albúm y lo reducimos  enviando true como argumento de isForNotify ya que si el bitmap es muy grande superando cierto límite
        // y es enviado directamente dentro de la intención la aplicación se romperá. De esta manera creamos el bitmap de un tamaño
        // de 96 x 96(isForNotify = true) para la notificación mientras que por defecto será de 500 x 500
        val songMetadata= getSongCover(applicationContext!!,songPath, isForNotify = true)

        if(exoPlayer.isPlaying){
            exoPlayer.stop()
            exoPlayer.release()
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

                    // Set info currentSongEntity
                    currentMusicState = MusicState(
                        idSong = song.id,
                        isPlaying=exoPlayer.isPlaying,
                        title = songPath.substringAfterLast("/","No named"),
                        artist = songMetadata!!.artist,
                        album = songMetadata.album,
                        albumArt = songMetadata.albumArt,
                        duration =(exoPlayer.duration),
                        songPath = songPath,
                        latestPlayed = false
                    )


                    // executeOnceTime nos servirá para evitar que el listener de exoplayer vuelva a mandar
                    // información que de la pista en reproducción que no requiere cambios constantes
                    // como la carátula del álbum, título, artista. A diferencia del tiempo transcurrido
                    if(!executeOnceTime)_songController?.currentTrack(currentMusicState)
                    executeOnceTime=true
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
    fun unregisterController(){
        _songController=null
    }
    fun startPlayer(song:SongEntity){
        song.pathLocation?.let {
            if(mPrefs.playerIsStop){songHandler.post(songRunnable)}
            // executeOnceTime nos servirá para evitar que el listener de exoplayer vuelva a mandar
            // información que de la pista en reproducción que no requiere cambios constantes
            // como la carátula del álbum, título, artista. A diferencia del tiempo transcurrido
            executeOnceTime=false
            setUpExoPlayer(song)
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

    private fun setMusicStateSaved(songState: SongStateWithDetail){
        val song=songState.songEntity
        val songPath=song.pathLocation.toString()
        songMetaData= getSongCover(applicationContext!!,songPath, isForNotify = true)!!
        // Set info currentSongEntity
        currentMusicState = MusicState(
                idSong = song.id,
                isPlaying = exoPlayer.isPlaying,
                title = songPath.substringAfterLast("/", "No named"),
                artist = songMetaData.artist,
                album = songMetaData.album,
                albumArt = songMetaData.albumArt,
                duration = songState.songState.songDuration,
                songPath = songPath,
                currentDuration = songState.songState.currentPosition,
                latestPlayed = true
            )
        exoPlayer.addMediaItem(MediaItem.fromUri(songPath))
        exoPlayer.seekTo(songState.songState.currentPosition)
        exoPlayer.prepare()
        _songController?.currentTrack(currentMusicState)
    }
    override fun onDestroy() {
        isForegroundService = false
        _songController?.stop()
        mediaSession.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onDestroy()
    }
    // TODO
    // Si queremos mantener las notificaciones una vez cerrada la aplicación
    // no debemos sobreescribir onTaskRemoved - volveremos más tarde para implementar algo con eso
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        cancelPersistentNotify(applicationContext)
        exitProcess(0)
    }
    inner class MusicPlayerServiceBinder : Binder() {
        fun getService(): MusicPlayerService {
            return this@MusicPlayerService
        }
    }
}