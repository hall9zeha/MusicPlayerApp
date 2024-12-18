package com.barryzeha.ktmusicplayer.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Notification.MediaStyle
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.barryzeha.audioeffects.common.EffectsPreferences
import com.barryzeha.audioeffects.common.EqualizerManager
import com.barryzeha.audioeffects.common.getEqualizerConfig
import com.barryzeha.core.common.ACTION_CLOSE
import com.barryzeha.core.common.MUSIC_PLAYER_SESSION
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.common.getSongMetadata
import com.barryzeha.core.model.ServiceSongListener
import com.barryzeha.core.model.SongAction
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
    @Inject
    lateinit var effectsPrefs:EffectsPreferences

    private var songsList: MutableList<SongEntity> = mutableListOf()

    private lateinit var mediaSession: MediaSession
    private lateinit var mediaStyle: MediaStyle
    private lateinit var notificationManager: NotificationManager
    private lateinit var mediaPlayerNotify:Notification
    private var playBackState:PlaybackState? = null
    private var mediaMetadata:MediaMetadata? = null

    private val binder: Binder = MusicPlayerServiceBinder()
    private var _activity:AppCompatActivity?= null
    private lateinit var exoPlayer:ExoPlayer
    private var positionReset= -1

    private var _songController: ServiceSongListener? = null
    val songController: ServiceSongListener get() = _songController!!
    private var isForegroundService = false
    private var currentMusicState = MusicState()
    val _currentMusicState:MusicState get() = currentMusicState

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
    private var mediaItemList:MutableList<MediaItem> = arrayListOf()
    private var songState:List<SongStateWithDetail> = arrayListOf()
    private var headsetReceiver:BroadcastReceiver?=null
    private var bluetoothReceiver:BroadcastReceiver?=null
    private var bluetoothIsConnect:Boolean = false
    // Para comparar el cambio de canción y enviar la metadata a la notificación multimedia
    private var idSong:Long=-1

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        mPrefs = MyApp.mPrefs
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mediaSession = MediaSession(this, MUSIC_PLAYER_SESSION)
        mPrefs.firstExecution=true

        mediaStyle = MediaStyle().setMediaSession(mediaSession.sessionToken)
        currentMusicState = MusicState(albumArt = getSongMetadata(applicationContext,null)!!.albumArt)
        mediaSession.setCallback(mediaSessionCallback())
        setUpRepository()
        initMusicStateLooper()
        setUpHeadsetAndBluetoothReceiver()


    }
    private fun setUpHeadsetAndBluetoothReceiver(){
        headsetReceiver = object:BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                if(action != null && action == Intent.ACTION_HEADSET_PLUG){
                    val state = intent.getIntExtra("state",-1)
                    if(state==0){
                        if(exoPlayer.isPlaying) {
                            exoPlayer.pause()
                            _songController?.let{
                                _songController?.pause()
                                _songController?.musicState(currentMusicState.copy(isPlaying = exoPlayer.isPlaying))
                            }?:run{
                                mPrefs.nextOrPrevFromNotify = true
                                mPrefs.controlFromNotify = true
                            }

                        }
                        Log.e("HEADSET_STATE","disconnect")

                    }else if(state == 1){
                        Log.e("HEADSET_STATE","connect")
                    }
                }
            }
        }
        bluetoothReceiver = object:BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {

                val action = intent?.action

                if (action != null) {
                    when (action) {
                        BluetoothDevice.ACTION_ACL_CONNECTED -> {
                            bluetoothIsConnect = true
                            Log.d("BLUETOOTH_STATE", "Bluetooth connected")

                            // Aquí puedes agregar la lógica cuando se conecta el Bluetooth
                        }
                        BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                                _songController?.let{
                                    _songController?.pause()
                                    _songController?.musicState(currentMusicState.copy(isPlaying = false))
                                }?:run{
                                    mPrefs.nextOrPrevFromNotify = true
                                    mPrefs.controlFromNotify = true
                                }

                            }
                        }
                        BluetoothAdapter.ACTION_STATE_CHANGED -> {
                            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                            when (state) {
                                BluetoothAdapter.STATE_OFF -> {
                                    Log.d("BLUETOOTH_STATE", "Bluetooth disconnected")
                                    // Aquí puedes agregar la lógica cuando se desconecta el Bluetooth
                                    if (exoPlayer.isPlaying) {
                                        exoPlayer.pause()
                                        _songController?.let{
                                            _songController?.pause()
                                            _songController?.musicState(currentMusicState.copy(isPlaying = false))
                                        }?:run{
                                            mPrefs.nextOrPrevFromNotify = true
                                            mPrefs.controlFromNotify = true
                                        }

                                    }
                                }
                                BluetoothAdapter.STATE_ON -> {
                                    Log.d("BluetoothReceiver", "Bluetooth adapter turned on")
                                    // Aquí puedes agregar lógica adicional cuando el adaptador Bluetooth se enciende
                                }
                            }
                        }
                    }
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
        registerReceiver(headsetReceiver,filter)

        val bluetoothFilter = IntentFilter().apply {

            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }

        registerReceiver(bluetoothReceiver,bluetoothFilter)
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

            override fun onPause() {
                super.onPause()
                _songController?.pause()
                exoPlayer.pause()
                if(_songController==null){
                    mPrefs.nextOrPrevFromNotify=true
                    mPrefs.controlFromNotify = true
                    mPrefs.isPlaying = exoPlayer.isPlaying
                }
            }

            override fun onPlay() {
                super.onPlay()
                _songController?.play()
                playerListener?.let{exoPlayer.addListener(it)}
                exoPlayer.play()
                if(_songController==null){
                    mPrefs.nextOrPrevFromNotify=true
                    mPrefs.controlFromNotify = true
                    mPrefs.isPlaying = exoPlayer.isPlaying
                }
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                _songController?.next()
                nextOrPrevTRack(mPrefs.currentIndexSong.toInt() +1)
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                _songController?.previous()
                nextOrPrevTRack(mPrefs.currentIndexSong.toInt() -1)
            }

            override fun onStop() {
                super.onStop()
            }

            override fun onCustomAction(action: String, extras: Bundle?) {
                if(ACTION_CLOSE == action){
                    exoPlayer.stop()
                    exoPlayer.release()
                    songHandler.removeCallbacks(songRunnable)

                    // Remove notification of foreground service process
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    _songController?.stop()
                    // Close application
                    _activity?.finish()
                    exitProcess(0)
                }
            }

        }
    }
    private fun setUpEqualizer(sessionId:Int){
        EqualizerManager.initEqualizer(sessionId)
        EqualizerManager.setEnabled(true)
        if(effectsPrefs.effectsIsEnabled){
            CoroutineScope(Dispatchers.IO).launch {
                val listOfBands = getEqualizerConfig(
                    effectsPrefs.effectType,
                    EqualizerManager.mEq!!.numberOfBands.toInt(),
                    effectsPrefs
                )
                listOfBands.forEachIndexed { index, value ->
                    EqualizerManager.setBand(index.toShort(),value)

                }
            }
        }
        else{
            EqualizerManager.setEnabled(false)

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
                nextOrPrevTRack(mPrefs.currentIndexSong.toInt() +1)
            }
            SongAction.Previous -> {
                _songController?.previous()
               nextOrPrevTRack(mPrefs.currentIndexSong.toInt() -1)
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

        return START_NOT_STICKY
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun setUpRepository(){
        exoPlayer = ExoPlayer.Builder(applicationContext)
            .build()
        setUpEqualizer(exoPlayer.audioSessionId)

        CoroutineScope(Dispatchers.Main).launch {
            // Para cargar por primera vez la lista de canciones de acuerdo al filtro guardado
            // si no hay algo seleccionado previamente solo devolverá la lista por defecto
            //val songs=repository.fetchAllSongsBy(mPrefs.playListSortOption)
            val songs=repository.fetchPlaylistOrderBy(mPrefs.playlistId.toLong(), mPrefs.playListSortOption)

            songState=repository.fetchSongState()
            // TODO Revisar, no cargar toda la lista antes del estado de la canción
            withContext(Dispatchers.IO) {
                songs.forEach { s ->
                    if (!songsList.contains(s)) {
                        songsList.add(s)
                    }
                    val mediaItem = MediaItem.Builder()
                        .setMediaId(s.id.toString())
                        .setUri(s.pathLocation.toString())
                        .build()
                    mediaItemList.add(mediaItem)

                }
                withContext(Dispatchers.Main) {
                    exoPlayer.addMediaItems(mediaItemList)
                    //exoPlayer.addMediaItem(mediaItem)
                }
                Log.e("ITEMS-MEDIA-S-POPULATE", mediaItemList.size.toString())
            }
            if(!songState.isNullOrEmpty()) {
                val songEntity=songState[0].songEntity
                if(songsList.contains(songEntity))setSongStateSaved(songState[0])

            }
            setUpExoplayerListener()
            playerListener?.let{listener->exoPlayer.addListener(listener)}
        }
    }
    private fun nextOrPrevTRack(position:Int){
        if(_songController==null){
            if(songsList.isNotEmpty()) {
                startPlayer(songsList[position])
                mPrefs.currentIndexSong = position.toLong()
            }
            mPrefs.nextOrPrevFromNotify=true
            mPrefs.controlFromNotify = true
            mPrefs.isPlaying = exoPlayer.isPlaying
        }
    }
    private fun initNotify(){
        currentMusicState?.let { newState ->
            idSong = newState.idSong
            playBackState = PlaybackState.Builder()
                .setState(
                    if (newState.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                    newState.currentDuration,
                    1f
                )
                // Los siguiente controles aparecerán en android 13 y 14
                .setActions(
                    PlaybackState.ACTION_SEEK_TO
                            or PlaybackState.ACTION_PLAY
                            or PlaybackState.ACTION_PAUSE
                            or PlaybackState.ACTION_SKIP_TO_NEXT
                            or PlaybackState.ACTION_SKIP_TO_PREVIOUS
                            or PlaybackState.ACTION_STOP
                )
                .addCustomAction(
                    PlaybackState.CustomAction.Builder(
                        ACTION_CLOSE,
                        ACTION_CLOSE,
                        com.barryzeha.core.R.drawable.ic_close
                    ).build()
                )
                .build()
            mediaMetadata = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, newState.title)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, newState.album)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, newState.artist)
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, newState.albumArt)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, newState.duration)
                .build()

            mediaSession.setPlaybackState(playBackState)
            mediaSession.setMetadata(mediaMetadata)

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
    // Usando la actualización de la notificación con info de la pista en reproducción desde el servicio mismo
    // nos ayuda a controlar el estado de la notificación cuando el móvil esta en modo de bloqueo
    // y ya no es necesario llamarlo cada vez desde onstartCommand, porque se estará actualizando en el bucle
    // dentro de la función initExoplayer()
    @SuppressLint("ForegroundServiceType")
    private fun updateNotify(){
        currentMusicState?.let { newState ->
            val updatePlaybackState = playBackState?.let{
                PlaybackState.Builder(it)
                    .setState(if (newState.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                        newState.currentDuration,
                        1f)
                    .build()
            }
            mediaSession.setPlaybackState(updatePlaybackState)
            if(idSong != newState.idSong) {// Comparamos los ids para saber si ha cambiado la canción
                val updateMediaMetadata = MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, newState.title)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, newState.album)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, newState.artist)
                    .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, newState.albumArt)
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, newState.duration)
                    .build()

                // Para android >=12
                mediaSession.setMetadata(updateMediaMetadata)
                // Reemplazamos temporalmente el nuevo id para la comparación
                idSong = newState.idSong

            // Para android <=10
            if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q){
                mediaPlayerNotify = notificationMediaPlayer(
                    this,
                    MediaStyle()
                        .setMediaSession(mediaSession.sessionToken)
                        .setShowActionsInCompactView(0, 1, 2),
                    currentMusicState
                )
            }
            notificationManager.notify(
                NOTIFICATION_ID,
                mediaPlayerNotify
            )
           }
        }
    }
    private fun initMusicStateLooper(){
        initNotify()
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
    private fun stopLooper(){
        songHandler.removeCallbacks(songRunnable)
        initNotify()
    }
    private fun startLooper(){
        songHandler.post(songRunnable)
    }
    fun updateNotify(musicState:MusicState){
        stopLooper()
        currentMusicState = musicState
        initNotify()
        startLooper()
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
    private fun findMediaItemIndexById(mediaItems:List<MediaItem>, mediaItemId:String):Int{
        return mediaItems.indexOfFirst { it.mediaId == mediaItemId }
    }

    private fun initExoPlayer(song:SongEntity){
        songEntity=song
        exoPlayer.seekTo(findMediaItemIndexById(mediaItemList,song.id.toString()),0)

        exoPlayer.prepare()
        exoPlayer.play()

    }

    private fun setUpExoplayerListener():Player.Listener?{
         playerListener = object : Player.Listener {
             override fun onPlaybackStateChanged(playbackState: Int) {
                 super.onPlaybackStateChanged(playbackState)
                 if (playbackState == Player.STATE_READY && exoPlayer.duration != C.TIME_UNSET) {
                        val song=if(positionReset>-1) songsList[positionReset] else songEntity
                         // Set info currentSongEntity
                         fetchSong(song)?.let{
                             currentMusicState=it
                             // Para encontrar la posición del item en la lista de nuestra vista
                             // por su id
                             mPrefs.idSong=it.idSong
                         }
                         // executeOnceTime nos servirá para evitar que el listener de exoplayer vuelva a mandar
                         // información que de la pista en reproducción que no requiere cambios constantes
                         // como la carátula del álbum, título, artista. A diferencia del tiempo transcurrido
                         if (!executeOnceTime) {
                             _songController?.currentTrack(currentMusicState)

                         }
                         executeOnceTime = true
                         mPrefs.isPlaying = exoPlayer.isPlaying
                     positionReset=-1

                 }
                 if(playbackState == Player.STATE_ENDED  && _songController==null){
                     if(mPrefs.currentIndexSong < songsList.size -1 ){
                         nextOrPrevTRack((mPrefs.currentIndexSong + 1).toInt())

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


                            songEntity = song
                            fetchSong(song)?.let {songInfo->
                                currentMusicState = songInfo.copy(
                                    isPlaying = exoPlayer.isPlaying,
                                    currentPosition = newPosition.mediaItemIndex.toLong(),

                                )
                                _songController?.currentTrack(currentMusicState)
                                // Para encontrar la posición del item en la lista de nuestra vista
                                // por su id
                                mPrefs.idSong = song.id
                                mPrefs.currentIndexSong = newPosition.mediaItemIndex.toLong()
                                if(_songController == null){
                                    mPrefs.controlFromNotify = true
                                    mPrefs.nextOrPrevFromNotify = true
                                    mPrefs.isPlaying = exoPlayer.isPlaying
                                    mPrefs.idSong = song.id

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
       if(!songsList.contains(song)){ songsList.add(song)
            val newMediaItem = MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.pathLocation.toString())
                .build()
            mediaItemList.add(newMediaItem)
            exoPlayer.addMediaItem(MediaItem.fromUri(song.pathLocation.toString()))
        }
    }
    fun removeMediaItem(song: SongEntity){

        val mediaItemIndex = findMediaItemIndexById(mediaItemList,song.id.toString())

        mediaItemList.removeAt(mediaItemIndex)
        if(songsList.contains(song)) {
            val index = songsList.indexOf(song)
            exoPlayer.removeMediaItem(index)
            songsList.remove(song)
        }
    }
    fun removeMediaItems(list: List<SongEntity>){
       list.forEach { song->
            removeMediaItem(song)
        }
    }
    fun populatePlayList(songs:List<SongEntity>){
        CoroutineScope(Dispatchers.IO).launch {
            songs.forEach { s ->
                if (!songsList.contains(s)) {
                    songsList.add(s)

                    val mediaItem = MediaItem.Builder()
                        .setMediaId(s.id.toString())
                        .setUri(s.pathLocation.toString())
                        .build()
                    mediaItemList.add(mediaItem)

                    withContext(Dispatchers.Main) {
                        exoPlayer.addMediaItem(mediaItem)

                    }
                }
            }
        }
    }
    // Ya que el fragmento de lista no es diferente para cada versión, en bass flavor se implemnta clearPlayList
    // con el parámetro (isSort), pero en exoplayer flavor no, aún así debemos ponerlo porque el fragmento de lista
    // lo usa así tanto para bass como exoplayer.
    fun clearPlayList(isSort:Boolean=false){
        mediaItemList.clear()
        songsList.clear()
        exoPlayer.clearMediaItems()
        //exoPlayer.release()
        currentMusicState=MusicState()
        _songController?.currentTrack(currentMusicState)
        positionReset=0
        executeOnceTime=false
    }
    fun unregisterController(){
        _songController=null
    }
    fun startPlayer(song:SongEntity){
        song.pathLocation?.let {
            if(mPrefs.isPlaying){songHandler.post(songRunnable)}
            // executeOnceTime nos servirá para evitar que el listener de exoplayer vuelva a mandar
            // información  de la pista en reproducción que no requiere cambios constantes
            // como la carátula del álbum, título, artista. A diferencia del tiempo transcurrido
            executeOnceTime=false
            initExoPlayer(song)
        }

    }
    fun pausePlayer(){
        if(exoPlayer.isPlaying){
            exoPlayer.pause()


        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    fun getSessionOrChannelId(): Int {
        return exoPlayer.audioSessionId
    }
    fun getNumberOfTrack():Pair<Int,Int>{
        if(songsList.isNotEmpty()) {
            val indexOfSong = songsList.indexOf(songEntity)
            return Pair((indexOfSong + 1),songsList.size)
        }
        return Pair(0,0)
    }
    fun resumePlayer(){
        if(!exoPlayer.isPlaying){
            exoPlayer.prepare()
            exoPlayer.play()
            isFirstTime=false
        }

    }
    fun nextSong(){
        exoPlayer.seekToNext()
    }
    fun prevSong(){
        exoPlayer.seekToPrevious()
            //exoPlayer.seekToPrevious()
            // retrocede al principio de la pista hay que hacer click dos veces
            // para que retroceda a la pista anterior
    }

    fun fastForward(){
        val currentPosition = exoPlayer.currentPosition
        val newPosition = (currentPosition + 2000).coerceAtMost(exoPlayer.duration)
        exoPlayer.seekTo(newPosition)
    }
    fun fastRewind(){
        val currentPosition = exoPlayer.currentPosition
        val newPosition = (currentPosition - 2000).coerceAtLeast(0)
        exoPlayer.seekTo(newPosition)
    }
    // Solo en bass flavour
    fun shuffleList(songList: MutableList<SongEntity> = arrayListOf()){

    }
    // Solo en bass flavour Reordenamos la lista nuevamente a su forma original
    fun sortList(){}

    fun getSongsList():List<SongEntity>{
        return songsList
    }
    fun setPlayerProgress(progress:Long){
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
    private fun setSongStateSaved(songState: SongStateWithDetail){

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

        //exoPlayer.seekTo(mPrefs.currentPosition.toInt(),songState.songState.currentPosition)
        exoPlayer.seekTo(findMediaItemIndexById(mediaItemList,song.id.toString()),songState.songState.currentPosition)

        exoPlayer.prepare()
        exoPlayer.playWhenReady=false
        _songController?.currentTrack(currentMusicState)
        // Al cargar la información de una pista guardada
        // se ejecutaba una primera vez el evento currentTRack de la interface
        // ya que el listener la ejecutaba una vez más debemos poner executeOnceTime = true
        // para evitarlo
        executeOnceTime = true
        mPrefs.isPlaying = false
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
        unregisterReceiver(headsetReceiver)
        unregisterReceiver(bluetoothReceiver)
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
        stopSelf()
        exitProcess(0)
    }
    inner class MusicPlayerServiceBinder : Binder() {
        fun getService(): MusicPlayerService {
            return this@MusicPlayerService
        }
    }
}