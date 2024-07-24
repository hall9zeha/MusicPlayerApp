package com.barryzeha.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.barryzeha.core.common.MyPreferences

import com.barryzeha.data.database.SongDatabase
import com.barryzeha.data.repository.MainRepository
import com.barryzeha.data.repository.MainRepositoryImpl
import dagger.Binds
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

private const val DATABASE_NAME = "SongDatabase"
@Module
@InstallIn(SingletonComponent::class)
class MainModule {
    @Provides
    @Singleton
    fun contextProvides(app:Application):Context = app.applicationContext

    @Provides
    @Singleton
    fun databaseProvides(app:Application) = Room.databaseBuilder(app.applicationContext,
        SongDatabase::class.java,
        DATABASE_NAME)
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    @Singleton
    fun preferencesProvides(app:Application):MyPreferences = MyPreferences(app.applicationContext)

}
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule{
    @Binds
    abstract fun mainRepositoryProvides (repository: MainRepositoryImpl):MainRepository
}