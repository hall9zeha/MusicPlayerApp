package com.barryzeha.ktmusicplayer.view.ui.fragments

import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.barryzeha.ktmusicplayer.view.ui.adapters.MusicListAdapter


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 9/2/25.
 * Copyright (c)  All rights reserved.
 **/

abstract class AbsBaseFragment(@LayoutRes layout:Int):BaseFragment(layout) {
  open val recyclerView:RecyclerView?=null
}