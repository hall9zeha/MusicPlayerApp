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
import android.telephony.PhoneStateListener
import android.telephony.PhoneStateListener.LISTEN_CALL_STATE
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.annotation.OptIn
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
import com.barryzeha.core.R
import com.barryzeha.core.common.AB_LOOP
import com.barryzeha.core.common.ACTION_CLOSE
import com.barryzeha.core.common.ACTION_FAVORITE
import com.barryzeha.core.common.CLEAR_MODE
import com.barryzeha.core.common.MUSIC_PLAYER_SESSION
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.common.fetchShortFileMetadata
import com.barryzeha.core.common.getBitmap
import com.barryzeha.core.common.getSongMetadata
import com.barryzeha.core.model.ServiceSongListener
import com.barryzeha.core.model.SongAction
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.core.model.entities.SongMode
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
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
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
    private val serviceScope = CoroutineScope(Job() + Main)
    private var songRunnable: Runnable = Runnable {}
    private var songHandler: Handler = Handler(Looper.getMainLooper())
    private var musicState:MusicState?=null
    private lateinit var mPrefs:MyPreferences
    private var songEntity:SongEntity=SongEntity()
    private lateinit var songMetaData:MusicState
    private var playerListener:Player.Listener?=null
    private var songs:MutableList<SongEntity> = arrayListOf()
    private var mediaItemList:MutableList<MediaItem> = arrayListOf()
    private var songState:List<SongStateWithDetail> = arrayListOf()
    private var headsetReceiver:BroadcastReceiver?=null
    private var bluetoothReceiver:BroadcastReceiver?=null
    private var bluetoothIsConnect:Boolean = false
    // Para comparar el cambio de canción y enviar la metadata a la notificación multimedia
    private var idSong:Long=-1
    private var firstCallingToSongState:Boolean = true
    // For A-B looper
    private var startAbLoopPosition:Long=0
    private var endAbLopPosition:Long=0
    val handler = Handler(Looper.getMainLooper())
    var runnable: Runnable? = null

    // Thelephony manager para controlar la reproducción en las llamadas
    private var phoneCallStateReceiver:BroadcastReceiver?=null
    private var telephonyManager: TelephonyManager?=null
    private var isPlayingBeforeCallPhone:Boolean = false
    // En esta lista cargamos momentaneamente las las canciones del fragmento AlbumDetail
    private var playingQueue:MutableList<SongEntity> = mutableListOf()
    private var mediaItemQueue:MutableList<MediaItem> = arrayListOf()
    private var playingQueueList:Boolean = false
    private var finalizedPopulatePlaylist=false

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        mPrefs = MyApp.mPrefs
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mediaSession = MediaSession(this, MUSIC_PLAYER_SESSION)
        mPrefs.isPopulateServicePlaylist=false

        mediaStyle = MediaStyle().setMediaSession(mediaSession.sessionToken)
        currentMusicState = MusicState(albumArt = getSongMetadata(applicationContext,null)!!.albumArt)
        mediaSession.setCallback(mediaSessionCallback())
        setUpPlaylist()
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

    fun setupPhoneCallStateReceiver(){
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        phoneCallStateReceiver = object: BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.S){
                    telephonyManager?.registerTelephonyCallback(context?.mainExecutor!!,object:
                        TelephonyCallback(), TelephonyCallback.CallStateListener{
                        override fun onCallStateChanged(state: Int) {
                            when (state) {
                                TelephonyManager.CALL_STATE_IDLE -> {
                                    if(!playingState() && isPlayingBeforeCallPhone){
                                        if(checkIfPhoneIsLock())resumePlayer()
                                        else _songController?.play()
                                        isPlayingBeforeCallPhone = false
                                    }
                                }
                                TelephonyManager.CALL_STATE_OFFHOOK -> {
                                }
                                TelephonyManager.CALL_STATE_RINGING -> {
                                    if(playingState()){
                                        if(checkIfPhoneIsLock())pausePlayer()
                                        else _songController?.pause()
                                        isPlayingBeforeCallPhone = true

                                    }
                                }
                            }
                        }
                    })
                }else {
                    val phoneStateListener = object : PhoneStateListener() {
                        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                            super.onCallStateChanged(state, phoneNumber)
                            when (state) {
                                TelephonyManager.CALL_STATE_IDLE -> {
                                    if(!playingState() && isPlayingBeforeCallPhone){
                                        if(checkIfPhoneIsLock())resumePlayer()
                                        else _songController?.play()
                                        isPlayingBeforeCallPhone = false
                                    }
                                }
                                TelephonyManager.CALL_STATE_OFFHOOK -> Log.d("PHONE_MANAGER","OFF-HOOK")
                                TelephonyManager.CALL_STATE_RINGING -> {
                                    if(playingState()){
                                        if(checkIfPhoneIsLock())pausePlayer()
                                        else _songController?.pause()
                                        isPlayingBeforeCallPhone = true
                                    }
                                }
                            }
                        }
                    }
                    telephonyManager?.listen(phoneStateListener, LISTEN_CALL_STATE)
                }
            }
        }
        registerReceiver(phoneCallStateReceiver, IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED))
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
                    setPlayingState(exoPlayer.isPlaying)
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
                    setPlayingState(exoPlayer.isPlaying)
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
                if(ACTION_FAVORITE == action){
                    serviceScope.launch(Dispatchers.IO) {
                        repository.updateFavoriteSong(!currentMusicState.isFavorite,currentMusicState.idSong)
                        currentMusicState = currentMusicState.copy(isFavorite = !currentMusicState.isFavorite)
                        // Recreamos la notificación para que el ícono de nuestro estado de favoritos cambie
                        initNotify()
                    }
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
        else{ EqualizerManager.setEnabled(false)}
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
                    setPlayingState(exoPlayer.isPlaying)
                }
            }
            SongAction.Resume -> {
                _songController?.play()
                playerListener?.let{exoPlayer.addListener(it)}
                exoPlayer.play()
                if(_songController==null){
                    mPrefs.nextOrPrevFromNotify=true
                    mPrefs.controlFromNotify = true
                    setPlayingState(exoPlayer.isPlaying)
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
            SongAction.Favorite->{
                serviceScope.launch(Dispatchers.IO) {
                repository.updateFavoriteSong(!currentMusicState.isFavorite,currentMusicState.idSong)
                currentMusicState = currentMusicState.copy(isFavorite = !currentMusicState.isFavorite)
                // Recreamos la notificación para que el ícono de nuestro estado de favoritos cambie
                initNotify()
                }
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
    private fun setUpPlaylist(){
        exoPlayer = ExoPlayer.Builder(applicationContext)
            .build()
        setUpEqualizer(exoPlayer.audioSessionId)
        serviceScope.launch{
            // Para cargar por primera vez la lista de canciones de acuerdo al filtro guardado
            // si no hay algo seleccionado previamente solo devolverá la lista por defecto
            //val songs=repository.fetchAllSongsBy(mPrefs.playListSortOption)
            val songs=repository.fetchPlaylistOrderBy(mPrefs.playlistId.toLong(), mPrefs.playListSortOption)
            // TODO Revisar, no cargar toda la lista antes del estado de la canción
            withContext(Dispatchers.IO) {
                songs.forEach { s ->
                    songsList.add(s)
                    mediaItemList.add(convertToMediaItem(s))
                }
                withContext(Main) {
                    exoPlayer.addMediaItems(mediaItemList)
                }

            }
            setUpExoplayerListener()
            playerListener?.let{listener->exoPlayer.addListener(listener)}
        }
    }
    private fun convertToMediaItem(song:SongEntity):MediaItem{
        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.pathLocation.toString())
            .build()
    }
    private fun nextOrPrevTRack(position:Int){
        if(_songController==null){
            if(songsList.isNotEmpty()) {
                startPlayer(songsList[position])
                mPrefs.currentIndexSong = position.toLong()
            }
            mPrefs.nextOrPrevFromNotify=true
            mPrefs.controlFromNotify = true
            setPlayingState(exoPlayer.isPlaying)
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
                        ACTION_FAVORITE,
                        ACTION_FAVORITE,
                        if(newState.isFavorite) R.drawable.ic_favorite_fill else R.drawable.ic_favorite
                    ).build()
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
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, getBitmap(this,newState.songPath, isForNotify = true))
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
                    .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, getBitmap(this,newState.songPath, isForNotify = true))
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
    private fun updateNotifyForLegacySdkVersions(){
        // Para android <=10
        currentMusicState?.let { newState ->
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                val updatePlaybackState = playBackState?.let {
                    PlaybackState.Builder(it)
                        .setState(
                            if (playingState()) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                            currentMusicState.currentDuration,
                            1f
                        )
                        .build()
                }
                // Actualizamos el progreso y estado de reproducción de la canción
                mediaSession.setPlaybackState(updatePlaybackState)
                mediaPlayerNotify = notificationMediaPlayer(
                    this,
                    MediaStyle()
                        .setMediaSession(mediaSession.sessionToken)
                        .setShowActionsInCompactView(0, 1, 2),
                    currentMusicState.copy(isPlaying = playingState())
                )
                notificationManager.notify(
                    NOTIFICATION_ID,
                    mediaPlayerNotify
                )
            }
        }
    }
    fun getStateSaved() {
        if(firstCallingToSongState) {
            serviceScope.launch{
                songState = repository.fetchSongState()
                if (!songState.isNullOrEmpty()) setSongStateSaved(songState[0])
            }
        }
        //firstCallingToSongState=false
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
    @Synchronized
    private fun findMediaItemIndexById(mediaItems:List<MediaItem>, mediaItemId:String):Int{
        return mediaItems.indexOfFirst { it.mediaId == mediaItemId }
    }
    @Synchronized
    private fun findSongInSongList(songList:List<SongEntity>, idSong:Long):Int{
        return try{songList.indexOfFirst { it.id == idSong }}catch(e:Exception){0}
    }

    private fun initExoPlayer(song:SongEntity){
        songEntity=song
        exoPlayer.seekTo(findMediaItemIndexById(mediaItemList,song.id.toString()),0)
        exoPlayer.prepare()
        exoPlayer.play()
        setPlayingState(true)

    }
    @OptIn(UnstableApi::class)
    private fun setUpExoplayerListener():Player.Listener?{
         playerListener = object : Player.Listener {
             override fun onPlaybackStateChanged(playbackState: Int) {
                 super.onPlaybackStateChanged(playbackState)
                 if (playbackState == Player.STATE_READY && exoPlayer.duration != C.TIME_UNSET) {
                        val song=if(positionReset>-1) (if(mPrefs.isOpenQueue) playingQueue[positionReset]else songsList[positionReset]) else songEntity
                         // Set info currentSongEntity
                     setPlayingState(exoPlayer.isPlaying)
                     fetchSongMetadata(song)?.let{
                             currentMusicState=it
                             // Para encontrar la posición del item en la lista de nuestra vista
                             // por su id
                             // por ahora lo guardamos en onPositionDiscontinuity
                             //mPrefs.idSong=it.idSong
                         }
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
                            val song=if(mPrefs.isOpenQueue)playingQueue[newPosition.mediaItemIndex]else songsList[newPosition.mediaItemIndex]
                            songEntity = song
                            fetchSongMetadata(song)?.let { songInfo->
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
                                    setPlayingState(exoPlayer.isPlaying)
                                    mPrefs.idSong = song.id
                                }
                            }
                        }
                    }
             }
             override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                 super.onMediaItemTransition(mediaItem, reason)
                 //Si ya no estamos reproduciendo la lista: que puede ser álbum, artista etc
                 // es porque volveremos a la lista principal y habremos limpiado playingQueue
                 // entonces que volveremos a reproducir la pista que estábamos reproduciendo en playingQueue
                 if(playingQueueList) {
                     if (exoPlayer.mediaItemCount == mediaItemList.size) {
                         //TODO reproducir la misma canción en la lista principal que se estaba reproduciendo en la lista de playingQueue
                         exoPlayer.seekTo(
                             findMediaItemIndexById(
                                 mediaItemList,
                                 mPrefs.idSong.toString()
                             ), currentMusicState.currentPosition
                         )
                         playingQueueList=false
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
        serviceScope.launch(Dispatchers.IO) {
           if (songs.size != mediaItemList.count()) {
                songs.forEach { s ->
                    songsList.add(s)
                    val mediaItem = MediaItem.Builder()
                        .setMediaId(s.id.toString())
                        .setUri(s.pathLocation.toString())
                        .build()
                    mediaItemList.add(mediaItem)
                }
                withContext(Main) {
                    exoPlayer.setMediaItems(mediaItemList)
                   findSongInSongList(songsList,mPrefs.idSong).let {index->
                       val song=songsList[index]
                       mPrefs.idSong = song.id
                       setSongStateSaved(
                           SongStateWithDetail(
                               songState = SongState(),
                               songEntity = song
                           )
                       )
                   }
                }
            }
        }
        mPrefs.isPopulateServicePlaylist=false
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
    }
    fun unregisterController(){
        _songController=null
    }
    private fun checkIfPhoneIsLock():Boolean{
        if(_songController==null){
            mPrefs.nextOrPrevFromNotify=true
            mPrefs.controlFromNotify = true
        }
        return _songController==null
    }
    fun playingState():Boolean{
        return mPrefs.isPlaying
    }
    fun currentSongState():MusicState{
        return currentMusicState
    }
    fun playListSize():Int{
        return if(songsList.isNotEmpty()) songsList.size else 0
    }
    private fun setPlayingState(state:Boolean){
        mPrefs.isPlaying=state
    }
    fun checkIfSongIsFavorite(id:Long){
        serviceScope.launch(Dispatchers.IO) {
            fetchSongMetadata(repository.fetchSongById(id))?.let{data->
              currentMusicState = data
              initNotify()
          }
        }
    }
    fun openQueue(songs:List<SongEntity>, startPosition:Int){
        mPrefs.currentIndexSong = startPosition.toLong()
        playingQueue = songs.toMutableList()
        songs.forEach { s->mediaItemQueue.add(convertToMediaItem(s)) }
        exoPlayer.clearMediaItems()
        exoPlayer.addMediaItems(mediaItemQueue)
        startPlayer(songs[startPosition])
        playingQueueList = true
    }
    fun startPlayer(song:SongEntity){
        song.pathLocation?.let {
            if(playingState()){songHandler.post(songRunnable)}
            initExoPlayer(song)
        }

    }
    fun pausePlayer(){
        if(playingState()){
            exoPlayer.pause()
            setPlayingState(exoPlayer.isPlaying)
        }
        updateNotifyForLegacySdkVersions()
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
        if(exoPlayer.mediaItemCount == mediaItemList.size) {
            if (!exoPlayer.isPlaying) {
                exoPlayer.prepare()
                exoPlayer.play()
                setPlayingState(true)
            }
            updateNotifyForLegacySdkVersions()
        }else{
            Toast.makeText(this, "cargando medios...", Toast.LENGTH_SHORT).show()
            setPlayingState(false)
        }
    }
    fun nextSong(){
        setPlayingState(exoPlayer.isPlaying)
        exoPlayer.seekToNextMediaItem()
        clearABLoopOfPreferences()
    }
    fun prevSong(){
        setPlayingState(exoPlayer.isPlaying)
        exoPlayer.seekToPreviousMediaItem()
        clearABLoopOfPreferences()
        //exoPlayer.seekToPrevious()
        // retrocede al principio de la pista hay que hacer click dos veces
        // para que retroceda a la pista anterior

    }
    fun reloadIndexOfSong(){
        // Obtenemos la posición en la lista principal de la pista que hayamos reproducido
        // de cualquier otra lista como AlbumDetail
        exoPlayer.clearMediaItems()
        mediaItemQueue.clear()
        exoPlayer.addMediaItems(mediaItemList)
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
    // A-B looper
    fun setStartPositionForAbLoop() {startAbLoopPosition = exoPlayer.currentPosition}
    fun setEndPositionAbLoop() {
        endAbLopPosition = exoPlayer.currentPosition
        startAbLoop()
    }
    fun stopAbLoop(){
        if(mPrefs.songMode == AB_LOOP) mPrefs.songMode = CLEAR_MODE
        runnable?.let{
            handler.removeCallbacks(it)
        }
    }
    private fun startAbLoop(){
        runnable = Runnable {
            val currentPosition = exoPlayer.currentPosition
            if (currentPosition >= endAbLopPosition) {
                exoPlayer.seekTo(startAbLoopPosition)
            }
            handler.postDelayed(runnable!!, 500)
        }
        // Inicia el ciclo que revisará cada segundo
        handler.postDelayed(runnable!!, 500)
    }
    fun clearABLoopOfPreferences(){
        if(mPrefs.songMode == AB_LOOP) mPrefs.songMode = CLEAR_MODE
        runnable?.let{
            handler.removeCallbacks(it)
        }
    }
    fun getSongsList():List<SongEntity>{
        return songsList
    }
    fun getCurrentSongPosition():Int = mPrefs.currentIndexSong.toInt()?:0
    fun setCurrentSongPosition(position:Int) {mPrefs.currentPosition=position.toLong()}
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
        songEntity=songState.songEntity
        // Set info currentSongEntity
        fetchSongMetadata(songEntity)?.let{ musicState->
            currentMusicState = musicState.copy(
                currentDuration = songState.songState.currentPosition,
                isFavorite = songEntity.favorite,
                latestPlayed = true
            )
        }
        //FIXME arreglar la forma de obtener la posición del mediaitem
        //exoPlayer.seekTo(findMediaItemIndexById(mediaItemList,songEntity.id.toString()),songState.songState.currentPosition)
        exoPlayer.seekTo(mPrefs.currentIndexSong.toInt() - 1 ,songState.songState.currentPosition)
        exoPlayer.prepare()
        exoPlayer.playWhenReady=false
        _songController?.currentTrack(currentMusicState)
        setPlayingState(false)
    }

    private fun fetchSongMetadata(song:SongEntity):MusicState?{
        try {
                val songPath = song.pathLocation.toString()
                val songMetadata = fetchShortFileMetadata(applicationContext!!, songPath)!!

                return MusicState(
                    idSong = song.id,
                    isPlaying = playingState(),
                    title = songMetadata.title,
                    artist = songMetadata.artist,
                    album = songMetadata.album,
                    isFavorite = song.favorite,
                    duration = songMetadata.songLength,
                    songPath = songPath,
                    latestPlayed = false

                )

        }catch(ex:Exception) {
            ex.printStackTrace()
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
        clearABLoopOfPreferences()
        stopSelf()
        exitProcess(0)
    }
    inner class MusicPlayerServiceBinder : Binder() {
        fun getService(): MusicPlayerService {
            return this@MusicPlayerService
        }
    }
}