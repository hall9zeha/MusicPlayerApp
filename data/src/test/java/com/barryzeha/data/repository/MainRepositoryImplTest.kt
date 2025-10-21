package com.barryzeha.data.repository

import android.util.Log
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.model.entities.PlaylistEntity
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.core.model.entities.SongState
import com.barryzeha.core.model.entities.SongStateWithDetail
import com.barryzeha.data.dao.PlaylistDAO
import com.barryzeha.data.dao.SongDao
import com.barryzeha.data.database.SongDatabase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest


import org.junit.Before
import org.junit.Test


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 18/10/25.
 * Copyright (c)  All rights reserved.
 **/

class MainRepositoryImplTest {

    lateinit var repository: MainRepository
    lateinit var mockSongDao: SongDao
    lateinit var mockPlaylistDao: PlaylistDAO
    lateinit var mockSongDb: SongDatabase
    lateinit var mockPrefs: MyPreferences
    var fakeSongs: List<SongEntity> = listOf()
    var fakeSongStateWithDetail:List<SongStateWithDetail> = listOf()
    var fakeSong: SongEntity = SongEntity()
    var fakeSongState: SongState = SongState()
    var fakePlayList: PlaylistEntity = PlaylistEntity()

    var BY_ALBUM = 1
    var BY_ARTIST = 2

    @Before
    fun setup() {
        mockSongDb = mockk(relaxed = true)
        mockSongDao = mockk(relaxed = true)
        mockPlaylistDao = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)

        every { mockSongDb.getSongDao() } returns mockSongDao
        every {mockSongDb.getPlaylistDao()} returns mockPlaylistDao

        repository = MainRepositoryImpl(mockSongDb, mockPrefs)
        fakeSongs = listOf(
            SongEntity(id = 1, description = "Song A", artist = "Artist A",album="Album 2", favorite = true),
            SongEntity(id = 2, description = "Song B", artist = "Artist B",album="Album 3", favorite = false),
            SongEntity(id = 3, description = "Song C", artist = "Aartist 1",album="Album 1", favorite=true)
        )

