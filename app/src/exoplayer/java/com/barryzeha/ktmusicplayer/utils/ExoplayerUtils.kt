package com.barryzeha.ktmusicplayer.utils

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.SongEntity


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/10/25.
 * Copyright (c)  All rights reserved.
 **/

fun SongEntity.convertToMediaItem(): MediaItem{
    val metaData = MediaMetadata.Builder()
        .setTitle(this.description)
        .setArtist(this.artist)
        .setAlbumTitle(this.album)
        .setAlbumArtist(this.album)
        .setGenre(this.genre)
        .setDurationMs(this.duration)
        .build()
    val extras = Bundle().apply{
        putBoolean("isFavorite",this@convertToMediaItem.favorite)
        putString("songPath",this@convertToMediaItem.pathLocation.toString())
        putBoolean("latestPlayed",false)
    }
    return MediaItem.Builder()
        .setMediaId(this.id.toString())
        .setUri(this.pathLocation.toString())
        .setMediaMetadata(metaData)
        .setTag(extras)
        .build()
}
fun ExoPlayer.metadataToMusicState(): MusicState?{
    val mediaItem = this.currentMediaItem ?: return null
    val metadata = mediaItem.mediaMetadata
    val extras = mediaItem.localConfiguration?.tag as? Bundle
    return MusicState(
        idSong = mediaItem.mediaId.toLongOrNull() ?: -1L,
        isPlaying = this.isPlaying,
        title = metadata.title.toString(),
        artist = metadata.artist.toString(),
        album = metadata.albumTitle.toString(),
        isFavorite = extras?.getBoolean("isFavorite")?:false,
        duration = metadata.durationMs?:0,
        songPath = extras?.getString("songPath")?:"",
        latestPlayed = extras?.getBoolean("latestPlayed")?:false

    )
}