package com.barryzeha.ktmusicplayer.view.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.barryzeha.ktmusicplayer.common.MAIN_FRAGMENT
import com.barryzeha.ktmusicplayer.common.SONG_LIST_FRAGMENT
import com.barryzeha.ktmusicplayer.view.fragments.ListPlayerFragment
import com.barryzeha.ktmusicplayer.view.fragments.MainPlayerFragment


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 23/4/24.
 * Copyright (c)  All rights reserved.
 **/

class PageCollectionAdapter(fragment:FragmentActivity, private val listOfTitles:List<String>):FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = listOfTitles.size

    override fun createFragment(position: Int): Fragment {
        return when(position){
            MAIN_FRAGMENT->MainPlayerFragment()
            SONG_LIST_FRAGMENT->ListPlayerFragment()
            else->MainPlayerFragment()
        }

    }

}