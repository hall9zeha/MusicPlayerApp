package com.barryzeha.core.common



import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.barryzeha.core.R
import com.barryzeha.core.model.entities.AudioMetadata
import com.barryzeha.core.model.entities.MusicState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.jvm.Throws
import kotlin.math.min

/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 1/5/24.
 * Copyright (c)  All rights reserved.
 **/

private const val CHANNEL_ID = "KtMusic_Notify_Id"
private const val CHANNEL_NAME = "KtMusic_Channel"
private const val NOTIFICATION_ID = 202405

const val TEXT_COLOR_PRIMARY_INVERSE_NO_DISABLE=0
const val COLOR_PRIMARY=1
const val COLOR_ACCENT = 2
const val COLOR_ON_PRIMARY=3
const val COLOR_SURFACE=4
const val COLOR_BACKGROUND=5
const val COLOR_TRANSPARENT=6

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
     var metadata: AudioFile? = null
     try {
         metadata = AudioFileIO.read(File(pathFile))
     } catch (e: Exception) {
         metadata = null
         Log.e("METADATA-FETCH", e.message.toString())

     }
    metadata?.let{
        val tag = metadata.tag
        val nameFile=try{metadata.file.name.substringBeforeLast(".")}catch(e:Exception){"Without name"}
        // retrieve covert art of song file uncomment if you want implement,
        /*val coverArtData = try{
            tag.firstArtwork.binaryData
        }catch(e:Exception){
            null
        }
        val bitmapCoverArt = getBitmap(context,coverArtData,true) ?: BitmapFactory.decodeStream(context.assets.open("placeholder_cover.jpg"))*/
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
        val channels = try { metadata.audioHeader.channels } catch (ex: Exception) { "unknown" }

        val sampleRate = try {metadata.audioHeader.sampleRate}catch(ex:Exception){0}
        val fileSizeBytes = File(pathFile).length()
        val fileSizeFormatted = formatFileSize(fileSizeBytes)

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
            format = format,
            freq = sampleRate.toString(),
            fileSize = fileSizeFormatted,
            channels = channels
            //coverArt = bitmapCoverArt
        )

    }
     return null
}
fun fetchShortFileMetadata(context: Context,pathFile:String):AudioMetadata? {
    val metadata = try{AudioFileIO.read(File(pathFile))}catch(e:Exception){null}
    metadata?.let {
        val tag = metadata.tag
        val nameFile = metadata.file.name.substringBeforeLast(".")
       /* val coverArtData = try{
        tag.firstArtwork.binaryData
        }catch(e:Exception){
            null
        }
        val bitmapCoverArt = getBitmap(context,coverArtData,true) ?: BitmapFactory.decodeStream(context.assets.open("placeholder_cover.jpg"))*/
        fun getTagField(fieldKey: FieldKey, defaultValue: String) =
            try {
                tag?.getFirst(fieldKey)?.takeIf { it.isNotEmpty() } ?: defaultValue
            } catch (ex: Exception) {
                defaultValue
            }
        // Extract metadata with default values
        val title = getTagField(FieldKey.TITLE, nameFile)
        val artist = getTagField(FieldKey.ARTIST, "Artist Unknown")
        val album = getTagField(FieldKey.ALBUM, "Album Unknown")


        // Extract audio header data with default values
        val bitRate = try { metadata.audioHeader.bitRate } catch (ex: Exception) { "" }
        val songLength = try { (metadata.audioHeader.trackLength * 1000).toLong() } catch (ex: Exception) { 0L }
        val songLengthFormatted = try { getTimeOfSong(songLength) } catch (ex: Exception) { "0" }

        return AudioMetadata(
            title = title,
            artist = artist,
            album = album,
            bitRate = bitRate,
            songLengthFormatted = songLengthFormatted,
            songLength = songLength,
            //coverArt = bitmapCoverArt
        )

    }
    return null
}
fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000 -> String.format("%.2f MB", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format("%.2f KB", bytes / 1_000.0)
        else -> "$bytes Bytes"
    }
}
fun getTimeOfSong(duration:Long):String{
    return String.format(
        Locale.ROOT,"::%02d:%02d",
        TimeUnit.MILLISECONDS.toMinutes(duration),
        TimeUnit.MILLISECONDS.toSeconds(duration) -
        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)))
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

fun getSongMetadata(context: Context, path: String?, isForNotify:Boolean=false): MusicState? {
    if(!path.isNullOrEmpty()){
        val metadata=fetchFileMetadata(context,path)
        mmr.setDataSource(path)
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
        albumArt = BitmapFactory.decodeStream(context.assets.open("ktmusic_icon.jpg"))
        )
}
fun getBitmap(context: Context,byteArray:ByteArray?,isForNotify: Boolean=false):Bitmap?{
    return byteArray?.let {
        val originalBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        if (isForNotify) scaleBitmap(originalBitmap, 156, 156)
        else originalBitmap
    }?:run{
       BitmapFactory.decodeStream(context.assets.open("ktmusic_icon.jpg"))
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
        android.R.color.transparent,
    ))
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

fun getEmbeddedSyncedLyrics(pathFile:String):String?{
    return  try{
        AudioFileIO.read(File(pathFile)).tagOrCreateDefault.getFirst(FieldKey.LYRICS)
    }catch(ex:Exception){
        return null
    }
    return null
}
fun keepScreenOn(activity:Activity, screenOn:Boolean){
    if(screenOn)activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}

