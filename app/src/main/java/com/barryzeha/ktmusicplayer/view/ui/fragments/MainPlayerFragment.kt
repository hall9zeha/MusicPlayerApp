package com.barryzeha.ktmusicplayer.view.ui.fragments

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.common.createTime
import com.barryzeha.core.common.getSongCover
import com.barryzeha.core.common.loadImage
import com.barryzeha.core.common.toObject
import com.barryzeha.core.model.SongController
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.ktmusicplayer.MyApp
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.databinding.FragmentMainPlayerBinding
import com.barryzeha.ktmusicplayer.service.MusicPlayerService
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import dagger.hilt.android.AndroidEntryPoint
import com.barryzeha.core.R as coreRes


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

@AndroidEntryPoint
class MainPlayerFragment : Fragment() , ServiceConnection{

    private var param1: String? = null
    private var param2: String? = null
    private var _bind:FragmentMainPlayerBinding ? = null
    private var isPlaying:Boolean = false
    private var currentMusicState = MusicState()
    private var songLists:MutableList<SongEntity> = arrayListOf()
    private var currentSelectedPosition=0
    private lateinit var mPrefs:MyPreferences
    private val bind:FragmentMainPlayerBinding get() = _bind!!
    private val mainViewModel:MainViewModel by viewModels(ownerProducer = {requireActivity()})
    //private val mainViewModel:MainViewModel by activityViewModels()
    private var musicPlayerService: MusicPlayerService?=null
    private var songController:SongController = object:SongController{
        override fun play() {
            bind.btnMainPlay.setIconResource(coreRes.drawable.ic_pause)
            musicPlayerService?.playingExoPlayer()
            mainViewModel.saveStatePlaying(true)
        }

        override fun pause() {
            bind.btnMainPlay.setIconResource(coreRes.drawable.ic_play)
            musicPlayerService?.pauseExoPlayer()
            mainViewModel.saveStatePlaying(false)
        }

        override fun next() {
            bind.btnMainNext.performClick()
        }

        override fun previous() {
            bind.btnMainPrevious.performClick()
        }

        override fun stop() {
            startOrUpdateService()
        }

        override fun musicState(musicState: MusicState?) {
            musicState?.let{
                mainViewModel.setMusicState(musicState)
            }
        }

        override fun currentTrack(musicState: MusicState?) {
            musicState?.let{
                mainViewModel.setCurrentTrack(musicState)
                if(!musicState.isPlaying){
                    if((songLists.size -1)  == currentSelectedPosition) {
                        bind.btnMainPlay.setIconResource(coreRes.drawable.ic_play)
                        mainViewModel.saveStatePlaying(false)
                        mainViewModel.setCurrentPosition(0)
                    }
                    else {
                        mainViewModel.saveStatePlaying(true)
                        bind.btnMainNext.performClick()}
                }else{
                    mainViewModel.saveStatePlaying(true)
                }
            }
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
        mPrefs = MyApp.mPrefs
        currentSelectedPosition = mPrefs.currentPosition.toInt()
        // Important is necessary setSelected to textview for able marquee autoscroll when text is long than textView size
        setUpObservers()
        setUpListeners()
        bind.tvSongDescription.setSelected(true)
        bind.tvSongArtist.setSelected(true)
        bind.tvSongAlbum.setSelected(true)

    }
    private fun setUpObservers(){
        bind.ivMusicCover.loadImage(coreRes.drawable.placeholder_cover)
        mainViewModel.fetchAllSongFromMain()
        mainViewModel.allSongFromMain.observe(viewLifecycleOwner){songs->
            if(songs.isNotEmpty()){
                songs.forEach {
                    if(!songLists.contains(it))songLists.add(it)
                }
            }
        }
        mainViewModel.currentTrack.observe(viewLifecycleOwner){
           it?.let{currentTrack->
                setUpSongInfo(currentTrack)
           }
        }
        mainViewModel.musicState.observe(viewLifecycleOwner){
            it?.let{musicState->
                setChangeInfoViews(musicState)
                startOrUpdateService()
            }
        }
        mainViewModel.isPlaying.observe(viewLifecycleOwner){statePlay->
            isPlaying=statePlay
            if (statePlay) {
                isPlaying = true
                bind.btnMainPlay.setIconResource(com.barryzeha.core.R.drawable.ic_pause)
            }else{
                bind.btnMainPlay.setIconResource(coreRes.drawable.ic_play)
            }
        }
        mainViewModel.currentSongListPosition.observe(viewLifecycleOwner){currentPosition->
            currentSelectedPosition=currentPosition
        }
        mainViewModel.songById.observe(viewLifecycleOwner){song->
            // Si se agregó una pista nueva desde la lista del segundo fragmento
            // el observador lo agregará a la lista de este fragmento principal
            song?.let{
                if(!songLists.contains(song)) songLists.add(song)
            }
        }

    }
    private fun setUpSongInfo(musicState: MusicState){
        currentMusicState=musicState
        bind.tvSongAlbum.text=musicState.album
        bind.tvSongArtist.text=musicState.artist
        bind.tvSongDescription.text = musicState.title
        bind.ivMusicCover.loadImage(musicState.albumArt)
        bind.tvSongTimeRest.text= createTime(musicState.currentDuration).third
        bind.tvSongTimeCompleted.text = createTime(musicState.duration).third

    }
    private fun setChangeInfoViews(musicState: MusicState){
        currentMusicState = musicState
        bind.mainSeekBar.max=musicState.duration.toInt()
        bind.mainSeekBar.progress=musicState.currentDuration.toInt()
        bind.tvSongTimeRest.text= createTime(musicState.currentDuration).third
        bind.tvSongTimeCompleted.text = createTime(musicState.duration).third

    }
    private fun setUpListeners()=with(bind){
        btnMainPlay.setOnClickListener {
            if (songLists.size > 0) {
                if (!currentMusicState.isPlaying && currentMusicState.duration <= 0) {
                    musicPlayerService?.startPlayer(getSongOfList(currentSelectedPosition).pathLocation.toString())
                    bind.btnMainPlay.setIconResource(coreRes.drawable.ic_pause)

                } else {
                    if (isPlaying) {
                        musicPlayerService?.pauseExoPlayer(); btnMainPlay.setIconResource( com.barryzeha.core.R.drawable.ic_play)
                        mainViewModel.saveStatePlaying(false)
                    } else {
                        musicPlayerService?.playingExoPlayer(); btnMainPlay.setIconResource(com.barryzeha.core.R.drawable.ic_pause)
                        mainViewModel.saveStatePlaying(true)
                    }
                }

            }
        }
        btnMainPrevious.setOnClickListener{
            if (currentSelectedPosition > 0) {
                     musicPlayerService?.startPlayer(getSongOfList(currentSelectedPosition -1).pathLocation.toString())
              }
        }
        btnMainNext.setOnClickListener {
            if(currentSelectedPosition<=songLists.size -1){
                   musicPlayerService?.startPlayer(getSongOfList(currentSelectedPosition +1).pathLocation.toString())
            }
        }
        bind.mainSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            var userSelectPosition=0
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    bind.tvSongTimeRest.text = createTime(progress.toLong()).third
                    musicPlayerService?.setExoPlayerProgress(progress.toLong())
                    userSelectPosition=progress
                    seekBar?.progress=progress

                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
              //isUserSeeking=true
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                //isUserSeeking=false
                bind.mainSeekBar.progress=userSelectPosition

            }

        })
    }
    private fun getSongOfList(position:Int): SongEntity{
        mainViewModel.setCurrentPosition(position)
        val song = songLists[currentSelectedPosition]
        return song
    }
    private fun linkToService(){
        context?.bindService(startOrUpdateService(),this, Context.BIND_AUTO_CREATE)
    }
    private fun startOrUpdateService():Intent{
        val serviceIntent = Intent (context, MusicPlayerService::class.java).apply {
            putExtra("musicState", currentMusicState)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(requireContext(), serviceIntent)
        } else activity?.startService(serviceIntent)

        return serviceIntent
    }
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as MusicPlayerService.MusicPlayerServiceBinder
        musicPlayerService = binder.getService()
        musicPlayerService!!.setSongController(songController)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicPlayerService=null
    }
    override fun onStart() {
        super.onStart()
        linkToService()
    }

    override fun onResume() {
        super.onResume()
        musicPlayerService?.setSongController(songController)
        if(currentMusicState.isPlaying && mPrefs.nextOrPrevFromNotify){
            val song=songLists[mPrefs.currentPosition.toInt()]
            val songMetadata= getSongCover(requireContext(),song.pathLocation)
            val newState = MusicState(
                title = song.pathLocation.toString().substringAfterLast("/","No named")!!,
                artist = songMetadata!!.artist,
                album = songMetadata!!.album,
                albumArt = songMetadata!!.albumArt,)
            setUpSongInfo(newState)
        }
        mPrefs.nextOrPrevFromNotify=false
    }

    override fun onPause() {
        super.onPause()
        musicPlayerService?.unregisterController()
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