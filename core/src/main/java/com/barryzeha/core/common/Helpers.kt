package com.barryzeha.core.common

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.concurrent.TimeUnit


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 1/5/24.
 * Copyright (c)  All rights reserved.
 **/


fun checkPermissions(context: Context, permission:String, isGranted:(Boolean) ->Unit){
    if(ContextCompat.checkSelfPermission(context,permission)==PackageManager.PERMISSION_GRANTED){
        isGranted(true)
    }else{
        isGranted(false)
    }
}
fun getTimeOfSong(duration:Long):String{
    return String.format(
        Locale.ROOT,"%02d:%02d",
        TimeUnit.MILLISECONDS.toMinutes(duration.toLong()),
        TimeUnit.MILLISECONDS.toSeconds(duration.toLong()) -
        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration.toLong())))
}