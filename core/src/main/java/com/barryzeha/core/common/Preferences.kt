package com.barryzeha.core.common

import android.content.Context
import com.google.gson.Gson
import javax.inject.Inject


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
private const val CURRENT_VIEW = "currentView"
private const val PLAY_LIST_SORT_OPTION="playListSortOption"
private const val FIRST_EXECUTION = "firstExecution"

private const val PLAYLIST_ID = "playlistId"
private const val STORAGE_DIR_SAF_URI = "storageDirSAFUri"
private const val GLOBAL_THEME = "globalTheme"
private const val THEME_CHANGED = "themeChanged"
private const val SCROLL_TO_POSITION = "scrollToPosition"
private const val SAVE_CURRENT_FRAGMENT_OF_NAV = "SaveFragmentOfNav"
private const val IS_OPEN_QUEUE = "isOpenQueue"

private const val TOTAL_ITEM_SONGS = "totalItemSongs"


class  MyPreferences @Inject constructor(private val context: Context){
    val gson = Gson()
    private var myPreferences = context.getSharedPreferences(PREFERENCES_FILE,Context.MODE_PRIVATE)
    fun cleanPreferences(){
        myPreferences= context.getSharedPreferences(PREFERENCES_FILE,Context.MODE_PRIVATE)
        myPreferences.edit().clear().apply()
    }
    var globalTheme:Int
        get() = myPreferences.getInt(GLOBAL_THEME, 0)
        set(value){myPreferences.edit().putInt(GLOBAL_THEME,value).commit()}
    var themeChanged:Boolean
        get() = myPreferences.getBoolean(THEME_CHANGED, false)
        set(value) = myPreferences.edit().putBoolean(THEME_CHANGED,value).apply()
    var nextOrPrevFromNotify:Boolean
        get()= myPreferences.getBoolean(PREV_OR_NEXT,false)
        set(value)=myPreferences.edit().putBoolean(PREV_OR_NEXT,value).apply()
    var controlFromNotify:Boolean
        get()= myPreferences.getBoolean(CONTROL_FROM_NOTIFY_2,false)
        set(value)=myPreferences.edit().putBoolean(CONTROL_FROM_NOTIFY_2,value).apply()
    var isPlaying:Boolean
        get()= myPreferences.getBoolean(PLAYER_IS_STOP,false)
        set(value)=myPreferences.edit().putBoolean(PLAYER_IS_STOP,value).apply()
    var firstExecution:Boolean
        get()= myPreferences.getBoolean(FIRST_EXECUTION,false)
        set(value)=myPreferences.edit().putBoolean(FIRST_EXECUTION,value).apply()

    var currentIndexSong:Long
        get()=myPreferences.getLong(CURRENT_POSITION,-1)
        set(value)=myPreferences.edit().putLong(CURRENT_POSITION,value).apply()
    var totalItemSongs:Int
        get()=myPreferences.getInt(TOTAL_ITEM_SONGS,0)
        set(value)=myPreferences.edit().putInt(TOTAL_ITEM_SONGS,value).apply()

    var idSong:Long
        get()=myPreferences.getLong(ID_SONG,-1)
        set(value)=myPreferences.edit().putLong(ID_SONG,value).apply()
    var musicStateJsonSaved:String?
        get()=myPreferences.getString(MUSIC_STATE,"")
        set(value)=myPreferences.edit().putString(MUSIC_STATE,value).apply()
    var currentPosition:Long
        get()=myPreferences.getLong(CURRENT_DURATION,0)
        set(value)=myPreferences.edit().putLong(CURRENT_DURATION,value).apply()
    var songMode:Int
        get()=myPreferences.getInt(SONG_MODE,-1)
        // Usamos commit() porque necesitamos que la escritura de valores sea síncrona en esta propiedad
        set(value){myPreferences.edit().putInt(SONG_MODE,value).commit()}
    var currentView:Int
        get()=myPreferences.getInt(CURRENT_VIEW,-1)
        set(value)=myPreferences.edit().putInt(CURRENT_VIEW,value).apply()
    var playListSortOption:Int
        get()=myPreferences.getInt(PLAY_LIST_SORT_OPTION,0)
        set(value)=myPreferences.edit().putInt(PLAY_LIST_SORT_OPTION,value).apply()
    var playlistId:Int
        get()=myPreferences.getInt(PLAYLIST_ID,0)
        set(value)=myPreferences.edit().putInt(PLAYLIST_ID,value).apply()
    var directorySAFUri:String?
        get()=myPreferences.getString(STORAGE_DIR_SAF_URI,"")
        set(value)=myPreferences.edit().putString(STORAGE_DIR_SAF_URI,value).apply()
    var scrollToPosition:Boolean
        get()= myPreferences.getBoolean(SCROLL_TO_POSITION,false)
        set(value)=myPreferences.edit().putBoolean(SCROLL_TO_POSITION,value).apply()
    var saveFragmentOfNav:Int
        get()= myPreferences.getInt(SAVE_CURRENT_FRAGMENT_OF_NAV,-1)
        set(value)= myPreferences.edit().putInt(SAVE_CURRENT_FRAGMENT_OF_NAV,value).apply()
    var isOpenQueue:Boolean
        get() = myPreferences.getBoolean(IS_OPEN_QUEUE,false)
        set(value) = myPreferences.edit().putBoolean(IS_OPEN_QUEUE,value).apply()

    fun clearIdSongInPrefs(){
        myPreferences.edit().remove(ID_SONG).apply()
    }
    fun clearCurrentPosition(){
        myPreferences.edit().remove(CURRENT_POSITION).apply()
    }
}