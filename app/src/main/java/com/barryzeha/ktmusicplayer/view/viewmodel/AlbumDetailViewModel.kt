package com.barryzeha.ktmusicplayer.view.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.common.ScopedViewModel
import com.barryzeha.core.common.SingleMutableLiveData
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.data.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 23/3/25.
 * Copyright (c)  All rights reserved.
 **/
@HiltViewModel
 class AlbumDetailViewModel @Inject constructor(private val repository: MainRepository, private val mPrefs: MyPreferences):
 ScopedViewModel() {
 private var _songsByAlbum: MutableLiveData<List<SongEntity>> = MutableLiveData()
 val songsByAlbum: LiveData<List<SongEntity>> = _songsByAlbum

 fun fetchSongsByAlbum(album:String){
   launch(Dispatchers.IO){
    val songs = repository.fetchSongsByAlbum(album)
    withContext(Dispatchers.Main){_songsByAlbum.value = songs}
   }
 }
 override fun onCleared() {
  destroyScope()
  super.onCleared()
 }
}