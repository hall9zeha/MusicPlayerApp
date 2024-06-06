package com.barryzeha.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.core.model.entities.SongState
import com.barryzeha.data.dao.SongDao



/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/4/24.
 * Copyright (c)  All rights reserved.
 **/

@Database(entities = [SongEntity::class, SongState::class], version = 1, exportSchema = false)
abstract class SongDatabase:RoomDatabase() {
    abstract fun getSongDao(): SongDao
}