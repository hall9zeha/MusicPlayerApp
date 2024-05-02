package com.barryzeha.core.entities

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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SongEntity

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
