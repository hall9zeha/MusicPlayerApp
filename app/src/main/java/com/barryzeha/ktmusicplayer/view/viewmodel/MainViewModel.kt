package com.barryzeha.ktmusicplayer.view.viewmodel

import android.content.ServiceConnection
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.barryzeha.core.common.ScopedViewModel
import com.barryzeha.core.common.SingleMutableLiveData
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.PlaylistEntity
import com.barryzeha.core.model.entities.PlaylistWithSongsCrossRef
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.core.model.entities.SongState
import com.barryzeha.core.model.entities.SongStateWithDetail
import com.barryzeha.data.repository.MainRepository
import com.barryzeha.ktmusicplayer.MyApp
import com.barryzeha.ktmusicplayer.MyApp.Companion.mPrefs
import com.barryzeha.ktmusicplayer.service.MusicPlayerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    private var countItemsInserted:Long=0
    private var itemsCount:Long=0

    private var _allSongs:MutableLiveData<List<SongEntity>> = MutableLiveData()
    val allSongs:LiveData<List<SongEntity>> = _allSongs

    private var _songState:MutableLiveData<List<SongStateWithDetail>> = MutableLiveData()
    val songState:LiveData<List<SongStateWithDetail>> = _songState

    private var _orderBySelection:SingleMutableLiveData<Int> = SingleMutableLiveData()
    val orderBySelection:LiveData<Int> = _orderBySelection

    private var _allSongFromMain:MutableLiveData<List<SongEntity>> = MutableLiveData()
    val allSongFromMain:LiveData<List<SongEntity>> = _allSongFromMain

    private var _registerRowInserted:MutableLiveData<Long> = MutableLiveData()
    val registerRowInserted:LiveData<Long> = _registerRowInserted


    private var _isFavorite:MutableLiveData<Boolean> = MutableLiveData()
    val isFavorite:LiveData<Boolean> = _isFavorite

    private var _updatedRow:MutableLiveData<Int> = MutableLiveData()
    val updateRow:LiveData<Int> = _updatedRow

    private var _deletedRow:MutableLiveData<Int> = MutableLiveData()
    val deletedRow:LiveData<Int> = _deletedRow

    private var _deleteAllRows:SingleMutableLiveData<Int> = SingleMutableLiveData()
    val deleteAllRows:LiveData<Int> = _deleteAllRows

    private var _songById:SingleMutableLiveData<SongEntity> = SingleMutableLiveData()
    val songById:LiveData<SongEntity> = _songById

    private var _currentSongListPosition:MutableLiveData<Int> = MutableLiveData()
    val currentSongListPosition:LiveData<Int> = _currentSongListPosition

    private var _musicState:MutableLiveData<MusicState> = MutableLiveData()
    val musicState:LiveData<MusicState> = _musicState

    private var _currentTrack:MutableLiveData<MusicState> = MutableLiveData()
    val currentTrack:LiveData<MusicState> = _currentTrack

    private var _isPlaying:MutableLiveData<Boolean> = MutableLiveData()
    val isPlaying:LiveData<Boolean> = _isPlaying

    private var _serviceInstance:MutableLiveData<Pair<ServiceConnection,MusicPlayerService>> = MutableLiveData()
    val serviceInstance:LiveData<Pair<ServiceConnection,MusicPlayerService>> = _serviceInstance

    private var _processedRegistersInfo:SingleMutableLiveData<Pair<Int,Int>> = SingleMutableLiveData()
    val progressRegisterSaved:LiveData<Pair<Int,Int>> = _processedRegistersInfo

    private var _createdPlayList:SingleMutableLiveData<Long> = SingleMutableLiveData()
    val createdPlayList:LiveData<Long> = _createdPlayList

    private var _playLists:MutableLiveData<List<PlaylistEntity>> = MutableLiveData()
    val playLists:LiveData<List<PlaylistEntity>> = _playLists

    private var _playlistWithSongRefInserted:SingleMutableLiveData<Long> = SingleMutableLiveData()
    val playlistWithSongRefInserted:LiveData<Long> = _playlistWithSongRefInserted

    // Song tag edited
    private var _isSongTagEdited:SingleMutableLiveData<SongEntity> = SingleMutableLiveData()
    val isSongTagEdited:LiveData<SongEntity> = _isSongTagEdited

    init{
        initScope()

        CoroutineScope(Dispatchers.IO).launch {
            awaitAll(
                async{ getPlaylistWithSongsBy( mPrefs.playlistId,mPrefs.playListSortOption)},
                async{ getPlayLists() }
            )
        }
    }
    fun setItemsCount(itemsCount:Int){
        this.itemsCount = itemsCount.toLong()
    }
    fun fetchAllSong(){
      launch{
          _allSongs.value=repository.fetchAllSongs()
         }
    }
    private suspend fun getPlaylistWithSongsBy(playlistId:Int,field:Int){
        _allSongs.postValue(repository.fetchPlaylistOrderBy(playlistId.toLong(), field))
    }
    fun fetchPlaylistWithSongsBy(playlistId:Int,field:Int){
        launch{
            _allSongs.value =repository.fetchPlaylistOrderBy(playlistId.toLong(), field)
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

    fun setOrderListBy(selection:Int){
        launch { _orderBySelection.value=selection }
    }
    fun saveNewSong(songEntity: SongEntity){

        launch {
            val idInserted=repository.saveNewSong(songEntity)
            getSongById(idInserted)
           _processedRegistersInfo.value = Pair(itemsCount.toInt(), countItemsInserted.toInt())
           Log.e("SAVE-NEW-SONG", "$itemsCount --: $countItemsInserted" )
        }
    }

    fun saveSongs(songList:List<SongEntity>){
        itemsCount=songList.size.toLong()
        launch(Dispatchers.IO){

                songList.forEach { song ->
                    withContext(Dispatchers.Main){
                        saveNewSong(song)
                    }
                }
        }
    }

    fun saveSongState(songState: SongState){
        launch {
             repository.saveSongState(songState)
        }
    }
    fun updateSong(songEntity: SongEntity){
        launch {
            val rowUpdated=repository.updateSong(songEntity)
            if(rowUpdated>0){
                checkIfIsFavorite(songEntity.id)
            }
        }
    }
    fun updateFavoriteSong(isFavorite:Boolean,idSong:Long){
        launch {
            val rowUpdated=repository.updateFavoriteSong(isFavorite,idSong)
            if(rowUpdated>0){
                checkIfIsFavorite(idSong)
            }
        }
    }
    fun checkIfIsFavorite(idSong:Long){
        launch{
            val entity = repository.fetchSongById(idSong)
            entity?.let {e->
                _isFavorite.value = e.favorite
            }
        }
    }
    fun deleteSong(songEntity: SongEntity){
      launch {
            _deletedRow.value = repository.deleteSong(songEntity.id)
        }
    }
    fun deleteSong(itemList:List<SongEntity>){
        launch {
            val songIds:MutableList<Long> = arrayListOf()
            itemList.forEach {item->
                songIds.add(item.id)

            }
            repository.deleteSong(songIds)
        }
    }
    fun deleteAllSongs(){
        launch {
            _deleteAllRows.value = repository.deleteAllSongs()
            repository.deleteAllSongsState()
        }
    }
    fun getSongById(idSong:Long){
        launch {
            _songById.value=repository.fetchSongById(idSong)
            countItemsInserted++
            if(itemsCount==countItemsInserted || itemsCount== countItemsInserted-1){
                fetchAllSong()
                itemsCount=0
                countItemsInserted =0
            }
            Log.e("SAVE-NEW-SONG", "$itemsCount --: $countItemsInserted" )
        }
    }
    // for SongState
    fun removeSongState(idSong: Long){
        launch{
            repository.deleteSongState(idSong)
        }
    }
    // For playlist
    fun createPlayList(playlistEntity:PlaylistEntity){
        launch{
            _createdPlayList.value = repository.createPlayList(playlistEntity)
        }
    }
    fun fetchPlaylists(){
        launch{
           _playLists.value= repository.fetchPlaylists()
        }
    }
    private suspend fun getPlayLists(){
        _playLists.postValue(repository.fetchPlaylists())
    }
    // For playlist with songs cross ref
    fun savePlaylistWithSongRef(playlistWithSongsCrossRef: PlaylistWithSongsCrossRef){
        launch{
            _playlistWithSongRefInserted.value=repository.savePlaylistWithSongCrossRef(playlistWithSongsCrossRef)
        }
    }
    fun deletePlayList(playlistId: Long){
        launch{
            repository.deletePlaylist(playlistId)
        }
    }
    fun updatePlaylist(playlistEntity: PlaylistEntity){
        launch {
            repository.updatePlaylist(playlistEntity)
        }
    }
    // Temporal values, not insert to database
    fun setCurrentPosition(position:Int){
        launch {
            _currentSongListPosition.value = position
            MyApp.mPrefs.currentIndexSong=position.toLong()
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
    fun setServiceInstance(serviceConnection:ServiceConnection,serviceInstance:MusicPlayerService){
        launch {
            _serviceInstance.value=Pair(serviceConnection,serviceInstance)
        }
    }
    // Song tag edited
    fun setIsSongTagEdited(songEntity:SongEntity){
        launch{
            _isSongTagEdited.value = songEntity
        }
    }
    override fun onCleared() {
        destroyScope()
        super.onCleared()
    }



}