package com.barryzeha.data.repository

import android.media.MediaPlayer
import android.util.Log
import androidx.media3.exoplayer.ExoPlayer
import com.barryzeha.core.common.BY_ALBUM
import com.barryzeha.core.common.BY_ARTIST
import com.barryzeha.core.common.BY_GENRE
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
    override suspend fun fetchAllSongs(): List<SongEntity> = withContext(Dispatchers.IO){
        dao.fetchAllSongs()
    }

    override suspend fun fetchAllSongsBy(field: Int): List<SongEntity> = withContext(Dispatchers.IO){
        when(field){
            BY_ALBUM-> dao.fetchAllSongByAlbum()
            BY_ARTIST-> dao.fetchAllSongByArtist()
            BY_GENRE-> dao.fetchAllSongByGenre()
            else->dao.fetchAllSongs()
        }
    }

    override suspend fun fetchAllFavorites(): List<SongEntity>  = withContext(Dispatchers.IO){
        dao.fetchAllFavorites()
    }

    override suspend fun fetchSongById(idSong: Long): SongEntity = withContext(Dispatchers.IO){
        dao.fetchSongById(idSong)
    }

    override suspend fun saveNewSong(song: SongEntity): Long = withContext(Dispatchers.IO) {
        dao.saveNewSong(song)
    }

    override suspend fun saveSongs(songList: List<SongEntity>): LongArray = withContext(Dispatchers.IO){
        dao.saveSongs(songList)
    }

    override suspend fun updateSong(song: SongEntity): Int = withContext(Dispatchers.IO){
        dao.updateSong(song)
    }

    override suspend fun updateFavoriteSong(isFavorite: Boolean, idSong: Long): Int = withContext(Dispatchers.IO){
        dao.updateFavoriteSong(isFavorite,idSong)
    }

    override suspend fun deleteSong(idSong: Long): Int= withContext(Dispatchers.IO) {
        dao.deleteSong(idSong)
    }

    override suspend fun deleteSong(songIds: List<Long>): Int = withContext(Dispatchers.IO){
        dao.deleteSong(songIds)
    }

    override suspend fun deleteAllSongs(): Int = withContext(Dispatchers.IO) {
        dao.deleteAllSongs()
    }

    // SongState
    override suspend fun fetchSongState(): List<SongStateWithDetail> = withContext(Dispatchers.IO){
        dao.fetchSongState()
    }

    override suspend fun saveSongState(songState: SongState): Long = withContext(Dispatchers.IO){
        dao.saveSongState(songState)

    }

    override suspend fun updateSongState(songState: SongState): Int = withContext(Dispatchers.IO){
        dao.updateSongState(songState)
    }



}