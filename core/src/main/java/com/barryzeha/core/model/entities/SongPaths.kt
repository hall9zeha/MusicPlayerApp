package com.barryzeha.core.model.entities

import kotlinx.serialization.Serializable

/****
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 21/07/26.
 * Copyright (c)  All rights reserved.
 ***/
@Serializable
data class SongPaths(
    val paths:List<String>
)
