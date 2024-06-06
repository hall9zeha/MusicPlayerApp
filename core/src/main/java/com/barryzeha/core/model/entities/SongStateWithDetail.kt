package com.barryzeha.core.model.entities

import androidx.room.Embedded
import androidx.room.Relation


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 6/6/24.
 * Copyright (c)  All rights reserved.
 **/

data class SongStateWithDetail(
    @Embedded val songState: SongState,
    @Relation(
        parentColumn = "idSong", // Column of SongState entity
        entityColumn = "id" // Column of SongEntity
    )
    val songEntity: SongEntity
)