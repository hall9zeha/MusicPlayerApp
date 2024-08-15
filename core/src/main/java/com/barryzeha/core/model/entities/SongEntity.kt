package com.barryzeha.core.model.entities

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
    val id:Long=0,
    val description:String?="",
    val artist:String="",
    val album:String="",
    val genre:String="",
    val pathLocation:String?="",
    val parentDirectory:String?="",
    val timestamp:Long=0,
    val duration:Long=0,
    val bitrate:String?="",
    val format:String?="",
    val favorite:Boolean=false,

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
