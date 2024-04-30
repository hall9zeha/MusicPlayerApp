package com.barryzeha.data.repository

import com.barryzeha.core.entities.SongEntity


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/4/24.
 * Copyright (c)  All rights reserved.
 **/

interface MainRepository {
    suspend fun fetchAllSongs():List<SongEntity>
    suspend fun fetchSongById(idSong:Long):SongEntity
    suspend fun saveNewSong(song:SongEntity):Long
    suspend fun deleteSong(idSong:Long):Int

}