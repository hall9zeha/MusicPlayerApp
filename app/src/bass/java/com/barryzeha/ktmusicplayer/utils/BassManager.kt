package com.barryzeha.ktmusicplayer.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.ktmusicplayer.MyApp
import com.un4seen.bass.BASS
import com.un4seen.bass.BASS.BASS_INFO
import java.io.File
import java.util.Timer
import java.util.TimerTask


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
private var mainChannel:Int?=0
private val handler = Handler(Looper.getMainLooper())
private var checkRunnable: Runnable? = null
private var updateTimer: Timer? = null
private var idSong:Long?=null

open class BassManager {

    private var instance: BassManager? = null
    private  lateinit var playbackManager:PlaybackManager
    init {
        BASS.BASS_Init(-1, 44100, 0)
    }
     fun getInstance(playbackManager: PlaybackManager):BassManager?{
        instance?.let{ return it}?:run{

            instance=BassManager()
            this.playbackManager = playbackManager
            if (!BASS.BASS_Init(-1, SAMPLE192, BASS.BASS_DEVICE_FREQ)) {
                Log.i(TAG, "Can't initialize device");
                Log.i(TAG, "init with sample " + SAMPLE96 + "Hz");
                if (!BASS.BASS_Init(-1, SAMPLE96, BASS.BASS_DEVICE_FREQ)) {
                    Log.i(TAG, "Can't initialize device");
                    Log.i(TAG, "init with sample " + SAMPLE48 + "Hz");
                    if (!BASS.BASS_Init(-1, SAMPLE48, BASS.BASS_DEVICE_FREQ)) {
                        Log.i(TAG, "Can't initialize device");
                        Log.i(TAG, "init with sample " + SAMPLE44 + "Hz");
                        if (!BASS.BASS_Init(-1, SAMPLE44, BASS.BASS_DEVICE_FREQ)) {
                            Log.i(TAG, "Can't initialize device");
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
        }
        configure()
        val nativeDir =MyApp.context.applicationInfo.nativeLibraryDir
        val pluginsList = File(nativeDir).list { dir, name -> name.matches("libbass.+\\.so|libtags\\.so".toRegex()) }
        pluginsList?.forEach { plugin->
             BASS.BASS_PluginLoad(plugin,0)
            Log.e("NATIVE-LIB", plugin )
         }
         Log.e("NATIVE-LIB", pluginsList.size.toString())
         Log.e("NATIVE-LIB", nativeDir)
        return instance
    }
    private fun configure(){
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_FLOATDSP, 1);
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_BUFFER, 10);
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_SRC, 3);
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_SRC_SAMPLE, 3);
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
    private fun stopRunnable(){
        checkRunnable?.let{
            handler.removeCallbacks(it)
            checkRunnable = null
        }
    }
    fun setSongStateSaved(channel:Int, position:Long){
        mainChannel = channel
        val positionBytes = getCurrentPositionToBytes(position)
        BASS.BASS_ChannelSetPosition(channel, positionBytes, BASS.BASS_POS_BYTE);
    }
    fun streamCreateFile(song:SongEntity){
        // Cleaning a previous track if have anyone
        BASS.BASS_StreamFree(getActiveChannel())
        // Creating the new channel for playing
        mainChannel = BASS.BASS_StreamCreateFile(song.pathLocation, 0, 0, BASS.BASS_SAMPLE_FLOAT)
    }
    fun repeatSong(){
        BASS.BASS_ChannelPlay(getActiveChannel(), true);
    }
    fun channelPlay(currentSongPosition:Long){
        BASS.BASS_ChannelSetAttribute(getActiveChannel(),BASS.BASS_ATTRIB_VOL,1F)
        // Convertir la posición actual (en milisegundos) a bytes con bassManager?.getCurrentPositionToBytes
        BASS.BASS_ChannelSetPosition(getActiveChannel(),getCurrentPositionToBytes(currentSongPosition),BASS.BASS_POS_BYTE)
        BASS.BASS_ChannelPlay(getActiveChannel()!!, false)
    }
    fun channelPause(){
        BASS.BASS_ChannelPause(getActiveChannel())
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
    fun getCurrentPositionToBytes(position: Long):Long{
        return if(mainChannel!=null)BASS.BASS_ChannelSeconds2Bytes(mainChannel!!, position / 1000.0)else 0L
    }
    fun setActiveChannel(channel:Int){
        mainChannel=channel
    }
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

    interface PlaybackManager{
        fun onFinishPlayback()
    }
}