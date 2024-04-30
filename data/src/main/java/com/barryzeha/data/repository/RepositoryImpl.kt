package com.barryzeha.data.repository

import com.barryzeha.core.entities.SongEntity


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/4/24.
 * Copyright (c)  All rights reserved.
 **/

class RepositoryImpl:Repository {
    override suspend fun fetchAllSongs(): List<SongEntity> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchSongById(idSong: Long): SongEntity {
        TODO("Not yet implemented")
    }

    override suspend fun saveNewSong(song: SongEntity): Long {
        TODO("Not yet implemented")
    }

    override suspend fun deleteSong(idSong: Long): Int {
        TODO("Not yet implemented")
    }
}