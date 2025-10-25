package com.barryzeha.ktmusicplayer.view.viewmodel


import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.data.dao.PlaylistDAO
import com.barryzeha.data.dao.SongDao
import com.barryzeha.data.database.SongDatabase
import com.barryzeha.data.repository.MainRepository
import com.barryzeha.data.repository.MainRepositoryImpl
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 22/10/25.
 * Copyright (c)  All rights reserved.
 **/
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    val testDispatcher = StandardTestDispatcher()

    lateinit var mockPlaylistDao: PlaylistDAO

    lateinit var mockRepository: MainRepository
    lateinit var mockPreferences: MyPreferences
    lateinit var viewModel:MainViewModel
    var fakeSongs:List<SongEntity> = listOf()
    var fakeSong:SongEntity = SongEntity()


    @Before
    fun setup(){
        Dispatchers.setMain(testDispatcher)
        mockRepository = mockk<MainRepository>()
        mockPlaylistDao = mockk(relaxed = true)
        mockPreferences = mockk(relaxed = true)

        fakeSongs=listOf(
            SongEntity(id = 1, description = "Fake Song 1", album = "album 1"),
            SongEntity(id = 2, description = "Fake Song 2", album ="album 2"),
            SongEntity(id = 3, description = "Fake Song 3", album = "album 3")
        )
        fakeSong = SongEntity(
            id = 4, description = "Fake Song 4", album = "album 4"
        )
        coEvery { mockRepository.fetchPlaylistOrderBy(any(), any()) } returns emptyList()
        coEvery { mockRepository.fetchPlaylists() } returns emptyList()
        viewModel = MainViewModel(mockRepository, mockPreferences)

    }

    @After
    fun tearDown(){
        Dispatchers.resetMain()
    }

    @Test
    fun fetchAllSong()= runTest{

        coEvery { mockRepository.fetchAllSongs() } returns fakeSongs

        viewModel.fetchAllSong()
        advanceUntilIdle()

        val result = viewModel.allSongs.getOrAwaitValue()
        assertThat(result).isEqualTo(fakeSongs)
    }
    @Test
    fun getSongById()=runTest{
        coEvery { mockRepository.fetchSongById(1) } returns fakeSongs.first()
        coEvery {mockRepository.fetchAllSongs()} returns listOf(fakeSongs.first())

        viewModel.getSongById(1)
        advanceUntilIdle()

        coVerify(exactly = 1) {mockRepository.fetchSongById(1) }
        coVerify(exactly = 1) {mockRepository.fetchAllSongs() }

        assertThat(viewModel.allSongs.value?.first()?.id).isEqualTo(fakeSongs.first().id)
    }

    @Test
    fun fetchPlaylistWithSongsBy()=runTest{
        coEvery{ mockRepository.fetchPlaylistOrderBy(1,1)} returns fakeSongs

        viewModel.fetchPlaylistWithSongsBy(1,1)
        advanceUntilIdle()
        val result = viewModel.allSongs.getOrAwaitValue()

        coVerify(exactly = 1) {mockRepository.fetchPlaylistOrderBy(1,1)  }
        assertThat(result.size).isEqualTo(fakeSongs.size)
        assertThat(result).isEqualTo(fakeSongs)
    }

    @Test
    fun saveNewSong() = runTest{

        coEvery{ mockRepository.saveNewSong(fakeSong)} returns 1L
        coEvery{mockRepository.fetchSongById(1L)} returns fakeSong
        coEvery { mockRepository.fetchAllSongs() } returns listOf(fakeSong)

        viewModel.setItemsCount(1)
        viewModel.saveNewSong(fakeSong)
        advanceUntilIdle()

        coVerify(exactly = 1) {mockRepository.saveNewSong(fakeSong) }
        coVerify(exactly = 1) {mockRepository.fetchSongById(1L) }

        val info = viewModel.progressRegisterSaved.getOrAwaitValue()

        assertThat(info.first).isEqualTo(1)
        assertThat(info.second).isEqualTo(1)

    }

    @Test
    fun saveNewSong_emitsIntermediateProgressValue()=runTest{

        coEvery{mockRepository.fetchSongById(1L)} returns fakeSong
        coEvery{mockRepository.fetchAllSongs()} returns listOf(fakeSong)

        val emittedValues = mutableListOf<Pair<Int,Int>>()

        val observer = Observer<Pair<Int,Int>>{value->
            emittedValues.add(value)
        }
        viewModel.setItemsCount(1)
        viewModel.progressRegisterSaved.observeForever(observer)
        viewModel.getSongById(1L)
        advanceUntilIdle()

        viewModel.progressRegisterSaved.removeObserver(observer)

        assertThat(emittedValues).contains(Pair(1,1))
    }

    fun<T> LiveData<T>.getOrAwaitValue(
        time:Long = 2,
        timeUnit:TimeUnit = TimeUnit.SECONDS
    ):T{
        var data:T? = null
        val latch = CountDownLatch(1)
        val observer = object: Observer<T> {
            override fun onChanged(value: T) {
                data = value
                latch.countDown()
                this@getOrAwaitValue.removeObserver(this)
            }
        }
        this.observeForever(observer)

        if(!latch.await(time,timeUnit)){
            throw TimeoutException("LiveData value was never set.")
        }
        return data as T
    }

}