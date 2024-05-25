package com.barryzeha.core.model

import com.barryzeha.core.model.entities.MusicState


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 25/5/24.
 * Copyright (c)  All rights reserved.
 **/

interface MainSongController {
 fun play()
 fun pause()
 fun next()
 fun previous()
 fun stop()
 fun musicState(musicState: MusicState?)
 fun currentTrack(musicState: MusicState?)
}