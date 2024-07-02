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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.exoplayer.ExoPlayer
import com.barryzeha.core.common.MUSIC_PLAYER_SESSION
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.common.getSongMetadata
import com.barryzeha.core.model.SongAction
import com.barryzeha.core.model.ServiceSongListener
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.core.model.entities.SongMode
import com.barryzeha.core.model.entities.SongStateWithDetail
import com.barryzeha.data.repository.MainRepository
import com.barryzeha.ktmusicplayer.MyApp
import com.barryzeha.ktmusicplayer.common.NOTIFICATION_ID
import com.barryzeha.ktmusicplayer.common.cancelPersistentNotify
import com.barryzeha.ktmusicplayer.common.notificationMediaPlayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.system.exitProcess


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 16/5/24.
 * Copyright (c)  All rights reserved.
 **/

@AndroidEntryPoint
class MusicPlayerService : Service(){

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

    private var _songController: ServiceSongListener? = null
    val songController: ServiceSongListener get() = _songController!!
    private var isForegroundService = false
    private var currentMusicState = MusicState()
    private var songRunnable: Runnable = Runnable {}
    private var songHandler: Handler = Handler(Looper.getMainLooper())
    private var executeOnceTime:Boolean=false
    private var musicState:MusicState?=null
    private lateinit var mPrefs:MyPreferences
    private var songEntity:SongEntity=SongEntity()
    private lateinit var songMetaData:MusicState
    private var playerListener:Player.Listener?=null
    private var isFirstTime=true
    private var songs:MutableList<SongEntity> = arrayListOf()
    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        mPrefs = MyApp.mPrefs
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mediaSession = MediaSession(this, MUSIC_PLAYER_SESSION)
        mediaStyle = MediaStyle().setMediaSession(mediaSession.sessionToken)
        currentMusicState = MusicState(albumArt = getSongMetadata(applicationContext,null)!!.albumArt)
        mediaSession.setCallback(mediaSessionCallback())
        setUpRepository()
        initMusicStateLooper()

    }

    private fun mediaSessionCallback():MediaSession.Callback{
        return object:MediaSession.Callback(){
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
            override fun onSeekTo(pos: Long) {
                super.onSeekTo(pos)
                exoPlayer.seekTo(pos)
            }

        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        musicState = intent?.getParcelableExtra<MusicState>("musicState")

        when (SongAction.values()[intent?.action?.toInt() ?: SongAction.Nothing.ordinal]) {
            SongAction.Pause -> {
               _songController?.pause()
                exoPlayer.pause()
                if(_songController==null){
                    mPrefs.nextOrPrevFromNotify=true
                    mPrefs.controlFromNotify = true
                    mPrefs.isPlaying = exoPlayer.isPlaying
                }
            }
            SongAction.Resume -> {
                _songController?.play()
                playerListener?.let{exoPlayer.addListener(it)}
                exoPlayer.play()
                if(_songController==null){
                    mPrefs.nextOrPrevFromNotify=true
                    mPrefs.controlFromNotify = true
                    mPrefs.isPlaying = exoPlayer.isPlaying
                }
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
        exoPlayer = ExoPlayer.Builder(applicationContext)
            .build()
        CoroutineScope(Dispatchers.Main).launch {
            val songs=repository.fetchAllSongs()
            val songState=repository.fetchSongState()
            // TODO Revisar, no cargar toda la lista antes del estado de la canción
            withContext(Dispatchers.IO) {
                songs.forEach { s ->
                    if (!songsList.contains(s)) {
                        songsList.add(s)
                    }
                    withContext(Dispatchers.Main) {
                        exoPlayer.addMediaItem(MediaItem.fromUri(s.pathLocation.toString()))
                    }
                }
            }
            if(!songState.isNullOrEmpty()) {
                setMusicStateSaved(songState[0])
            }
            setUpExoplayerListener()
            playerListener?.let{listener->exoPlayer.addListener(listener)}
        }
    }
    private fun nextOrPrevTRack(position:Int){
        if(_songController==null){
            if(songsList.isNotEmpty()) {
                startPlayer(songsList[position],position)
                mPrefs.currentPosition = position.toLong()
            }
            mPrefs.nextOrPrevFromNotify=true
            mPrefs.controlFromNotify = true
            mPrefs.isPlaying = exoPlayer.isPlaying
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
                        .setActions(PlaybackState.ACTION_SEEK_TO)
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
    private fun initMusicStateLooper(){
            songRunnable = Runnable {
                if(exoPlayer.currentPosition>0) {
                    currentMusicState = currentMusicState.copy(
                        isPlaying = exoPlayer.isPlaying,
                        currentDuration = exoPlayer.currentPosition,
                        duration = exoPlayer.duration,
                        latestPlayed = false
                    )
                }
                //if(exoPlayer.isPlaying) {
                    _songController?.musicState(currentMusicState)
                    updateNotify()
                //}
                setUpExoPlayerRepeatMode()

               songHandler.postDelayed(songRunnable, 500)
            }
            songHandler.post(songRunnable)

    }
    private fun setUpExoPlayerRepeatMode(){
        when(mPrefs.songMode){
            SongMode.RepeatAll.ordinal->{

                exoPlayer.repeatMode=Player.REPEAT_MODE_ALL
            }
            SongMode.RepeatOne.ordinal->{
                exoPlayer.repeatMode=Player.REPEAT_MODE_ONE

            }
            SongMode.Shuffle.ordinal->{
                exoPlayer.shuffleModeEnabled = true
            }
            else->{
                exoPlayer.repeatMode= REPEAT_MODE_OFF
                exoPlayer.shuffleModeEnabled = false
            }

        }
    }
    private fun initExoPlayer(song:SongEntity,position:Int){

        songEntity=song
        exoPlayer.seekTo(position,0)
        exoPlayer.prepare()
        exoPlayer.play()

    }
    private fun setUpExoplayerListener():Player.Listener?{

         playerListener = object : Player.Listener {
             override fun onPlaybackStateChanged(playbackState: Int) {
                 super.onPlaybackStateChanged(playbackState)
                 if (playbackState == Player.STATE_READY && exoPlayer.duration != C.TIME_UNSET) {
                     val songPath = songEntity.pathLocation.toString()
                     val songMetadata= getSongMetadata(applicationContext!!,songPath, isForNotify = true)
                     // Set info currentSongEntity
                         fetchSong(songEntity)?.let{currentMusicState=it}
                         // executeOnceTime nos servirá para evitar que el listener de exoplayer vuelva a mandar
                         // información que de la pista en reproducción que no requiere cambios constantes
                         // como la carátula del álbum, título, artista. A diferencia del tiempo transcurrido
                         if (!executeOnceTime) _songController?.currentTrack(currentMusicState)
                         executeOnceTime = true
                         mPrefs.isPlaying = exoPlayer.isPlaying


                 }
                 if(playbackState == Player.STATE_ENDED  && _songController==null){
                     if(mPrefs.currentPosition < songsList.size -1 ){
                         nextOrPrevTRack((mPrefs.currentPosition + 1).toInt())

                     }
                 }
                 else if (playbackState == Player.STATE_ENDED) {
                     currentMusicState = currentMusicState.copy(
                         isPlaying = false,
                         latestPlayed = false
                     )
                     _songController?.currentTrack(currentMusicState)

                 }

             }
             override fun onPositionDiscontinuity(
                 oldPosition: Player.PositionInfo,
                 newPosition: Player.PositionInfo,
                 reason: Int
             ) {
                 super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                    if(oldPosition.mediaItemIndex != newPosition.mediaItemIndex) {
                        if (songsList.isNotEmpty()) {
                            val song=songsList[newPosition.mediaItemIndex]
                            fetchSong(song)?.let {
                                currentMusicState = it.copy(
                                    currentPosition = newPosition.mediaItemIndex.toLong()
                                )
                                _songController?.currentTrack(currentMusicState)
                                mPrefs.currentPosition = newPosition.mediaItemIndex.toLong()
                                if(_songController == null){
                                    mPrefs.controlFromNotify = true
                                    mPrefs.nextOrPrevFromNotify = true
                                    mPrefs.isPlaying = exoPlayer.isPlaying
                                }
                            }
                        }
                    }

             }

         }

       return playerListener

    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    fun setActivity(activity:AppCompatActivity){
        this._activity=activity

    }
    fun setSongController(controller:ServiceSongListener){
        _songController=controller

    }
    fun setNewMediaItem(song:SongEntity){
        exoPlayer.addMediaItem(MediaItem.fromUri(song.pathLocation.toString()))
        if(!songsList.contains(song)) songsList.add(song)
    }
    fun removeMediaItem(song: SongEntity){
        if(songsList.contains(song)) {
            val index = songsList.indexOf(song)
            exoPlayer.removeMediaItem(index)
            songsList.remove(song)
        }
    }
    fun unregisterController(){
        _songController=null
    }
    fun startPlayer(song:SongEntity, position:Int){
        song.pathLocation?.let {
            if(mPrefs.isPlaying){songHandler.post(songRunnable)}
            // executeOnceTime nos servirá para evitar que el listener de exoplayer vuelva a mandar
            // información  de la pista en reproducción que no requiere cambios constantes
            // como la carátula del álbum, título, artista. A diferencia del tiempo transcurrido
            executeOnceTime=false
            initExoPlayer(song,position)
        }

    }
    fun pauseExoPlayer(){
        if(exoPlayer.isPlaying){
            exoPlayer.pause()

        }
    }
    fun playingExoPlayer(){
        if(!exoPlayer.isPlaying){
            exoPlayer.prepare()
            exoPlayer.play()
            isFirstTime=false
        }

    }
    fun setExoPlayerProgress(progress:Long){
        exoPlayer.seekTo(progress)
    }
    fun setMusicList(){
        songs.forEach { s ->
            if (!songsList.contains(s)) {
                songsList.add(s)
                exoPlayer.addMediaItem(MediaItem.fromUri(s.pathLocation.toString()))
            }

        }
    }
    private fun setMusicStateSaved(songState: SongStateWithDetail){

        val song=songState.songEntity
        songEntity = song
        // Set info currentSongEntity
        fetchSong(song)?.let{musicState->
            currentMusicState = musicState.copy(
                currentDuration = songState.songState.currentPosition,
                latestPlayed = true
            )

        }

        // Al agregar todos los items de la lista al inicio, no necesitamos agregar uno nuevo,
        // lo necesitamos para la repetición de toda la lista
        //exoPlayer.addMediaItem(MediaItem.fromUri(songPath))

        // Cuando tenemos toda la lista de items desde el inicio, siempre comienza por el primer archivo de la lista
        // entonces para iniciar por el item de una posición específica usamos lo siguiente:
        //exoPlayer.seekToDefaultPosition(mPrefs.currentPosition.toInt())
        //exoPlayer.addMediaItem(MediaItem.fromUri(songPath))
        exoPlayer.seekTo(mPrefs.currentPosition.toInt(),songState.songState.currentPosition)
        exoPlayer.prepare()
        exoPlayer.playWhenReady=false
        _songController?.currentTrack(currentMusicState)


    }
    private fun fetchSong(song:SongEntity):MusicState?{
        try {
        val songPath = song.pathLocation.toString()
        val songMetadata = getSongMetadata(applicationContext!!, songPath, isForNotify = true)!!
        return MusicState(
            idSong = song.id,
            isPlaying = exoPlayer.isPlaying,
            title = songMetadata.title,
            artist = songMetadata.artist,
            album = songMetadata.album,
            albumArt = songMetadata.albumArt,
            duration = songMetadata.duration,
            songPath = songPath,
            latestPlayed = false
        )
        }catch(ex:Exception) {
            return null
        }
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