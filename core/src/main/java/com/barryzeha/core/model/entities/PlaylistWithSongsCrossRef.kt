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
  primaryKeys = ["playlistId", "songId"],
          foreignKeys = [
            ForeignKey(
              entity = PlaylistEntity::class,
              parentColumns = arrayOf("idPlaylist"),
              childColumns = arrayOf("playlistId"),
              onDelete = ForeignKey.CASCADE
            ),
            ForeignKey(
              entity = SongEntity::class,
              parentColumns = arrayOf("id"),
              childColumns = arrayOf("songId"),
              onDelete = ForeignKey.CASCADE
            )

          ],
  indices = [
    Index("playlistId"),
    Index("songId")
  ]
)
data class PlaylistWithSongsCrossRef(
  val playlistId:Long=0,
  val songId:Long=0
)
