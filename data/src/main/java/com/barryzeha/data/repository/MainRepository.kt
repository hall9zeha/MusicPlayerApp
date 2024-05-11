package com.barryzeha.data.repository

import android.media.MediaPlayer
import com.barryzeha.core.entities.SongEntity
import kotlinx.coroutines.flow.Flow


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/4/24.
 * Copyright (c)  All rights reserved.
 **/

interface MainRepository {
    // Room Database
    suspend fun fetchAllSongs():List<SongEntity>
    suspend fun fetchSongById(idSong:Long):SongEntity
    suspend fun saveNewSong(song:SongEntity):Long
    suspend fun deleteSong(idSong:Long):Int

    // UI Flows
    suspend fun fetchCurrentTimeOfSong(mediaPlayer:MediaPlayer): Flow<Triple<Int,Int,String>>
}