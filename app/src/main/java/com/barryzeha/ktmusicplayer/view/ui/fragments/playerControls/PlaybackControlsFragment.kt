package com.barryzeha.ktmusicplayer.view.ui.fragments.playerControls

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import com.barryzeha.core.common.createTime
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.SongMode
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.common.changeBackgroundColor
import com.barryzeha.ktmusicplayer.databinding.SmallPlayerControlsBinding
import com.barryzeha.ktmusicplayer.view.ui.fragments.playlistFragment.ListFragment
import com.barryzeha.core.R as coreRes

/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 9/2/25.
 * Copyright (c)  All rights reserved.
 **/

class PlaybackControlsFragment : AbsPlaybackControlsFragment(R.layout.small_player_controls){
    private var _bind:SmallPlayerControlsBinding?=null
    private val bind get() = _bind!!
    override val binding: SmallPlayerControlsBinding
        get() = bind

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _bind = SmallPlayerControlsBinding.bind(view)
        mainViewModel.sharedControlsPlayerFragmentInstance(this)
        setupObservers()

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
        mainViewModel.progressRegisterSaved.observe(viewLifecycleOwner) { (totalRegisters, count) ->
            setNumberOfTracks(count)
        }
        mainViewModel.musicState.observe(viewLifecycleOwner){
            updateUI(it)
        }
        mainViewModel.isPlaying.observe(viewLifecycleOwner){
            updatePlayerStateUI(it)
        }
    }
    fun updatePlayerStateUI(statePlay:Boolean)=with(bind){
        if (statePlay)btnPlay.setIconResource(coreRes.drawable.ic_circle_pause)
        else btnPlay.setIconResource(coreRes.drawable.ic_play)
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