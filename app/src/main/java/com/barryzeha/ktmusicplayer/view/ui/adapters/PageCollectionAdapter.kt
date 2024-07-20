package com.barryzeha.ktmusicplayer.view.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.barryzeha.core.common.*
import com.barryzeha.ktmusicplayer.view.ui.fragments.ListPlayerFragment
import com.barryzeha.ktmusicplayer.view.ui.fragments.MainPlayerFragment
import com.barryzeha.ktmusicplayer.view.ui.fragments.SettingsFragment
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 23/4/24.
 * Copyright (c)  All rights reserved.
 **/


class PageCollectionAdapter(mainViewModel: MainViewModel,fragment:FragmentActivity, private val listOfTitles:List<String>):FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = listOfTitles.size
    init {
        mainViewModel.fetchAllSong()

    }
    override fun createFragment(position: Int): Fragment {
        return when(position){
            MAIN_FRAGMENT-> MainPlayerFragment()
            SONG_LIST_FRAGMENT-> ListPlayerFragment()
            SETTINGS_FRAGMENT -> SettingsFragment()
            else-> MainPlayerFragment()
        }

    }

}