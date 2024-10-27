package com.barryzeha.core.model.entities

import android.os.Parcelable
import androidx.room.Embedded
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
    entityColumn = "idPlaylistCreator"
  )
 val songs:List<SongEntity>

):Parcelable
