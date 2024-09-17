package com.barryzeha.ktmusicplayer.utils

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.un4seen.bass.BASS
import com.un4seen.bass.BASS.BASS_INFO


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
        return instance
    }
    private fun configure(){

    }
    fun startCheckingPlayback(){
        stopRunnable()
        checkRunnable = object:Runnable{
            override fun run() {
                if (BASS.BASS_ChannelIsActive(getActiveChannel()) == BASS.BASS_ACTIVE_STOPPED) {
                    playbackManager?.onFinishPlayback()
                }else{
                    handler.postDelayed(this,500)
                }

            }
        }
        handler.post(checkRunnable!!)
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
    fun getCurrentPositionToBytes(position: Long):Long{
        return BASS.BASS_ChannelSeconds2Bytes(mainChannel!!, position / 1000.0)
    }
    fun setActiveChannel(channel:Int){
        mainChannel=channel
    }
    fun getActiveChannel():Int{
        return mainChannel?:0
    }
    fun getCurrentPositionInSeconds(channel: Int): Long {
        return BASS.BASS_ChannelBytes2Seconds(channel, getBytesPosition(channel)).toLong() * 1000
    }

    fun getDuration(channel: Int): Long {
        return BASS.BASS_ChannelBytes2Seconds(channel, getBytesTotal(channel)).toLong() * 1000
    }

    private fun getBytesPosition(channel:Int): Long {
        return BASS.BASS_ChannelGetPosition(channel, BASS.BASS_POS_BYTE)
    }
    private fun getBytesTotal(channel: Int): Long {
        return BASS.BASS_ChannelGetLength(channel, BASS.BASS_POS_BYTE)
    }
    fun releasePlayback(){
        BASS.BASS_ChannelStop(getActiveChannel())
        BASS.BASS_Free()
        instance=null
    }
    interface PlaybackManager{
        fun onFinishPlayback()
    }
}