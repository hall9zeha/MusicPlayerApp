package com.barryzeha.ktmusicplayer.view.ui.fragments.playerControls

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.SeekBar
import androidx.annotation.LayoutRes
import com.barryzeha.core.common.CLEAR_MODE
import com.barryzeha.core.common.REPEAT_ALL
import com.barryzeha.core.common.REPEAT_ONE
import com.barryzeha.core.common.SHUFFLE
import com.barryzeha.core.common.createTime
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.core.model.entities.SongMode
import com.barryzeha.ktmusicplayer.common.changeBackgroundColor
import com.barryzeha.ktmusicplayer.databinding.SmallPlayerControlsBinding
import com.barryzeha.ktmusicplayer.view.ui.fragments.AbsBaseFragment
import com.barryzeha.ktmusicplayer.view.ui.fragments.playlistFragment.ListFragment
import javax.annotation.Nullable

/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 9/2/25.
 * Copyright (c)  All rights reserved.
 **/

abstract class AbsPlaybackControlsFragment(@LayoutRes layout:Int): AbsBaseFragment(layout) {
    open val binding:SmallPlayerControlsBinding?=null
    protected  var listFragmentInstance:ListFragment?=null

    // Forward and rewind
    private var fastForwardingOrRewind = false
    private var fastForwardOrRewindHandler: Handler? = null
    private var forwardOrRewindRunnable:Runnable?=null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }
    private fun setupObservers(){
        mainViewModel.fragmentInstance.observe(viewLifecycleOwner){instance->
            if(instance is ListFragment) {
                listFragmentInstance = instance as ListFragment
                setNumberOfTracks()
            }
        }
    }
    private fun setupListeners()=with(binding){
        this?.let {
            btnPlay.setOnClickListener {
                if (musicPlayerService?.playListSize()!! > 0) {
                    if (!musicPlayerService!!.playingState() && musicPlayerService?.currentSongState()!!.duration <= 0) musicPlayerService?.getSongsList()
                        ?.get(0)
                        ?.let { song ->
                            musicPlayerService?.startPlayer(song)
                        }
                    else {
                        if (musicPlayerService?.playingState()!!) {
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
                } else {
                    getSongOfAdapter(-1)?.let { song ->
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
                        btnRepeat.setIconResource(com.barryzeha.core.R.drawable.ic_repeat_all)
                        btnRepeat.backgroundTintList =
                            changeBackgroundColor(requireContext(), false)
                        btnShuffle.backgroundTintList =
                            changeBackgroundColor(requireContext(), false)

                        mPrefs.songMode = CLEAR_MODE
                    }

                    SongMode.RepeatAll.ordinal -> {
                        // Second: repeat one
                        btnRepeat.setIconResource(com.barryzeha.core.R.drawable.ic_repeat_one)
                        btnShuffle.backgroundTintList =
                            changeBackgroundColor(requireContext(), false)
                        mPrefs.songMode = REPEAT_ONE
                    }

                    else -> {
                        // First: active repeat All
                        btnRepeat.backgroundTintList = changeBackgroundColor(requireContext(), true)
                        btnShuffle.backgroundTintList =
                            changeBackgroundColor(requireContext(), false)
                        mPrefs.songMode = REPEAT_ALL
                    }
                }
            }
            btnShuffle.setOnClickListener {
                when (mPrefs.songMode) {
                    SongMode.Shuffle.ordinal -> {
                        btnShuffle.backgroundTintList =
                            changeBackgroundColor(requireContext(), false)
                        mPrefs.songMode = CLEAR_MODE
                    }

                    else -> {
                        btnShuffle.backgroundTintList =
                            changeBackgroundColor(requireContext(), true)
                        mPrefs.songMode = SHUFFLE
                        btnRepeat.setIconResource(com.barryzeha.core.R.drawable.ic_repeat_all)
                        btnRepeat.backgroundTintList =
                            changeBackgroundColor(requireContext(), false)
                    }
                }
            }
        }
    }
    private fun fastForwardOrRewind(isForward:Boolean){
        fastForwardOrRewindHandler = Handler(Looper.getMainLooper())
        forwardOrRewindRunnable = Runnable{
            fastForwardingOrRewind = if(isForward) binding?.btnNext?.isPressed!!
            else binding?.btnPrevious?.isPressed!!
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
    fun setNumberOfTracks(@Nullable itemCount: Int?=null){
        listFragmentInstance?.setNumberOfTrack()?.let{(currentTrack, totalTracks)->
            binding?.tvNumberSong?.text=String.format("#%s/%s", currentTrack, itemCount?:totalTracks)
        }
    }
    private fun getSongOfAdapter(idSong: Long): SongEntity?{
        val song = if(idSong>-1){ listFragmentInstance?.musicListAdapter?.getSongById(idSong)}else{
            // Buscamos en la posición 1 porque primero tendremos un item header en la posición 0
            listFragmentInstance?.musicListAdapter?.getSongByPosition(1)
        }
        song?.let{
            val (numberedPosition, realPosition) =  listFragmentInstance?.musicListAdapter?.getPositionByItem(it)!!
            mainViewModel.setCurrentPosition(realPosition)
            mPrefs.currentIndexSong = numberedPosition.toLong()
            listFragmentInstance?.setNumberOfTrack(true)
            return song
        }
        return null
    }
    override fun onStart() {
        super.onStart()
        setupListeners()
    }
}