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

// Función para procesar múltiples rutas de directorios de forma secuencial
fun processSongPaths(
    paths: List<String>,  // Lista de directorios a procesar
    fileProcessed: (realPathFromFile: String, parentDir: String, audioMetaData: AudioMetadata) -> Unit
) {
    val channel = Channel<File>(Channel.UNLIMITED)  // Canal sin límite de buffer

    // Corutina para encolar archivos en el canal
    CoroutineScope(Dispatchers.IO).launch {
        try {
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
            processFile(file, MyApp.context, fileProcessed)

        }
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

private suspend fun processFile(
    file: File,
    context: Context,
    fileProcessed: (realPathFromFile: String, parentDir: String, audioMetaData: AudioMetadata) -> Unit
){
    if (AudioFileType().verify(file.absolutePath)) {
        val uri = getUriFromFile(file, context)
        operationsMutex.withLock {
            val realPathFromFile = getRealPathFromURI(uri!!, context)
            val parentDir = getParentDirectories(uri.path.toString())
            val metadata = fetchFileMetadata(context, realPathFromFile!!)


        /* Log.e("ITEM-FILE  ->", filePath)
            Log.e("ITEM-FILE  ->", uri.toString())
            Log.e("ITEM-FILE  ->", realPathFromFile.toString())
            Log.e("ITEM-FILE  ->", parentDir ?: "")

         */
         // Guardar en el ViewModel si es necesario
            withContext(Dispatchers.Main) {
                fileProcessed(realPathFromFile,parentDir,metadata)
            }
        }

    }

}

