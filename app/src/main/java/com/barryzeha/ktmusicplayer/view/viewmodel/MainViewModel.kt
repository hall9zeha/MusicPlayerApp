package com.barryzeha.ktmusicplayer.view.viewmodel

import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.data.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/4/24.
 * Copyright (c)  All rights reserved.
 **/

@HiltViewModel
class MainViewModel @Inject constructor(private val repository:MainRepository):ViewModel() {

    private var _allSongs:MutableLiveData<List<SongEntity>> = MutableLiveData()
    val allSongs:LiveData<List<SongEntity>> = _allSongs

    private var _registerRowInserted:MutableLiveData<Long> = MutableLiveData()
    val registerRowInserted:LiveData<Long> = _registerRowInserted

    private var _songById:MutableLiveData<SongEntity> = MutableLiveData()
    val songById:LiveData<SongEntity> = _songById

    private var _currentTimeOfSong:MutableLiveData<Triple<Int,Int,String>> = MutableLiveData()
    val currentTimeOfSong:LiveData<Triple<Int,Int,String>> = _currentTimeOfSong

    private var _currentSongListPosition:MutableLiveData<Int> = MutableLiveData()
    val currentSongListPosition:LiveData<Int> = _currentSongListPosition

    fun fetchAllSong(){
        viewModelScope.launch {
            _allSongs.value = repository.fetchAllSongs()
        }
    }
    fun saveNewSong(songEntity: SongEntity){
        viewModelScope.launch {
            val idInserted=repository.saveNewSong(songEntity)
            getSongById(idInserted)
        }
    }
    fun getSongById(idSong:Long){
        viewModelScope.launch {
            _songById.value = repository.fetchSongById(idSong)
        }
    }
    fun fetchCurrentTimeOfSong(mediaPlayer:MediaPlayer){
        viewModelScope.launch {
            repository.fetchCurrentTimeOfSong(mediaPlayer)
                .catch { Log.e("ERROR_FLOW",it.message.toString() ) }
                .collect{_currentTimeOfSong.value = it}
        }
    }
    fun setCurrentPosition(position:Int){
        viewModelScope.launch {
            _currentSongListPosition.value = position
        }
    }



}