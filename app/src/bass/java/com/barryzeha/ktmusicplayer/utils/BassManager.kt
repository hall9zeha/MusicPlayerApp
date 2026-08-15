package com.barryzeha.ktmusicplayer.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.barryzeha.audioeffects.common.EffectsPreferences
import com.barryzeha.core.R
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.ktmusicplayer.MyApp
import com.un4seen.bass.BASS
import com.un4seen.bass.BASS.BASS_ACTIVE_PAUSED
import com.un4seen.bass.BASS.BASS_ACTIVE_PLAYING
import com.un4seen.bass.BASS.BASS_ACTIVE_STOPPED
import com.un4seen.bass.BASS.BASS_ChannelSetSync
import com.un4seen.bass.BASS.BASS_INFO
import com.un4seen.bass.BASS.BASS_SYNC_MIXTIME
import com.un4seen.bass.BASS.BASS_SYNC_POS
import java.io.File
import java.util.Timer


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 12/9/24.
 * Copyright (c)  All rights reserved.
 **/
private const val SAMPLE44 = 44100
private const val SAMPLE48 = 48000
private const val SAMPLE96 = 96000
private const val SAMPLE192 = 192000
private const val TAG = "BASS-MANAGER"

private var updateTimer: Timer? = null
private var idSong:Long?=null
private var lastState = -1
private var lastChannel = -1

class BassManager {
    private var mainChannel:Int?=0
    private var abSyncHandle: Int = 0
    private var abLoopEnabled=false
    //
    private var playbackThread: HandlerThread? = null
    private var playbackHandler: Handler? = null

    private var abThread: HandlerThread? = null
    private var abHandler: Handler? = null
    //
    private var checkRunnable: Runnable? = null
    private  var playbackManager:PlaybackManager?=null
    private var _effectPrefs: EffectsPreferences?=null

