package com.barryzeha.mfilepicker.common

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 4/8/24.
 * Copyright (c)  All rights reserved.
 **/
private const val PREFERENCES_FILE="filePickerPreferences"
private const val LAST_DIRS_SELECTED="lastDirsSelected"
class Preferences @Inject constructor(private val context:Context){
    private var myPreferences = context.getSharedPreferences(PREFERENCES_FILE,Context.MODE_PRIVATE)
    private val gson = Gson()
    fun cleanPreferences(){
        myPreferences = context.getSharedPreferences(PREFERENCES_FILE,Context.MODE_PRIVATE)
        myPreferences.edit().clear().apply()
    }
    var lastDirs: ArrayList<String>?
        get():ArrayList<String>? {
            val listJson = myPreferences.getString(LAST_DIRS_SELECTED, null)
            val type = object : TypeToken<ArrayList<String>>() {}.type
            return gson.fromJson(listJson, type) ?: ArrayList()
        }
        set(value) {
            val json = gson.toJson(value)
            myPreferences.edit().putString(LAST_DIRS_SELECTED, json).apply()
        }
    fun clearLastDirs(){
        myPreferences.edit().remove(LAST_DIRS_SELECTED).apply()
    }
}