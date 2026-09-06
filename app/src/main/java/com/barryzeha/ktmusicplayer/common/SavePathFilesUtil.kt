package com.barryzeha.ktmusicplayer.common

import android.content.Context
import com.barryzeha.core.common.fetchCompleteFileMetadata
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.ktmusicplayer.MyApp
import com.barryzeha.mfilepicker.common.util.getParentDirectories
import com.barryzeha.mfilepicker.filetype.AudioFileType
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
// Function to process multiple directory paths sequentially
fun processSongPaths(
    paths: List<String>,  // List of directories to process
    itemsCount:(itemsNum:Int)->Unit,
    fileProcessed: (song:SongEntity) -> Unit
) {
   audioFileCount=0
    // Coroutine to queue files in the channel
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // We count how many audio files there are
            paths.forEach { path ->
                countAudioFile(File(path))
            }
            itemsCount(audioFileCount)
            scanAudioFiles(paths,fileProcessed){}
        } finally {

        }
    }
}
suspend fun scanAudioFiles(paths: List<String>, fileProcessed: (SongEntity) -> Unit,audioFilesScanFound:(List<SongEntity>)->Unit){
    paths.forEach { path ->
        enqueueFiles(File(path),fileProcessed)
    }
    audioFilesScanFound(audioFilesFound.toList())
    audioFilesFound.clear()
}
private suspend fun enqueueFiles(file: File,fileProcessed: (SongEntity) -> Unit) {
    if (file.isDirectory) {
        // Queue files in the directory recursively
        file.listFiles()?.forEach { subFile ->
            enqueueFiles(subFile,fileProcessed)
        }
    } else {
        // Send file to process
        processFile(file,MyApp.context, fileProcessed)
    }
}
private fun countAudioFile(file: File) {
    if (file.isDirectory) {
        file.listFiles()?.forEach { subFile ->
            countAudioFile(subFile)
        }
    } else {
        // We check that it is an audio file
        if(AudioFileType().verify(file.name))audioFileCount++
    }
}
private suspend fun processFile(
    file: File,
    context: Context,
    fileProcessed: (SongEntity) -> Unit
){
    if (AudioFileType().verify(file.name)) {
            val realPathFromFile = file.absolutePath
            val parentDir = getParentDirectories(file.path.toString())
            val metadata = fetchCompleteFileMetadata(context, realPathFromFile)
            metadata?.let {
                val song = SongEntity(
                    idPlaylistCreator = MyApp.mPrefs.playlistId.toLong(),
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
                // Output metadata of the processed file
                withContext(Dispatchers.Main) {
                    metadata?.let {
                        fileProcessed(song)
                    }
                }
            }
    }
}

