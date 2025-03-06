package com.barryzeha.ktmusicplayer.view.ui.activities

import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Bundle
import android.os.IBinder
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.common.startOrUpdateService
import com.barryzeha.core.model.ServiceSongListener
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.ktmusicplayer.service.MusicPlayerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 16/2/25.
 * Copyright (c)  All rights reserved.
 **/
@AndroidEntryPoint
open class AbsMusicServiceActivity : AppCompatActivity(), ServiceSongListener {
    private val mMusicPlayerServiceListeners = ArrayList<ServiceSongListener>()
    private var serviceConnection: ServiceConnection? = null
    open var musicPlayerService: MusicPlayerService? = null

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
    override fun onDestroy() {
        super.onDestroy()
        musicPlayerService?.unregisterController()
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

    override fun onServiceConnected(conn: ServiceConnection, service: IBinder?) {
        val binder = service as MusicPlayerService.MusicPlayerServiceBinder
        serviceConnection = conn
        musicPlayerService = binder.getService()
        for (listener in mMusicPlayerServiceListeners) {
            listener.onServiceConnected(conn, service)
        }

    }

    override fun onServiceDisconnected() {
        for (listener in mMusicPlayerServiceListeners) {
            listener.onServiceDisconnected()
        }
    }
}
