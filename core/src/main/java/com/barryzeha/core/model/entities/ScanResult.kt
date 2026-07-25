package com.barryzeha.core.model.entities

/****
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 25/07/26.
 * Copyright (c)  All rights reserved.
 ***/

data class ScanResult(
    val newSongs:List<String>,
    val deletedSongPaths:List<String>
)
