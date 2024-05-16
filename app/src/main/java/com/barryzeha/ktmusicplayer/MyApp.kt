package com.barryzeha.ktmusicplayer

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import com.barryzeha.ktmusicplayer.common.createNotificationChannel

import dagger.hilt.android.HiltAndroidApp


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/4/24.
 * Copyright (c)  All rights reserved.
 **/

@HiltAndroidApp
class MyApp:Application() {
    companion object{
        @SuppressLint("StaticFieldLeak")
        private var _context: Context?=null
        val context  get() = _context!!
    }
    override fun onCreate() {
        if(_context==null){
            _context = this
        }
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(this)
        }
    }
}