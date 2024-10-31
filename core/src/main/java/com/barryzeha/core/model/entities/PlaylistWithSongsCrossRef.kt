package com.barryzeha.core.model.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/10/24.
 * Copyright (c)  All rights reserved.
 **/
@Entity(
  primaryKeys = ["idPlaylist", "id"],
          foreignKeys = [
            ForeignKey(
              entity = PlaylistEntity::class,
              parentColumns = arrayOf("idPlaylist"),
              childColumns = arrayOf("idPlaylist"),
              onDelete = ForeignKey.CASCADE
            ),
            ForeignKey(
              entity = SongEntity::class,
              parentColumns = arrayOf("id"),
              childColumns = arrayOf("id"),
              onDelete = ForeignKey.CASCADE
            )

          ],
  indices = [
    Index("idPlaylist"),
    Index("id")
  ]
)
data class PlaylistWithSongsCrossRef(
  val idPlaylist:Long=0,
  val id:Long=0
)
