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
  suspend fun fetchPlaylists():List<PlaylistWithSongs>

  @Transaction
  @Query("select * from PlaylistEntity where playListName =:name")
  suspend fun fetchPlaylistByName(name:String):List<PlaylistEntity>

 /*@Transaction
 @Query("select * from SongEntity,(select idPlaylist from PlaylistEntity where idPlaylist =:idPlaylist limit 1) as playlist where idPlaylistCreator = playlist.idPlaylist ")
 suspend fun fetchPlaylistDefault(idPlaylist: Long):List<SongEntity>

 @Transaction
 @Query("select * from SongEntity,(select idPlaylist from PlaylistEntity where idPlaylist =:idPlaylist limit 1) as playlist where idPlaylistCreator = playlist.idPlaylist  order by album asc ")
 suspend fun fetchPlaylistByAlbum(idPlaylist: Long):List<SongEntity>

 @Transaction
 @Query("select * from SongEntity,(select idPlaylist from PlaylistEntity where idPlaylist =:idPlaylist limit 1) as playlist where idPlaylistCreator = playlist.idPlaylist order by genre asc ")
 suspend fun fetchPlaylistByGenre(idPlaylist: Long):List<SongEntity>

 @Transaction
 @Query("select * from SongEntity,(select idPlaylist from PlaylistEntity where idPlaylist =:idPlaylist limit 1) as playlist where idPlaylistCreator = playlist.idPlaylist order by artist asc ")
 suspend fun fetchPlaylistByArtist(idPlaylist: Long):List<SongEntity>
*/
 @Transaction
 @Query("""
      SELECT * FROM SongEntity, 
      (SELECT idPlaylist FROM PlaylistEntity WHERE idPlaylist = :idPlaylist LIMIT 1) AS playlist 
      WHERE idPlaylistCreator = playlist.idPlaylist 
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