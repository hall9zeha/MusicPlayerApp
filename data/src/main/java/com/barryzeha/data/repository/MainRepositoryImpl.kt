package com.barryzeha.data.repository

import android.media.MediaPlayer
import com.barryzeha.core.common.createTime
import com.barryzeha.core.entities.SongEntity
import com.barryzeha.data.database.SongDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/4/24.
 * Copyright (c)  All rights reserved.
 **/

class MainRepositoryImpl @Inject constructor(db: SongDatabase):MainRepository {
    private val dao = db.getSongDao()
    // Room database
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

    // UI Flows
    override suspend fun fetchCurrentTimeOfSong(mediaPlayer:MediaPlayer): Flow<String> {
        return flow{
                while(true) {
                    if(mediaPlayer.isPlaying) {
                        emit(createTime(mediaPlayer.currentPosition))
                        delay(1000)
                    }
                }
            }.flowOn(Dispatchers.IO)
        }

}