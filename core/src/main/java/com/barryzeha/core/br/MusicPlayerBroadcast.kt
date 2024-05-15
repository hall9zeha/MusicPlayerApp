package com.barryzeha.core.br

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 15/5/24.
 * Copyright (c)  All rights reserved.
 **/

class MusicPlayerBroadcast:BroadcastReceiver() {
 override fun onReceive(context: Context?, intent: Intent?) {
    if(intent?.action == "com.barryzeha.ktmusicplayer.ACTION_TOAST"){
      Toast.makeText(context, "Hola world", Toast.LENGTH_SHORT).show()
     Log.e("RECEIVER-CLICK", "onReceive" )
    }
 }
}