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
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import kotlin.math.min

/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 1/5/24.
 * Copyright (c)  All rights reserved.
 **/

private const val CHANNEL_ID = "KtMusic_Notify_Id"
private const val CHANNEL_NAME = "KtMusic_Channel"
private const val NOTIFICATION_ID = 202405
private const val DEFAULT_COVER_ART_ASSET = "ktmusic_icon.jpg"

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
            if(permission=="android.permission.MANAGE_EXTERNAL_STORAGE" && Build.VERSION.SDK_INT >=Build.VERSION_CODES.R ) {
                if (Environment.isExternalStorageManager()) {
                    permissionsGranted.add(Pair(permission, true))
                    grantedCount++
                } else {
                    permissionsGranted.add(Pair(permission, false))
                }
            }else{
                permissionsGranted.add(Pair(permission, false))
            }
        }
   }
    isGranted((grantedCount == permissions.size),permissionsGranted)
}

fun <T> startOrUpdateService(context: Context,service:Class<T>,serviceConn:ServiceConnection,musicState: MusicState=MusicState()){
    val serviceIntent = Intent (context, service).apply {
        putExtra(MUSIC_STATE_EXTRA, musicState)
    }
    if (!context.isServiceRunning(service) ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, serviceIntent)
        } else context.startService(serviceIntent)
    }
    context.bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE)
}
 fun fetchCompleteFileMetadata(context: Context, pathFile:String):AudioMetadata?{
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
        val artist = getTagField(FieldKey.ARTIST, "Unknown artist")
        val album = getTagField(FieldKey.ALBUM, ALBUM_UNKNOWN)
        val albumArtist = getTagField(FieldKey.ALBUM_ARTIST, "")
        val genre = getTagField(FieldKey.GENRE, "Unknown genre")
        val title = getTagField(FieldKey.TITLE, nameFile)
        val comment = getTagField(FieldKey.COMMENT, "No Comment")
        val year = getTagField(FieldKey.YEAR, "Unknown year")
        val track = getTagField(FieldKey.TRACK, "Unknown track")
        val discNumber = getTagField(FieldKey.DISC_NO, "Unknown disc")
        val composer = getTagField(FieldKey.COMPOSER, "Unknown composer")
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
            albumArtist=albumArtist,
            genre = genre,
            title = title,
            comment = comment,
            year = year,
            track = track,
            discNumber = discNumber,
            composer = composer,
            artistSort = artistSort,
            bitRate = normalizeBitrate(bitRate,format),
            songLengthFormatted = songLengthFormatted,
            songLength = songLength,
            format = format,
            freq = normalizeSampleRate(sampleRate.toString(),format),
            fileSize = fileSizeFormatted,
            channels = channels
            //coverArt = bitmapCoverArt
        )

    }
     return null
}
private fun normalizeBitrate(biteRateRaw:String,format:String):String{
    val onlyNumbers = if(!biteRateRaw.isNullOrEmpty()){biteRateRaw.filter{it.isDigit()}}else "0"
    if (format.equals("DSF", ignoreCase = true) ||
        format.equals("DFF", ignoreCase = true)
    ) {
        return (onlyNumbers.toInt() / 1000).toString()
    }
    return onlyNumbers
}
private fun normalizeSampleRate(sampleRateRaw:String, format:String):String{
    val raw = sampleRateRaw.toIntOrNull()?:return "0"
    if (!format.equals("DSF", ignoreCase = true) &&
        !format.equals("DFF", ignoreCase = true)
    ) {
        return (raw / 1000).toString()
    }
    val basePcmRate = raw / 128
    return (basePcmRate / 1000).toString()
}
fun fetchTimeOfSong(pathFile: String?):AudioMetadata?{
    var metadata: AudioFile? = null
    try {
        metadata = AudioFileIO.read(File(pathFile))
    } catch (e: Exception) {
        metadata = null
        Log.e("METADATA-FETCH", e.message.toString())

    }
    metadata?.let{
        val songLength = try { (metadata.audioHeader.trackLength * 1000).toLong() } catch (ex: Exception) { 0L }
        return AudioMetadata(
             songLength = songLength)

    }
    return null
}
fun fetchShortMetadataAlbumInfo(context: Context,pathFile:String):AudioMetadata?{
    val metadata = try{AudioFileIO.read(File(pathFile))}catch(e:Exception){null}
    metadata?.let {
        val tag = metadata.tag
        val nameFile = metadata.file.name.substringBeforeLast(".")
             fun getTagField(fieldKey: FieldKey, defaultValue: String) =
            try {
                tag?.getFirst(fieldKey)?.takeIf { it.isNotEmpty() } ?: defaultValue
            } catch (ex: Exception) {
                defaultValue
            }
        // Extract metadata with default values

        val title = getTagField(FieldKey.TITLE, nameFile)
        val artist = getTagField(FieldKey.ARTIST, "Unknown artist")
        val album = getTagField(FieldKey.ALBUM, "Unknown album")
        val albumArtist = getTagField(FieldKey.ALBUM_ARTIST, "")
        val year = getTagField(FieldKey.YEAR, "Unknown year")
        val songLength = try { (metadata.audioHeader.trackLength * 1000).toLong()} catch (ex: Exception) { 0L }
        return AudioMetadata(
            title = title,
            artist = artist,
            album = album,
            songLength = songLength,
            albumArtist = albumArtist,
            year = year
        )
    }
    return null
}
fun fetchShortFileMetadata(context: Context,pathFile:String):AudioMetadata? {
    val metadata = try{AudioFileIO.read(File(pathFile))}catch(e:Exception){null}
    metadata?.let {
        val tag = metadata.tag
        val nameFile = metadata.file.name.substringBeforeLast(".")

        fun getTagField(fieldKey: FieldKey, defaultValue: String) =
            try {
                tag?.getFirst(fieldKey)?.takeIf { it.isNotEmpty() } ?: defaultValue
            } catch (ex: Exception) {
                defaultValue
            }
        // Extract metadata with default values
        val format = try { metadata.audioHeader.format } catch (ex: Exception) { "unknown" }
        val title = getTagField(FieldKey.TITLE, nameFile)
        val artist = getTagField(FieldKey.ARTIST, "Unknown artist")
        val album = getTagField(FieldKey.ALBUM, "Unknown album")
        // Extract audio header data with default values
        val bitRate = try { metadata.audioHeader.bitRate } catch (ex: Exception) { "" }
        val songLength = try { (metadata.audioHeader.trackLength * 1000).toLong()} catch (ex: Exception) { 0L }
        val songLengthFormatted = try { getTimeOfSong(songLength) } catch (ex: Exception) { "0" }

        return AudioMetadata(
            title = title,
            artist = artist,
            album = album,
            bitRate = normalizeBitrate(bitRate, format),
            songLengthFormatted = songLengthFormatted,
            songLength = songLength,
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

fun createTime(duration: Long): Triple<Int,Int,String> {
    val hours = TimeUnit.MILLISECONDS.toHours(duration)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(hours)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(minutes) - TimeUnit.HOURS.toSeconds(hours)
    // Formatear la duración en un String

    val formattedDuration = if(hours>0){
        String.format(Locale.ROOT,"%02d:%02d:%02d",hours, minutes, seconds)}
    else{
        String.format(Locale.ROOT,"%02d:%02d", minutes, seconds)
    }
    return Triple(minutes.toInt(),seconds.toInt(),formattedDuration)
}
fun getSongMetadata(context: Context, path: String?,withBitmap:Boolean=false, isForNotify:Boolean=false): MusicState? {
    if(!path.isNullOrEmpty()){
        val metadata= fetchShortFileMetadata(context,path)
        val bitmap = if(withBitmap)getBitmap(context,path,isForNotify)!!else null
        metadata?.let {
            return if(bitmap !=null) MusicState(
                title = metadata?.title!!,
                artist = metadata?.artist!!,
                album = metadata?.album!!,
                duration = metadata.songLength,
                albumArt = bitmap
            )else   MusicState(
                    title = metadata?.title!!,
                    artist = metadata?.artist!!,
                    album = metadata?.album!!,
                    duration = metadata.songLength)

        }
        return null
    }
    return MusicState(
        artist = "Unknown artist",
        album ="Unknown album",
        albumArt = BitmapFactory.decodeStream(context.assets.open(DEFAULT_COVER_ART_ASSET))
        )
}
fun getBitmap(context: Context,pathFile:String?,isForNotify: Boolean=false):Bitmap?{
    if (pathFile.isNullOrEmpty()) {
        return BitmapFactory.decodeStream(context.assets.open(DEFAULT_COVER_ART_ASSET))
    }
    // Intentar extraer el cover art usando Jaudiotagger
    val artworkBytes: ByteArray? = try {
        val audioFile = AudioFileIO.read(File(pathFile))
        val tag = audioFile.tag
        val artwork = tag?.firstArtwork
        artwork?.binaryData
    } catch (ex: Exception) {
        Log.e("BITMAP_ERROR", "Error extrayendo coverart con Jaudiotagger: ${ex.message}")
        null
    }
    // Crear el bitmap si existe
    val bitmap = artworkBytes?.let {
        BitmapFactory.decodeByteArray(it, 0, it.size)
    }
    // Fallback si no hay artwork o decode falló
    val finalBitmap = bitmap ?: BitmapFactory.decodeStream(context.assets.open(DEFAULT_COVER_ART_ASSET))

    // Scaling opcional para notificaciones
    return if (isForNotify) {
        try {
            scaleBitmap(finalBitmap, 156, 156)
        } catch (ex: Exception) {
            finalBitmap
        }
    } else {
        finalBitmap
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
        //TODO upgrade to material you with material 1.13.0
        com.google.android.material.R.attr.colorAccent,
        // *********************************************
        com.google.android.material.R.attr.colorOnPrimary,
        com.google.android.material.R.attr.colorSurface,
        android.R.attr.colorBackground,
        android.R.color.transparent
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

fun shareSong(context:Context, filePath:String){
    try {
        val file = File(filePath)
        if(!file.exists()){
            Toast.makeText(context, "File not exist", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type="*/*"
            putExtra(Intent.EXTRA_STREAM,uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent,context.getString(R.string.share_song_with)))

    }catch(e:Exception){
        e.printStackTrace()
        Log.e("SHARE_SONG_ERROR", e.message.toString() )
    }
}
fun uriToPathFileFromMediaStore(
    context: Context,
    uri: Uri,
): String? {

    val projection = arrayOf(MediaStore.Audio.Media.DATA)

    context.contentResolver.query(
        uri,
        projection,
        null,
        null,
        null
    )?.use { cursor ->

        val index = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
        if (index != -1 && cursor.moveToFirst()) {
            val path = cursor.getString(index)
            if (!path.isNullOrEmpty()) {
                return path
            }
        }
    }
    return null
}
