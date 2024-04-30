package com.barryzeha.ktmusicplayer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/4/24.
 * Copyright (c)  All rights reserved.
 **/

@HiltAndroidApp
class MyApp:Application() {
    override fun onCreate() {
        super.onCreate()
    }
}