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
    @Query("select * from SongEntity order by album ASC")
    suspend fun fetchAllSongByAlbum():List<SongEntity>

    @Transaction
    @Query("select * from SongEntity order by genre ASC")
    suspend fun fetchAllSongByGenre():List<SongEntity>

    @Transaction
    @Query("select * from SongEntity order by artist ASC")
    suspend fun fetchAllSongByArtist():List<SongEntity>

    // boolean literal "TRUE" is "1" in room database register
    @Transaction
    @Query("select * from SongEntity where favorite=1")
    suspend fun fetchAllFavorites():List<SongEntity>

    @Transaction
    @Query("select * from SongEntity where id = :idSong")
    suspend fun fetchSongById(idSong:Long): SongEntity
    @Transaction
    @Insert
    suspend fun saveNewSong(song: SongEntity):Long

    @Transaction
    @Insert
    suspend fun saveSongs(songList:List<SongEntity>):LongArray

    @Transaction
    @Update
    suspend fun updateSong(song:SongEntity):Int

    @Transaction
    @Query("update SongEntity set favorite=:isFavorite where id=:idSong")
    suspend fun updateFavoriteSong(isFavorite:Boolean, idSong: Long):Int

    @Query("delete  from SongEntity where id = :idSong ")
    suspend fun deleteSong(idSong:Long):Int

    @Transaction
    @Query("delete from SongEntity where id in (:ids)")
    suspend fun deleteSong(ids:List<Long>):Int

    @Transaction
    @Query("delete from SongEntity")
    suspend fun deleteAllSongs():Int

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