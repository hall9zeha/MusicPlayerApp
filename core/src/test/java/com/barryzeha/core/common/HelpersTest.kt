package com.barryzeha.core.common

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.barryzeha.core.model.entities.SongEntity
import io.mockk.coEvery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/****
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 1/08/26.
 * Copyright (c)  All rights reserved.
 ***/
@OptIn(ExperimentalCoroutinesApi::class)
class HelpersTest {
    ///storage/emulated/0/Music
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    val testDispatcher = StandardTestDispatcher()

    var fakeLibraryPaths:List<String> = listOf()
    var fakeSongPaths:List<String> = listOf()

    @Before
    fun setup(){
        Dispatchers.setMain(testDispatcher)
        fakeSongPaths = listOf(
            "/storage/emulated/0/Music/Disc1/MyFavoriteSong1.mp3",
            "/storage/emulated/0/Music/Disc2/MyFavoriteSong2.flac",
            "/storage/emulated/0/Download/Disc3/MyFavoriteSong3.dsf"
        )
        fakeLibraryPaths = listOf(
            "/storage/emulated/0/Music",
            "/storage/emulated/0/Download"
        )
    }
    @After
    fun tearDown(){
        Dispatchers.resetMain()
    }
    @Test
    fun reconstructLibraryPathsTest() = runTest{
        val result = reconstructLibraryPaths(fakeSongPaths)
        assert(result.containsAll(fakeLibraryPaths))
    }
    @Test
    fun compareSongPathsTest(){
        val currentSongPaths = listOf(
            "/storage/emulated/0/Music/Disc2/MyFavoriteSong2.flac",
            "/storage/emulated/0/Download/Disc3/MyFavoriteSong3.dsf",
            "/storage/emulated/0/Music/Disc4/MyFavoriteSong4.wav"
        )
        val previousSongPaths = listOf(
            "/storage/emulated/0/Music/Disc1/MyFavoriteSong1.mp3",
            "/storage/emulated/0/Music/Disc2/MyFavoriteSong2.flac",
            "/storage/emulated/0/Download/Disc3/MyFavoriteSong3.dsf"
        )
        val deletedSongPathsExpected = listOf(
            "/storage/emulated/0/Music/Disc1/MyFavoriteSong1.mp3"
        )
        val newSongPathsExpected = listOf(
            "/storage/emulated/0/Music/Disc4/MyFavoriteSong4.wav"
        )
        val scanSongList = listOf<SongEntity>(
            SongEntity(
                id = 1,
                description = "MyFavoriteSong2",
                album = "Disc1",
                pathLocation = "/storage/emulated/0/Music/Disc2/MyFavoriteSong2.flac"
            ),
            SongEntity(
                id = 2,
                description = "MyFavoriteSong3",
                album = "Disc2",
                pathLocation = "/storage/emulated/0/Music/Disc3/MyFavoriteSong3.dsf"  ),
            SongEntity(
                id = 3,
                description = "MyFavoriteSong4",
                album = "Disc4",
                pathLocation = "/storage/emulated/0/Music/Disc4/MyFavoriteSong4.wav"
            )
        )
        val songEntityAddedExpected = listOf(
            SongEntity(
                id = 3,
                description = "MyFavoriteSong4",
                album = "Disc4",
                pathLocation = "/storage/emulated/0/Music/Disc4/MyFavoriteSong4.wav"
            )
        )
        val result = compareSongPaths(currentSongPaths.toSet(), previousSongPaths.toSet(),scanSongList)
        assert(result.deletedSongPaths.containsAll(deletedSongPathsExpected))
        assert(result.newSongs.containsAll(songEntityAddedExpected))
    }

}