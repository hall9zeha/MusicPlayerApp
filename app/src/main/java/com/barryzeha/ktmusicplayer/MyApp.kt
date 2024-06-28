package com.barryzeha.ktmusicplayer

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
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
    private var musicPlayerService: MusicPlayerService?=null
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(this)
        }
        startOrUpdateService(this, MusicPlayerService::class.java,this)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as MusicPlayerService.MusicPlayerServiceBinder
        musicPlayerService = binder.getService()

    }
    override fun onServiceDisconnected(name: ComponentName?) {
        musicPlayerService = null
    }

}