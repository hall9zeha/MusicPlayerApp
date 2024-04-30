package com.barryzeha.di

import android.app.Application
import androidx.room.Room
import com.barryzeha.data.database.SongDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/4/24.
 * Copyright (c)  All rights reserved.
 **/

private const val DATABASE_NAME = "MusicDatabase"
@Module
@InstallIn(SingletonComponent::class)
class MainModule {
    @Provides
    @Singleton
    fun databaseProvides(app:Application) = Room.databaseBuilder(app.applicationContext,
        SongDatabase::class.java,
        DATABASE_NAME)
        .fallbackToDestructiveMigration()
        .build()

}