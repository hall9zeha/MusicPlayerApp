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
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
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
import com.barryzeha.core.common.REPEAT_ALL
import com.barryzeha.core.common.REPEAT_ONE
import com.barryzeha.core.common.SHUFFLE
import com.barryzeha.core.common.getSongMetadata
import com.barryzeha.core.model.SongAction
import com.barryzeha.core.model.ServiceSongListener
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
import com.barryzeha.ktmusicplayer.utils.BassManager
import com.un4seen.bass.BASS
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject
import kotlin.system.exitProcess


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 16/5/24.
 * Copyright (c)  All rights reserved.
 **/

@AndroidEntryPoint
class MusicPlayerService : Service(),BassManager.PlaybackManager{
    private val NEXT =0
    private val PREVIOUS =1
    @Inject
    lateinit var repository: MainRepository
    @Inject
    lateinit var effectsPrefs:EffectsPreferences

    private var songsList: MutableList<SongEntity> = mutableListOf()
    private  var bassManager:BassManager?=null
    private var mainChannel:Int=0
    private var currentSongPosition:Long=0
    private var updateTimer: Timer? = null
    private var indexOfSong:Int=0

    private lateinit var mediaSession: MediaSession
    private lateinit var mediaStyle: MediaStyle
    private lateinit var notificationManager: NotificationManager
    private lateinit var mediaPlayerNotify:Notification
    private val binder: Binder = MusicPlayerServiceBinder()
    private var _activity:AppCompatActivity?= null
    private lateinit var exoPlayer:ExoPlayer
    private var positionReset= -1

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

    private var songs:MutableList<SongEntity> = arrayListOf()
    private var songState:List<SongStateWithDetail> = arrayListOf()
    private var headsetReceiver:BroadcastReceiver?=null
    private var bluetoothReceiver:BroadcastReceiver?=null
    private var bluetoothIsConnect:Boolean = false

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()

