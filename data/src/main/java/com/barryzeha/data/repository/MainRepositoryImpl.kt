package com.barryzeha.data.repository

import com.barryzeha.core.common.BY_ALBUM
import com.barryzeha.core.common.BY_ARTIST
import com.barryzeha.core.common.BY_FAVORITE
import com.barryzeha.core.common.BY_GENRE
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.model.entities.PlaylistEntity
import com.barryzeha.core.model.entities.PlaylistWithSongs
import com.barryzeha.core.model.entities.PlaylistWithSongsCrossRef
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.core.model.entities.SongState
import com.barryzeha.core.model.entities.SongStateWithDetail
import com.barryzeha.data.database.SongDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/4/24.
 * Copyright (c)  All rights reserved.
 **/

class MainRepositoryImpl @Inject constructor(db: SongDatabase,val prefs:MyPreferences):MainRepository {
    private val songDao = db.getSongDao()
    private val playlistDao = db.getPlaylistDao()
    // Room database
    override suspend fun fetchAllSongs(): List<SongEntity> = withContext(Dispatchers.IO){
        songDao.fetchAllSongs()
    }

    override suspend fun fetchAllSongsBy(field: Int): List<SongEntity> = withContext(Dispatchers.IO){
        when(field){
            BY_ALBUM-> songDao.fetchAllSongByAlbum()
            BY_ARTIST-> songDao.fetchAllSongByArtist()
            BY_GENRE-> songDao.fetchAllSongByGenre()
            BY_FAVORITE->songDao.fetchAllFavorites()
            else->songDao.fetchAllSongs()
        }
    }

    override suspend fun fetchAllFavorites(): List<SongEntity>  = withContext(Dispatchers.IO){
        songDao.fetchAllFavorites()
    }

    override suspend fun fetchSongById(idSong: Long): SongEntity = withContext(Dispatchers.IO){
        songDao.fetchSongById(idSong)
    }

    override suspend fun saveNewSong(song: SongEntity): Long = withContext(Dispatchers.IO) {
        songDao.saveNewSong(song)
    }

    override suspend fun saveSongs(songList: List<SongEntity>): LongArray = withContext(Dispatchers.IO){
        songDao.saveSongs(songList)
    }

    override suspend fun updateSong(song: SongEntity): Int = withContext(Dispatchers.IO){
        songDao.updateSong(song)
    }

    override suspend fun updateFavoriteSong(isFavorite: Boolean, idSong: Long): Int = withContext(Dispatchers.IO){
        songDao.updateFavoriteSong(isFavorite,idSong)
    }

    override suspend fun deleteSong(idSong: Long): Int= withContext(Dispatchers.IO) {
        songDao.deleteSong(idSong)
    }

    override suspend fun deleteSong(songIds: List<Long>): Int = withContext(Dispatchers.IO){
        songDao.deleteSong(songIds)
    }

    override suspend fun deleteAllSongs(): Int = withContext(Dispatchers.IO) {
        songDao.deleteAllSongs()
    }

    // SongState
    override suspend fun fetchSongState(): List<SongStateWithDetail> = withContext(Dispatchers.IO){
        songDao.fetchSongState()
    }

    override suspend fun saveSongState(songState: SongState): Long = withContext(Dispatchers.IO){
        songDao.saveSongState(songState)

    }

    override suspend fun updateSongState(songState: SongState): Int = withContext(Dispatchers.IO){
        songDao.updateSongState(songState)
    }

    override suspend fun deleteSongState(idSong: Long): Int= withContext(Dispatchers.IO) {
        songDao.deleteSongState(idSong)
    }

    // Playlist

    override suspend fun createPlayList(playlistEntity: PlaylistEntity): Long = withContext(Dispatchers.IO) {
        playlistDao.createPlaylist(playlistEntity)
    }

    override suspend fun updatePlaylist(name: String, idPlaylist: Long): Int  = withContext(Dispatchers.IO){
        playlistDao.updatePlaylist(name, idPlaylist)
    }

    override suspend fun deletePlaylist(id: Long): Int = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(id)
    }

    override suspend fun deleteAllPlaylist(playlistEntities: List<PlaylistEntity>) = withContext(Dispatchers.IO) {
        playlistDao.deleteAllPlaylists(playlistEntities)
    }

    override suspend fun fetchPlaylists(): List<PlaylistEntity>  = withContext(Dispatchers.IO){
        playlistDao.fetchAllPlaylists()
    }

    override suspend fun fetchPlaylistWithSongs(): List<PlaylistWithSongs> = withContext(Dispatchers.IO) {
        playlistDao.fetchPlaylistWithSongs()
    }

    // For playlist and songs cross ref table
    override suspend fun savePlaylistWithSongCrossRef(playlistWithSongsCrossRef: PlaylistWithSongsCrossRef): Long = with(Dispatchers.IO){
        playlistDao.savePlaylistWithSongCrossRef(playlistWithSongsCrossRef)
    }

    override suspend fun deletePlaylistWithSongCrossRef(playlistWithSongsCrossRef: PlaylistWithSongsCrossRef): Int = with(Dispatchers.IO) {
        playlistDao.deletePlaylistWithSongCrossRef(playlistWithSongsCrossRef)
    }

    override suspend fun fetchPlaylistOrderBy(
        idPlaylist: Long,
        orderByField: Int
    ): List<SongEntity> = withContext(Dispatchers.IO) {
        if(prefs.playlistId>0) {
            when (orderByField) {
                BY_ALBUM -> playlistDao.fetchPlaylistOrderBy(idPlaylist, "album")
                BY_ARTIST -> playlistDao.fetchPlaylistOrderBy(idPlaylist, "artist")
                BY_GENRE -> playlistDao.fetchPlaylistOrderBy(idPlaylist, "genre")
                BY_FAVORITE -> playlistDao.fetchPlaylistOrderBy(idPlaylist, "favorite")
                else -> {
                    // BY_DEFAULT
                    fetchAllSongsBy(orderByField)
                }
            }
        }else{
            fetchAllSongsBy(orderByField)
        }
    }

    override suspend fun fetchPlaylistByFavorites(idPlaylist: Long): List<SongEntity> = withContext(Dispatchers.IO) {
        playlistDao.fetchPlaylistByFavorites(idPlaylist)
    }
}