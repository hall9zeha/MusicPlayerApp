package com.barryzeha.ktmusicplayer.common

import android.content.Context
import com.barryzeha.core.common.fetchFileMetadata
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.ktmusicplayer.MyApp
import com.barryzeha.mfilepicker.common.util.getParentDirectories
import com.barryzeha.mfilepicker.filetype.AudioFileType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 8/8/24.
 * Copyright (c)  All rights reserved.
 **/

private  const  val CONSUMER_COUNT = 4
private val operationsMutex = Mutex()
private var audioFileCount:Int=0
// Función para procesar múltiples rutas de directorios de forma secuencial
fun processSongPaths(
    paths: List<String>,  // Lista de directorios a procesar
    itemsCount:(itemsNum:Int)->Unit,
    fileProcessed: (song:SongEntity) -> Unit
    //filesProcessed:(List<SongEntity>)->Unit
) {
    val channel = Channel<File>(Channel.UNLIMITED)  // Canal sin límite de buffer
    audioFileCount=0
    var listFilesProcessed:MutableList<SongEntity> = arrayListOf()

    // Corutina para encolar archivos en el canal
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // Contamos cuantos archivos de audio hay
            paths.forEach { path ->
                countAudioFile(File(path))
            }
            itemsCount(audioFileCount)

            // Encolar archivos de todos los directorios
            paths.forEach { path ->
                enqueueFiles(File(path), channel)
            }
        } finally {
            channel.close()  // Cerrar el canal cuando haya terminado de enviar datos
        }
    }
    // Corutina única para procesar archivos secuencialmente
     CoroutineScope(Dispatchers.IO).launch {
            for (file in channel) {
                processFile(file, MyApp.context,fileProcessed)
                /*processFile(file, MyApp.context)?.let{song->
                    listFilesProcessed.add(song)
                }*/

            }
            //filesProcessed(listFilesProcessed)
        }
}

private fun enqueueFiles(file: File, channel: Channel<File>) {
    if (file.isDirectory) {
        // Encolar archivos en el directorio recursivamente
        file.listFiles()?.forEach { subFile ->
            enqueueFiles(subFile, channel)
        }
    } else {
            // Enviar archivo al canal
            channel.trySend(file).isSuccess
    }
}
private fun countAudioFile(file: File) {
    if (file.isDirectory) {
        file.listFiles()?.forEach { subFile ->
            countAudioFile(subFile)
        }
    } else {
        //Comprobamos que sea un archivo de audio
        if(AudioFileType().verify(file.name))audioFileCount++
    }
}
private suspend fun processFile(
    file: File,
    context: Context,
    fileProcessed: (SongEntity) -> Unit
){
    if (AudioFileType().verify(file.absolutePath)) {
        operationsMutex.withLock {
            val realPathFromFile = file.absolutePath
            val parentDir = getParentDirectories(file.path.toString())
            val metadata = fetchFileMetadata(context, realPathFromFile!!)
         /*Log.e("ITEM-FILE  ->", filePath)
            Log.e("ITEM-FILE  ->", uri.toString())
            Log.e("ITEM-FILE  ->", realPathFromFile.toString())
            Log.e("ITEM-FILE  ->", parentDir ?: "")*/
           val song = SongEntity(
                pathLocation = realPathFromFile,
                parentDirectory = parentDir,
                description = metadata!!.title,
                duration = metadata!!.songLength,
                bitrate = metadata!!.bitRate,
                artist = metadata!!.artist!!,
                album = metadata!!.album!!,
                genre = metadata!!.genre!!,
                timestamp = Date().time
            )
         // Guardar en el ViewModel si es necesario
            withContext(Dispatchers.Main) {
                metadata?.let {
                    fileProcessed(song)
                }
            }
        }

    }

}/*
private suspend fun processFile(
    file: File,
    context: Context
):SongEntity?{
    if (AudioFileType().verify(file.absolutePath)) {
        val uri = getUriFromFile(file, context)
        operationsMutex.withLock {
            val realPathFromFile = getRealPathFromURI(uri, context)
            val parentDir = getParentDirectories(uri.path.toString())
            val metadata = fetchFileMetadata(context, realPathFromFile!!)
            metadata?.let {
                return SongEntity(
                    pathLocation = realPathFromFile,
                    parentDirectory = parentDir,
                    description = metadata.title,
                    duration = metadata.songLength,
                    bitrate = metadata.bitRate,
                    artist = metadata.artist!!,
                    album = metadata.album!!,
                    genre = metadata.genre!!,
                    timestamp = Date().time
                )
           }
        }

    }
    return null
}
*/


