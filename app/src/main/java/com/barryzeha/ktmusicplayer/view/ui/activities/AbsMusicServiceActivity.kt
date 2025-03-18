package com.barryzeha.ktmusicplayer.view.ui.activities

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.IBinder
import androidx.activity.viewModels
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.common.startOrUpdateService
import com.barryzeha.core.model.ServiceSongListener
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.ktmusicplayer.service.MusicPlayerService
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 16/2/25.
 * Copyright (c)  All rights reserved.
 **/
@AndroidEntryPoint
open class AbsMusicServiceActivity : AppCompatActivity(),ServiceConnection, ServiceSongListener {
    private val mMusicPlayerServiceListeners = ArrayList<ServiceSongListener>()
    private var serviceConnection: ServiceConnection? = null
    open var musicPlayerService: MusicPlayerService? = null
    val mainViewModel:MainViewModel by viewModels()

    @Inject
    lateinit var defaultPrefs: SharedPreferences

    @Inject
    lateinit var mPrefs: MyPreferences

    fun registerMusicServiceListener(listener: ServiceSongListener?) {
        if (listener != null) {
            mMusicPlayerServiceListeners.add(listener)
        }
    }
    fun unregisterMusicServiceListener(listener: ServiceSongListener?) {
        if (listener != null) {
            mMusicPlayerServiceListeners.remove(listener)
        }
    }
    @CallSuper
    override fun onStart() {
        super.onStart()
        startOrUpdateService(this,MusicPlayerService::class.java,this)
    }
    @CallSuper
    override fun onResume() {
        super.onResume()
        musicPlayerService?.setSongController(this)
    }
    @CallSuper
    override fun onPause() {
        super.onPause()
        musicPlayerService?.unregisterController()
    }
    @CallSuper
    override fun onStop() {
        super.onStop()
        //Guardamos el número total de pistas en preferencias para mostrala luego en nuestras vistas ex: #4/500
        mPrefs.totalItemSongs=musicPlayerService?.getSongsList()?.count()!!
    }
    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        musicPlayerService?.unregisterController()
        musicPlayerService?.let{unbindService(this)}
    }

    override fun play() {
        for (listener in mMusicPlayerServiceListeners) {
            listener.play()
        }
    }

    override fun pause() {
        for (listener in mMusicPlayerServiceListeners) {
            listener.pause()
        }
    }

    override fun next() {
        for (listener in mMusicPlayerServiceListeners) {
            listener.next()
        }
    }

    override fun previous() {
        for (listener in mMusicPlayerServiceListeners) {
            listener.previous()
        }
    }

    override fun stop() {
        for (listener in mMusicPlayerServiceListeners) {
            listener.stop()
        }
    }

    override fun musicState(musicState: MusicState?) {
        for (listener in mMusicPlayerServiceListeners) {
            listener.musicState(musicState)
        }
    }

    override fun currentTrack(musicState: MusicState?) {
        for (listener in mMusicPlayerServiceListeners) {
            listener.currentTrack(musicState)
        }
    }

    override fun onServiceConnected(conn: ServiceConnection, service: IBinder?) { }
    override fun onServiceDisconnected() {
        musicPlayerService = null
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as MusicPlayerService.MusicPlayerServiceBinder
        serviceConnection = this
        musicPlayerService = binder.getService()
        musicPlayerService?.setActivity(this)
        musicPlayerService?.setSongController(this)
        mainViewModel.setServiceInstance(serviceConnection!!,musicPlayerService!!)
        for (listener in mMusicPlayerServiceListeners) {
            listener.onServiceConnected(serviceConnection!!, service)
        }
    }
    override fun onServiceDisconnected(name: ComponentName?) {
        for (listener in mMusicPlayerServiceListeners) {
            listener.onServiceDisconnected()
        }
    }
}
