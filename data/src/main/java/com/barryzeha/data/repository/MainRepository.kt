package com.barryzeha.data.repository

import android.media.MediaPlayer
import androidx.media3.exoplayer.ExoPlayer
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.core.model.entities.SongState
import com.barryzeha.core.model.entities.SongStateWithDetail
import kotlinx.coroutines.flow.Flow


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/4/24.
 * Copyright (c)  All rights reserved.
 **/

interface MainRepository {
    // Room Database
    // SongEntity
    suspend fun fetchAllSongs():List<SongEntity>
    suspend fun fetchAllSongsBy(field:Int):List<SongEntity>
    suspend fun fetchSongById(idSong:Long): SongEntity
    suspend fun saveNewSong(song: SongEntity):Long
    suspend fun updateSong(song:SongEntity):Int
    suspend fun deleteSong(idSong:Long):Int
    suspend fun deleteAllSongs():Int

    // SongState
    suspend fun fetchSongState():List<SongStateWithDetail>
    suspend fun saveSongState(songState: SongState):Long
    suspend fun updateSongState(songState: SongState):Int

    // UI Flows
    suspend fun fetchCurrentTimeOfSong(mediaPlayer:ExoPlayer): Flow<Triple<Int,Int,String>>
}