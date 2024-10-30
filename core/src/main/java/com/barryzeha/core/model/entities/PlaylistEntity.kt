package com.barryzeha.core.model.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 26/10/24.
 * Copyright (c)  All rights reserved.
 **/
@Parcelize
@Entity(indices = [Index(value = ["idPlaylist"], unique = true)])
data class PlaylistEntity(
 @PrimaryKey(autoGenerate = true)
 val idPlaylist:Long=0,
 val playListName:String="",

):Parcelable {
 override fun equals(other: Any?): Boolean {
  if (this === other) return true
  if (javaClass != other?.javaClass) return false

  other as PlaylistEntity

  return idPlaylist == other.idPlaylist
 }

 override fun hashCode(): Int {
  return idPlaylist.hashCode()
 }
}
