package com.barryzeha.ktmusicplayer.common

import com.barryzeha.ktmusicplayer.br.MusicPlayerBroadcast
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.barryzeha.core.R
import com.barryzeha.core.model.SongAction
import com.barryzeha.core.model.entities.MusicState

import com.barryzeha.ktmusicplayer.view.ui.activities.MainActivity


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 16/5/24.
 * Copyright (c)  All rights reserved.
 **/

private const val CHANNEL_ID = "KtMusic_Notify_Id"
private const val CHANNEL_NAME = "KtMusic_Channel"
const val NOTIFICATION_ID = 202405
@RequiresApi(Build.VERSION_CODES.O)
fun createNotificationChannel(context: Context){
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        channel.setAllowBubbles(false)
    }

    channel.setBypassDnd(true)

    notificationManager.createNotificationChannel(channel)
}

@Suppress("Deprecation")
fun notificationMediaPlayer(context: Context, mediaStyle: Notification.MediaStyle, state: MusicState): Notification {
    val pMainIntent = PendingIntent.getActivity(
        context,
        123,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE)

    val builder = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
        Notification.Builder(context, CHANNEL_ID)
    }else Notification.Builder(context)
    val playPauseIntent = Intent(context, MusicPlayerBroadcast::class.java)
        .setAction(
            if (state.isPlaying) SongAction.Pause.ordinal.toString() else SongAction.Resume.ordinal.toString()
        )
    val playPausePI = PendingIntent.getBroadcast(
        context,
        1,
        playPauseIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val playPauseAction = Notification.Action.Builder(
        Icon.createWithResource(
            context,
            if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
        ),
        "PlayPause",
        playPausePI
    ).build()

    val previousIntent = Intent(context, MusicPlayerBroadcast::class.java)
        .setAction(SongAction.Previous.ordinal.toString())
    val previousPI = PendingIntent.getBroadcast(
        context,
        2,
        previousIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val previousAction = Notification.Action.Builder(
        Icon.createWithResource(context, R.drawable.ic_back),
        "Previous",
        previousPI
    ).build()

    val nextIntent = Intent(context, MusicPlayerBroadcast::class.java)
        .setAction(SongAction.Next.ordinal.toString())
    val nextPI = PendingIntent.getBroadcast(
        context,
        3,
        nextIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val nextAction = Notification.Action.Builder(
        Icon.createWithResource(context, R.drawable.ic_next),
        "Previous",
        nextPI
    ).build()

    // Action close notify
    val closeIntent = Intent(context, MusicPlayerBroadcast::class.java)
        .setAction(SongAction.Close.ordinal.toString())
    val closePI = PendingIntent.getBroadcast(
        context,
        4,
        closeIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val closeAction = Notification.Action.Builder(
        Icon.createWithResource(context,R.drawable.ic_close),
        "Close",
        closePI
    ).build()

    return builder
        .setStyle(mediaStyle)
        .setSmallIcon(R.drawable.ic_play)
        .setLargeIcon(state.albumArt)
        .setOnlyAlertOnce(true)
        .setOngoing(true)
        .setContentIntent(pMainIntent)
        .setContentTitle(state.title)
        .setContentText(state.artist)
        .addAction(previousAction)
        .addAction(playPauseAction)
        .addAction(nextAction)
        .addAction(null)
        .addAction(closeAction)
        .build()

}
fun cancelPersistentNotify(context:Context){
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val builder = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
        Notification.Builder(context, CHANNEL_ID)
    }else Notification.Builder(context)
    builder
        .setSmallIcon(R.drawable.ic_play)
        .setOngoing(false) // Hacer que la notificación ya no sea persistente
    notificationManager.notify(NOTIFICATION_ID,builder.build())
    notificationManager.cancelAll()

}
