package com.barryzeha.core.common

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.view.View
import com.google.android.material.snackbar.Snackbar


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 1/5/24.
 * Copyright (c)  All rights reserved.
 **/
 
fun Activity.showSnackBar(view: View, msg:String, duration:Int = Snackbar.LENGTH_SHORT)=
    Snackbar.make(view,msg,duration).show()

@Suppress("DEPRECATION")
fun <T> Context.isServiceRunning(service:Class<T>):Boolean{
    return (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
        .getRunningServices(Integer.MAX_VALUE)
        .any{it.service.className == service.name}
}