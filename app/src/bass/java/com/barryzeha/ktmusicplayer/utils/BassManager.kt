package com.barryzeha.ktmusicplayer.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.barryzeha.core.R
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.ktmusicplayer.MyApp
import com.un4seen.bass.BASS
import com.un4seen.bass.BASS.BASS_INFO
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

class BassManager {
    private var mainChannel:Int?=0
    private val handler = Handler(Looper.getMainLooper())
    private val aBLoopHandler = Handler(Looper.getMainLooper())
    private var checkRunnable: Runnable? = null
    private  var playbackManager:PlaybackManager?=null
    companion object {
        // For A-B looper
        private var startAbLoopPosition:Long=0
        private var endAbLopPosition:Long=0
        @SuppressLint("StaticFieldLeak")
        private var context: Context?=null
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: BassManager? = null

        fun getInstance():BassManager{
            return instance ?: synchronized(this) {
                instance ?: BassManager().apply {
                    initializeBass()
                }.also { instance = it }
            }
        }

    }
    private fun initializeBass(){
        context= MyApp.context
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
        configure()
        val nativeDir =MyApp.context.applicationInfo.nativeLibraryDir
        val pluginsList = File(nativeDir).list { dir, name -> name.matches("libbass.+\\.so|libtags\\.so".toRegex()) }
        pluginsList?.forEach { plugin->
            BASS.BASS_PluginLoad(plugin,0)
        }
    }
    private fun configure(){
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_FLOATDSP, 1)
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_BUFFER, 10)
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_SRC, 3)
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_SRC_SAMPLE, 3)
    }
    fun startCheckingPlayback(){
        stopRunnable()
        checkRunnable = object:Runnable{
            override fun run() {
                if (BASS.BASS_ChannelIsActive(getActiveChannel()) == BASS.BASS_ACTIVE_STOPPED) {
                    playbackManager?.onFinishPlayback()
                }
                handler.postDelayed(this,500)
            }
        }
        handler.post(checkRunnable!!)
    }
    fun stopCheckingPlayback(){
        stopRunnable()
    }
    fun unregisterOnFinishPlayback(){
        playbackManager=null
    }
    fun registerOnFinishPlayback(mPlaybackManager: PlaybackManager){
        playbackManager = mPlaybackManager
    }
    private fun stopRunnable(){
        checkRunnable?.let{
            handler.removeCallbacks(it)
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
        BASS.BASS_ChannelSetAttribute(getActiveChannel(),BASS.BASS_ATTRIB_VOL,1F)
        // Convert the current position (in milliseconds) to bytes with  bassManager?.getCurrentPositionToBytes
        BASS.BASS_ChannelSetPosition(getActiveChannel(),getCurrentPositionToBytes(currentSongProgress),BASS.BASS_POS_BYTE)
        BASS.BASS_ChannelPlay(getActiveChannel()!!, false)
    }
    fun channelPause(){
        BASS.BASS_ChannelPause(getActiveChannel())
    }
    fun channelStop(){
        BASS.BASS_ChannelStop(getActiveChannel())
    }
    fun fastForwardOrRewind(isForward:Boolean,currentProgress: (Long) -> Unit){
        val progressOnSeconds = getCurrentPositionInSeconds(getActiveChannel())
        val forwardProgress = if(isForward)progressOnSeconds + 2000 else progressOnSeconds - 2000
        setChannelProgress(forwardProgress){currentProgress(it)}

    }

    fun setChannelProgress(progress:Long, currentProgress:(Long)->Unit){

        val progressBytes = BASS.BASS_ChannelSeconds2Bytes(getActiveChannel(), progress / 1000.0)
        /*updateTimer?.cancel()
        updateTimer = Timer()
        updateTimer?.schedule(object : TimerTask() {
            override fun run() {*/
                // Ajusta la posición del canal
        BASS.BASS_ChannelSetPosition(getActiveChannel(), progressBytes, BASS.BASS_POS_BYTE)
        currentProgress(progress)
          /*  }
        }, 100) */// Retraso en milisegundos para evitar los chirridos al desplazarse en el seekbar

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
        startAbLoopPosition = getCurrentPositionInSeconds(getActiveChannel())
    }
    fun setAbLoopEnd(){
        endAbLopPosition = getCurrentPositionInSeconds(getActiveChannel())
        startAbLoop()
    }
    private fun startAbLoop(){
        val currentPosition = getCurrentPositionInSeconds(getActiveChannel())
        if(currentPosition >= endAbLopPosition){
            BASS.BASS_ChannelSetPosition(getActiveChannel(),getCurrentPositionToBytes(startAbLoopPosition),BASS.BASS_POS_BYTE)
        }
        aBLoopHandler.postDelayed({
            startAbLoop()
        },500)

    }
    fun stopAbLoop() = aBLoopHandler.removeCallbacksAndMessages(null)

    fun getActiveChannel():Int{
        return mainChannel?:0
    }
    fun getCurrentPositionInSeconds(channel: Int): Long {
        return if(getActiveChannel() !=0)BASS.BASS_ChannelBytes2Seconds(channel, getBytesPosition(channel)).toLong() * 1000 else 0
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
        BASS.BASS_ChannelStop(getActiveChannel())
        BASS.BASS_PluginFree(0)
        BASS.BASS_Free()
        stopRunnable()
        instance=null
    }

    fun clearBassChannel() {
        mainChannel = null
        BASS.BASS_StreamFree(getActiveChannel())
        BASS.BASS_ChannelSetPosition( getActiveChannel(),getCurrentPositionToBytes(0),BASS.BASS_POS_BYTE)
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
        fun onFinishPlayback()
    }
}