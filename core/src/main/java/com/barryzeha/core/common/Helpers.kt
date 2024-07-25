package com.barryzeha.core.common



import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.barryzeha.core.model.entities.AudioMetadata

import com.barryzeha.core.model.entities.MusicState
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 1/5/24.
 * Copyright (c)  All rights reserved.
 **/

private const val CHANNEL_ID = "KtMusic_Notify_Id"
private const val CHANNEL_NAME = "KtMusic_Channel"
private const val NOTIFICATION_ID = 202405
val mmr = MediaMetadataRetriever()
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

fun <T> startOrUpdateService(context: Context,service:Class<T>,serviceConn:ServiceConnection,musicState: MusicState=MusicState()){
    val serviceIntent = Intent (context, service).apply {
        putExtra("musicState", musicState)
    }
    if (!context.isServiceRunning(service) ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, serviceIntent)
        } else context.startService(serviceIntent)
    }
    context.bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE)

}

 fun fetchFileMetadata(context: Context, pathFile:String):AudioMetadata{
    val metadata = AudioFileIO.read(File(pathFile))
    val tag = metadata.tag
    val nameFile=metadata.file.name.substringBeforeLast(".")
    // retrieve covert art of song file uncomment if you want implement,
    /*val coverArtData = try{
        tag.firstArtwork.binaryData
    }catch(e:Exception){
        null
    }
    val bitmapCoverArt = getBitmap(context,coverArtData,true) ?: BitmapFactory.decodeStream(context.assets.open("placeholder_cover.jpg"))*/

    return AudioMetadata(
        artist=try{if(tag.getFirst(FieldKey.ARTIST).isNullOrEmpty())"Artist unknown" else tag.getFirst(FieldKey.ARTIST) } catch(ex:Exception){"Artist Unknown" },
        album=try{if(tag.getFirst(FieldKey.ALBUM).isNullOrEmpty())"Album unknown" else tag.getFirst(FieldKey.ALBUM) }catch(ex:Exception){"Album Unknown"},
        genre=try{tag.getFirst(FieldKey.GENRE)}catch(ex:Exception){"Unknown"},
        title=try{if(tag.getFirst(FieldKey.TITLE).isNullOrEmpty())nameFile else tag.getFirst(FieldKey.TITLE)}catch(ex:Exception){nameFile},
        comment=try{tag.getFirst(FieldKey.COMMENT)}catch(ex:Exception){"No comment"},
        year=try{tag.getFirst(FieldKey.YEAR)}catch(ex:Exception){"Unknown year"},
        track=try{tag.getFirst(FieldKey.TRACK)}catch(ex:Exception){"Unknown"},
        discNumber=try{tag.getFirst(FieldKey.DISC_NO)}catch(ex:Exception){"Unknown"},
        composer=try{tag.getFirst(FieldKey.COMPOSER)}catch(ex:Exception){"Unknown"},
        artistSort = try{tag.getFirst(FieldKey.ARTIST_SORT)}catch(ex:Exception){""},
        bitRate = try{metadata.audioHeader.bitRate}catch(ex:Exception){""},
        songLengthFormatted = try{getTimeOfSong((metadata.audioHeader.trackLength * 1000).toLong())}catch(ex:Exception){"0"},
        songLength = try{(metadata.audioHeader.trackLength * 1000).toLong()}catch(ex:Exception){0},
        format = try{metadata.audioHeader.format}catch (ex:Exception){"unknown"},
        //coverArt = bitmapCoverArt
    )
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

fun getSongMetadata(context: Context, path: String?, isForNotify:Boolean=false): MusicState? {
    if(!path.isNullOrEmpty()){
        val metadata=fetchFileMetadata(context,path)
        mmr.setDataSource(path)
        //val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        //val album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
        val bitmap = getBitmap(context,mmr.embeddedPicture,isForNotify)!!
        return MusicState(
            title = metadata.title!!,
            artist = metadata.artist!!,
            album = metadata.album!!,
            duration = metadata.songLength,
            albumArt = bitmap
        )
    }
    return MusicState(
        artist = "Unknown",
        album ="Album Unknown",
        albumArt = BitmapFactory.decodeStream(context.assets.open("placeholder_cover.jpg"))
        )
}
fun getBitmap(context: Context,byteArray:ByteArray?,isForNotify: Boolean=false):Bitmap?{
    return byteArray?.let {
        val originalBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        if (isForNotify) scaleBitmap(originalBitmap, 96, 96)
        else originalBitmap
    }?:run{
       BitmapFactory.decodeStream(context.assets.open("placeholder_cover.jpg"))
    }
}
fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
    val originalWidth = bitmap.width
    val originalHeight = bitmap.height
    val scaleWidth = maxWidth.toFloat() / originalWidth
    val scaleHeight = maxHeight.toFloat() / originalHeight
    val scale = min(scaleWidth, scaleHeight)
    val matrix = Matrix()
    matrix.postScale(scale, scale)
    return Bitmap.createBitmap(bitmap, 0, 0, originalWidth, originalHeight, matrix, true)
}
fun mColorList(context:Context)=
    context.obtainStyledAttributes(intArrayOf(
        android.R.attr.textColorPrimaryInverseNoDisable,
        android.R.attr.colorPrimary,
        com.google.android.material.R.attr.colorAccent,
        com.google.android.material.R.attr.colorOnPrimary,
        com.google.android.material.R.attr.colorSurface,
        android.R.attr.colorBackground,
        android.R.color.transparent
    ))

fun getParentDirectories(path: String): String {
    // Encontrar la posición del nombre del archivo (última '/' antes del nombre de archivo)
    val lastIndex = path.lastIndexOf('/')
    if (lastIndex == -1) {
        return path  // Si no se encuentra '/', retornar el path completo
    }

    // Encontrar la posición de 'primary' en el path
    val primaryIndex = path.indexOf("primary")
    if (primaryIndex == -1) {
        return path  // Si no se encuentra 'primary', retornar el path completo
    }

    // Encontrar el primer '/' después de 'primary'
    val firstSlashAfterPrimary = path.indexOf('/', primaryIndex)
    if (firstSlashAfterPrimary == -1 || firstSlashAfterPrimary >= lastIndex) {
        return if (primaryIndex == -1) {
            path
       }else{
            return path.substring(primaryIndex + "primary:".length, lastIndex)
        }

          // Si no se encuentra '/', retornar el path completo
    }

    // Recortar el path desde el primer '/' después de 'primary' hasta el nombre del archivo
    val directoriosRecortados = path.substring(firstSlashAfterPrimary + 1, lastIndex)
    return directoriosRecortados
}