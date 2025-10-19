package com.barryzeha.data.repository

import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.data.dao.SongDao
import com.barryzeha.data.database.SongDatabase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 18/10/25.
 * Copyright (c)  All rights reserved.
 **/

class MainRepositoryImplTest {

  lateinit var repository: MainRepositoryImpl
  lateinit var mockSongDao: SongDao
  lateinit var mockSongDb: SongDatabase
  lateinit var mockPrefs: MyPreferences

  @Before
  fun setup(){
    mockSongDb = mockk(relaxed=true)
    mockSongDao = mockk(relaxed = true)
    mockPrefs = mockk(relaxed = true)
    every{mockSongDb.getSongDao()} returns mockSongDao
    repository = MainRepositoryImpl(mockSongDb,mockPrefs)


  }
 @Test
 fun `fetchAllSongs returns list from dao`() = runTest {
  // Arrange
  val fakeSongs = listOf(
   SongEntity(id = 1, description = "Song A", artist = "Artist A"),
   SongEntity(id = 2, description = "Song B", artist = "Artist B")
  )
  coEvery { mockSongDao.fetchAllSongs() } returns fakeSongs

  // Act
  val result = repository.fetchAllSongs()

  // Assert
  assertThat(result.first().description!!,true).equals(fakeSongs.first().description.toString())
  coVerify { mockSongDao.fetchAllSongs() }
 }


}