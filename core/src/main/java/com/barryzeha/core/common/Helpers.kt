package com.barryzeha.core.common

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat


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