    companion object {
        // For A-B looper
        private var startAbLoopPosition:Long=0
        private var endAbLopPosition:Long=0
        @SuppressLint("StaticFieldLeak")
        private var context: Context?=null
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: BassManager? = null

        fun getInstance(effectPrefs: EffectsPreferences):BassManager{
            return instance ?: synchronized(this) {
                instance ?: BassManager().apply {
                    _effectPrefs = effectPrefs
                    initializeBass()
                }.also { instance = it }
            }
        }
    }
    private fun initializeBass(){
        initThreads()
        context= MyApp.context
        configure()
        if (!BASS.BASS_Init(-1, SAMPLE192, BASS.BASS_DEVICE_FREQ)) {
            Log.i(TAG, "Can't initialize device")
            Log.i(TAG, "init with sample " + SAMPLE96 + "Hz")
            if (!BASS.BASS_Init(-1, SAMPLE96, BASS.BASS_DEVICE_FREQ)) {
                Log.i(TAG, "Can't initialize device")
                Log.i(TAG, "init with sample " + SAMPLE48 + "Hz")
                if (!BASS.BASS_Init(-1, SAMPLE48, BASS.BASS_DEVICE_FREQ)) {
                    Log.i(TAG, "Can't initialize device")
                    Log.i(TAG, "init with sample " + SAMPLE44 + "Hz")
                    if (!BASS.BASS_Init(-1, SAMPLE44, BASS.BASS_DEVICE_FREQ)) {
                        Log.i(TAG, "Can't initialize device")
                    }
                }
            }
        }
        val info = BASS_INFO()
        if (BASS.BASS_GetInfo(info)) {
            Log.i(TAG, "Min Buffer :" + info.minbuf)
            Log.i(TAG, "Direct Sound Ver :" + info.dsver)
            Log.i(TAG, "Latency :" + info.latency)
            Log.i(TAG, "speakers :" + info.speakers)
            Log.i(TAG, "freq :" + info.freq)
        }

        val nativeDir =MyApp.context.applicationInfo.nativeLibraryDir
        val pluginsList = File(nativeDir).list { dir, name -> name.matches("libbass.+\\.so|libtags\\.so".toRegex()) }
        pluginsList?.forEach { plugin->
            BASS.BASS_PluginLoad(plugin,0)
        }
    }
    private fun initThreads(){
        playbackThread = HandlerThread("BassPlaybackThread").apply { start() }
        playbackHandler = Handler(playbackThread!!.looper)
        abThread = HandlerThread("BassABThread").apply { start() }
        abHandler = Handler(abThread!!.looper)
    }
    private fun configure(){
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_FLOATDSP, 1)
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_PERIOD, 10)
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_BUFFER, 80)
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_SRC, 3)
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_SRC_SAMPLE, 3)
    }
    fun startCheckingPlayback(){
        if(checkRunnable != null) return
        stopRunnable()
        checkRunnable = object:Runnable{
            override fun run() {
                val currentState =  BASS.BASS_ChannelIsActive(getActiveChannel())
                val currentChannel = getActiveChannel()
                if(currentChannel !=0) {
                    playbackManager?.onPlaybackState()
                }
                if(currentState != lastState) {
                    when (currentState) {
                        BASS_ACTIVE_PLAYING -> {
                            playbackManager?.onPlayingChanged()
                        }
                        BASS_ACTIVE_PAUSED -> {
                            playbackManager?.onPlayingChanged()
                        }
                        BASS_ACTIVE_STOPPED -> {
                            // Para que no se active con cada cambio de canción, sino solo cuando se detiene la reproducción y termina una pista
                            // debemos remover el evento de finalización de la pista momentaneamente al cambiar de canción y volver a suscribirnos cuando la siguiente pista esté lista.
                            // El evento solo debe iniciarse si la pista está reproduciendose, y detenerse al cambiar de canción o al pausar la reproducción.
                            // De esta manera, evitamos que se active el evento de finalización al cambiar de canción o al pausar, y solo se activa cuando la pista realmente termina de reproducirse.
                            if(lastState == BASS_ACTIVE_PLAYING) {
                                playbackManager?.onPlaybackFinished()
                            }
                        }
                    }
                    lastState=currentState
                }
                if(currentChannel != lastChannel){
                    playbackManager?.onPlayingChanged()
                    lastChannel = currentChannel
                }
                playbackHandler?.postDelayed(this,500)
            }
        }
        playbackHandler?.post(checkRunnable!!)
    }
    fun stopCheckingPlayback(){
        stopRunnable()
    }
    fun unregisterPlaybackState(){
        playbackManager=null
    }
    fun registerPlaybackState(mPlaybackManager: PlaybackManager){
        playbackManager = mPlaybackManager
    }
    private fun stopRunnable(){
        checkRunnable?.let{
            playbackHandler?.removeCallbacks(it)
            checkRunnable = null
        }
    }
    fun setSongStateSaved(channel:Int, position:Long){
        mainChannel = channel
        val positionBytes = getCurrentPositionToBytes(position)
        BASS.BASS_ChannelSetPosition(channel, positionBytes, BASS.BASS_POS_BYTE)
    }
    fun streamCreateFile(song:SongEntity){
        // Cleaning a previous track if have anyone
        BASS.BASS_StreamFree(getActiveChannel())
        // Creating the new channel for playing
        mainChannel = BASS.BASS_StreamCreateFile(song.pathLocation, 0, 0, BASS.BASS_SAMPLE_FLOAT)

    }

    fun channelPlay(currentSongProgress:Long){
        val volumeValue = if(_effectPrefs?.effectsIsEnabled!!) _effectPrefs?.getVolumeSeekBandValue(_effectPrefs?.effectType!!, R.id.volume)?.div(15f) else 1f
        BASS.BASS_ChannelSetAttribute(getActiveChannel(),BASS.BASS_ATTRIB_VOL,1F)
        // Convert the current position (in milliseconds) to bytes with  bassManager?.getCurrentPositionToBytes
        BASS.BASS_ChannelSetPosition(getActiveChannel(),getCurrentPositionToBytes(currentSongProgress),BASS.BASS_POS_BYTE)
        BASS.BASS_ChannelPlay(getActiveChannel()!!, false)
        if (abLoopEnabled) {
            restoreABLoopSyncOnly()
        }
    }
    fun channelPause(){
        BASS.BASS_ChannelPause(getActiveChannel())
    }
    fun channelStop(){
        BASS.BASS_ChannelStop(getActiveChannel())
    }
    fun fastForwardOrRewind(isForward:Boolean,currentProgress: (Long) -> Unit){
        val progressOnSeconds = getCurrentPositionInSeconds()
        val forwardProgress = if(isForward)progressOnSeconds + 2000 else progressOnSeconds - 2000
        setChannelProgress(forwardProgress){currentProgress(it)}

    }
    fun setChannelProgress(progress:Long, currentProgress:(Long)->Unit){
        var finalProgress = progress
        // Para el bucle A-B, si el progreso se sale del rango establecido, lo ajustamos al inicio del bucle
        if(abLoopEnabled){
            if(progress !in startAbLoopPosition..endAbLopPosition){
                finalProgress = startAbLoopPosition
            }
        }
        val progressBytes = BASS.BASS_ChannelSeconds2Bytes(getActiveChannel(), finalProgress / 1000.0)

        BASS.BASS_ChannelSetPosition(getActiveChannel(), progressBytes, BASS.BASS_POS_BYTE)
        currentProgress(progress)
        // Retraso en milisegundos para evitar los chirridos al desplazarse en el seekbar
        //Enviamos el progreso al playbackManager para actualizar el seekbar en la notificación multimedia
        playbackManager?.onPlaybackProgress(progress)
    }
    fun repeatSong(){
        BASS.BASS_ChannelPlay(getActiveChannel(), true)
    }
    private fun getCurrentPositionToBytes(position: Long):Long{
        return if(mainChannel!=null)BASS.BASS_ChannelSeconds2Bytes(mainChannel!!, position / 1000.0)else 0L
    }
    fun setActiveChannel(channel:Int){
        mainChannel=channel
    }
    fun setAbLoopStar(){
        startAbLoopPosition = getCurrentPositionInSeconds()
    }
    fun setAbLoopEnd(){
        endAbLopPosition = getCurrentPositionInSeconds()
        startAbLoop()
        abLoopEnabled = true
    }
    private fun startAbLoop(){
        if (abSyncHandle != 0) {
            BASS.BASS_ChannelRemoveSync(getActiveChannel(), abSyncHandle)
            abSyncHandle = 0
        }
        BASS.BASS_ChannelSetPosition(getActiveChannel(),getCurrentPositionToBytes(startAbLoopPosition),BASS.BASS_POS_BYTE)
        val endBytes = getCurrentPositionToBytes(endAbLopPosition)
        abSyncHandle=BASS_ChannelSetSync(
            getActiveChannel(),
            BASS_SYNC_POS,
            endBytes,
            {_,_,_,_->
                val startBytes= getCurrentPositionToBytes(startAbLoopPosition)
                BASS.BASS_ChannelSetPosition(getActiveChannel(),startBytes,BASS.BASS_POS_BYTE)
            },
            BASS_SYNC_MIXTIME
        )
    }
    // Para reactivar A-B loop después de pausar la pista, si es que A-B loop está habilitado
    private fun restoreABLoopSyncOnly(){
        val channel = getActiveChannel()
        if(channel==0) return
        if (abSyncHandle != 0) {
            BASS.BASS_ChannelRemoveSync(getActiveChannel(), abSyncHandle)
            abSyncHandle = 0
        }
        val startBytes = getCurrentPositionToBytes(startAbLoopPosition)
        val endBytes = getCurrentPositionToBytes(endAbLopPosition)
        val current = getBytesPosition(channel)
        if (current < startBytes || current > endBytes) {
            BASS.BASS_ChannelSetPosition(channel, startBytes, BASS.BASS_POS_BYTE)
        }
        abSyncHandle = BASS_ChannelSetSync(
            channel,
            BASS_SYNC_POS,
            endBytes,
            { _, _, _, _ ->

                BASS.BASS_ChannelSetPosition(channel, startBytes, BASS.BASS_POS_BYTE)
            },
            BASS_SYNC_MIXTIME
        )
    }
    fun stopAbLoop(){
        val channel = getActiveChannel()
        if (channel != 0 && abSyncHandle != 0) {
            BASS.BASS_ChannelRemoveSync(channel, abSyncHandle)
            abSyncHandle = 0
        }
        abLoopEnabled = false
    }

    fun getActiveChannel():Int{
        return mainChannel?:0
    }
    fun getCurrentPositionInSeconds(): Long {
        return if(getActiveChannel() !=0)BASS.BASS_ChannelBytes2Seconds(getActiveChannel(), getBytesPosition(getActiveChannel())).toLong() * 1000 else 0
    }

    fun getDuration(channel: Int): Long {
        return if(getActiveChannel()!=0)BASS.BASS_ChannelBytes2Seconds(channel, getBytesTotal(channel)).toLong() * 1000 else 0
    }

    private fun getBytesPosition(channel:Int): Long {
        return BASS.BASS_ChannelGetPosition(channel, BASS.BASS_POS_BYTE)
    }
    private fun getBytesTotal(channel: Int): Long {
        return BASS.BASS_ChannelGetLength(channel, BASS.BASS_POS_BYTE)
    }
    fun releasePlayback(){
        stopAbLoop()
        stopRunnable()
        BASS.BASS_ChannelStop(getActiveChannel())
        BASS.BASS_StreamFree(getActiveChannel())
        BASS.BASS_PluginFree(0)
        BASS.BASS_Free()
        unregisterPlaybackState()

        playbackThread?.quitSafely()
        abThread?.quitSafely()
        playbackThread = null
        abThread = null
        playbackHandler = null
        abHandler = null

        instance=null
    }

    fun clearBassChannel() {
        BASS.BASS_StreamFree(getActiveChannel())
        BASS.BASS_ChannelSetPosition( getActiveChannel(),getCurrentPositionToBytes(0),BASS.BASS_POS_BYTE)
        mainChannel = null
    }

    fun getBassErrorMessage(code: Int): String {
        return context?.let {ctx->
        when (code) {
                1 -> ctx.getString(R.string.BASS_ERROR_MEM)
                2 -> ctx.getString(R.string.BASS_ERROR_FILEOPEN)
                3 -> ctx.getString(R.string.BASS_ERROR_DRIVER)
                4 -> ctx.getString(R.string.BASS_ERROR_BUFLOST)
                5 -> ctx.getString(R.string.BASS_ERROR_HANDLE)
                6 -> ctx.getString(R.string.BASS_ERROR_FORMAT)
                7 -> ctx.getString(R.string.BASS_ERROR_POSITION)
                8 -> ctx.getString(R.string.BASS_ERROR_INIT)
                9 -> ctx.getString(R.string.BASS_ERROR_START)
                14 -> ctx.getString(R.string.BASS_ERROR_ALREADY)
                17 -> ctx.getString(R.string.BASS_ERROR_NOTAUDIO)
                18 -> ctx.getString(R.string.BASS_ERROR_NOCHAN)
                19 -> ctx.getString(R.string.BASS_ERROR_ILLTYPE)
                20 -> ctx.getString(R.string.BASS_ERROR_ILLPARAM)
                21 -> ctx.getString(R.string.BASS_ERROR_NO3D)
                22 -> ctx.getString(R.string.BASS_ERROR_NOEAX)
                23 -> ctx.getString(R.string.BASS_ERROR_DEVICE)
                24 -> ctx.getString(R.string.BASS_ERROR_NOPLAY)
                25 -> ctx.getString(R.string.BASS_ERROR_FREQ)
                27 -> ctx.getString(R.string.BASS_ERROR_NOTFILE)
                29 -> ctx.getString(R.string.BASS_ERROR_NOHW)
                31 -> ctx.getString(R.string.BASS_ERROR_EMPTY)
                32 -> ctx.getString(R.string.BASS_ERROR_NONET)
                33 -> ctx.getString(R.string.BASS_ERROR_CREATE)
                34 -> ctx.getString(R.string.BASS_ERROR_NOFX)
                37 -> ctx.getString(R.string.BASS_ERROR_NOTAVAIL)
                38 -> ctx.getString(R.string.BASS_ERROR_DECODE)
                39 -> ctx.getString(R.string.BASS_ERROR_DX)
                40 -> ctx.getString(R.string.BASS_ERROR_TIMEOUT)
                41 -> ctx.getString(R.string.BASS_ERROR_FILEFORM)
                42 -> ctx.getString(R.string.BASS_ERROR_SPEAKER)
                43 -> ctx.getString(R.string.BASS_ERROR_VERSION)
                44 -> ctx.getString(R.string.BASS_ERROR_CODEC)
                45 -> ctx.getString(R.string.BASS_ERROR_ENDED)
                46 -> ctx.getString(R.string.BASS_ERROR_BUSY)
                47 -> ctx.getString(R.string.BASS_ERROR_UNSTREAMABLE)
                48 -> ctx.getString(R.string.BASS_ERROR_PROTOCOL)
                49 -> ctx.getString(R.string.BASS_ERROR_DENIED)
                50 -> ctx.getString(R.string.BASS_ERROR_FREEING)
                51 -> ctx.getString(R.string.BASS_ERROR_CANCEL)
                500 -> ctx.getString(R.string.BASS_ERROR_JAVA_CLASS)

                else -> ctx.getString(R.string.UNKNOWN_ERROR)
            }
        }?:run{
            "Unknown error"
        }
    }
    interface PlaybackManager{
        fun onPlaybackState()
        fun onPlayingChanged()
        fun onPlaybackProgress(progress:Long)
        fun onPlaybackFinished()
    }
}