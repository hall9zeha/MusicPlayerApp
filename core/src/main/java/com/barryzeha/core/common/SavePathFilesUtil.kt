package com.barryzeha.core.common

import android.annotation.SuppressLint
import android.content.Context
import com.barryzeha.core.model.entities.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 8/8/24.
 * Copyright (c)  All rights reserved.
 **/


private var audioFileCount:Int=0
private var audioFilesFound: MutableList<SongEntity> = mutableListOf()
private var audioFilesPath:MutableList<String> = mutableListOf()
@SuppressLint("StaticFieldLeak")
private var mPrefs: MyPreferences?=null
// Function to process multiple directory paths sequentially
fun processSongPaths(
    context: Context,
    preferences: MyPreferences,
    selectedPaths: List<String>,  // List of directories of the user selected to process
    itemsCount:(itemsNum:Int)->Unit,
    fileProcessed: (song:SongEntity) -> Unit,
    scanCompleted:suspend (Pair<List<SongEntity>,List<String>>)->Unit
) {
   audioFileCount=0
    // Coroutine to queue files in the channel
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // We count how many audio files there are
            selectedPaths.forEach { path ->
                countAudioFile(File(path))
            }
            itemsCount(audioFileCount)

            // Enqueue files from all directories
            /*paths.forEach { path ->
                enqueueFiles(File(path),fileProcessed)
            }*/
            // Testing the new function to scan audio files and return their paths
            scanAudioFiles(context,preferences,selectedPaths,fileProcessed,scanCompleted)
        } finally {

        }
    }
}
suspend fun scanAudioFiles(context:Context, preferences:MyPreferences, paths: List<String>, fileProcessed: (SongEntity) -> Unit, scanCompleted:suspend (Pair<List<SongEntity>,List<String>>)->Unit){
    mPrefs=preferences
    paths.forEach { path ->
        enqueueFiles(context,File(path),fileProcessed)
    }
    scanCompleted(Pair(audioFilesFound.toList(),audioFilesPath.toList()))
    audioFilesFound.clear()
    audioFilesPath.clear()
}
private suspend fun enqueueFiles(context:Context,file: File,fileProcessed: (SongEntity) -> Unit) {
    if (file.isDirectory) {
        // Queue files in the directory recursively
        file.listFiles()?.forEach { subFile ->
            enqueueFiles(context,subFile,fileProcessed)
        }
    } else {
        // Send file to process
        processFile(file,context, fileProcessed)
    }
}
private fun countAudioFile(file: File) {
    if (file.isDirectory) {
        file.listFiles()?.forEach { subFile ->
            countAudioFile(subFile)
        }
    } else {
        // We check that it is an audio file
        if(verifyAudioFile(file.name))audioFileCount++
    }
}
private suspend fun processFile(
    file: File,
    context: Context,
    fileProcessed: (SongEntity) -> Unit
){
    if (verifyAudioFile(file.name)) {
            val realPathFromFile = file.absolutePath
            val parentDir = getParentDirectories(file.path.toString())
            val metadata = fetchCompleteFileMetadata(context, realPathFromFile)
            metadata?.let {
                val song = SongEntity(
                    idPlaylistCreator = mPrefs?.playlistId?.toLong()?:0,
                    pathLocation = realPathFromFile,
                    parentDirectory = parentDir,
                    description = metadata.title,
                    duration = metadata.songLength,
                    bitrate = metadata.bitRate,
                    artist = metadata.artist,
                    album = metadata.album,
                    genre = metadata.genre,
                    timestamp = Date().time
                )
                audioFilesFound.add(song)
                audioFilesPath.add(realPathFromFile)
                // Output metadata of the processed file
                withContext(Dispatchers.Main) {
                    metadata?.let {
                        fileProcessed(song)
                    }
                }
            }
    }
}

