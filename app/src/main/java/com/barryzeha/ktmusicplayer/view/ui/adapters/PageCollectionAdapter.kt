package com.barryzeha.ktmusicplayer.view.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.barryzeha.core.common.*
import com.barryzeha.ktmusicplayer.view.ui.fragments.AlbumDetailFragment
import com.barryzeha.ktmusicplayer.view.ui.fragments.ListPlayerFragment
import com.barryzeha.ktmusicplayer.view.ui.fragments.MainPlayerFragment
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 23/4/24.
 * Copyright (c)  All rights reserved.
 **/


class PageCollectionAdapter(mainViewModel: MainViewModel,fragment:FragmentActivity, private val listOfTitles:List<String>):FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = listOfTitles.size

    override fun createFragment(position: Int): Fragment {
        return when(position){
            MAIN_FRAGMENT-> MainPlayerFragment()
            SONG_LIST_FRAGMENT-> ListPlayerFragment()
            else-> MainPlayerFragment()
        }

    }

}