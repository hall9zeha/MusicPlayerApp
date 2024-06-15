package com.barryzeha.ktmusicplayer.view.ui.fragments

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.common.createTime
import com.barryzeha.core.common.getSongCover
import com.barryzeha.core.common.loadImage
import com.barryzeha.core.common.startOrUpdateService
import com.barryzeha.core.model.SongController
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.core.model.entities.SongState
import com.barryzeha.ktmusicplayer.MyApp
import com.barryzeha.ktmusicplayer.databinding.FragmentMainPlayerBinding
import com.barryzeha.ktmusicplayer.service.MusicPlayerService
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
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
            activity?.finish()

        }

        override fun musicState(musicState: MusicState?) {
            musicState?.let{
                // Había un error de parpadeo especialmente en los textViews con marquee_forever dezplazables.
                // Se debía que al recibir las actualizaciones cada 500ms las volvía a enviar a mi viewModel:
                // mainViewModel.setMusicState(musicState) y recién dentro del observador actualizaba las vistas
                // esto probablemente generaba más retardo en la actualización de las vistas y un trabajo innecesario
                // pero sin el viewModel el estado no sobrevive  a un cambio de orientación, a veces lo hace y a veces no.
                // Al agregar una implementación de ScopedViewModel como base para las clases ViewModels ayudó a solucionar el problema
                // por ahora
            mainViewModel.setMusicState(musicState)
            //setChangeInfoViews(musicState)
            }
        }

        override fun currentTrack(musicState: MusicState?) {
            musicState?.let{
                 if(!musicState.isPlaying){
                    if((songLists.size -1)  == mPrefs.currentPosition.toInt() && !musicState.latestPlayed) {
                        bind.btnMainPlay.setIconResource(coreRes.drawable.ic_play)
                        mainViewModel.saveStatePlaying(false)
                        //mainViewModel.setCurrentPosition(0)

                    }
                    else if(musicState.duration>0 && musicState.latestPlayed){
                        bind.btnMainPlay.setIconResource(coreRes.drawable.ic_play)
                        mainViewModel.saveStatePlaying(false)
                        mainViewModel.setCurrentTrack(musicState)

                    }
                    else {
                        mainViewModel.saveStatePlaying(true)
                        bind.btnMainNext.performClick()
                       mainViewModel.setCurrentTrack(musicState)
                    }
                }else{
                    mainViewModel.saveStatePlaying(true)
                    mainViewModel.setCurrentTrack(musicState)
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
            setChangeInfoViews(it)
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
        currentMusicState = musicState
        val albumArt = getSongCover(requireContext(), musicState.songPath)?.albumArt
        bind.tvSongAlbum.text = musicState.album
        bind.tvSongArtist.text = musicState.artist
        bind.tvSongDescription.text = musicState.title
        bind.ivMusicCover.loadImage(albumArt!!)
        bind.mainSeekBar.max = musicState.duration.toInt()
        bind.tvSongTimeRest.text = createTime(musicState.currentDuration).third
        bind.tvSongTimeCompleted.text = createTime(musicState.duration).third
        Log.e("CURRENT-TRACK", musicState.toString() )
    }
    private fun setChangeInfoViews(musicState: MusicState){
        currentMusicState = musicState
        mPrefs.currentDuration = musicState.currentDuration
        bind.mainSeekBar.max=musicState.duration.toInt()
        bind.mainSeekBar.progress = musicState.currentDuration.toInt()
        bind.tvSongTimeRest.text = createTime(musicState.currentDuration).third
        bind.tvSongTimeCompleted.text = createTime(musicState.duration).third
        updateService()

    }
    private fun setUpListeners()=with(bind){
        btnMainPlay.setOnClickListener {
            if (songLists.size > 0) {
                if (!currentMusicState.isPlaying && currentMusicState.duration <= 0) {
                    musicPlayerService?.startPlayer(getSongOfList(currentSelectedPosition))
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
                     musicPlayerService?.startPlayer(getSongOfList(currentSelectedPosition -1))
              }
        }
        btnMainNext.setOnClickListener {
            if(currentSelectedPosition<songLists.size-1){
                   musicPlayerService?.startPlayer(getSongOfList(currentSelectedPosition +1))
            }else{
                musicPlayerService?.startPlayer(getSongOfList(0))
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
        mPrefs.currentPosition = position.toLong()
        mainViewModel.setCurrentPosition(position)
        val song = songLists[position]
        return song
    }
    private fun updateService(){
        startOrUpdateService(requireContext(),MusicPlayerService::class.java,this,currentMusicState)
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
        startOrUpdateService(requireContext(),MusicPlayerService::class.java,this,currentMusicState)
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
    override fun onStop() {
        super.onStop()
        context?.unbindService(this)
        super.onDestroyView()
        if(currentMusicState.idSong>0) {
            mainViewModel.saveSongState(
                SongState(
                    idSongState = 1,
                    idSong = currentMusicState.idSong,
                    songDuration = currentMusicState.duration,
                    // El constante cambio del valor currentMusicstate.currentDuration(cada 500ms), hace que a veces se guarde y aveces no
                    // de modo que guardamos ese valor con cada actualización de mPrefs.currentDuration y lo extraemos al final, cuando cerramos la app,
                    // por el momento
                    currentPosition = mPrefs.currentDuration
                )
            )
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        _bind=null
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