        bassManager = BassManager()
        bassManager?.getInstance(this)

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
                            _songController?.pause()
                            _songController?.musicState(currentMusicState.copy(isPlaying = exoPlayer.isPlaying))
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

                        }
                        BluetoothAdapter.ACTION_STATE_CHANGED -> {
                            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                            when (state) {
                                BluetoothAdapter.STATE_OFF -> {
                                    Log.d("BLUETOOTH_STATE", "Bluetooth disconnected")
                                    // Aquí puedes agregar la lógica cuando se desconecta el Bluetooth
                                    if (exoPlayer.isPlaying) {
                                        exoPlayer.pause()
                                        _songController?.pause()
                                        _songController?.musicState(currentMusicState.copy(isPlaying = false))
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
    override fun onFinishPlayback() {
        if(indexOfSong<songsList.size -1){
            when(mPrefs.songMode){
                REPEAT_ONE->{BASS.BASS_ChannelPlay(bassManager?.getActiveChannel()!!, true);}
                SHUFFLE->{
                    indexOfSong = (songsList.indices).random()
                    play(songsList[indexOfSong])
                }
                else->{if(indexOfSong == songsList.size-1)nextSong()}
            }
        }else{
            when(mPrefs.songMode){
                REPEAT_ALL->{ play(songsList[0])}
                SHUFFLE->{
                }
                else->{setMusicForPlayer(songsList[0])}
            }
        }
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
                setPlayerProgress(pos)
            }

            override fun onPause() {
                super.onPause()
                if(_songController !=null)_songController?.pause()
                else pausePlayer()
                if(_songController==null){
                    mPrefs.nextOrPrevFromNotify=true
                    mPrefs.controlFromNotify = true
                    mPrefs.isPlaying = false
                }
            }

            override fun onPlay() {
                super.onPlay()
                if(_songController !=null)_songController?.play()
                else play(null)
                if(_songController==null){
                    mPrefs.nextOrPrevFromNotify=true
                    mPrefs.controlFromNotify = true
                    mPrefs.isPlaying = true
                }
            }
            override fun onSkipToNext() {
                super.onSkipToNext()
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
                    bassManager?.releasePlayback()
                    songHandler.removeCallbacks(songRunnable)
                    // Remove notification of foreground service process
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    _songController?.stop()
                    // Close application
                    _activity?.finish()
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
                    Log.e("SESSION-ID", index.toString() + "--" + value.toString())
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
               if(_songController != null) _songController?.pause()
               else pausePlayer()
                if(_songController==null){
                    mPrefs.nextOrPrevFromNotify=true
                    mPrefs.controlFromNotify = true
                    mPrefs.isPlaying = false
                }
            }
            SongAction.Resume -> {
                if(_songController !=null)_songController?.play()
                else play(null)
                if(_songController==null){
                    mPrefs.nextOrPrevFromNotify=true
                    mPrefs.controlFromNotify = true
                    mPrefs.isPlaying = true
                }
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

    @OptIn(UnstableApi::class)
    private fun setUpRepository(){
        exoPlayer = ExoPlayer.Builder(applicationContext)
            .build()
        setUpEqualizer(exoPlayer.audioSessionId)

        CoroutineScope(Dispatchers.Main).launch {
            // Para cargar por primera vez la lista de canciones de acuerdo al filtro guardado
            // si no hay algo seleccionado previamente solo devolverá la lista por defecto
            val songs=repository.fetchAllSongsBy(mPrefs.playListSortOption)

            songState=repository.fetchSongState()
            // TODO Revisar, no cargar toda la lista antes del estado de la canción
            withContext(Dispatchers.IO) {
                songs.forEach { s ->
                    if (!songsList.contains(s)) {
                        songsList.add(s)
                    }
               }
            }
            if(!songState.isNullOrEmpty()) {
                val songEntity=songState[0].songEntity
                if(songsList.contains(songEntity))setMusicStateSaved(songState[0])
            }

        }
    }
    private fun nextOrPrevTrack(action:Int){
            if(songsList.isNotEmpty()) {
                when(action){
                    NEXT->nextSong()
                    PREVIOUS->prevSong()
                }
              //mPrefs.currentPosition = position.toLong()
            }
            mPrefs.nextOrPrevFromNotify=true
            mPrefs.controlFromNotify = true


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
                       /* .setActions(PlaybackState.ACTION_PLAY_PAUSE)
                        .setActions(PlaybackState.ACTION_SEEK_TO)*/

                        // Los siguiente controles aparecerán en android 13 y 14
                        .setActions(PlaybackState.ACTION_SEEK_TO
                                or PlaybackState.ACTION_PLAY
                                or PlaybackState.ACTION_PAUSE
                                or PlaybackState.ACTION_SKIP_TO_NEXT
                                or PlaybackState.ACTION_SKIP_TO_PREVIOUS
                                or PlaybackState.ACTION_STOP
                          )
                        .addCustomAction(PlaybackState.CustomAction.Builder(
                            ACTION_CLOSE,
                            ACTION_CLOSE,
                            com.barryzeha.core.R.drawable.ic_close
                        ).build())
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
               if(bassManager?.getActiveChannel() != 0) {
                    currentMusicState = currentMusicState.copy(
                        isPlaying = mPrefs.isPlaying,
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

    private fun findItemSongIndexById(idSong:Long):Int?{
        if(songsList.isNotEmpty()) {
            return songsList.indexOfFirst{it.id == idSong}
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
        CoroutineScope(Dispatchers.IO).launch {
            songs.forEach { s ->
                if (!songsList.contains(s)) {
                    songsList.add(s)
                }
            }
            Log.e("ITEMS-MEDIA-S-POPULATE", songsList.size.toString())
        }
    }
    fun clearPlayList(){
        songsList.clear()
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
            play(song)
        }
    }
    private fun play(song:SongEntity?){

        if(songsList.isNotEmpty()) {
            song?.let {
                BASS.BASS_StreamFree(bassManager?.getActiveChannel()!!)
                // Cleaning a previous track if have anyone
                songEntity = it
                currentSongPosition = 0
                findItemSongIndexById(song.id)?.let { pos -> indexOfSong = pos }
                mainChannel = BASS.BASS_StreamCreateFile(it.pathLocation, 0, 0, BASS.BASS_SAMPLE_FLOAT)
                bassManager?.setActiveChannel(mainChannel)

            } ?: run {
                mainChannel = BASS.BASS_StreamCreateFile(songEntity.pathLocation,0,0,BASS.BASS_SAMPLE_FLOAT)
                bassManager?.setActiveChannel(mainChannel)

            }
            if (bassManager?.getActiveChannel() != 0) {
                BASS.BASS_ChannelSetAttribute(bassManager?.getActiveChannel()!!,BASS.BASS_ATTRIB_VOL,1F)
                // Convertir la posición actual (en milisegundos) a bytes con bassManager?.getCurrentPositionToBytes
                BASS.BASS_ChannelSetPosition( bassManager?.getActiveChannel()!!,bassManager?.getCurrentPositionToBytes(currentSongPosition)!!,BASS.BASS_POS_BYTE)
                BASS.BASS_ChannelPlay(bassManager?.getActiveChannel()!!, false);

                bassManager?.startCheckingPlayback()
                mPrefs.isPlaying = true
                mPrefs.idSong = songEntity.id
                currentMusicState = fetchSong(songEntity)?.copy(
                    isPlaying = mPrefs.isPlaying,
                    idSong = songEntity.id,
                )!!

            }
            song?.let {
                _songController?.currentTrack(currentMusicState)
            }
        }
    }
    fun pausePlayer(){
        mPrefs.isPlaying = false
        currentSongPosition=bassManager?.getCurrentPositionInSeconds(mainChannel)?:0
        BASS.BASS_ChannelPause(mainChannel)
    }

    @OptIn(UnstableApi::class)
    fun getSessionId(): Int {
        return exoPlayer.audioSessionId
    }
    fun resumePlayer(){
        play(null)

    }
    fun nextSong(){
        if(songsList.isNotEmpty()){
            if(indexOfSong < songsList.size -1) {
                if(mPrefs.songMode == SHUFFLE)indexOfSong = (songsList.indices).random()
                else indexOfSong  += 1

                val song = songsList[indexOfSong]
                if(mPrefs.isPlaying)play(song)
                else setMusicForPlayer(song)
            }else{
                if(mPrefs.songMode == SHUFFLE){
                    indexOfSong = (songsList.indices).random()
                    if(mPrefs.isPlaying)play(songsList[indexOfSong])
                    else setMusicForPlayer(songsList[indexOfSong])
                }
                else {
                    indexOfSong = 0
                    setMusicForPlayer(songsList[indexOfSong])
                }

            }
        }

    }
    fun prevSong(){
        if(songsList.isNotEmpty()){
            if(indexOfSong > 0) {
                if(mPrefs.songMode == SHUFFLE)indexOfSong = (songsList.indices).random()
                else indexOfSong -=1

                val song = songsList[indexOfSong]
                if(mPrefs.isPlaying)play(song)
                else setMusicForPlayer(song)
            }
        }
    }
    private fun setMusicForPlayer(song: SongEntity){
        val songState = SongStateWithDetail(SongState(currentPosition = 0),song)
        mPrefs.idSong = song.id
        setMusicStateSaved(songState)
    }
    fun setPlayerProgress(progress:Long){
        // Convierte el progreso en milisegundos a bytes
        val progressBytes = BASS.BASS_ChannelSeconds2Bytes(bassManager?.getActiveChannel()!!, progress / 1000.0)
        updateTimer?.cancel()
        updateTimer = Timer()
        updateTimer?.schedule(object : TimerTask() {
            override fun run() {
                // Ajusta la posición del canal
                BASS.BASS_ChannelSetPosition(bassManager?.getActiveChannel()!!, progressBytes, BASS.BASS_POS_BYTE)
                currentSongPosition = progress
            }
        }, 15) // Retraso en milisegundos para evitar los chirridos al desplazarse en el seekbar

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
        currentSongPosition=songState.songState.currentPosition
        val channel = BASS.BASS_StreamCreateFile(songState.songEntity.pathLocation, 0, 0, BASS.BASS_SAMPLE_FLOAT)
        bassManager?.setSongStateSaved(channel,songState.songState.currentPosition )
        findItemSongIndexById(songState.songEntity.id)?.let{
            indexOfSong=it
        }
        _songController?.currentTrack(currentMusicState)
        // Al cargar la información de una pista guardada
        // se ejecutaba una primera vez el evento currentTRack de la interface
        // ya que el listener la ejecutaba una vez más debemos poner executeOnceTime = true
        // para evitarlo
        executeOnceTime = true
        mPrefs.isPlaying=false
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