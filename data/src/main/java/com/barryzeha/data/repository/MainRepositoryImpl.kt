package com.barryzeha.data.repository

import com.barryzeha.core.entities.SongEntity
import com.barryzeha.data.database.SongDatabase
import javax.inject.Inject


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/4/24.
 * Copyright (c)  All rights reserved.
 **/

class MainRepositoryImpl @Inject constructor(db: SongDatabase):MainRepository {
    private val dao = db.getSongDao()
    override suspend fun fetchAllSongs(): List<SongEntity> {
        return dao.fetchAllSongs()
    }

    override suspend fun fetchSongById(idSong: Long): SongEntity {
        return dao.fetchSongById(idSong)
    }

    override suspend fun saveNewSong(song: SongEntity): Long {
        return dao.saveNewSong(song)
    }

    override suspend fun deleteSong(idSong: Long): Int {
        return dao.deleteSong(idSong)
    }
}