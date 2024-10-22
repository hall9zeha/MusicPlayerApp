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
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import com.barryzeha.audioeffects.common.EffectsPreferences
import com.barryzeha.audioeffects.common.EqualizerManager
import com.barryzeha.core.common.ACTION_CLOSE
import com.barryzeha.core.common.DEFAULT_DIRECTION
import com.barryzeha.core.common.MUSIC_PLAYER_SESSION
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.common.NEXT
import com.barryzeha.core.common.PREVIOUS
import com.barryzeha.core.common.REPEAT_ALL
import com.barryzeha.core.common.REPEAT_ONE
import com.barryzeha.core.common.SHUFFLE
import com.barryzeha.core.common.getSongMetadata
import com.barryzeha.core.common.showSnackBar
import com.barryzeha.core.model.ServiceSongListener
import com.barryzeha.core.model.SongAction
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.core.model.entities.SongState
import com.barryzeha.core.model.entities.SongStateWithDetail
import com.barryzeha.data.repository.MainRepository
import com.barryzeha.ktmusicplayer.MyApp
import com.barryzeha.ktmusicplayer.common.NOTIFICATION_ID
import com.barryzeha.ktmusicplayer.common.cancelPersistentNotify
import com.barryzeha.ktmusicplayer.common.notificationMediaPlayer
import com.barryzeha.ktmusicplayer.utils.BassManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
class MusicPlayerService : Service(),BassManager.PlaybackManager{


    @Inject
    lateinit var repository: MainRepository
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
    val songController: ServiceSongListener get() = _songController!!
    private var isForegroundService = false
    private var currentMusicState = MusicState()
    private var songRunnable: Runnable = Runnable {}
    private var songHandler: Handler = Handler(Looper.getMainLooper())
    private var executeOnceTime:Boolean=false
    private var musicState:MusicState?=null
    private lateinit var mPrefs:MyPreferences
    private var songEntity:SongEntity=SongEntity()

