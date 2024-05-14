package com.barryzeha.core.common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.barryzeha.core.R

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.TimeUnit


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 1/5/24.
 * Copyright (c)  All rights reserved.
 **/

private const val CHANNEL_ID = "KtMusic_Notify_Id"
private const val CHANNEL_NAME = "KtMusic_Channel"
private const val NOTIFICATION_ID = 202405
fun checkPermissions(context: Context, permissions:List<String>, isGranted:(Boolean, List<Pair<String,Boolean>>) ->Unit){
    val permissionsGranted:MutableList<Pair<String,Boolean>> = mutableListOf()
    var grantedCount=0
    permissions.forEach {permission->
        if(ContextCompat.checkSelfPermission(context,permission) == PackageManager.PERMISSION_GRANTED){
            permissionsGranted.add(Pair(permission,true))
            grantedCount++
        }else{
            permissionsGranted.add(Pair(permission,false))
        }
   }
    isGranted((grantedCount == permissions.size),permissionsGranted)
}
fun getTimeOfSong(duration:Long):String{
    return String.format(
        Locale.ROOT,"::%02d:%02d",
        TimeUnit.MILLISECONDS.toMinutes(duration),
        TimeUnit.MILLISECONDS.toSeconds(duration) -
        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)))
}
fun getRealPathFromURI(uri: Uri, context: Context): String? {
    val returnCursor = context.contentResolver.query(uri, null, null, null, null)
    val nameIndex =  returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    val sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE)
    returnCursor.moveToFirst()
    val name = returnCursor.getString(nameIndex)
    val size = returnCursor.getLong(sizeIndex).toString()
    val file = File(context.filesDir, name)
    try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(file)
        var read = 0
        val maxBufferSize = 1 * 1024 * 1024
        val bytesAvailable: Int = inputStream?.available() ?: 0
        //int bufferSize = 1024;
        val bufferSize = Math.min(bytesAvailable, maxBufferSize)
        val buffers = ByteArray(bufferSize)
        while (inputStream?.read(buffers).also {
                if (it != null) {
                    read = it
                }
            } != -1) {
            outputStream.write(buffers, 0, read)
        }
        Log.e("File Size", "Size " + file.length())
        inputStream?.close()
        outputStream.close()
        Log.e("File Path", "Path " + file.path)

    } catch (e: java.lang.Exception) {
        Log.e("Exception", e.message!!)
    }
    return file.path
}
fun getBitrate(pathFile: String): Int? {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(pathFile)
    val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toInt()
    retriever.release()
    return bitrate?.div(1000)
}
fun createTime(duration: Int): Triple<Int,Int,String> {
    var stringTime = ""
    val min = duration / 1000 / 60
    val sec = duration / 1000 % 60
    stringTime += "$min:"
    if (sec < 10) {
        stringTime += "0"
    }
    stringTime += sec
    return Triple(min,sec,stringTime)
}

// Notifications

fun sendNotification(
    context: Context,
    title: String, /*notifyId:Int,*/ launchActivity:AppCompatActivity
     ){
    cancelNotification(context, NOTIFICATION_ID)
    val mainIntent = Intent(context,launchActivity::class.java)
    mainIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

    val pendingIntent = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
        PendingIntent.getActivity(context,NOTIFICATION_ID,mainIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }else{
        PendingIntent.getActivity(context, NOTIFICATION_ID.toInt(), mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    val notificationManager = getSystemService(context, NotificationManager::class.java) as NotificationManager
    createNotificationChannel(notificationManager)
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)

    builder.setCustomNotification(context,title,"", pendingIntent)
    notificationManager.notify(NOTIFICATION_ID,builder.build())
}
fun cancelNotification(context:Context,idNotify:Int){
    val notificationManager = getSystemService(context,NotificationManager::class.java) as NotificationManager?
    notificationManager!!.cancel(idNotify)
}
fun createNotificationChannel(notificationManager:NotificationManager){
    if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.O){
        val notificationChannel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply{
            enableLights(true)
        }
        notificationManager.createNotificationChannel(notificationChannel)
    }
}
fun NotificationCompat.Builder.setCustomNotification(
    context: Context,
    title: String,
    content: String,
    pendingIntent: PendingIntent
):NotificationCompat.Builder{

    val remoteViews = RemoteViews(context.packageName, R.layout.notify_controls_layout)
    remoteViews.setTextViewText(R.id.tvTitle,title)
    remoteViews.setOnClickPendingIntent(R.id.btnPlay,pendingIntent)
    setSmallIcon(R.drawable.ic_play)
    setContentIntent(pendingIntent)
    setAutoCancel(true)
    setCustomBigContentView(remoteViews)


    return this
}
