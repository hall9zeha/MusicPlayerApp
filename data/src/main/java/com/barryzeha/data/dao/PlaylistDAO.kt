package com.barryzeha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.barryzeha.core.model.entities.PlaylistEntity
import com.barryzeha.core.model.entities.PlaylistWithSongs
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
  // agregadas a dichas listas, caso contrario cuando sea 0 por defecto. en ambos casos se ordenará por el campo enviado a través del
  // parámetro orderBy que pueden ser(album, genre, artist)

 @Transaction@Query("""
    SELECT * FROM SongEntity  
    WHERE (:idPlaylist >0 AND idPlaylistCreator =  (SELECT idPlaylist FROM PlaylistEntity WHERE idPlaylist = :idPlaylist LIMIT 1))
       OR (:idPlaylist = 0 AND :orderBy !='favorite')
       OR (:orderBy = 'favorite' AND favorite = 1)
       
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