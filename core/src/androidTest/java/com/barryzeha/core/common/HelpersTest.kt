package com.barryzeha.core.common

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.barryzeha.core.model.entities.LibraryPaths
import com.barryzeha.core.model.entities.SongPaths
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith

/****
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 1/08/26.
 * Copyright (c)  All rights reserved.
 ***/
@RunWith(AndroidJUnit4::class)
class HelpersTest {
    val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun loadLibraryPathsSuccessTest() = runTest{
        val expectedPaths = listOf(
            "/storage/emulated/0/Music",
            "/storage/emulated/0/Download"
        )
        val json = Json.encodeToString(
            LibraryPaths(expectedPaths)
        )
        context.openFileOutput("library_paths.json", Context.MODE_PRIVATE).use {
            it.write(json.toByteArray())
        }
        val result = loadLibraryPaths(context)
        assertThat(result).containsExactlyElementsIn(expectedPaths)
    }
    @Test
    fun loadSongPathsSuccessTest()= runTest{
        val expectedPaths = listOf(
            "/storage/emulated/0/Music/MyFavoriteAlbum/Song1.mp3",
            "/storage/emulated/0/Download/MySelectedAlbum/Song2.flac"
        )
        val json = Json.encodeToString(
            SongPaths(expectedPaths)
        )
        context.openFileOutput("song_paths.json", Context.MODE_PRIVATE).use {
            it.write(json.toByteArray())
        }
        val result = loadSongPaths(context)
        assertThat(result).containsExactlyElementsIn(expectedPaths)
    }
    @Test
    fun readJsonFileTestSuccess() = runTest{
        val expectedJson = """{"paths":["A","B"]}"""
        context.openFileOutput("library_paths.json", Context.MODE_PRIVATE).use {
            it.write(expectedJson.toByteArray())
        }
        val result = readJsonFile(context, "library_paths.json")
        assertThat(result).isEqualTo(expectedJson)
    }
    @Test
    fun readJsonFileTestFileNotFound() = runTest{
        val result = readJsonFile(context, "non_existent_file.json")
        assertThat(result).isNull()
    }

    @Test
    fun saveSongPathsIfSongPathsNotEmptyTest() = runTest{
        val pathsToSave = listOf(
            "/storage/emulated/0/Music/MyFavoriteAlbum/Song1.mp3",
            "/storage/emulated/0/Download/MySelectedAlbum/Song4.flac",
            "/storage/emulated/0/Music/MyNewSelection/Song5.dsf"
        )
        val loadedSongPaths = listOf(
            "/storage/emulated/0/Music/MyFavoriteAlbum/Song1.mp3",
            "/storage/emulated/0/Download/MySelectedAlbum/Song2.flac")

        val pathsMergedExpected =  (loadedSongPaths + pathsToSave).distinct()


        saveSongPaths(context, loadedSongPaths){}
        saveSongPaths(context, pathsToSave){}
        val result = loadSongPaths(context)
        assertThat(result).containsExactlyElementsIn(pathsMergedExpected)
    }
    @Test
    fun saveLibraryPathsIfLibraryPathsNotEmptyTest() = runTest{
        val pathsToSave = listOf(
            "/storage/emulated/0/Music",
            "/storage/emulated/0/Download",
            "/storage/emulated/0/Music/MyNewSelection"
        )
        val loadedSongPaths = listOf(
            "/storage/emulated/0/Music",
            "/storage/emulated/0/Download")

        val pathsMergedExpected =  (loadedSongPaths + pathsToSave).distinct()


        saveSelectedPaths(context, loadedSongPaths)
        saveSelectedPaths(context, pathsToSave)
        val result = loadLibraryPaths(context)
        assertThat(result).containsExactlyElementsIn(pathsMergedExpected)
    }
    @Test
    fun saveSongPathsIfSongPathsIsEmptyTest() = runTest{
        val pathsToSave = listOf(
            "/storage/emulated/0/Music/MyFavoriteAlbum/Song1.mp3",
            "/storage/emulated/0/Download/MySelectedAlbum/Song4.flac",
            "/storage/emulated/0/Music/MyNewSelection/Song5.dsf"
        )
        saveSongPaths(context, pathsToSave){}
        val result = loadSongPaths(context)
        assertThat(result).containsExactlyElementsIn(pathsToSave)
    }
    @Test
    fun saveLibraryPathsIfLibraryPathsIsEmptyTest() = runTest{
        val pathsToSave = listOf(
            "/storage/emulated/0/Music",
            "/storage/emulated/0/Download",
            "/storage/emulated/0/Music/MyNewSelection"
        )
        saveSelectedPaths(context, pathsToSave)
        val result = loadLibraryPaths(context)
        assertThat(result).containsExactlyElementsIn(pathsToSave)
    }
}