package com.barryzeha.core.model.entities

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 16/5/24.
 * Copyright (c)  All rights reserved.
 **/

@Parcelize
data class MusicState(
    val idSong:Long=0,
    val isPlaying: Boolean = false,
    val currentDuration: Long = 0,
    val title: String = "",
    val album: String = "",
    val artist: String = "",
    val albumArt: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
    val duration: Long = 0,
    val songPath: String = "",
    val currentPosition:Long=0L,
    val latestPlayed:Boolean=false

) : Parcelable