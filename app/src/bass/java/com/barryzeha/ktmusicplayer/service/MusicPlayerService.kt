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
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
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
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import com.barryzeha.audioeffects.common.EffectsPreferences
import com.barryzeha.audioeffects.common.EqualizerManager
import com.barryzeha.core.common.AB_LOOP
import com.barryzeha.core.common.ACTION_CLOSE
import com.barryzeha.core.common.ACTION_FAVORITE
import com.barryzeha.core.common.CLEAR_MODE
import com.barryzeha.core.common.DEFAULT_DIRECTION
import com.barryzeha.core.common.MUSIC_PLAYER_SESSION
import com.barryzeha.core.common.MUSIC_STATE_EXTRA
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.common.NEXT
import com.barryzeha.core.common.PREVIOUS
import com.barryzeha.core.common.REPEAT_ALL
import com.barryzeha.core.common.REPEAT_ONE
import com.barryzeha.core.common.SHUFFLE
import com.barryzeha.core.common.fetchShortFileMetadata
import com.barryzeha.core.common.getBitmap
import com.barryzeha.core.common.getSongMetadata
import com.barryzeha.core.common.showSnackBar
import com.barryzeha.core.model.ServiceSongListener
import com.barryzeha.core.model.SongAction
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.core.model.entities.SongState
import com.barryzeha.core.model.entities.SongStateWithDetail
import com.barryzeha.data.repository.MainRepository
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.common.NOTIFICATION_ID
import com.barryzeha.ktmusicplayer.common.cancelPersistentNotify
import com.barryzeha.ktmusicplayer.common.notificationMediaPlayer
import com.barryzeha.ktmusicplayer.common.toMusicState
import com.barryzeha.ktmusicplayer.utils.BassManager
import com.google.android.material.snackbar.Snackbar
import com.un4seen.bass.BASS.BASS_ErrorGetCode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random
import kotlin.system.exitProcess
import com.barryzeha.core.R as coreRes


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 16/5/24.
 * Copyright (c)  All rights reserved.
 **/

@AndroidEntryPoint
class MusicPlayerService : Service(), BassManager.PlaybackManager{
    @Inject
    lateinit var repository: MainRepository
    @Inject
    lateinit var mPrefs:MyPreferences
    @Inject
    lateinit var effectsPrefs:EffectsPreferences
    private var mainSongsList: MutableList<SongEntity> = mutableListOf()
    private  var bassManager:BassManager?=null
    private var currentSongProgress:Long=0
    private var indexOfSong:Int=0
    private lateinit var mediaSession: MediaSession
    private lateinit var mediaStyle: MediaStyle
    private lateinit var notificationManager: NotificationManager
    private lateinit var mediaPlayerNotify:Notification
    private var playBackState:PlaybackState? = null
    private var mediaMetadata:MediaMetadata? = null

    private val binder: Binder = MusicPlayerServiceBinder()
    private var _activity:AppCompatActivity?= null

    private var _songController: ServiceSongListener? = null
    private var isForegroundService = false
    private var currentMusicState = MusicState()
    private var executeOnceTime:Boolean=false
    private var musicState:MusicState?=null
    private var songEntity:SongEntity=SongEntity()
    private val serviceScope = CoroutineScope(Job() + Main)
    private var songState:List<SongStateWithDetail> = arrayListOf()
    private var headsetReceiver:BroadcastReceiver?=null
    private var bluetoothReceiver:BroadcastReceiver?=null
    private var bluetoothIsConnect:Boolean = false
    private var nextOrPrevAnimValue=-1
    private var listIsShuffled:Boolean=false
    private var playListEnded:Boolean=false
    private var isOpenQueue:Boolean=false
    // Para comparar el cambio de canción y enviar la metadata a la notificación multimedia
    private var idSong:Long=-1
    private var firstCallingToSongState:Boolean = true
    // En esta lista cargamos momentaneamente las canciones del fragmento AlbumDetail
    private var playingQueue:MutableList<SongEntity> = mutableListOf()
    private var trackStateLoaded=false
    // Audio focus handle to calling
    private lateinit var audioManager: AudioManager
    private var focusRequest: AudioFocusRequest?=null
    private lateinit var audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener
    // true ONLY if playback was paused due to audio focus loss
    private var pausedByAudioFocusHandling:Boolean=false

