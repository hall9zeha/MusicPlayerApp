package com.barryzeha.data.repository

import android.media.MediaPlayer
import androidx.media3.exoplayer.ExoPlayer
import com.barryzeha.core.model.entities.PlaylistEntity
import com.barryzeha.core.model.entities.PlaylistWithSongs
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.core.model.entities.SongState
import com.barryzeha.core.model.entities.SongStateWithDetail
import kotlinx.coroutines.flow.Flow


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/4/24.
 * Copyright (c)  All rights reserved.
 **/

interface MainRepository {
    // Room Database
    // SongEntity
    suspend fun fetchAllSongs():List<SongEntity>
    suspend fun fetchAllSongsBy(field:Int):List<SongEntity>
    suspend fun fetchAllFavorites():List<SongEntity>
    suspend fun fetchSongById(idSong:Long): SongEntity
    suspend fun saveNewSong(song: SongEntity):Long
    suspend fun saveSongs(songList:List<SongEntity>):LongArray
    suspend fun updateSong(song:SongEntity):Int
    suspend fun updateFavoriteSong(isFavorite:Boolean, idSong:Long):Int
    suspend fun deleteSong(idSong:Long):Int
    suspend fun deleteSong(songIds:List<Long>):Int
    suspend fun deleteAllSongs():Int

    // SongState
    suspend fun fetchSongState():List<SongStateWithDetail>
    suspend fun saveSongState(songState: SongState):Long
    suspend fun updateSongState(songState: SongState):Int
    suspend fun deleteSongState(idSong:Long):Int

    //PlayList
    suspend fun createPlayList(playlistEntity:PlaylistEntity):Long
    suspend fun updatePlaylist(name:String, idPlaylist:Long):Int
    suspend fun deletePlaylist(id:Long):Int
    suspend fun deleteAllPlaylist(playlistEntities:List<PlaylistEntity>)
    suspend fun fetchPlaylist():List<PlaylistWithSongs>
    suspend fun fetchPlaylistOrderBy(idPlaylist:Long,orderByField:Int):List<SongEntity>
    suspend fun fetchPlaylistByFavorites(idPlaylist: Long):List<SongEntity>
}