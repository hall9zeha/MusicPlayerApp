package com.barryzeha.core.model.entities

import android.graphics.Bitmap


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 9/6/24.
 * Copyright (c)  All rights reserved.
 **/

data class AudioMetadata(
    val artist: String="",
    val album: String="",
    val albumArtist:String="",
    val genre:String="",
    val title: String="",
    val comment: String="",
    val year: String="",
    val track: String="",
    val discNumber: String="",
    val composer: String="",
    val artistSort: String="",
    val coverArt: Bitmap?=null,
    val bitRate:String="",
    val songLengthFormatted:String="",
    val songLength:Long=0,
    val format:String="",
    val freq:String="",
    val fileSize:String="",
    val channels:String=""
)