    override fun onCreate() {
        super.onCreate()
        bassManager = BassManager.getInstance()
        bassManager?.registerPlaybackState(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mediaSession = MediaSession(this, MUSIC_PLAYER_SESSION)
        mPrefs.isPopulateServicePlaylist=false
        mediaStyle = MediaStyle().setMediaSession(mediaSession.sessionToken)
        currentMusicState = MusicState(albumArt = getSongMetadata(applicationContext,null)!!.albumArt)
        mediaSession.setCallback(mediaSessionCallback())
        setUpPlaylist()
        setUpHeadsetAndBluetoothReceiver()
        setupAudioFocusListener()

    }
    private fun setUpHeadsetAndBluetoothReceiver(){
        headsetReceiver = object:BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                if(action != null && action == Intent.ACTION_HEADSET_PLUG){
                    val state = intent.getIntExtra("state",-1)
                    if(state==0){
                        if(playingState()) {
                            setPlayingState(false)
                            bassManager?.channelPause()
                            _songController?.let{controller->
                                controller.pause()
                                controller.musicState(currentMusicState.copy(isPlaying = playingState()))
                            }?:run{
                                mPrefs.playOrPauseFromNotify = true
                                mPrefs.skipFromNotify = true
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
                            // lógica cuando se conecta el Bluetooth
                        }
                        BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                            // En android >=12 esta parte del código detecta cuando el dispositivo
                            // bluetooth está apagado
                            if (playingState()) {
                                setPlayingState(false)
                                bassManager?.channelPause()
                                _songController?.let{controller->
                                    controller.pause()
                                    controller.musicState(currentMusicState.copy(isPlaying = false))
                                }?:run{
                                    mPrefs.playOrPauseFromNotify = true
                                    mPrefs.skipFromNotify = true
                                }
                            }
                        }
                        BluetoothAdapter.ACTION_STATE_CHANGED -> {
                            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                            when (state) {
                                BluetoothAdapter.STATE_OFF -> {
                                    Log.d("BLUETOOTH_STATE", "Bluetooth disconnected")
                                    // Lógica cuando se desconecta el Bluetooth
                                    if (playingState()) {
                                        setPlayingState(false)
                                        bassManager?.channelPause()
                                        _songController?.let{controller->
                                            controller.pause()
                                            controller.musicState(currentMusicState.copy(isPlaying = false))
                                        }?:run{
                                            mPrefs.playOrPauseFromNotify = true
                                            mPrefs.skipFromNotify = true
                                        }
                                    }
                                }
                                BluetoothAdapter.STATE_ON -> {
                                    Log.d("BluetoothReceiver", "Bluetooth adapter turned on")

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
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        registerReceiver(bluetoothReceiver,bluetoothFilter)
    }
    private fun setupAudioFocusListener(){
        audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener{focusChange->
            when(focusChange){
                AudioManager.AUDIOFOCUS_LOSS->{}
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT->{
                    if(playingState()) {
                        pausedByAudioFocusHandling = true
                        if (playOrPauseFromNotify()) pausePlayer()
                        else _songController?.pause()
                    }
                }
                AudioManager.AUDIOFOCUS_GAIN->{
                    if(pausedByAudioFocusHandling) {
                        if (playOrPauseFromNotify()) resumePlayer()
                        else _songController?.play()
                    }
                    // Audio focus cycle finished reset flag
                    pausedByAudioFocusHandling = false
                }
            }
        }
    }
    private fun requestAudioFocus(): Boolean{
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.O) {
            if(focusRequest==null) {
                focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .build()
            }
            focusRequest?.let{audioManager.requestAudioFocus(it) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED}?:run{false}
        }
        else{
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN,

            )== AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }
    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }
    @OptIn(UnstableApi::class)
    private fun setUpPlaylist(){
        serviceScope.launch {
            initNotify()
            withContext(Dispatchers.IO) {
                // Para cargar por primera vez la lista de canciones de acuerdo al filtro guardado
                // si no hay algo seleccionado previamente solo devolverá la lista por defecto
                val songs=repository.fetchPlaylistOrderBy(mPrefs.playlistId.toLong(), mPrefs.playListSortOption)
                songs.forEach { s ->
                      mainSongsList.add(s)
                }
                findItemSongIndexById(mPrefs.idSong)?.let {indexOfSong = it}
                _songController?.onPlaylistLoaded()
            }
        }
    }
    fun loadPlaybackSavedState() {
        if(firstCallingToSongState) {
                serviceScope.launch(Dispatchers.IO) {
                    songState = repository.fetchSongState()
                    if (songState.isNotEmpty()) setSongStateSaved(songState[0])

                }
        }
    }
    private fun stopLoop(){
        bassManager?.unregisterPlaybackState()
        initNotify()
    }
    private fun startLoop(){
        bassManager?.registerPlaybackState(this)
    }
    fun updateNotify(musicState:MusicState){
        stopLoop()
        currentMusicState = musicState
        initNotify()
        startLoop()
    }
    // Funciona con las últimas versiones de Android a partir de Android 10
    private fun mediaSessionCallback():MediaSession.Callback{
        return object:MediaSession.Callback(){
            override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                if(Intent.ACTION_MEDIA_BUTTON == mediaButtonIntent.action){
                    val event = mediaButtonIntent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                    event?.let {
                        // Para evitar el lanzamiento de los eventos dos veces cuando usamos dispositivos bluetooth
                        // usamos la condición if (event.action == KeyEvent.ACTION_UP)
                        if (event.action == KeyEvent.ACTION_UP) {
                            when (it.keyCode) {
                                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                    if (_songController == null) {
                                        play(null)
                                        mPrefs.playOrPauseFromNotify = true
                                    } else {
                                        _songController?.play()
                                    }
                                }
                                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                    if (_songController == null) {
                                        pausePlayer()
                                        mPrefs.playOrPauseFromNotify = true
                                    } else {
                                        _songController?.pause()
                                    }
                                }
                                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                                    nextOrPrevTrack(NEXT)
                                }
                                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                                    nextOrPrevTrack(PREVIOUS)
                                }
                                else -> {}
                            }
                        }
                    }
                }
                return false
            }
            override fun onSeekTo(pos: Long) {
                super.onSeekTo(pos)
                setPlayerProgress(pos)
            }
            override fun onPause() {
                super.onPause()
                setPlayingState(false)
                if(_songController !=null)_songController?.pause()
                else pausePlayer()
                playOrPauseFromNotify()
            }
            override fun onPlay() {
                super.onPlay()
                setPlayingState(true)
                if(_songController !=null)_songController?.play()
                else resumePlayer()
                playOrPauseFromNotify()
            }
            override fun onSkipToNext() {
                super.onSkipToNext()
                //Controla el evento desde la notificación android >=12
                nextOrPrevTrack(NEXT)
            }
            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                nextOrPrevTrack(PREVIOUS)
            }
            override fun onStop() {
                super.onStop()
            }
            override fun onCustomAction(action: String, extras: Bundle?) {
                if(ACTION_CLOSE == action){
                    setPlayingState(false)
                    pausePlayer()
                    serviceScope.launch(Dispatchers.IO) {
                        delay(500)
                        bassManager?.unregisterPlaybackState()
                        bassManager?.releasePlayback()
                        _songController?.stop()
                        setPlayingState(false)
                        clearABLoopOfPreferences()
                        // Remove notification of foreground service process
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                        } else {
                            @Suppress("DEPRECATION")
                            stopForeground(true)
                        }
                        stopSelf()
                        // Close application
                        _activity?.finish()
                        // exitProcess elimina completamente la notificación y el servicio
                        exitProcess(0)
                    }
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
    // Funciona con versiones legadas de Android
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        musicState = intent?.getParcelableExtra<MusicState>(MUSIC_STATE_EXTRA)
        if(songEntity.favorite !=musicState?.isFavorite ) initNotify()
        when (SongAction.values()[intent?.action?.toInt() ?: SongAction.Nothing.ordinal]) {
            SongAction.Pause -> {
                setPlayingState(false)
                if (_songController != null) _songController?.pause()
                else pausePlayer()
                playOrPauseFromNotify()
            }
            SongAction.Resume -> {
                setPlayingState(true)
                if(_songController !=null)_songController?.play()
                else resumePlayer()
                playOrPauseFromNotify()
            }
            SongAction.Stop -> {
                _songController?.stop()
            }
            SongAction.Next -> {
                nextOrPrevTrack(NEXT)
            }
            SongAction.Previous -> {
                nextOrPrevTrack(PREVIOUS)
            }
            SongAction.Favorite -> {
                serviceScope.launch(Dispatchers.IO) {
                    repository.updateFavoriteSong(!currentMusicState.isFavorite,currentMusicState.idSong)
                    currentMusicState = currentMusicState.copy(isFavorite = !currentMusicState.isFavorite)
                    // Recreamos la notificación para que el ícono de nuestro estado de favoritos cambie
                    initNotify()
                }
            }
            SongAction.Close -> {
                setPlayingState(false)
                bassManager?.stopCheckingPlayback()
                bassManager?.unregisterPlaybackState()
                bassManager?.channelStop()
                clearABLoopOfPreferences()
                // Remove notification of foreground service process
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                _songController?.stop()
                // Close application
                _activity?.finish()
                stopSelf()
            }
            SongAction.Nothing -> {}
        }
      return START_NOT_STICKY
    }
    private fun nextOrPrevTrack(action:Int){
            if(mainSongsList.isNotEmpty()) {
                when(action){
                    NEXT->nextSong()
                    PREVIOUS->prevSong()
                }
            }
        if(_songController==null) {
            mPrefs.skipFromNotify = true
        }
    }