        fakeSong = SongEntity(
            id = 4,
            description = "Test Song",
            artist = "Test Artist"
        )
        fakeSongState = SongState(
            idSongState = 1,
            idSong = 4,
            songDuration = 212400,
            currentPosition = 3566
        )
        fakeSongStateWithDetail = listOf(
            SongStateWithDetail(fakeSongState,fakeSong)
        )
        fakePlayList = PlaylistEntity(
            idPlaylist = 1,
            playListName = "My fake playlist"
        )
    }
    // Songs
    @Test
    fun `fetchAllSongs returns list from dao`() = runTest {
        // Arrange

        coEvery { mockSongDao.fetchAllSongs() } returns fakeSongs

        // Act
        val result = repository.fetchAllSongs()

        // Assert
        assertThat(
            result.first().description!!
        ).isEqualTo(fakeSongs.first().description.toString())
        coVerify { mockSongDao.fetchAllSongs() }

    }
    @Test
    fun fetchAllSortSongsBySpecificField()= runTest{
        // Return songs by artist sorted by descending
        coEvery { mockSongDao.fetchAllSongByArtist() } returns fakeSongs.sortedBy { it.artist }
        val result = repository.fetchAllSongsBy(BY_ARTIST)
        // Then, in the newly sorted result list, the first artist (at index 0) is equal to the third artist in the fakeSongs list.
        assertThat(
            result.first().artist
        ).isEqualTo(fakeSongs[2].artist)
    }
    @Test
    fun fetchSongById() = runTest {
        coEvery{mockSongDao.fetchSongById(3)} returns fakeSongs.first { it.id == 3.toLong() }

        val result = repository.fetchSongById(3)

        assertThat(result.description.toString()).isEqualTo("Song C")
    }

    @Test
    fun saveNewSong()=runTest {
        coEvery{ mockSongDao.saveNewSong(fakeSong)} returns 4L

        val result = repository.saveNewSong(fakeSong)

        coVerify(exactly = 1){mockSongDao.saveNewSong(fakeSong)}
        assertThat(result).isEqualTo(4L)
    }

    @Test
    fun updateSong() = runTest {

        coEvery { mockSongDao.updateSong(fakeSong) } returns 4

        val result = repository.updateSong(fakeSong)

        coVerify(exactly = 1){mockSongDao.updateSong(fakeSong)}
        assertThat(result).isEqualTo(4)
    }

    @Test
    fun deleteSong()=runTest {
        coEvery { mockSongDao.deleteSong(1)} returns 1

        val result = repository.deleteSong(1)

        coVerify(exactly = 1) {mockSongDao.deleteSong(1)  }
        assertThat(result).isEqualTo(1)
    }

    // Song state

    @Test
    fun fetchSongState()=runTest {
        coEvery {mockSongDao.fetchSongState()} returns fakeSongStateWithDetail

        val result = repository.fetchSongState()

        assertThat(result.first().songState.idSong).isEqualTo(4)
    }

    @Test
    fun saveSongState()= runTest{
        coEvery{mockSongDao.saveSongState(fakeSongState)} returns 4L

        val result = repository.saveSongState(fakeSongState)

        coVerify(exactly = 1) { mockSongDao.saveSongState(fakeSongState) }
        assertThat(result).isEqualTo(4L)
    }

    @Test
    fun updateSongState() = runTest{

        coEvery{mockSongDao.updateSongState(fakeSongState)} returns 1

        val result = repository.updateSongState(fakeSongState)

        coVerify(exactly = 1) { mockSongDao.updateSongState(fakeSongState) }
        assertThat(result).isEqualTo(1)
    }

    @Test
    fun deleteSongState() = runTest {
        coEvery{mockSongDao.deleteSongState(1)} returns 1

        val result = repository.deleteSongState(1)

        coVerify(exactly = 1){ mockSongDao.deleteSongState(1)}
        assertThat(result).isEqualTo(1)
    }

    // Playlist

    @Test
    fun fetchPlaylist() = runTest {
        coEvery { mockPlaylistDao.fetchAllPlaylists() } returns listOf(fakePlayList)

        val result = repository.fetchPlaylists()

        coVerify(exactly = 1) {mockPlaylistDao.fetchAllPlaylists()  }
        assertThat(result).isEqualTo(listOf(fakePlayList))
        assertThat(result.first().playListName).isEqualTo(fakePlayList.playListName)
    }

    @Test
    fun createPlayList() = runTest {

        coEvery { mockPlaylistDao.createPlaylist(fakePlayList) } returns 1L

        val result = repository.createPlayList(fakePlayList)

        coVerify(exactly=1) {mockPlaylistDao.createPlaylist(fakePlayList)  }
        assertThat(result).isEqualTo(1L)

    }

    @Test
    fun updatePlayList() = runTest {
        coEvery{ mockPlaylistDao.updatePlaylist(fakePlayList)} returns 1

        val result = repository.updatePlaylist(fakePlayList)

        coVerify(exactly = 1) { mockPlaylistDao.updatePlaylist(fakePlayList) }
        assertThat(result).isEqualTo(1)
    }

    @Test
    fun deletePlayList() = runTest {
        coEvery{mockPlaylistDao.deletePlaylist(1)} returns 1

        val result = repository.deletePlaylist(1)

        coVerify(exactly=1) {mockPlaylistDao.deletePlaylist(1)  }
        assertThat(result).isEqualTo(1)
    }

    @Test
    fun fetchPlaylistOrderedBy()= runTest {
        coEvery { mockPlaylistDao.fetchPlaylistOrderBy(1L,"album") }returns fakeSongs.sortedBy { it.album }

        val result = repository.fetchPlaylistOrderBy(1L,BY_ALBUM)

        coVerify(exactly = 1) {mockPlaylistDao.fetchPlaylistOrderBy(1L,"album")  }
        assertThat(result.first().album).isEqualTo(fakeSongs[2].album)
    }

    @Test
    fun fetchPlaylistByFavorites()=runTest {
        coEvery {mockPlaylistDao.fetchPlaylistByFavorites(1)} returns fakeSongs.filter { it.favorite ==true }

        val result = repository.fetchPlaylistByFavorites(1)

        coVerify(exactly = 1) {mockPlaylistDao.fetchPlaylistByFavorites(1)  }
        assertThat(result.size).isEqualTo(2)
        assertThat(result.first().favorite).isEqualTo(true)
        assertThat(result[1].favorite).isEqualTo(true)
    }
}