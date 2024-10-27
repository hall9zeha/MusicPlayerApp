package com.barryzeha.core.model.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 26/10/24.
 * Copyright (c)  All rights reserved.
 **/
@Parcelize
@Entity
data class PlaylistEntity(
 @PrimaryKey(autoGenerate = true)
 val idPlaylist:Long=0,
 val playListName:String="",

):Parcelable