   private fun initNotify(){
        currentMusicState?.let { newState ->
            idSong = newState.idSong

        playBackState = PlaybackState.Builder()
            .setState(
                if (newState.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                bassManager?.getCurrentPositionInSeconds()!!,
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
                    if(newState.isFavorite)coreRes.drawable.ic_favorite_fill else coreRes.drawable.ic_favorite
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
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART,  getBitmap(this,newState.songPath, isForNotify = true))
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
    private fun updateNotify(progress: Long? = null) {
        currentMusicState?.let { newState ->
            val updatePlaybackState = playBackState?.let {
                PlaybackState.Builder(it)
                    .setState(
                        if (playingState()) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                        progress ?: bassManager?.getCurrentPositionInSeconds()!!,
                        1f
                    )
                    .build()
            }
            // Actualizamos el progreso y estado de reproducción de la canción
            mediaSession.setPlaybackState(updatePlaybackState)

            // Actualizamos la información que se muestra de la canción
            val updateMediaMetadata = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, newState.title)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, newState.album)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, newState.artist)
                .putBitmap(
                    MediaMetadata.METADATA_KEY_ALBUM_ART,
                    getBitmap(this, newState.songPath, isForNotify = true)
                )
                .putLong(MediaMetadata.METADATA_KEY_DURATION, newState.duration)
                .build()

            // Para android >=10 y legados, actualizamos la metadata de la sesión para que se actualice el contenido de la notificación
            mediaSession.setMetadata(updateMediaMetadata)
            // Reemplazamos temporalmente el nuevo id para la comparación
            idSong = newState.idSong

            // Para android legado actualizamos la notificación completa porque en algunos casos no se actualiza el contenido de la notificación solo con actualizar la metadata de la sesión
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
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
   private fun findItemSongIndexById(idSong:Long):Int?{
       if(isOpenQueue()){
           if (playingQueue.isNotEmpty()) {
               val index = playingQueue.indexOfFirst { it.id == idSong }
               return if (index > -1) index else 0
           }
       }else {
           if (mainSongsList.isNotEmpty()) {
               val index = mainSongsList.indexOfFirst { it.id == idSong }
               return if (index > -1) index else 0
           }
       }
        return null
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
    @Synchronized
    fun setNewMediaItem(song:SongEntity){
       if(!mainSongsList.contains(song)){mainSongsList.add(song)}
    }
    // From opening song from intent action
    fun setIsOpenedFromIntent(value: Boolean){
    }
    fun onOpenFromIntent(){
        _songController?.onPlaylistLoaded()
    }
    fun removeMediaItem(song: SongEntity){
       if(mainSongsList.contains(song)) {
           mainSongsList.remove(song)
        }
    }
    fun removeMediaItems(list: List<SongEntity>){
       list.forEach { song->
            removeMediaItem(song)
        }
    }
    fun populatePlayList(songs:List<SongEntity>){
        serviceScope.launch(Dispatchers.IO) {
            if(songs.size != mainSongsList.size) {
                songs.forEach { s -> mainSongsList.add(s)}
                // Volvemos a obtener la posición de la pista en
                // la nueva lista (importante si se ha ordenado la lista por artista , album, favoritos, etc)
                findItemSongIndexById(mPrefs.idSong)?.let {
                    indexOfSong = it

                } ?: run {
                    indexOfSong = 0
                    withContext(Main) {
                        setMusicForPlay(mainSongsList[0])
                    }
                }
                mPrefs.currentIndexSong = indexOfSong.toLong()
            }
        }
        mPrefs.isPopulateServicePlaylist = false
    }
    fun clearPlayList(isSort:Boolean){
        mainSongsList.clear()
        // Cuando no es para ordenar la lista por(artista, álbum, género) será para eliminar y cargar una nueva lista de reproducción
        if(!isSort){
            setPlayingState(false)
            currentMusicState = MusicState()
            _songController?.currentTrack(MusicState())
            bassManager?.setSongStateSaved(0,0)
            bassManager?.clearBassChannel()
        }else{
            _songController?.currentTrack(currentMusicState)
        }
        executeOnceTime=false
    }
    fun unregisterController(){
        _songController=null
    }
    private fun playOrPauseFromNotify():Boolean{
        if(_songController==null){
            mPrefs.playOrPauseFromNotify=true
        }
        return _songController==null
    }
    private fun isUIDetachedFromService():Boolean{
        if(_songController==null){
            mPrefs.skipFromNotify = true
        }
        return _songController==null
    }
    fun currentSongState():MusicState{
        return currentMusicState
    }
    fun playListSize():Int{
        return if(mainSongsList.isNotEmpty()) mainSongsList.size else 0
    }
    fun playingState():Boolean{
        return mPrefs.isPlaying
    }
    private fun setPlayingState(state:Boolean){
        mPrefs.isPlaying=state
    }
    fun isTrackStateLoaded() = trackStateLoaded
    fun setTrackStateLoaded(value:Boolean){trackStateLoaded = value}
    fun isOpenQueue():Boolean = isOpenQueue
    private fun setIsOpenQueue(state:Boolean){
        isOpenQueue = state
    }
    fun playlistEnded():Boolean=playListEnded
    private fun setPlaylistEnded(state:Boolean){
        playListEnded=state
    }
    fun checkIfSongIsFavorite(id:Long){
        if(id>0) {
            serviceScope.launch(Dispatchers.IO) {
                stopLoop()
                fetchSongMetadata(repository.fetchSongById(id))?.let{ metadata->
                    currentMusicState = metadata
                    initNotify()
                }
                startLoop()
            }
        }
    }
    fun openQueue(songs:List<SongEntity>, startPosition:Int){
        setIsOpenQueue(true)
        mPrefs.currentIndexSong = startPosition.toLong()
        indexOfSong = startPosition
        playingQueue = songs.toMutableList()
        play(playingQueue[startPosition])
    }
    fun startPlayer(song:SongEntity){
        song.pathLocation?.let {
            bassManager?.unregisterPlaybackState()
            executeOnceTime=false
            pausedByAudioFocusHandling=false
            if(requestAudioFocus()) {
                play(song)
            }
        }
    }
    private fun play(song:SongEntity?){
            if (mainSongsList.isNotEmpty()) {
                setPlaylistEnded(false)
                //try {
                    song?.let {
                        clearABLoopOfPreferences()
                        songEntity = it
                        currentSongProgress = 0
                        bassManager?.streamCreateFile(song)
                        findItemSongIndexById(song.id)?.let { pos -> indexOfSong = pos }
                        executeOnceTime = true
                    } ?: run {
                        executeOnceTime = false
                    }
                    if (bassManager?.getActiveChannel() != 0) {
                        EqualizerManager.applyEqualizer(
                            bassManager?.getActiveChannel()!!,
                            effectsPrefs
                        )
                        bassManager?.registerPlaybackState(this)
                        bassManager?.channelPlay(currentSongProgress)
                        bassManager?.startCheckingPlayback()
                        setPlayingState(true)
                        mPrefs.idSong = songEntity.id
                        currentMusicState = fetchSongMetadata(songEntity)?.copy(
                            isPlaying = playingState(),
                            idSong = songEntity.id,
                            isFavorite = songEntity.favorite,
                            latestPlayed = false,
                            nextOrPrev = nextOrPrevAnimValue
                        )!!

                        mPrefs.currentIndexSong = indexOfSong.toLong()
                    } else {
                        val errorCode = BASS_ErrorGetCode()
                        val errorMsg = bassManager?.getBassErrorMessage(errorCode)
                        _activity?.showSnackBar(
                            _activity?.findViewById(R.id.mainCoordinatorLayout)!!,
                            errorMsg?.let{it.substringAfter(":")}?:run{coreRes.string.cantPlayMsg.toString()},
                            Snackbar.LENGTH_LONG
                        )
                        setPlayingState(if(isOpenQueue)indexOfSong < playingQueue.size-1 else indexOfSong < mainSongsList.size-1)
                        nextSong()
                    }
                    song?.let {
                        if (executeOnceTime) _songController?.currentTrack(currentMusicState)
                    }
               /* }catch(ex:Exception){
                    Log.e("PLAY-ERROR", ex.message.toString() )
                }*/
            }
    }
    fun getSessionOrChannelId(): Int {
        return bassManager?.getActiveChannel()!!
    }
    fun pausePlayer(){
        setPlayingState(false)
        currentSongProgress=bassManager?.getCurrentPositionInSeconds()?:0
        bassManager?.channelPause()
    }
    fun resumePlayer(){
        pausedByAudioFocusHandling=false
        if(requestAudioFocus()) {
            play(null)
        }
    }
    fun nextSong(){
        clearABLoopOfPreferences()
        bassManager?.unregisterPlaybackState()
        if(isOpenQueue()){
            if(playingQueue.isNotEmpty()){
                if(indexOfSong < playingQueue.size -1){
                    if(mPrefs.songMode == SHUFFLE) indexOfSong = Random.nextInt(0,playingQueue.size-1)
                    else indexOfSong +=1
                }else{
                    indexOfSong = 0
                }
            }
        }else {
            if (mainSongsList.isNotEmpty()) {
                if (indexOfSong < mainSongsList.size - 1) {
                    if(mPrefs.songMode == SHUFFLE) indexOfSong = Random.nextInt(0,mainSongsList.size-1)
                    else indexOfSong += 1
                } else {
                    indexOfSong = 0
                }
            }
        }
        nextOrPrevAnimValue = NEXT
        setOrPlaySong(indexOfSong, NEXT)
        isUIDetachedFromService()
        mPrefs.currentIndexSong = indexOfSong.toLong()
    }
    private fun reconcileCurrentSongInQueue(){
        if (songEntity.id>0 && !mainSongsList.contains(songEntity)) {
            mainSongsList.add(songEntity)
            indexOfSong = mainSongsList.lastIndex
        }
    }
    fun prevSong(){
        clearABLoopOfPreferences()
        bassManager?.unregisterPlaybackState()
        if(mainSongsList.isNotEmpty()){
            reconcileCurrentSongInQueue()
            if(indexOfSong > 0) {
                if(isOpenQueue()){
                    if(mPrefs.songMode == SHUFFLE) indexOfSong =Random.nextInt(0, playingQueue.size-1)
                    else  indexOfSong -=1
                }else{
                    if(mPrefs.songMode == SHUFFLE) indexOfSong =Random.nextInt(0, mainSongsList.size-1)
                    else  indexOfSong -=1
                }
                nextOrPrevAnimValue = PREVIOUS
                setOrPlaySong(indexOfSong, PREVIOUS)
                isUIDetachedFromService()
                mPrefs.currentIndexSong = indexOfSong.toLong()
            }
        }
    }
    fun reloadIndexOfSong(){
        // Obtenemos la posición en la lista principal de la pista que hayamos reproducido
        // de cualquier otra lista como AlbumDetail
        setIsOpenQueue(false)
        indexOfSong = findItemSongIndexById(songEntity.id)!!
    }
    fun fastForward(){
        bassManager?.fastForwardOrRewind(isForward=true){currentSongProgress=it}
    }
    fun fastRewind(){
        bassManager?.fastForwardOrRewind(isForward=false){currentSongProgress=it}
    }
    // A-B looper
    fun setStartPositionForAbLoop() = bassManager?.setAbLoopStar()
    fun setEndPositionAbLoop() = bassManager?.setAbLoopEnd()
    fun stopAbLoop() = bassManager?.stopAbLoop()
    fun clearABLoopOfPreferences(){
        bassManager?.stopAbLoop()
        if(mPrefs.songMode == AB_LOOP) mPrefs.songMode = CLEAR_MODE
    }
    fun getSongsList():List<SongEntity>{
        return if(isOpenQueue()) playingQueue else mainSongsList
    }
    fun getCurrentSongPosition():Int = mPrefs.currentIndexSong.toInt()?:0
    fun setCurrentSongPosition(position:Int) {mPrefs.currentPosition=position.toLong()}
    private fun setOrPlaySong(indexOfSong:Int,animDirection:Int= DEFAULT_DIRECTION){
        setPlaylistEnded(false)
        val song = if(isOpenQueue())playingQueue[indexOfSong] else mainSongsList[indexOfSong]
        if (playingState()){
            play(song)
        }else{
            setMusicForPlay(song, animDirection)
        }
        saveStateOfSong(song)
    }
    private fun setMusicForPlay(song: SongEntity, animDirection:Int= DEFAULT_DIRECTION){
        val songState = SongStateWithDetail(SongState(currentPosition = 0),song)
        findItemSongIndexById(song.id)?.let {indexOfSong = it}
        mPrefs.idSong = song.id
        setSongStateSaved(songState, animDirection)
    }
    fun setPlayerProgress(progress:Long){
       bassManager?.setChannelProgress(progress){currentSongProgress=it}
    }
    private fun saveStateOfSong(song:SongEntity){
        serviceScope.launch {
            delay(1000)
            if (currentMusicState.idSong > 0) {
                val songState = SongState(
                    idSongState = 1,
                    idSong = song.id,
                    songDuration = song.duration,
                    currentPosition = currentMusicState.currentPosition
                )
                repository.saveSongState(songState)
            }
        }
    }
    private fun setSongStateSaved(songState: SongStateWithDetail, animDirection:Int= DEFAULT_DIRECTION){
        val song = songState.songEntity

        songEntity = song
        isUIDetachedFromService()

        // Set info currentSongEntity
        fetchSongMetadata(song)?.let { musicState ->
            currentMusicState = musicState.copy(
                currentDuration = songState.songState.currentPosition,
                duration = songState.songEntity.duration,
                latestPlayed = true,
                nextOrPrev = animDirection,
                isFavorite = song.favorite
            )
            _songController?.currentTrack(currentMusicState)
        }
        if (!playingState()) {
            currentSongProgress = songState.songState.currentPosition
            bassManager?.streamCreateFile(songState.songEntity)
            bassManager?.setSongStateSaved(
                bassManager?.getActiveChannel()!!,
                songState.songState.currentPosition
            )
        }
        setPlayingState(playingState())
        //_songController?.currentTrack(currentMusicState)
        // Al cargar la información de una pista guardada
        // se ejecutaba una primera vez el evento currentTrack de la interface
        // ya que el listener la ejecutaba una vez más, debemos poner executeOnceTime = true
        // para evitarlo
        executeOnceTime = true
        setTrackStateLoaded(true)
        bassManager?.startCheckingPlayback()
        bassManager?.registerPlaybackState(this@MusicPlayerService)
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
            duration = songMetadata.songLength,
            isFavorite = song.favorite,
            songPath = songPath,
            latestPlayed = false
        )
        }catch(ex:Exception) {
            return null
        }
    }
    override fun onPlaybackState() {
        currentMusicState = songEntity.toMusicState().copy(
            isPlaying = playingState(),
            currentDuration = bassManager?.getCurrentPositionInSeconds()
                ?: 0,
            duration = bassManager?.getDuration(bassManager?.getActiveChannel()!!) ?: 0,
            latestPlayed = false
        )
        _songController?.musicState(currentMusicState)
    }
    override fun onPlayingChanged() {
        updateNotify()
    }
    override fun onPlaybackProgress(progress:Long) {
        updateNotify(progress)
    }
    override fun onPlaybackFinished() {
        if(isOpenQueue()){
            if (indexOfSong < playingQueue.size - 1 && playingQueue.isNotEmpty()) {
                when (mPrefs.songMode) {
                    REPEAT_ONE -> {if (playingState()) {bassManager?.repeatSong()}}
                    SHUFFLE->{
                    indexOfSong = (playingQueue.indices).random()
                    play(playingQueue[indexOfSong])
                }
                   else -> {if (playingState()) nextSong()}
                }
            } else {
                when (mPrefs.songMode) {
                    REPEAT_ALL -> { play(playingQueue[0])}
                    REPEAT_ONE -> {if (playingState()) {bassManager?.repeatSong()}}
                    else -> {
                        setPlayingState(false)
                        if (playingQueue.isNotEmpty()) setMusicForPlay(playingQueue[0])
                        bassManager?.channelStop()
                        setPlaylistEnded(true)
                    }
                }
            }
        }else {
            if (indexOfSong < mainSongsList.size -1 && mainSongsList.isNotEmpty()) {
                when (mPrefs.songMode) {
                    REPEAT_ONE -> {if (playingState()) {bassManager?.repeatSong()}}
                    SHUFFLE->{
                    indexOfSong = (mainSongsList.indices).random()
                    play(mainSongsList[indexOfSong])
                }
                    else -> {if (playingState()) nextSong() }
                }
            } else {
                when (mPrefs.songMode) {
                    REPEAT_ALL -> {play(mainSongsList[0])}
                    REPEAT_ONE -> {if (playingState()) {bassManager?.repeatSong()}}
                    else -> {
                        setPlayingState(false)
                        if (mainSongsList.isNotEmpty()) setMusicForPlay(mainSongsList[0])
                        bassManager?.channelStop()
                        //Finish play list
                       setPlaylistEnded(true)
                    }
                }
            }
        }
    }
    override fun onDestroy() {
        unregisterReceiver(headsetReceiver)
        unregisterReceiver(bluetoothReceiver)
        isForegroundService = false
        _songController?.stop()
        mediaSession.release()
        bassManager?.releasePlayback()
        // Remove notification of foreground service process
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        clearABLoopOfPreferences()
        abandonAudioFocus()
        super.onDestroy()
    }
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        cancelPersistentNotify(applicationContext)
        stopSelf()
    }
    inner class MusicPlayerServiceBinder : Binder() {
        fun getService(): MusicPlayerService {
            return this@MusicPlayerService
        }
    }
}