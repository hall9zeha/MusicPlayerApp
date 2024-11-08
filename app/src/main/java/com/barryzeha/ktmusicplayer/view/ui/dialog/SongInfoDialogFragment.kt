package com.barryzeha.ktmusicplayer.view.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.core.R as coreRes
import com.barryzeha.ktmusicplayer.databinding.SongInfoLayoutBinding


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 8/11/24.
 * Copyright (c)  All rights reserved.
 **/

class SongInfoDialogFragment: DialogFragment() {
 private  var _bind:SongInfoLayoutBinding?=null
 private val bind:SongInfoLayoutBinding get() = _bind!!

 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setStyle(STYLE_NORMAL, coreRes.style.myFullScreenDialog)
 }

 override fun onCreateView(
  inflater: LayoutInflater,
  container: ViewGroup?,
  savedInstanceState: Bundle?
 ): View? {
  activity?.let{
    _bind = SongInfoLayoutBinding.inflate(inflater, container,false)
    _bind?.let{b->
      // Listeners of header icons
      return b.root
    }
  }
  return super.onCreateView(inflater, container, savedInstanceState)
 }

 override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
  super.onViewCreated(view, savedInstanceState)
 }

}