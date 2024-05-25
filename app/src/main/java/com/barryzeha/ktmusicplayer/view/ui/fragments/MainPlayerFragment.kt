package com.barryzeha.ktmusicplayer.view.ui.fragments

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.barryzeha.core.common.createTime
import com.barryzeha.core.model.MainSongController
import com.barryzeha.core.model.SongController
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.databinding.FragmentMainPlayerBinding
import com.barryzeha.ktmusicplayer.service.MusicPlayerService
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.log


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

@AndroidEntryPoint
class MainPlayerFragment : Fragment() , ServiceConnection{

    private var param1: String? = null
    private var param2: String? = null
    private var _bind:FragmentMainPlayerBinding ? = null
    private val bind:FragmentMainPlayerBinding get() = _bind!!
    private val mainViewModel:MainViewModel by viewModels(ownerProducer = {requireActivity()})
    //private val mainViewModel:MainViewModel by activityViewModels()
    private var musicPlayerService: MusicPlayerService?=null
    private var songController:MainSongController = object:MainSongController{
        override fun play() {
            Log.e("PLAY-INTERFACE", "PLAY" )
        }

        override fun pause() {
            Log.e("PAUSE-INTERFACE", "PAUSE" )
        }

        override fun next() {

        }

        override fun previous() {

        }

        override fun stop() {

        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity?.let{
            _bind = FragmentMainPlayerBinding.inflate(inflater,container,false)
            _bind?.let{bind->
                return bind.root
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Important is necessary setSelected to textview for able marquee autoscroll when text is long than textView size
        setUpObservers()
        setUpListeners()
        bind.tvSongDescription.setSelected(true)
        bind.tvSongArtist.setSelected(true)
        bind.tvSongAlbum.setSelected(true)

    }
    private fun setUpObservers(){
        mainViewModel.currentTrack.observe(viewLifecycleOwner){
            it?.let{currentTrack->
                setUpSongInfo(currentTrack)
            }
        }
        mainViewModel.musicState.observe(viewLifecycleOwner){
            it?.let{musicState->
                bind.pbLinear.max=musicState.duration.toInt()
                bind.pbLinear.progress=musicState.currentDuration.toInt()
                bind.tvSongTimeRest.text= createTime(musicState.currentDuration).third
            }
        }
    }
    private fun setUpSongInfo(musicState: MusicState){
        bind.tvSongDescription.setSelected(true)
        bind.tvSongArtist.setSelected(true)
        bind.tvSongAlbum.setSelected(true)

        bind.tvSongAlbum.text=musicState.album
        bind.tvSongArtist.text=musicState.artist
        bind.tvSongDescription.text = musicState.title
        Glide.with(requireContext())
            .load(musicState.albumArt)
            .fitCenter()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(bind.ivMusicCover)
        bind.pbLinear.max=musicState.duration.toInt()
        bind.pbLinear.progress=musicState.currentDuration.toInt()
        bind.tvSongTimeRest.text= createTime(musicState.currentDuration).third
        bind.tvSongTimeCompleted.text = createTime(musicState.duration).third

    }
    private fun setUpListeners()=with(bind){
        btnMainPlay.setOnClickListener{
            musicPlayerService?.pauseExoPlayer()
        }
    }
    private fun linkToService(){
        context?.bindService(Intent (context, MusicPlayerService::class.java),this, Context.BIND_AUTO_CREATE)
    }
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as MusicPlayerService.MusicPlayerServiceBinder
        musicPlayerService = binder.getService()
        musicPlayerService!!.setMainSongController(songController)


    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicPlayerService=null
    }
    override fun onStart() {
        super.onStart()
        linkToService()
    }

    override fun onDestroy() {
        super.onDestroy()
        _bind=null
        context?.unbindService(this)
    }
    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MainPlayerFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}