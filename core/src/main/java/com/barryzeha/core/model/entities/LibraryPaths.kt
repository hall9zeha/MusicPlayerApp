package com.barryzeha.core.model.entities

import kotlinx.serialization.Serializable

/****
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 18/07/26.
 * Copyright (c)  All rights reserved.
 ***/
@Serializable
data class LibraryPaths(
    val paths:List<String>
)
