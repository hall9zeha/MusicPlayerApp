package com.barryzeha.ktmusicplayer.view.ui.fragments.playerControls

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.barryzeha.ktmusicplayer.view.ui.adapters.MusicListAdapter
import com.barryzeha.ktmusicplayer.view.ui.fragments.AbsBaseFragment
import com.barryzeha.ktmusicplayer.view.ui.fragments.BaseFragment
import com.barryzeha.ktmusicplayer.view.ui.fragments.playlistFragment.ListFragment


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 9/2/25.
 * Copyright (c)  All rights reserved.
 **/

abstract class AbsPlaybackControlsFragment(@LayoutRes layout:Int): AbsBaseFragment(layout) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}