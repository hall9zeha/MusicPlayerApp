package com.barryzeha.ktmusicplayer.common

import android.content.Context
import com.barryzeha.core.common.fetchFileMetadata
import com.barryzeha.core.common.getParentDirectories
import com.barryzeha.core.common.getRealPathFromURI
import com.barryzeha.core.common.getUriFromFile
import com.barryzeha.core.model.entities.AudioMetadata
import com.barryzeha.ktmusicplayer.MyApp
import com.barryzeha.mfilepicker.filetype.AudioFileType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 8/8/24.
 * Copyright (c)  All rights reserved.
 **/

private  const  val CONSUMER_COUNT = 4
private val operationsMutex = Mutex()

fun processSongPaths(path:String?=null,
                     baseDirectory: String? = null,
                     fileProcessed:(realPathFromFile:String,parentDir:String,audioMetaData:AudioMetadata)->Unit){
    if (path != null) {
        val channel = Channel<String>(Channel.UNLIMITED)  // Canal sin límite de buffer

        // Lanzar una corutina para enviar rutas al canal
        CoroutineScope(Dispatchers.IO).launch {
            try {
                enqueueFiles(File(path), baseDirectory ?: "", channel)
            } finally {
                channel.close()  // Cerrar el canal cuando haya terminado de enviar datos
            }
        }

        // Lanzar corutinas consumidoras para procesar archivos
        repeat(CONSUMER_COUNT) {
            CoroutineScope(Dispatchers.IO).launch {
                for (filePath in channel) {
                    processFile(filePath, MyApp.context,fileProcessed)
                }
            }
        }
    }
}
private fun enqueueFiles(file: File, baseDir: String, channel: Channel<String>) {
    if (file.isDirectory) {
        // Encolar archivos en el directorio recursivamente
        file.listFiles()?.forEach { subFile ->
            enqueueFiles(subFile, baseDir, channel)
        }
    } else {
        // Enviar ruta del archivo al canal
        channel.trySend(file.absolutePath).isSuccess
    }
}

private suspend fun processFile(
    filePath: String,
    context: Context,
    fileProcessed: (realPathFromFile: String, parentDir: String, audioMetaData: AudioMetadata) -> Unit
) {
    if (AudioFileType().verify(filePath)) {
        val uri = if (AudioFileType().verify(filePath)) {
            getUriFromFile(File(filePath), context)
        } else {
            getUriFromFile(File(filePath), context)
        }
        operationsMutex.withLock {
            val realPathFromFile = getRealPathFromURI(uri!!, context)
            val parentDir = getParentDirectories(uri.path.toString())
            val metadata = fetchFileMetadata(context, realPathFromFile!!)

           /* Log.e("ITEM-FILE  ->", filePath)
            Log.e("ITEM-FILE  ->", uri.toString())
            Log.e("ITEM-FILE  ->", realPathFromFile.toString())
            Log.e("ITEM-FILE  ->", parentDir ?: "")*/

            // Guardar en el ViewModel si es necesario
            withContext(Dispatchers.Main) {
                fileProcessed(realPathFromFile,parentDir,metadata)
            }
        }
    }
}