package com.barryzeha.core.model

import com.barryzeha.core.model.entities.MusicState


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 16/5/24.
 * Copyright (c)  All rights reserved.
 **/

interface SongController {
 fun play()
 fun pause()
 fun next()
 fun previous()
 fun stop()
 fun musicState(musicState: MusicState?)
}