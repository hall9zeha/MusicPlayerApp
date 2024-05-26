package com.barryzeha.ktmusicplayer.view.viewmodel

import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.exoplayer.ExoPlayer
import com.barryzeha.core.model.entities.MusicState
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

    private var _musicState:MutableLiveData<MusicState> = MutableLiveData()
    val musicState:LiveData<MusicState> = _musicState

    private var _currentTrack:MutableLiveData<MusicState> = MutableLiveData()
    val currentTrack:LiveData<MusicState> = _currentTrack

    private var _isPlaying:MutableLiveData<Boolean> = MutableLiveData()
    val isPlaying:LiveData<Boolean> = _isPlaying

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
    fun fetchCurrentTimeOfSong(mediaPlayer:ExoPlayer){
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

    fun setMusicState(musicState: MusicState){
        viewModelScope.launch {
            _musicState.value = musicState
        }
    }
    fun setCurrentTrack(musicState: MusicState){
        viewModelScope.launch {
            _currentTrack.value = musicState
        }
    }
    fun saveStatePlaying(isPlaying:Boolean){
        viewModelScope.launch {
            _isPlaying.value = isPlaying
        }
    }


}