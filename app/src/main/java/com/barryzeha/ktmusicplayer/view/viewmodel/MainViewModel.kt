package com.barryzeha.ktmusicplayer.view.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barryzeha.core.entities.SongEntity
import com.barryzeha.data.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

    fun fetchAllSong(){
        viewModelScope.launch {
            _allSongs.value = repository.fetchAllSongs()
        }
    }
    fun saveNewSong(songEntity: SongEntity){
        viewModelScope.launch {
            _registerRowInserted.value = repository.saveNewSong(songEntity)
        }
    }



}