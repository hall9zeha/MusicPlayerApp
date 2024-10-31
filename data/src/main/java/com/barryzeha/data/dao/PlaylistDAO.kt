package com.barryzeha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.barryzeha.core.model.entities.PlaylistEntity
import com.barryzeha.core.model.entities.PlaylistWithSongs
import com.barryzeha.core.model.entities.PlaylistWithSongsCrossRef
import com.barryzeha.core.model.entities.SongEntity


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 27/10/24.
 * Copyright (c)  All rights reserved.
 **/
@Dao
interface PlaylistDAO {
  @Insert
  suspend fun createPlaylist(playList:PlaylistEntity):Long

  @Query("update PlaylistEntity set playListName =:name where idPlaylist =:idPlaylist")
  suspend fun updatePlaylist(name:String,idPlaylist:Long):Int

  @Query("delete from PlaylistEntity where idPlaylist =:id")
  suspend fun deletePlaylist(id:Long):Int

  @Delete
  suspend fun deletePlayList(playlistEntity:PlaylistEntity)

  @Delete
  suspend fun deleteAllPlaylists(playlistEntities:List<PlaylistEntity>)


  @Transaction
  @Query("select * from PlaylistEntity")
  suspend fun fetchPlaylistWithSongs():List<PlaylistWithSongs>

  @Transaction
  @Query("select * from PlaylistEntity")
  suspend fun fetchAllPlaylists():List<PlaylistEntity>

  @Transaction
  @Query("select * from PlaylistEntity where playListName =:name")
  suspend fun fetchPlaylistByName(name:String):List<PlaylistEntity>

  // Cuando idPlayList sea mayor que 0 (significa que ya habrá listas creadas) se buscará las canciones, que hayan sido
  // agregadas a dichas listas, caso contrario (:orderBy !='favorite') se cargará todas la canciones. en ambos casos se ordenará por el campo enviado a través del
  // parámetro orderBy que pueden ser(album, genre, artist)
  // el caso favoritos (:orderBy = 'favorite' AND favorite = 1), nos devolverá solo aquellos que tengan el valor 1(true) en el campo correspondiente

  // For playlist and songs cross ref table
  @Transaction
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun savePlaylistWithSongCrossRef(playlistWithSong: PlaylistWithSongsCrossRef):Long

  @Transaction
  @Delete
  suspend fun deletePlaylistWithSongCrossRef(playlistWithSong: PlaylistWithSongsCrossRef):Int

 @Transaction@Query("""
    SELECT * FROM SongEntity  
    WHERE (:orderBy !='favorite' and  id in (SELECT id FROM PlaylistWithSongsCrossRef WHERE idPlaylist = :idPlaylist))
        OR (:orderBy = 'favorite' AND favorite = 1 and  id in (SELECT id FROM PlaylistWithSongsCrossRef WHERE idPlaylist = :idPlaylist))
       
    ORDER BY CASE 
        WHEN :orderBy = 'album' THEN album 
        WHEN :orderBy = 'genre' THEN genre 
        WHEN :orderBy = 'artist' THEN artist 
        ELSE NULL 
    END
""")
 suspend fun fetchPlaylistOrderBy(idPlaylist: Long, orderBy: String): List<SongEntity>

 @Transaction
 @Query("select * from SongEntity,(select idPlaylist from PlaylistEntity where idPlaylist =:idPlaylist limit 1) as playlist where idPlaylistCreator = playlist.idPlaylist and favorite = 1 ")
 suspend fun fetchPlaylistByFavorites(idPlaylist: Long):List<SongEntity>



}