    private var songState:List<SongStateWithDetail> = arrayListOf()
    private var headsetReceiver:BroadcastReceiver?=null
    private var bluetoothReceiver:BroadcastReceiver?=null
    private var bluetoothIsConnect:Boolean = false
    private var nextOrPrevAnimValue=-1
    private var listIsShuffled:Boolean=false

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
                        if(mPrefs.isPlaying) {
                            mPrefs.isPlaying=false
                            bassManager?.channelPause()
                            _songController?.pause()
                            _songController?.musicState(currentMusicState.copy(isPlaying = mPrefs.isPlaying))
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
                            if (mPrefs.isPlaying) {
                                mPrefs.isPlaying = false
                                bassManager?.channelPause()
                                _songController?.pause()
                                _songController?.musicState(currentMusicState.copy(isPlaying = false))
                            }
                        }
                        BluetoothAdapter.ACTION_STATE_CHANGED -> {
                            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                            when (state) {

                                BluetoothAdapter.STATE_OFF -> {
                                    Log.d("BLUETOOTH_STATE", "Bluetooth disconnected")
                                    // Logica cuando se desconecta el Bluetooth
                                    if (mPrefs.isPlaying) {
                                        mPrefs.isPlaying = false
                                        bassManager?.channelPause()
                                        _songController?.pause()
                                        _songController?.musicState(currentMusicState.copy(isPlaying = false))
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

    @OptIn(UnstableApi::class)
    private fun setUpRepository(){
        CoroutineScope(Dispatchers.Main).launch {

            // TODO Revisar, no cargar toda la lista antes del estado de la canción
            withContext(Dispatchers.IO) {
                // Para cargar por primera vez la lista de canciones de acuerdo al filtro guardado
                // si no hay algo seleccionado previamente solo devolverá la lista por defecto
                val songs=repository.fetchAllSongsBy(mPrefs.playListSortOption)
                songState=repository.fetchSongState()

                songs.forEach { s ->
                    //if (!songsList.contains(s)) {
                        songsList.add(s)
                    //}
                }
                // Comprobamos si la opción shuffle está activada al iniciar el servicio
                if(mPrefs.songMode == SHUFFLE){
                    shuffleList()
                }
            }
            if(!songState.isNullOrEmpty()) {
                val songEntity=songState[0].songEntity
                if(songsList.contains(songEntity))setMusicStateSaved(songState[0])
            }

        }
    }
    private fun initMusicStateLooper(){
        initNotify()
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
                mPrefs.isPlaying=false
                if(_songController !=null)_songController?.pause()
                else pausePlayer()
                checkIfPhoneIsLock()
            }

            override fun onPlay() {
                super.onPlay()
                if(_songController !=null)_songController?.play()
                else play(null)
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
                    mPrefs.isPlaying=false
                    pausePlayer()
                    CoroutineScope(Dispatchers.IO).launch {
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
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        musicState = intent?.getParcelableExtra<MusicState>("musicState")
        when (SongAction.values()[intent?.action?.toInt() ?: SongAction.Nothing.ordinal]) {
            SongAction.Pause -> {
                mPrefs.isPlaying=false
                if(_songController != null) _songController?.pause()
                else pausePlayer()
                checkIfPhoneIsLock()
            }
            SongAction.Resume -> {
                if(_songController !=null)_songController?.play()
                else play(null)
                checkIfPhoneIsLock()
            }
            SongAction.Stop -> {
                _songController?.stop()

            }
            SongAction.Next -> {
                //TODO trying probar en android 8
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
            val updateMediaMetadata = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, newState.title)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, newState.album)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, newState.artist)
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, newState.albumArt)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, newState.duration)
                .build()

            // Para android >=12
            if(mediaMetadata != updateMediaMetadata) mediaSession.setMetadata(updateMediaMetadata)

            notificationManager.notify(
                NOTIFICATION_ID,
                mediaPlayerNotify
            )
        }

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
            // Volvemos a obtener la posición de la pista en
            // la nueva lista (importante si se ha ordenado la lista por artista , album, favoritos, etc)
            findItemSongIndexById(mPrefs.idSong)?.let {
                indexOfSong = it
            }?:run{
                indexOfSong = 0
            }
            if(mPrefs.songMode == SHUFFLE)shuffleList()
            mPrefs.currentIndexSong = indexOfSong.toLong()
        }
    }
    fun clearPlayList(isSort:Boolean){
        songsList.clear()
        // Cuando no es para ordenar la lista será para eliminar
        if(!isSort){
            mPrefs.isPlaying=false
            _songController?.currentTrack(MusicState())
            bassManager?.setSongStateSaved(0,0)
            bassManager?.clearBassChannel()
        }else{
            _songController?.currentTrack(currentMusicState)
        }
        //bassManager?.clearBassChannel()
        executeOnceTime=false
    }
    fun unregisterController(){
        _songController=null
    }
    private fun checkIfPhoneIsLock(){
        if(_songController==null){
            mPrefs.nextOrPrevFromNotify=true
            mPrefs.controlFromNotify = true
        }
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
            if (songsList.isNotEmpty()) {
                song?.let {
                    songEntity = it
                    currentSongPosition = 0
                    bassManager?.streamCreateFile(song)
                    findItemSongIndexById(song.id)?.let { pos -> indexOfSong = pos }
                    executeOnceTime = true
                } ?: run {
                    bassManager?.streamCreateFile(songEntity)
                    executeOnceTime = false
                }
                if (bassManager?.getActiveChannel() != 0) {
                    bassManager?.channelPlay(currentSongPosition)
                    bassManager?.startCheckingPlayback()

                    mPrefs.isPlaying = true
                    mPrefs.idSong = songEntity.id
                    currentMusicState = fetchSong(songEntity)?.copy(
                        isPlaying = mPrefs.isPlaying,
                        idSong = songEntity.id,
                        latestPlayed = false,
                        nextOrPrev = nextOrPrevAnimValue
                    )!!

                    EqualizerManager.applyEqualizer(bassManager?.getActiveChannel()!!, effectsPrefs)

                } else {
                    _activity?.showSnackBar(
                        _activity?.findViewById(android.R.id.content)!!,
                        "Can't player this file, will be deleted or error format",
                        Snackbar.LENGTH_LONG
                    )
                }
                song?.let {
                    if (executeOnceTime) _songController?.currentTrack(currentMusicState)

                }
            }
    }
    fun pausePlayer(){
        mPrefs.isPlaying = false
        currentSongPosition=bassManager?.getCurrentPositionInSeconds(bassManager?.getActiveChannel()!!)?:0
        bassManager?.channelPause()
        bassManager?.stopCheckingPlayback()
    }

