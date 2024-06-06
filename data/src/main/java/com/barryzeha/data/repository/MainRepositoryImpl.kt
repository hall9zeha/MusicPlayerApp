package com.barryzeha.data.repository

import android.media.MediaPlayer
import androidx.media3.exoplayer.ExoPlayer
import com.barryzeha.core.common.createTime
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.core.model.entities.SongState
import com.barryzeha.core.model.entities.SongStateWithDetail
import com.barryzeha.data.database.SongDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
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

    // SongState
    override suspend fun fetchSongState(): List<SongStateWithDetail> {
        return dao.fetchSongState()
    }

    override suspend fun saveSongState(songState: SongState): Long {
        return dao.saveSongState(songState)
    }

    override suspend fun updateSongState(songState: SongState): Int {
        return dao.updateSongState(songState)
    }

    // UI Flows
    override suspend fun fetchCurrentTimeOfSong(mediaPlayer:ExoPlayer): Flow<Triple<Int,Int,String>> {
        return flow{
                while(true) {
                    val isPlaying = withContext(Dispatchers.Main){ mediaPlayer.isPlaying}
                    if(isPlaying) {
                        val formattedTime= withContext(Dispatchers.Main){createTime(mediaPlayer.currentPosition)}
                        emit(formattedTime)
                        delay(1000)
                    }
                }
            }.flowOn(Dispatchers.IO)
        }

}