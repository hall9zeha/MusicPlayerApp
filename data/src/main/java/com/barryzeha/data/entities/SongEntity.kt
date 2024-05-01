package com.barryzeha.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/4/24.
 * Copyright (c)  All rights reserved.
 **/
@Entity
data class SongEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("id")
    val id:Long,
    val description:String?,
    val pathLocation:String?,
    val timestamp:Long,
    val duration:Long,
    val bitrate:String?,
    val format:String
)