    fun getSessionOrChannelId(): Int {
        return bassManager?.getActiveChannel()!!
    }
    fun resumePlayer(){
        play(null)

    }
    fun nextSong(){
        if(songsList.isNotEmpty()){
            if(indexOfSong < songsList.size - 1 ) {
                /*if(mPrefs.songMode == SHUFFLE)indexOfSong = (songsList.indices).random()
                else indexOfSong  += 1*/
                indexOfSong +=1
            }else{
               /* if(mPrefs.songMode == SHUFFLE){indexOfSong = (songsList.indices).random()}
                else indexOfSong = 0*/
                indexOfSong = 0
            }
            nextOrPrevAnimValue = NEXT
            setOrPlaySong(indexOfSong, NEXT)
            checkIfPhoneIsLock()
            mPrefs.currentIndexSong = indexOfSong.toLong()

        }

    }
    fun prevSong(){
        if(songsList.isNotEmpty()){
            if(indexOfSong > 0) {
                /*if(mPrefs.songMode == SHUFFLE)indexOfSong = (songsList.indices).random()
                else indexOfSong -=1*/
                indexOfSong -=1
                nextOrPrevAnimValue = PREVIOUS
                setOrPlaySong(indexOfSong, PREVIOUS)
                checkIfPhoneIsLock()
                mPrefs.currentIndexSong = indexOfSong.toLong()

            }

        }
    }
    // Probamos nueva forma de implementar shuffle
    fun shuffleList(){
        //CoroutineScope(Dispatchers.IO).launch {
            //if(!listIsShuffled) {
                songsList.shuffle()
                findItemSongIndexById(mPrefs.idSong)?.let {
                    indexOfSong = it
                } ?: run {
                    indexOfSong = 0
                }

            //}

        //}
        listIsShuffled = true
    }
    // Reordenamos la lista nuevamente a su forma original
    fun sortList(){
        if(listIsShuffled) {
            CoroutineScope(Dispatchers.IO).launch {
                songsList.clear()
                songsList = repository.fetchAllSongsBy(mPrefs.playListSortOption).toMutableList()
                findItemSongIndexById(mPrefs.idSong)?.let {
                    indexOfSong = it
                } ?: run {
                    indexOfSong = 0
                }
            }
        }
        listIsShuffled = false
    }
    fun getSongsList():List<SongEntity>{
        return songsList
    }
    private fun setOrPlaySong(indexOfSong:Int,animDirection:Int= DEFAULT_DIRECTION){
        if (mPrefs.isPlaying)play(songsList[indexOfSong])
        else setMusicForPlayer(songsList[indexOfSong], animDirection)

    }
    private fun setMusicForPlayer(song: SongEntity, animDirection:Int= DEFAULT_DIRECTION){
        val songState = SongStateWithDetail(SongState(currentPosition = 0),song)
        mPrefs.idSong = song.id
        setMusicStateSaved(songState, animDirection)

    }
    fun setPlayerProgress(progress:Long){
       bassManager?.setChannelProgress(progress){currentSongPosition=it}
    }

    private fun setMusicStateSaved(songState: SongStateWithDetail,animDirection:Int= DEFAULT_DIRECTION){

           val song = songState.songEntity
           songEntity = song
           // Set info currentSongEntity
           fetchSong(song)?.let { musicState ->
               currentMusicState = musicState.copy(
                   currentDuration = songState.songState.currentPosition,
                   latestPlayed = true,
                   nextOrPrev = animDirection
               )

           }
           mPrefs.isPlaying = false
           currentSongPosition = songState.songState.currentPosition
           bassManager?.streamCreateFile(songState.songEntity)
           bassManager?.setSongStateSaved(
               bassManager?.getActiveChannel()!!,
               songState.songState.currentPosition
           )
           findItemSongIndexById(songState.songEntity.id)?.let {
               indexOfSong = it
           }
           _songController?.currentTrack(currentMusicState)
           // Al cargar la información de una pista guardada
           // se ejecutaba una primera vez el evento currentTRack de la interface
           // ya que el listener la ejecutaba una vez más debemos poner executeOnceTime = true
           // para evitarlo
           executeOnceTime = true
    }
    private fun fetchSong(song:SongEntity):MusicState?{

        try {
        val songPath = song.pathLocation.toString()
        val songMetadata = getSongMetadata(applicationContext!!, songPath, isForNotify = true)!!
        return MusicState(
            idSong = song.id,
            isPlaying = mPrefs.isPlaying,
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
    override fun onFinishPlayback() {
        if(indexOfSong<songsList.size -1 && songsList.isNotEmpty()){
            when(mPrefs.songMode){
                REPEAT_ONE-> {
                    if (mPrefs.isPlaying) {
                        bassManager?.repeatSong()
                    }
                }
                //TODO probamos nueva forma de barajar la lista comentamos por ahora
               /* SHUFFLE->{
                    indexOfSong = (songsList.indices).random()
                    play(songsList[indexOfSong])
                }*/
                else->{
                    if(mPrefs.isPlaying)nextSong()
                }
            }

        }else{
            when(mPrefs.songMode){
                REPEAT_ALL->{ play(songsList[0])}
                else->{
                    //Todo revisar el cambio de canción y mejorar
                    //if(indexOfSong  songsList.size) {
                    if (songsList.isNotEmpty()) setMusicForPlayer(songsList[0])
                    bassManager?.stopCheckingPlayback()
                    //}
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