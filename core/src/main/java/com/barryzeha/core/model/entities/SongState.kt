package com.barryzeha.core.model.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 6/6/24.
 * Copyright (c)  All rights reserved.
 **/

@Entity(
 tableName = "SongState",
 foreignKeys = [
    ForeignKey(
        entity = SongEntity::class,
        parentColumns =["id"],
        childColumns = ["idSongState"],
        onDelete = ForeignKey.CASCADE
    )
 ]
)
data class SongState(
 @PrimaryKey(autoGenerate = true)
 val idSongState:Long =0,
 val idSong:Long?=null,
 val songDuration:Long=0,
 val currentPosition:Long=0,
) {
 override fun equals(other: Any?): Boolean {
  if (this === other) return true
  if (javaClass != other?.javaClass) return false

  other as SongState

  return idSongState == other.idSongState
 }

 override fun hashCode(): Int {
  return idSongState.hashCode()
 }
}
