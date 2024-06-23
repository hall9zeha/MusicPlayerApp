package com.barryzeha.ktmusicplayer.view.viewmodel

import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.exoplayer.ExoPlayer
import com.barryzeha.core.common.ScopedViewModel
import com.barryzeha.core.common.SingleMutableLiveData
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.core.model.entities.SongState
import com.barryzeha.core.model.entities.SongStateWithDetail
import com.barryzeha.data.repository.MainRepository
import com.barryzeha.ktmusicplayer.MyApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/4/24.
 * Copyright (c)  All rights reserved.
 **/

@HiltViewModel
class MainViewModel @Inject constructor(private val repository:MainRepository):ScopedViewModel() {

    private var _allSongs:MutableLiveData<List<SongEntity>> = MutableLiveData()
    val allSongs:LiveData<List<SongEntity>> = _allSongs

    private var _songState:MutableLiveData<List<SongStateWithDetail>> = MutableLiveData()
    val songState:LiveData<List<SongStateWithDetail>> = _songState

    private var _allSongFromMain:MutableLiveData<List<SongEntity>> = MutableLiveData()
    val allSongFromMain:LiveData<List<SongEntity>> = _allSongFromMain

    private var _registerRowInserted:MutableLiveData<Long> = MutableLiveData()
    val registerRowInserted:LiveData<Long> = _registerRowInserted

    private var _deletedRow:MutableLiveData<Int> = MutableLiveData()
    val deletedRow:LiveData<Int> = _deletedRow

    private var _songById:SingleMutableLiveData<SongEntity> = SingleMutableLiveData()
    val songById:LiveData<SongEntity> = _songById

    private var _currentTimeOfSong:MutableLiveData<Triple<Int,Int,String>> = MutableLiveData()
    val currentTimeOfSong:LiveData<Triple<Int,Int,String>> = _currentTimeOfSong

    private var _currentSongListPosition:MutableLiveData<Int> = MutableLiveData()
    val currentSongListPosition:LiveData<Int> = _currentSongListPosition

    private var _musicState:MutableLiveData<MusicState> = MutableLiveData()
    val musicState:LiveData<MusicState> = _musicState

    private var _currentTrack:MutableLiveData<MusicState> = MutableLiveData()
    val currentTrack:LiveData<MusicState> = _currentTrack

    private var _isPlaying:MutableLiveData<Boolean> = MutableLiveData()
    val isPlaying:LiveData<Boolean> = _isPlaying

    init{
        initScope()
    }
    fun fetchAllSong(){
      launch{
          _allSongs.value=repository.fetchAllSongs()
         }
    }
    fun fetchAllSongFromMain(){
        launch {
            _allSongFromMain.value=repository.fetchAllSongs()
        }
    }
    // SongState
    fun fetchSongState(){
        launch{
            _songState.value=repository.fetchSongState()
        }
    }
    //*************************************
    fun saveNewSong(songEntity: SongEntity){
        launch {
            val idInserted=repository.saveNewSong(songEntity)
            fetchAllSong()
            //getSongById(idInserted)
        }
    }
    fun saveSongState(songState: SongState){
        launch {
             repository.saveSongState(songState)
        }
    }
    fun deleteSong(songEntity: SongEntity){
      launch {
            _deletedRow.value = repository.deleteSong(songEntity.id)
        }
    }
    fun getSongById(idSong:Long){
        launch {
            _songById.value=repository.fetchSongById(idSong)
        }
    }
    fun fetchCurrentTimeOfSong(mediaPlayer:ExoPlayer){
       launch {
            repository.fetchCurrentTimeOfSong(mediaPlayer)
                .catch { Log.e("ERROR_FLOW",it.message.toString() ) }
                .collect{_currentTimeOfSong.value = it}
        }
    }

    // Temporal values, not insert with database
    fun setCurrentPosition(position:Int){
        launch {
            _currentSongListPosition.value = position
            MyApp.mPrefs.currentPosition=position.toLong()
        }
    }


    fun setMusicState(musicState: MusicState){
        launch {
            _musicState.value = musicState
        }
    }
    fun setCurrentTrack(musicState: MusicState){
       launch {
            _currentTrack.value = musicState
        }
    }
    fun saveStatePlaying(isPlaying:Boolean){
       launch {
            _isPlaying.value = isPlaying
        }
    }

    override fun onCleared() {
        destroyScope()
        super.onCleared()
    }



}