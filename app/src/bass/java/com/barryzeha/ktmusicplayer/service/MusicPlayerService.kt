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
import com.barryzeha.core.R as coreRes
import com.barryzeha.ktmusicplayer.common.NOTIFICATION_ID
import com.barryzeha.ktmusicplayer.common.cancelPersistentNotify
import com.barryzeha.ktmusicplayer.common.notificationMediaPlayer
import com.barryzeha.ktmusicplayer.utils.BassManager
import com.google.android.material.snackbar.Snackbar
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
    private var songsList: MutableList<SongEntity> = mutableListOf()
    private  var bassManager:BassManager?=null
    private var currentSongPosition:Long=0
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
    private var songRunnable: Runnable = Runnable {}
    private var songHandler: Handler = Handler(Looper.getMainLooper())
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
    // Para comparar el cambio de canción y enviar la metadata a la notificación multimedia
    private var idSong:Long=-1
    private var firstCallingToSongState:Boolean = true
    // Thelephony manager para controlar la reproducción durante las llamadas
    private var phoneCallStateReceiver:BroadcastReceiver?=null
    private var telephonyManager: TelephonyManager?=null
    private var isPlayingBeforeCallPhone:Boolean = false
    // En esta lista cargamos momentaneamente las canciones del fragmento AlbumDetail
    private var playingQueue:MutableList<SongEntity> = mutableListOf()


    override fun onCreate() {
        super.onCreate()

        bassManager = BassManager()
        bassManager?.getInstance(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mediaSession = MediaSession(this, MUSIC_PLAYER_SESSION)
        mPrefs.isPopulateServicePlaylist=false

        mediaStyle = MediaStyle().setMediaSession(mediaSession.sessionToken)
        currentMusicState = MusicState(albumArt = getSongMetadata(applicationContext,null)!!.albumArt)
        mediaSession.setCallback(mediaSessionCallback())
        setUpPlaylist()
        setUpHeadsetAndBluetoothReceiver()
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
                                    // Lógica cuando se desconecta el Bluetooth
                                    if (playingState()) {
                                        setPlayingState(false)
                                        bassManager?.channelPause()
                                        _songController?.let{controller->
                                            controller.pause()
                                            controller.musicState(currentMusicState.copy(isPlaying = false))
                                        }?:run{
                                            mPrefs.nextOrPrevFromNotify = true
                                            mPrefs.controlFromNotify = true
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

    fun setupPhoneCallStateReceiver(){
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        phoneCallStateReceiver = object: BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.S){
                    telephonyManager?.registerTelephonyCallback(context?.mainExecutor!!,object:TelephonyCallback(),TelephonyCallback.CallStateListener{
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

    @OptIn(UnstableApi::class)
    private fun setUpPlaylist(){
        serviceScope.launch {
            //TODO Revisar, no cargar toda la lista antes del estado de la canción
            withContext(Dispatchers.IO) {
                // Para cargar por primera vez la lista de canciones de acuerdo al filtro guardado
                // si no hay algo seleccionado previamente solo devolverá la lista por defecto
                val songs=repository.fetchPlaylistOrderBy(mPrefs.playlistId.toLong(), mPrefs.playListSortOption)
                songs.forEach { s ->
                      songsList.add(s)
                }
            }
            initMusicStateLoop()
        }
    }
    fun getStateSaved() {
        if(firstCallingToSongState) {
                serviceScope.launch(Dispatchers.IO) {
                    songState = repository.fetchSongState()
                    if(songState.isNotEmpty())setSongStateSaved(songState[0])
                }
        }
    }
    private fun initMusicStateLoop(){
        initNotify()
        songRunnable = Runnable {
            if(bassManager?.getActiveChannel() != 0) {
                currentMusicState = currentMusicState.copy(
                    isPlaying = playingState(),
                    currentDuration = bassManager?.getCurrentPositionInSeconds(bassManager?.getActiveChannel()!!)?:0,
                    duration = bassManager?.getDuration(bassManager?.getActiveChannel()!!)?:0,
                    latestPlayed = false
                )
            }
            _songController?.musicState(currentMusicState)
            updateNotify()
            songHandler.postDelayed(songRunnable, 500)
        }
        songHandler.post(songRunnable)

    }
    private fun stopLoop(){
        songHandler.removeCallbacks(songRunnable)
        initNotify()
    }
    private fun startLoop(){
        songHandler.post(songRunnable)
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
                                        mPrefs.nextOrPrevFromNotify = true
                                        mPrefs.controlFromNotify = true
                                    } else {
                                        _songController?.play()
                                    }
                                }

                                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                    if (_songController == null) {
                                        pausePlayer()
                                        mPrefs.nextOrPrevFromNotify = true
                                        mPrefs.controlFromNotify = true
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
                checkIfPhoneIsLock()
            }

            override fun onPlay() {
                super.onPlay()
                setPlayingState(true)
                if(_songController !=null)_songController?.play()
                else resumePlayer()
                checkIfPhoneIsLock()
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
                        delay(1000)
                        bassManager?.releasePlayback()
                        songHandler.removeCallbacks(songRunnable)
                        _songController?.stop()
                        // Remove notification of foreground service process
                        stopForeground(STOP_FOREGROUND_REMOVE)
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
                checkIfPhoneIsLock()
            }
            SongAction.Resume -> {
                setPlayingState(true)
                if(_songController !=null)_songController?.play()
                else resumePlayer()
                checkIfPhoneIsLock()
            }
            SongAction.Stop -> {
                _songController?.stop()

            }
            SongAction.Next -> {
                //TODO probar en android 8
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
                bassManager?.releasePlayback()
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
    private fun nextOrPrevTrack(action:Int){
            if(songsList.isNotEmpty()) {
                when(action){
                    NEXT->nextSong()
                    PREVIOUS->prevSong()
                }
            }
            mPrefs.nextOrPrevFromNotify=true
            mPrefs.controlFromNotify = true
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
    private fun updateNotify(){
            currentMusicState?.let { newState ->
                val updatePlaybackState = playBackState?.let {
                    PlaybackState.Builder(it)
                        .setState(
                            if (newState.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                            newState.currentDuration,
                            1f
                        )
                        .build()
                }
                // Actualizamos el progreso y estado de reproducción de la canción
                mediaSession.setPlaybackState(updatePlaybackState)

                // Actualizamos la información que se muestra de la canción
                if (idSong != newState.idSong) {// Comparamos los ids para saber si ha cambiado la canción
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
   private fun findItemSongIndexById(idSong:Long):Int?{
       if(mPrefs.isOpenQueue){
           if (playingQueue.isNotEmpty()) {
               val index = playingQueue.indexOfFirst { it.id == idSong }
               return if (index > -1) index else 0
           }
       }else {
           if (songsList.isNotEmpty()) {
               val index = songsList.indexOfFirst { it.id == idSong }
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
    fun setNewMediaItem(song:SongEntity){
       if(!songsList.contains(song)){ songsList.add(song)}
    }
    fun removeMediaItem(song: SongEntity){
       if(songsList.contains(song)) {
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
            if(songs.size != songsList.size) {
                songs.forEach { s -> songsList.add(s)}
                // Volvemos a obtener la posición de la pista en
                // la nueva lista (importante si se ha ordenado la lista por artista , album, favoritos, etc)
                findItemSongIndexById(mPrefs.idSong)?.let {
                    indexOfSong = it

                } ?: run {
                    indexOfSong = 0
                    withContext(Main) {
                        setMusicForPlay(songsList[0])
                    }
                }
                mPrefs.currentIndexSong = indexOfSong.toLong()
            }
        }
        mPrefs.isPopulateServicePlaylist = false
    }
    fun clearPlayList(isSort:Boolean){
        songsList.clear()
        // Cuando no es para ordenar la lista por(artista, álbum, género) será para eliminar y cargar una nueva lista de reproducción
        if(!isSort){
            setPlayingState(false)
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
    private fun checkIfPhoneIsLock():Boolean{
        if(_songController==null){
            mPrefs.nextOrPrevFromNotify=true
            mPrefs.controlFromNotify = true
        }
        return _songController==null
    }
    fun currentSongState():MusicState{
        return currentMusicState
    }
    fun playListSize():Int{
        return if(songsList.isNotEmpty()) songsList.size else 0
    }
    fun playingState():Boolean{
        return mPrefs.isPlaying
    }
    private fun setPlayingState(state:Boolean){
        mPrefs.isPlaying=state
    }
    fun checkIfSongIsFavorite(id:Long){
        serviceScope.launch(Dispatchers.IO) {
            currentMusicState = fetchSongMetadata(repository.fetchSongById(id))!!
            initNotify()
        }
    }
    fun openQueue(songs:List<SongEntity>, startPosition:Int){
        mPrefs.currentIndexSong = startPosition.toLong()
        indexOfSong = startPosition
        playingQueue = songs.toMutableList()
        play(playingQueue[startPosition])
    }
    fun startPlayer(song:SongEntity){
        song.pathLocation?.let {
            if(playingState()){songHandler.post(songRunnable)}
            // executeOnceTime nos servirá para evitar que el listener de exoplayer vuelva a mandar
            // información  de la pista en reproducción que no requiere cambios constantes
            // como la carátula del álbum, título, artista. A diferencia del tiempo transcurrido
            executeOnceTime=false
            play(song)
        }
    }
    private fun play(song:SongEntity?){
            if (songsList.isNotEmpty()) {
                //try {
                    song?.let {
                        songEntity = it
                        currentSongPosition = 0
                        bassManager?.streamCreateFile(song)
                        if(!mPrefs.isOpenQueue)findItemSongIndexById(song.id)?.let { pos -> indexOfSong = pos }
                        executeOnceTime = true
                    } ?: run {
                        bassManager?.streamCreateFile(songEntity)
                        executeOnceTime = false
                    }
                    if (bassManager?.getActiveChannel() != 0) {
                        bassManager?.channelPlay(currentSongPosition)
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

                        EqualizerManager.applyEqualizer(
                            bassManager?.getActiveChannel()!!,
                            effectsPrefs
                        )
                        mPrefs.currentIndexSong = indexOfSong.toLong()
                    } else {
                        _activity?.showSnackBar(
                            _activity?.findViewById(android.R.id.content)!!,
                            coreRes.string.cantPlayMsg,
                            Snackbar.LENGTH_LONG
                        )
                    }
                    song?.let {
                        if (executeOnceTime) _songController?.currentTrack(currentMusicState)
                    }
               /* }catch(ex:Exception){
                    Log.e("PLAY-ERROR", ex.message.toString() )
                }*/
            }
    }
    fun pausePlayer(){
        setPlayingState(false)
        currentSongPosition=bassManager?.getCurrentPositionInSeconds(bassManager?.getActiveChannel()!!)?:0
        bassManager?.channelPause()
        bassManager?.stopCheckingPlayback()
        updateNotifyForLegacySdkVersions()
    }
    fun getSessionOrChannelId(): Int {
        return bassManager?.getActiveChannel()!!
    }
    fun resumePlayer(){
        play(null)
        updateNotifyForLegacySdkVersions()
    }
    fun nextSong(){
        if(mPrefs.isOpenQueue){
            if(playingQueue.isNotEmpty()){
                if(indexOfSong < playingQueue.size -1){
                    if(mPrefs.songMode == SHUFFLE) indexOfSong = Random.nextInt(0,playingQueue.size-1)
                    else indexOfSong +=1
                }else{
                    indexOfSong = 0
                }
            }
        }else {
            if (songsList.isNotEmpty()) {
                if (indexOfSong < songsList.size - 1) {
                    if(mPrefs.songMode == SHUFFLE) indexOfSong = Random.nextInt(0,songsList.size-1)
                    else indexOfSong += 1
                } else {
                    indexOfSong = 0
                }
            }
        }
        nextOrPrevAnimValue = NEXT
        setOrPlaySong(indexOfSong, NEXT)
        checkIfPhoneIsLock()
        mPrefs.currentIndexSong = indexOfSong.toLong()

        stopAbLoop()
        clearABLoopOfPreferences()
    }
    fun prevSong(){
        if(songsList.isNotEmpty()){
            if(indexOfSong > 0) {
                if(mPrefs.isOpenQueue){
                    if(mPrefs.songMode == SHUFFLE) indexOfSong =Random.nextInt(0, playingQueue.size-1)
                    else  indexOfSong -=1
                }else{
                    if(mPrefs.songMode == SHUFFLE) indexOfSong =Random.nextInt(0, songsList.size-1)
                    else  indexOfSong -=1
                }
                nextOrPrevAnimValue = PREVIOUS
                setOrPlaySong(indexOfSong, PREVIOUS)
                checkIfPhoneIsLock()
                mPrefs.currentIndexSong = indexOfSong.toLong()
            }
        }
        stopAbLoop()
        clearABLoopOfPreferences()
    }
    fun reloadIndexOfSong(){
        // Obtenemos la posición en la lista principal de la pista que hayamos reproducido
        // de cualquier otra lista como AlbumDetail
        indexOfSong = findItemSongIndexById(songEntity.id)!!
    }
    fun fastForward(){
        bassManager?.fastForwardOrRewind(isForward=true){currentSongPosition=it}
    }
    fun fastRewind(){
        bassManager?.fastForwardOrRewind(isForward=false){currentSongPosition=it}
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
        return songsList
    }
    fun getCurrentSongPosition():Int = mPrefs.currentIndexSong.toInt()?:0
    fun setCurrentSongPosition(position:Int) {mPrefs.currentPosition=position.toLong()}
    private fun setOrPlaySong(indexOfSong:Int,animDirection:Int= DEFAULT_DIRECTION){
        if (mPrefs.isPlaying){
            play(if(mPrefs.isOpenQueue)playingQueue[indexOfSong] else songsList[indexOfSong])
        }
        else{
            setMusicForPlay(if(mPrefs.isOpenQueue)playingQueue[indexOfSong]else songsList[indexOfSong], animDirection)
        }
    }
    private fun setMusicForPlay(song: SongEntity, animDirection:Int= DEFAULT_DIRECTION){
        val songState = SongStateWithDetail(SongState(currentPosition = 0),song)
        mPrefs.idSong = song.id
        setSongStateSaved(songState, animDirection)

    }
    fun setPlayerProgress(progress:Long){
       bassManager?.setChannelProgress(progress){currentSongPosition=it}
    }

    private fun setSongStateSaved(songState: SongStateWithDetail, animDirection:Int= DEFAULT_DIRECTION){
            val song = songState.songEntity
            songEntity = song
            // Set info currentSongEntity
            fetchSongMetadata(song)?.let { musicState ->
                currentMusicState = musicState.copy(
                    currentDuration = songState.songState.currentPosition,
                    latestPlayed = true,
                    nextOrPrev = animDirection,
                    isFavorite = song.favorite
                )
            }
            setPlayingState(false)
            currentSongPosition = songState.songState.currentPosition
            bassManager?.streamCreateFile(songState.songEntity)
            bassManager?.setSongStateSaved(
                bassManager?.getActiveChannel()!!,
                songState.songState.currentPosition
            )
            findItemSongIndexById(songState.songEntity.id)?.let {
                indexOfSong = it
            }
            if (!checkIfPhoneIsLock()) {
               _songController?.currentTrack(currentMusicState)

            }
            // Al cargar la información de una pista guardada
            // se ejecutaba una primera vez el evento currentTRack de la interface
            // ya que el listener la ejecutaba una vez más debemos poner executeOnceTime = true
            // para evitarlo
            executeOnceTime = true


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
    override fun onFinishPlayback() {
        if(mPrefs.isOpenQueue){
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
                        if (playingQueue.isNotEmpty()) setMusicForPlay(playingQueue[0])
                        bassManager?.stopCheckingPlayback()
                    }
                }
            }
        }else {
            if (indexOfSong < songsList.size - 1 && songsList.isNotEmpty()) {
                when (mPrefs.songMode) {
                    REPEAT_ONE -> {if (playingState()) {bassManager?.repeatSong()}}
                    SHUFFLE->{
                    indexOfSong = (songsList.indices).random()
                    play(songsList[indexOfSong])
                }
                    else -> {if (playingState()) nextSong() }
                }
            } else {
                when (mPrefs.songMode) {
                    REPEAT_ALL -> {play(songsList[0])}
                    REPEAT_ONE -> {if (playingState()) {bassManager?.repeatSong()}}
                    else -> {
                        if (songsList.isNotEmpty()) setMusicForPlay(songsList[0])
                        bassManager?.stopCheckingPlayback()
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
        bassManager?.releasePlayback()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onDestroy()
    }
    // TODO
    // Si queremos mantener las notificaciones una vez cerrada la aplicación
    // no debemos sobreescribir onTaskRemoved - volveremos más tarde para implementar algo con eso
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        clearABLoopOfPreferences()
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