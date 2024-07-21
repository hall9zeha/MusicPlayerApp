package com.barryzeha.core.common

import android.content.Context
import com.google.gson.Gson


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 31/5/24.
 * Copyright (c)  All rights reserved.
 **/

private const val PREFERENCES_FILE = "kTMusicPreferences"
private const val CURRENT_POSITION = "currentPosition"
private const val CURRENT_DURATION = "currentDuration"
private const val MUSIC_STATE = "musicStateJson"
private const val PREV_OR_NEXT = "prevOrNext"
private const val CONTROL_FROM_NOTIFY_2 = "controlFromNotify2"
private const val PLAYER_IS_STOP = "playerIsStop"
private const val SONG_MODE = "songMode"
private const val ID_SONG = "idSong"
class MyPreferences(private val context: Context){
    val gson = Gson()
    private var myPreferences = context.getSharedPreferences(PREFERENCES_FILE,Context.MODE_PRIVATE)
    fun cleanPreferences(){
        myPreferences= context.getSharedPreferences(PREFERENCES_FILE,Context.MODE_PRIVATE)
        myPreferences.edit().clear().apply()
    }
    var nextOrPrevFromNotify:Boolean
        get()= myPreferences.getBoolean(PREV_OR_NEXT,false)
        set(value)=myPreferences.edit().putBoolean(PREV_OR_NEXT,value).apply()
    var controlFromNotify:Boolean
        get()= myPreferences.getBoolean(CONTROL_FROM_NOTIFY_2,false)
        set(value)=myPreferences.edit().putBoolean(CONTROL_FROM_NOTIFY_2,value).apply()
    var isPlaying:Boolean
        get()= myPreferences.getBoolean(PLAYER_IS_STOP,false)
        set(value)=myPreferences.edit().putBoolean(PLAYER_IS_STOP,value).apply()
    var currentPosition:Long
        get()=myPreferences.getLong(CURRENT_POSITION,0)
        set(value)=myPreferences.edit().putLong(CURRENT_POSITION,value).apply()
    var idSong:Long
        get()=myPreferences.getLong(ID_SONG,-1)
        set(value)=myPreferences.edit().putLong(ID_SONG,value).apply()
    var musicStateJsonSaved:String?
        get()=myPreferences.getString(MUSIC_STATE,"")
        set(value)=myPreferences.edit().putString(MUSIC_STATE,value).apply()
    var currentDuration:Long
        get()=myPreferences.getLong(CURRENT_DURATION,0)
        set(value)=myPreferences.edit().putLong(CURRENT_DURATION,value).apply()
    var songMode:Int
        get()=myPreferences.getInt(SONG_MODE,-1)
        set(value)=myPreferences.edit().putInt(SONG_MODE,value).apply()
}