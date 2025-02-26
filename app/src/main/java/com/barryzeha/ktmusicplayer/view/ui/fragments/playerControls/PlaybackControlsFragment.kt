package com.barryzeha.ktmusicplayer.view.ui.fragments.playerControls

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import com.barryzeha.core.common.CLEAR_MODE
import com.barryzeha.core.common.REPEAT_ALL
import com.barryzeha.core.common.REPEAT_ONE
import com.barryzeha.core.common.SHUFFLE
import com.barryzeha.core.common.createTime
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.core.model.entities.SongMode
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.core.R as coreRes
import com.barryzeha.ktmusicplayer.common.changeBackgroundColor
import com.barryzeha.ktmusicplayer.databinding.SmallPlayerControlsBinding
import com.barryzeha.ktmusicplayer.view.ui.adapters.MusicListAdapter
import com.barryzeha.ktmusicplayer.view.ui.fragments.playlistFragment.ListFragment

/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 9/2/25.
 * Copyright (c)  All rights reserved.
 **/

class PlaybackControlsFragment : AbsPlaybackControlsFragment(R.layout.small_player_controls){
    private var _bind:SmallPlayerControlsBinding?=null
    private val bind:SmallPlayerControlsBinding get() = _bind!!
    // Forward and rewind
    private var fastForwardingOrRewind = false
    private var fastForwardOrRewindHandler: Handler? = null
    private var forwardOrRewindRunnable:Runnable?=null
    private var musicListAdapter:MusicListAdapter?=null
    private var isPlaying:Boolean=false
    private var listFragmentInstance:ListFragment?=null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _bind = SmallPlayerControlsBinding.bind(view)
        mainViewModel.sharedControlsPlayerFragmentInstance(this)
        setupListeners()
        setupObservers()

    }
    fun setNumberOfTracks(){
        listFragmentInstance?.setNumberOfTrack()?.let{(currentTrack, totalTracks)->
            bind.tvNumberSong.text=String.format("#%s/%s", currentTrack, totalTracks)
        }
    }
    fun setAdapterInstance(musicAdapter:MusicListAdapter){
        musicListAdapter=musicAdapter
    }
    fun setListMusicFragmentInstance(instance:Any){
        listFragmentInstance = if(instance is ListFragment) instance else null
        listFragmentInstance?.setNumberOfTrack()
    }
    private fun setupObservers(){
        mainViewModel.fragmentInstance.observe(viewLifecycleOwner){instance->
            if(instance is ListFragment) {
                listFragmentInstance = instance as ListFragment
                setNumberOfTracks()
            }
        }
        mainViewModel.currentTrack.observe(viewLifecycleOwner){
            updateUIOnceTime(it)
            setNumberOfTracks()
        }
        mainViewModel.musicState.observe(viewLifecycleOwner){
            updateUI(it)
        }
        mainViewModel.isPlaying.observe(viewLifecycleOwner){
            updatePlayerStateUI(it)
        }
    }

    private fun setupListeners()=with(bind){
         btnPlay.setOnClickListener {
             if (musicPlayerService?.playListSize()!! > 0) {
                 if (!musicPlayerService!!.playingState() && musicPlayerService?.currentSongState()!!.duration <= 0) getSongOfAdapter(
                     mPrefs.idSong
                 )?.let { song ->
                     musicPlayerService?.startPlayer(song)
                 }
                 else {
                     if (isPlaying) {
                         musicPlayerService?.pausePlayer()
                         mainViewModel.saveStatePlaying(false)
                     } else {
                         musicPlayerService?.resumePlayer()
                         mainViewModel.saveStatePlaying(true)
                     }
                 }
             }
         }
         btnPrevious.setOnClickListener {
             if (musicPlayerService?.getCurrentSongPosition()!! > 0) {
                     musicPlayerService?.prevSong()
                     listFragmentInstance?.setNumberOfTrack(true)
                 setNumberOfTracks()
             }
         }
         btnNext.setOnClickListener {
             if (musicPlayerService?.getCurrentSongPosition()!! < musicPlayerService?.playListSize()!! - 1) {
                    musicPlayerService?.nextSong()
                    listFragmentInstance?.setNumberOfTrack(true)
                 setNumberOfTracks()
                 }
             else {
                 getSongOfAdapter(0)?.let { song ->
                     musicPlayerService?.startPlayer(song)
                     setNumberOfTracks()
                 }
             }
         }
         btnNext.setOnLongClickListener {
             fastForwardOrRewind(true)
             true
         }
         btnPrevious.setOnLongClickListener {
             fastForwardOrRewind(false)
             true
         }
         loadSeekBar.setOnSeekBarChangeListener(object :
             SeekBar.OnSeekBarChangeListener {
             override fun onProgressChanged(
                 seekBar: SeekBar?,
                 progress: Int,
                 fromUser: Boolean
             ) {
                 if (fromUser) {
                     tvInitTime.text = createTime(progress.toLong()).third
                     userSelectPosition = progress
                 }
             }
             override fun onStartTrackingTouch(seekBar: SeekBar?) {
                 isUserSeeking = true
             }
             override fun onStopTrackingTouch(seekBar: SeekBar?) {
                 isUserSeeking = false
                 musicPlayerService?.setPlayerProgress(seekBar?.progress?.toLong()!!)
                 loadSeekBar.progress = userSelectPosition
             }
         })
         btnRepeat.setOnClickListener {

             when (mPrefs.songMode) {
                 SongMode.RepeatOne.ordinal -> {
                     //  Third: deactivate modes
                     btnRepeat.setIconResource(coreRes.drawable.ic_repeat_all)
                     btnRepeat.backgroundTintList=changeBackgroundColor(requireContext(),false)
                     btnShuffle.backgroundTintList=changeBackgroundColor(requireContext(),false)

                     mPrefs.songMode = CLEAR_MODE
                 }
                 SongMode.RepeatAll.ordinal -> {
                     // Second: repeat one
                     btnRepeat.setIconResource(coreRes.drawable.ic_repeat_one)
                     btnShuffle.backgroundTintList=changeBackgroundColor(requireContext(),false)
                     mPrefs.songMode = REPEAT_ONE
                 }
                 else -> {
                     // First: active repeat All
                     btnRepeat.backgroundTintList=changeBackgroundColor(requireContext(),true)
                     btnShuffle.backgroundTintList=changeBackgroundColor(requireContext(),false)
                     mPrefs.songMode= REPEAT_ALL
                 }
             }
         }
          btnShuffle.setOnClickListener {
              when(mPrefs.songMode){
                  SongMode.Shuffle.ordinal->{
                      btnShuffle.backgroundTintList=changeBackgroundColor(requireContext(),false)
                      mPrefs.songMode= CLEAR_MODE
                  }
                  else->{
                      btnShuffle.backgroundTintList=changeBackgroundColor(requireContext(),true)
                      mPrefs.songMode= SHUFFLE
                      btnRepeat.setIconResource(coreRes.drawable.ic_repeat_all)
                      btnRepeat.backgroundTintList=changeBackgroundColor(requireContext(),false)
                  }
              }
          }
    }
    private fun fastForwardOrRewind(isForward:Boolean){
        fastForwardOrRewindHandler = Handler(Looper.getMainLooper())
        forwardOrRewindRunnable = Runnable{
            fastForwardingOrRewind = if(isForward) bind.btnNext.isPressed
            else bind.btnPrevious.isPressed
            if(fastForwardingOrRewind){
                if(isForward){musicPlayerService?.fastForward()}
                else{musicPlayerService?.fastRewind()}
            }else{
                fastForwardOrRewindHandler?.removeCallbacks(forwardOrRewindRunnable!!)
            }
            fastForwardOrRewindHandler?.postDelayed(forwardOrRewindRunnable!!,200)
        }
        fastForwardOrRewindHandler?.post(forwardOrRewindRunnable!!)
    }
    private fun getSongOfAdapter(idSong: Long): SongEntity?{
        val song = if(idSong>-1){ musicListAdapter?.getSongById(idSong)}else{
            // Buscamos en la posición 1 porque primero tendremos un item header en la posición 0
            musicListAdapter?.getSongByPosition(1)
        }
        song?.let{
            val (numberedPosition, realPosition) =  musicListAdapter?.getPositionByItem(it)!!
            mainViewModel.setCurrentPosition(realPosition)
            mPrefs.currentIndexSong = numberedPosition.toLong()
            listFragmentInstance?.setNumberOfTrack(true)
            recyclerView?.scrollToPosition(realPosition)
            return song
        }
        return null
    }
    fun updatePlayerStateUI(statePlay:Boolean)=with(bind){
        isPlaying=statePlay
        if (statePlay)btnPlay?.setIconResource(coreRes.drawable.ic_circle_pause)
        else btnPlay?.setIconResource(coreRes.drawable.ic_play)
    }
    fun updateUIOnceTime(musicState: MusicState)=with(bind){
        tvEndTime.text = createTime(musicState.duration).third
        loadSeekBar.max = musicState.duration.toInt()
        tvInitTime.text = createTime(musicState.currentDuration).third
    }
    private fun updateUI(musicState: MusicState)=with(bind){
        loadSeekBar.max = musicState.duration.toInt()
        tvInitTime.text = createTime(musicState.currentDuration).third
        loadSeekBar.progress = musicState.currentDuration.toInt()

    }
    @SuppressLint("ResourceType")
    private fun checkPlayerSongModePreferences()=with(bind){
        this?.let {
            when (mPrefs.songMode) {
                SongMode.RepeatOne.ordinal -> {
                    btnRepeat.setIconResource(coreRes.drawable.ic_repeat_one)
                    btnRepeat.backgroundTintList = changeBackgroundColor(requireContext(),true)
                }
                SongMode.RepeatAll.ordinal -> {
                   btnRepeat.backgroundTintList = changeBackgroundColor(requireContext(),true)
                }
                SongMode.Shuffle.ordinal ->{
                   btnShuffle.backgroundTintList = changeBackgroundColor(requireContext(),true)
                }
                else -> {
                    btnRepeat.setIconResource(coreRes.drawable.ic_repeat_all)
                    btnRepeat.backgroundTintList = changeBackgroundColor(requireContext(),false)
                    btnShuffle.backgroundTintList = changeBackgroundColor(requireContext(),false)
                }
            }
        }
    }
    override fun play() {
        super.play()
        musicPlayerService?.resumePlayer()
        mainViewModel.saveStatePlaying(true)
    }

    override fun pause() {
        super.pause()
        musicPlayerService?.pausePlayer()
        mainViewModel.saveStatePlaying(false)

    }
    override fun stop() {
        super.stop()
        activity?.finish()
    }

    override fun next() {
        super.next()
        setNumberOfTracks()
    }

    override fun previous() {
        super.previous()
        setNumberOfTracks()
    }

    override fun onResume() {
        super.onResume()
        checkPlayerSongModePreferences()
        setNumberOfTracks()
    }
}