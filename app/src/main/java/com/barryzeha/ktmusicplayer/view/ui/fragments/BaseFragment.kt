package com.barryzeha.ktmusicplayer.view.ui.fragments

import android.content.Context
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.model.ServiceSongListener
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.ktmusicplayer.service.MusicPlayerService
import com.barryzeha.ktmusicplayer.view.ui.activities.MainActivity
import com.barryzeha.ktmusicplayer.view.ui.adapters.MusicListAdapter
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/6/24.
 * Copyright (c)  All rights reserved.
 **/
@AndroidEntryPoint
open class BaseFragment(@LayoutRes layout: Int) : Fragment(layout), ServiceSongListener {

    @Inject
    lateinit var defaultPrefs: SharedPreferences

    @Inject
    lateinit var mPrefs:MyPreferences
    var baseActivity: MainActivity? = null
        private set
    protected val mainViewModel: MainViewModel by viewModels(ownerProducer = {requireActivity()})
    protected var serviceConnection:ServiceConnection?=null
    protected var musicPlayerService: MusicPlayerService?=null
    protected var isUserSeeking=false
    protected var userSelectPosition=0


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
        setUpObservers()
    }
    private fun setUpObservers(){
        mainViewModel.serviceInstance.observe(viewLifecycleOwner){(serviceConn, serviceInst)->
            serviceConnection=serviceConn
            musicPlayerService=serviceInst
        }
    }
    @CallSuper
    override fun onResume() {
        super.onResume()
        baseActivity?.registerSongListener(this)

    }
    @CallSuper
    override fun onPause() {
        super.onPause()
        baseActivity?.unregisterSongListener()
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
    override fun onServiceConnected(conn: ServiceConnection, service: IBinder?) {
    }
    override fun onServiceDisconnected() {}

}