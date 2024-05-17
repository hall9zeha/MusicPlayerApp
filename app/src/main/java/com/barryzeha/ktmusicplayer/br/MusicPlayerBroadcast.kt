package com.barryzeha.ktmusicplayer.br
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.barryzeha.core.model.SongAction
import com.barryzeha.ktmusicplayer.service.MusicPlayerService


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 15/5/24.
 * Copyright (c)  All rights reserved.
 **/

class MusicPlayerBroadcast: BroadcastReceiver() {
 override fun onReceive(context: Context?, intent: Intent?) {
    val serviceIntent = Intent(context, MusicPlayerService::class.java)
     val action = SongAction.values()[intent?.action?.toInt()?:SongAction.Nothing.ordinal]

     serviceIntent.action = action.ordinal.toString()
     if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
         context?.startForegroundService(serviceIntent)

     }else context?.startService(serviceIntent)
 }
}