package com.barryzeha.ktmusicplayer.common

import android.app.Activity
import com.barryzeha.ktmusicplayer.br.MusicPlayerBroadcast
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.PopupWindow
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.barryzeha.core.R
import com.barryzeha.core.common.BY_ALBUM
import com.barryzeha.core.common.BY_ARTIST
import com.barryzeha.core.common.BY_GENRE
import com.barryzeha.core.common.showDialog
import com.barryzeha.core.model.SongAction
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.ktmusicplayer.databinding.WindowPopupMenuBinding

import com.barryzeha.ktmusicplayer.view.ui.activities.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt


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
        Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

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
fun sortPlayList(sortedOption:Int, songList:List<SongEntity>, result:(songListSorted:MutableList<Any>)->Unit){
    if (songList.isNotEmpty()) {
        val itemList:MutableList<Any> = arrayListOf()
        val headerContentExtractor: (SongEntity) -> String = when (sortedOption) {
            BY_ALBUM -> { item -> item.album }
            BY_ARTIST -> { item -> item.artist }
            BY_GENRE -> { item -> item.genre }
            else -> { item -> item.parentDirectory.toString() }
        }

        CoroutineScope(Dispatchers.IO).launch {
            var temp=""
            songList.forEach { item->
                val headerContent= headerContentExtractor(item)
                if(temp==headerContent){
                    itemList.add(item)
                }else{
                    itemList.add(headerContent)
                    itemList.add(item)
                }
                temp = headerContent
            }

            withContext(Dispatchers.Main){
                result(itemList)
            }
        }
    }
}
fun onMenuItem(onItemClick:Boolean=true,activity:Activity, view:View,deleteItem:()->Unit, deleteAllItems:()->Unit){
    val popupView = WindowPopupMenuBinding.inflate(activity.layoutInflater) // Asegúrate de que este layout tenga el diseño que deseas

    val popupWindow = PopupWindow(popupView.root, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    if (!onItemClick) popupView.btnDeleteItem.visibility = View.GONE
    popupView.btnDeleteItem.setOnClickListener {
        deleteItem()
        popupWindow.dismiss()
    }
    popupView.btnDeleteAll.setOnClickListener {
        showDialog(activity, com.barryzeha.ktmusicplayer.R.string.delete_all,
            com.barryzeha.ktmusicplayer.R.string.delete_all_msg) {
            deleteAllItems()
        }
        popupWindow.dismiss()
    }
    // Mostrar el popup sobre el botón
    popupWindow.isFocusable = true

    popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(activity,com.barryzeha.ktmusicplayer.R.drawable.popup_window_background))

    // Posiciona el popup sobre el botón
    val location = IntArray(2)
    view.getLocationOnScreen(location)
    // Mostrar el PopupWindow en la posición deseada
    popupView.root.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
    popupWindow.width = LinearLayout.LayoutParams.WRAP_CONTENT
    popupWindow.height = View.MeasureSpec.makeMeasureSpec(popupView.root.measuredHeight, View.MeasureSpec.UNSPECIFIED)

    // Mostrar el PopupWindow
    popupWindow.showAtLocation(view, Gravity.NO_GRAVITY, location[0], location[1] - popupWindow.height - 16)

}
