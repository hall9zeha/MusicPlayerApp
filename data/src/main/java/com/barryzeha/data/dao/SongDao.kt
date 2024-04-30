package com.barryzeha.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.barryzeha.core.entities.SongEntity


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/4/24.
 * Copyright (c)  All rights reserved.
 **/

@Dao
interface  SongDao {

    @Query("select * from SongEntity")
    suspend fun fetchAllSongs():List<SongEntity>
    @Query("select * from SongEntity where id = :idSong")
    suspend fun fetchSongById(idSong:Long):SongEntity
    @Insert
    suspend fun saveNewSong(song:SongEntity):Long
    @Query("delete  from SongEntity where id = :idSong ")
    suspend fun deleteSong(idSong:Long):Int


}