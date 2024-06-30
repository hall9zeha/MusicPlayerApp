package com.barryzeha.core.model

import android.content.ServiceConnection
import android.os.IBinder
import com.barryzeha.core.model.entities.MusicState


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 16/5/24.
 * Copyright (c)  All rights reserved.
 **/

interface ServiceSongListener {
 fun play()
 fun pause()
 fun next()
 fun previous()
 fun stop()
 fun musicState(musicState: MusicState?)
 fun currentTrack(musicState: MusicState?)
 fun onServiceConnected(conn: ServiceConnection, service: IBinder?)
 fun onServiceBinder(binder: IBinder?)
 fun onServiceDisconnected()

}