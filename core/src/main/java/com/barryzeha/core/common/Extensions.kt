package com.barryzeha.core.common

import android.app.Activity
import android.view.View
import com.google.android.material.snackbar.Snackbar


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 1/5/24.
 * Copyright (c)  All rights reserved.
 **/
 
fun Activity.showSnackBar(view: View, msg:String, duration:Int = Snackbar.LENGTH_SHORT)=
    Snackbar.make(view,msg,duration).show()