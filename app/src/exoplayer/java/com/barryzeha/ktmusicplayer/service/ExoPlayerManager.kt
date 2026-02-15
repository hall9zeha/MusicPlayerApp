package com.barryzeha.ktmusicplayer.service

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

/****
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 14/02/26.
 * Copyright (c)  All rights reserved.
 ***/

class ExoPlayerManager(context: Context): Player.Listener{
    private var player: ExoPlayer = ExoPlayer.Builder(context).build()

    init{
        player.setWakeMode(C.WAKE_MODE_LOCAL)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        super.onPositionDiscontinuity(oldPosition, newPosition, reason)
    }
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
    }
    @OptIn(UnstableApi::class)
    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        super.onAudioSessionIdChanged(audioSessionId)
    }
}