package com.barryzeha.ktmusicplayer.di

import android.graphics.pdf.PdfDocument.Page
import androidx.fragment.app.FragmentActivity
import com.barryzeha.ktmusicplayer.view.ui.adapters.PageCollectionAdapter
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 27/6/24.
 * Copyright (c)  All rights reserved.
 **/

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    @Singleton
    fun pageAdapterProvides(mainViewModel: MainViewModel, fragment:FragmentActivity,titleList:List<String>):PageCollectionAdapter =
        PageCollectionAdapter(mainViewModel,fragment,titleList)
}