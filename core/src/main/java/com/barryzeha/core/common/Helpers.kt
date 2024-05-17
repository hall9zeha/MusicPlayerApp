package com.barryzeha.core.common



import android.app.Notification
import android.app.Notification.MediaStyle
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.media.MediaMetadataRetriever
import android.media.session.MediaSession
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.barryzeha.core.R

import com.barryzeha.core.model.SongAction
import com.barryzeha.core.model.entities.MusicState
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
fun createTime(duration: Long): Triple<Int,Int,String> {
    Log.e("DURATION", duration.toString() )
    val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) -
            TimeUnit.MINUTES.toSeconds(minutes)


    // Formatear la duración en un String
    val formattedDuration = String.format("%02d:%02d", minutes, seconds)
    return Triple(minutes.toInt(),seconds.toInt(),formattedDuration)
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
            setBypassDnd(true)
        }
        notificationManager.createNotificationChannel(notificationChannel)
    }
}

fun getSongCover(path: String?): Bitmap? {
    val mmr = MediaMetadataRetriever()
    mmr.setDataSource(path)

    return mmr.embeddedPicture?.let {
        BitmapFactory.decodeByteArray(it, 0, it.size)
    }
}