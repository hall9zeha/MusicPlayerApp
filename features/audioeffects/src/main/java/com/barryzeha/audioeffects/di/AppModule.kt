package com.barryzeha.audioeffects.di

import android.content.Context
import com.barryzeha.audioeffects.common.EffectsPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/8/24.
 * Copyright (c)  All rights reserved.
 **/

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

 @Provides
 @Singleton
 fun preferencesProvides(context:Context):EffectsPreferences = EffectsPreferences(context)
}