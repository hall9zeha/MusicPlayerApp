package com.barryzeha.ktmusicplayer.view.ui.fragments

import android.content.Context
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.barryzeha.core.model.ServiceSongListener
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.ktmusicplayer.view.ui.activities.MainActivity


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/6/24.
 * Copyright (c)  All rights reserved.
 **/

open class BaseFragment(@LayoutRes layout: Int) : Fragment(layout), ServiceSongListener {
    var baseActivity: MainActivity? = null
        private set
    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            baseActivity = context as MainActivity?
        } catch (e: ClassCastException) {
            Log.e("EXCEPTION", e.message.toString())
        }
    }
    override fun onDetach() {
        super.onDetach()
        baseActivity = null

    }
    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        baseActivity?.registerSongListener(this)
    }
    @CallSuper
    override fun onResume() {
        super.onResume()
        baseActivity?.registerSongListener(this)
    }
    @CallSuper
    override fun onDestroyView() {
        super.onDestroyView()
        baseActivity?.unregisterSongListener()
    }
    override fun play() {}
    override fun pause() {}
    override fun next() {}
    override fun previous() {}
    override fun stop() {}
    override fun musicState(musicState: MusicState?) {}
    override fun currentTrack(musicState: MusicState?) {}
    override fun onServiceConnected(conn: ServiceConnection, service: IBinder?) {}
    override fun onServiceDisconnected() {}

}