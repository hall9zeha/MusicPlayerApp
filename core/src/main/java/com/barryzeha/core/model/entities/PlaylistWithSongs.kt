package com.barryzeha.core.model.entities

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import kotlinx.parcelize.Parcelize


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 26/10/24.
 * Copyright (c)  All rights reserved.
 **/
@Parcelize
data class PlaylistWithSongs(
  @Embedded val playList:PlaylistEntity,
  @Relation(
    parentColumn = "idPlaylist",
    entityColumn = "id",
    associateBy = Junction(PlaylistWithSongsCrossRef::class)
  )
 val songs:List<SongEntity> = arrayListOf()

):Parcelable
