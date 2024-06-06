package com.barryzeha.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.core.model.entities.SongState
import com.barryzeha.core.model.entities.SongStateWithDetail


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/4/24.
 * Copyright (c)  All rights reserved.
 **/

@Dao
interface  SongDao {

    // SongEntity
    @Transaction
    @Query("select * from SongEntity")
    suspend fun fetchAllSongs():List<SongEntity>
    @Transaction
    @Query("select * from SongEntity where id = :idSong")
    suspend fun fetchSongById(idSong:Long): SongEntity
    @Transaction
    @Insert
    suspend fun saveNewSong(song: SongEntity):Long
    @Transaction
    @Query("delete  from SongEntity where id = :idSong ")
    suspend fun deleteSong(idSong:Long):Int

    // SongState
    @Transaction
    @Query("select * from SongState")
    suspend fun fetchSongState():List<SongStateWithDetail>
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSongState(songState: SongState):Long
    @Transaction
    @Update
    suspend fun updateSongState(songState: SongState):Int

}