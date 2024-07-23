package com.barryzeha.ktmusicplayer

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.common.startOrUpdateService
import com.barryzeha.ktmusicplayer.common.createNotificationChannel
import com.barryzeha.ktmusicplayer.service.MusicPlayerService

import dagger.hilt.android.HiltAndroidApp


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/4/24.
 * Copyright (c)  All rights reserved.
 **/

@HiltAndroidApp
class MyApp:Application() ,ServiceConnection{

    companion object{
        @SuppressLint("StaticFieldLeak")
        private var _context: Context?=null
        val context  get() = _context!!

        val mPrefs:MyPreferences by lazy { MyPreferences(context) }
    }
    override fun onCreate() {
        if(_context==null){
            _context = this
        }
        super.onCreate()
        setUpGlobalPreferences()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(this)
        }
        // Start service
        startOrUpdateService(this, MusicPlayerService::class.java,this)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {}
    override fun onServiceDisconnected(name: ComponentName?) {}
    private fun setUpGlobalPreferences(){
        val gPrefs=PreferenceManager.getDefaultSharedPreferences(this)
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.S){
            if(gPrefs.getBoolean("themeKey",false)){
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }else{
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

            }
        }
    }
}