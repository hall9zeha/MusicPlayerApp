package com.barryzeha.core.common

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.barryzeha.core.model.entities.LibraryPaths
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
    fun loadLibraryPathsTest() = runTest{
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
}