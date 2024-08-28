package com.barryzeha.core.common



import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
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
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.FileProvider
import com.barryzeha.core.R
import com.barryzeha.core.model.entities.AudioMetadata

import com.barryzeha.core.model.entities.MusicState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

 fun fetchFileMetadata(context: Context, pathFile:String):AudioMetadata?{
    val metadata = try{AudioFileIO.read(File(pathFile))}catch(e:Exception){null}

    // retrieve covert art of song file uncomment if you want implement,
    /*val coverArtData = try{
        tag.firstArtwork.binaryData
    }catch(e:Exception){
        null
    }
    val bitmapCoverArt = getBitmap(context,coverArtData,true) ?: BitmapFactory.decodeStream(context.assets.open("placeholder_cover.jpg"))*/
    metadata?.let{
        val tag = metadata.tag
        val nameFile=metadata.file.name.substringBeforeLast(".")

        fun getTagField(fieldKey: FieldKey, defaultValue: String)=
            try {
                tag?.getFirst(fieldKey)?.takeIf { it.isNotEmpty() } ?: defaultValue
            } catch (ex: Exception) {
                defaultValue
            }
        // Extract metadata with default values
        val artist = getTagField(FieldKey.ARTIST, "Artist Unknown")
        val album = getTagField(FieldKey.ALBUM, "Album Unknown")
        val genre = getTagField(FieldKey.GENRE, "Unknown Genre")
        val title = getTagField(FieldKey.TITLE, nameFile)
        val comment = getTagField(FieldKey.COMMENT, "No Comment")
        val year = getTagField(FieldKey.YEAR, "Unknown Year")
        val track = getTagField(FieldKey.TRACK, "Unknown")
        val discNumber = getTagField(FieldKey.DISC_NO, "Unknown")
        val composer = getTagField(FieldKey.COMPOSER, "Unknown")
        val artistSort = getTagField(FieldKey.ARTIST_SORT, "")

        // Extract audio header data with default values
        val bitRate = try { metadata.audioHeader.bitRate } catch (ex: Exception) { "" }
        val songLength = try { (metadata.audioHeader.trackLength * 1000).toLong() } catch (ex: Exception) { 0L }
        val songLengthFormatted = try { getTimeOfSong(songLength) } catch (ex: Exception) { "0" }
        val format = try { metadata.audioHeader.format } catch (ex: Exception) { "unknown" }

        return AudioMetadata(
            artist = artist,
            album = album,
            genre = genre,
            title = title,
            comment = comment,
            year = year,
            track = track,
            discNumber = discNumber,
            composer = composer,
            artistSort = artistSort,
            bitRate = bitRate,
            songLengthFormatted = songLengthFormatted,
            songLength = songLength,
            format = format
            //coverArt = bitmapCoverArt
        )

    }

     return null
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
        metadata?.let {
            return MusicState(
                title = metadata?.title!!,
                artist = metadata?.artist!!,
                album = metadata?.album!!,
                duration = metadata.songLength,
                albumArt = bitmap
            )
        }
        return null
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
        if (isForNotify) scaleBitmap(originalBitmap, 156, 156)
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
  /*  val emulatedRegex = "emulated/(\\d+)/".toRegex()
    val emulatedMatchResult = emulatedRegex.find(path)

    val storage=   emulatedMatchResult?.groups?.get(1)?.value ?:""*/

    val storage= getStorageIdentifier(path)

    Log.e("STORAGE-POSITION--:", storage.toString())
    // Encontrar la posición del nombre del archivo (última '/' antes del nombre de archivo)
    val lastIndex = path.lastIndexOf('/')
    if (lastIndex == -1) {
        return path.substringBeforeLast("/")  // Si no se encuentra '/', retornar el path completo
    }

    // Encontrar la posición de '0' o '1' en el path
    val primaryIndex = path.indexOf(storage.toString())
    if (primaryIndex == -1) {
        return path.substringBeforeLast("/")  // Si no se encuentra '0', retornar el path completo
    }

    // Encontrar el primer '/' después de 'primary'
    val firstSlashAfterPrimary = path.indexOf('/', primaryIndex)
    if (firstSlashAfterPrimary == -1 || firstSlashAfterPrimary >= lastIndex) {
        return if (primaryIndex == -1) {
            path.substringBeforeLast("/")
        } else {
            return path.substring(primaryIndex + storage.toString().length, lastIndex)
        }

        // Si no se encuentra '/', retornar el path completo
    }

    // Recortar el path desde el primer '/' después de '0' hasta el nombre del archivo
    val directoriosRecortados = path.substring(firstSlashAfterPrimary + 1, lastIndex)
    return directoriosRecortados

}
fun getStorageIdentifier(path: String): String? {
    // Expresiones regulares para detectar el número de almacenamiento en rutas emuladas y en tarjetas SD
    val emulatedRegex = "emulated/(\\d+)/".toRegex()
    val sdCardRegex = "storage/(extSdCard|external_sd)/".toRegex()

    // Intentar encontrar una coincidencia para emulated
    val emulatedMatchResult = emulatedRegex.find(path)
    if (emulatedMatchResult != null) {
        // Devolver solo el número de almacenamiento
        return emulatedMatchResult.groups[1]?.value
    }

    // Intentar encontrar una coincidencia para SD card
    val sdCardMatchResult = sdCardRegex.find(path)
    if (sdCardMatchResult != null) {
        // Devolver el nombre de la SD card (extSdCard o external_sd)
        return sdCardMatchResult.groups[1]?.value
    }

    // Si no se encuentra ninguna coincidencia, retornar null
    return null
}
fun getUriFromFile(file: File, context: Context): Uri {
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
fun showDialog(context:Context,titleRes:Int, msgRes:Int, block:()->Unit){
    val dialog= MaterialAlertDialogBuilder(context).apply {
        setTitle(titleRes)
        setMessage(msgRes)
        setPositiveButton(
            R.string.accept
        ) { dialog, _ ->
            block()
        }
        setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
    }
    dialog.show()

}