package com.barryzeha.mfilepicker.di

import android.app.Application
import android.content.Context
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.mfilepicker.common.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 4/8/24.
 * Copyright (c)  All rights reserved.
 **/
@Module
@InstallIn(SingletonComponent::class)
class AppModule {
 /*@Provides
 @Singleton
 fun contextProviders(app:Application):Context = app.applicationContext*/

 @Provides
 @Singleton
 fun preferencesProvides(context:Context): Preferences = Preferences(context)

}