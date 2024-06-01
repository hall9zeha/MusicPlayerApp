package com.barryzeha.core.common

import android.content.Context
import com.barryzeha.core.model.entities.MusicState


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 31/5/24.
 * Copyright (c)  All rights reserved.
 **/

private const val PREFERENCES_FILE = "kTMusicPreferences"
private const val CURRENT_POSITION = "currentPosition"
class MyPreferences(private val context: Context){
    //val gson = Gson()
    private var myPreferences = context.getSharedPreferences(PREFERENCES_FILE,Context.MODE_PRIVATE)
    fun cleanPreferences(){
        myPreferences= context.getSharedPreferences(PREFERENCES_FILE,Context.MODE_PRIVATE)
        myPreferences.edit().clear().apply()
    }
    var currentPosition:Long
        get()=myPreferences.getLong(CURRENT_POSITION,0)
        set(value)=myPreferences.edit().putLong(CURRENT_POSITION,value).apply()
    /*var musicStateSaved:MusicState
        get()=myPreferences.*/
}