package com.barryzeha.ktmusicplayer.view.ui.fragments

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.barryzeha.core.common.CLEAR_MODE
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.common.REPEAT_ALL
import com.barryzeha.core.common.REPEAT_ONE
import com.barryzeha.core.common.SHUFFLE
import com.barryzeha.core.common.createTime
import com.barryzeha.core.common.getSongMetadata
import com.barryzeha.core.common.loadImage
import com.barryzeha.core.common.mColorList
import com.barryzeha.core.common.startOrUpdateService
import com.barryzeha.core.model.ServiceSongListener
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.core.model.entities.SongMode
import com.barryzeha.core.model.entities.SongState
import com.barryzeha.ktmusicplayer.MyApp
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.databinding.FragmentMainPlayerBinding
import com.barryzeha.ktmusicplayer.service.MusicPlayerService
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.barryzeha.core.R as coreRes

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

@AndroidEntryPoint
class MainPlayerFragment : BaseFragment(R.layout.fragment_main_player) {

    private var param1: String? = null
    private var param2: String? = null
    private var bind:FragmentMainPlayerBinding ? = null
    private var isPlaying:Boolean = false
    private var currentMusicState = MusicState()
    private var songLists:MutableList<SongEntity> = arrayListOf()
    private var currentSelectedPosition=0
    private lateinit var mPrefs:MyPreferences
    private var isFavorite:Boolean = false
    private var serviceConnection:ServiceConnection?=null


    private val mainViewModel:MainViewModel by viewModels(ownerProducer = {requireActivity()})
    //private val mainViewModel:MainViewModel by activityViewModels()
    private var musicPlayerService: MusicPlayerService?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
            bind=FragmentMainPlayerBinding.bind(view)
            mPrefs = MyApp.mPrefs
            currentSelectedPosition = mPrefs.currentPosition.toInt()
            // Important is necessary setSelected to textview for able marquee autoscroll when text is long than textView size
            setUpObservers()
            setUpListeners()
            bind?.tvSongDescription?.setSelected(true)
            bind?.tvSongArtist?.setSelected(true)
            bind?.tvSongAlbum?.setSelected(true)

    }

    @SuppressLint("ResourceType")
    private fun checkPreferences()=with(bind){
        this?.let {

            when (mPrefs.songMode) {
                SongMode.RepeatOne.ordinal -> {

                    btnRepeat.setIconResource(coreRes.drawable.ic_repeat_one)
                    btnRepeat.backgroundTintList = ContextCompat.getColorStateList(
                        requireContext(),
                        coreRes.color.controls_colors
                    )?.withAlpha(128)
                }
                SongMode.RepeatAll.ordinal -> {
                    btnRepeat.backgroundTintList = ContextCompat.getColorStateList(
                        requireContext(),
                        coreRes.color.controls_colors
                    )?.withAlpha(128)
                }
                SongMode.Shuffle.ordinal ->{
                    btnShuffle.backgroundTintList = ContextCompat.getColorStateList(
                        requireContext(),
                        coreRes.color.controls_colors
                    )?.withAlpha(128)
                }
                else -> {
                    btnRepeat.setIconResource(coreRes.drawable.ic_repeat_all)
                    btnRepeat.backgroundTintList = ColorStateList.valueOf(
                        mColorList(requireContext()).getColor(5, 6)
                    )
                    btnShuffle.backgroundTintList = ColorStateList.valueOf(
                        mColorList(requireContext()).getColor(5, 6)
                    )

                }
            }
        }
    }
    private fun setUpObservers(){
        bind?.ivMusicCover?.loadImage(coreRes.drawable.placeholder_cover)
        mainViewModel.fetchAllSongFromMain()
        mainViewModel.allSongFromMain.observe(viewLifecycleOwner){songs->
            CoroutineScope(Dispatchers.IO).launch {
                if (songs.isNotEmpty()) {
                    songs.forEach {
                        if (!songLists.contains(it)) songLists.add(it)
                    }
                }

            }
        }

        mainViewModel.currentTrack.observe(viewLifecycleOwner){
           it?.let{currentTrack->
               mainViewModel.checkIfIsFavorite(currentTrack.idSong)
                updateUIOnceTime(currentTrack)
           }
        }
        mainViewModel.musicState.observe(viewLifecycleOwner){
            updateUI(it)
        }
        mainViewModel.isPlaying.observe(viewLifecycleOwner){statePlay->
            isPlaying=statePlay
            if (statePlay) {
                isPlaying = true
                bind?.btnMainPlay?.setIconResource(com.barryzeha.core.R.drawable.ic_pause)
            }else{
                bind?.btnMainPlay?.setIconResource(coreRes.drawable.ic_play)
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
        mainViewModel.isFavorite.observe(viewLifecycleOwner){isFavorite->
            this.isFavorite = isFavorite
            bind?.btnFavorite?.setIconResource(if(isFavorite)coreRes.drawable.ic_favorite_fill else coreRes.drawable.ic_favorite)
        }

    }

    override fun play() {
        super.play()
        bind?.btnMainPlay?.setIconResource(coreRes.drawable.ic_pause)
        musicPlayerService?.playingExoPlayer()
        mainViewModel.saveStatePlaying(true)
    }

    override fun pause() {
        super.pause()
        bind?.btnMainPlay?.setIconResource(coreRes.drawable.ic_play)
        musicPlayerService?.pauseExoPlayer()
        mainViewModel.saveStatePlaying(false)
    }

    override fun next() {
        super.next()
        bind?.btnMainNext?.performClick()
    }

    override fun previous() {
        super.previous()
        bind?.btnMainPrevious?.performClick()
    }

    override fun stop() {
        super.stop()
        activity?.finish()
    }

    override fun musicState(musicState: MusicState?) {
        super.musicState(musicState)
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
        super.currentTrack(musicState)
        // TODO corregir el caso 2 y el caso 3 ya no será necesario si usamos la lista agregada al inicio en mediaItems
        // pero aún falta obtener los metadatos de la reproducción en curso si es automática
        musicState?.let{
            if(!musicState.isPlaying){
                if((songLists.size -1)  == mPrefs.currentPosition.toInt() && !musicState.latestPlayed) {
                    bind?.btnMainPlay?.setIconResource(coreRes.drawable.ic_play)
                    mainViewModel.saveStatePlaying(false)
                    //mainViewModel.setCurrentPosition(0)
                    Log.e("CASO 1", "ACTIVO" )
                }
                else if(musicState.duration>0 && musicState.latestPlayed){
                    bind?.btnMainPlay?.setIconResource(coreRes.drawable.ic_play)
                    mainViewModel.saveStatePlaying(false)
                    mainViewModel.setCurrentTrack(musicState)
                    Log.e("CASO 2", "ACTIVO" )
                }
                else {
                    //mainViewModel.saveStatePlaying(true)
                    //bind?.btnMainNext?.performClick()
                    Log.e("CASO 3", "ACTIVO" )
                }
            }else{
                mainViewModel.saveStatePlaying(true)
                mainViewModel.setCurrentTrack(musicState)
                Log.e("CASO 4", "ACTIVO" )
            }

        }

    }
    override fun onServiceConnected(conn: ServiceConnection, service: IBinder?) {
        super.onServiceConnected(conn, service)
        val bind = service as MusicPlayerService.MusicPlayerServiceBinder
        musicPlayerService = bind.getService()
        this.serviceConnection=conn
        startOrUpdateService(requireContext(),MusicPlayerService::class.java,conn,currentMusicState)
    }
    override fun onServiceDisconnected() {
        super.onServiceDisconnected()
        musicPlayerService=null
    }

    private fun updateUIOnceTime(musicState: MusicState)=with(bind){
        this?.let {
            currentMusicState = musicState
            val albumArt = getSongMetadata(requireContext(), musicState.songPath)?.albumArt
            tvSongAlbum.text = musicState.album
            tvSongArtist.text = musicState.artist
            tvSongDescription.text = musicState.title
            ivMusicCover.loadImage(albumArt!!)
            mainSeekBar.max = musicState.duration.toInt()
            tvSongTimeRest.text = createTime(musicState.currentDuration).third
            tvSongTimeCompleted.text = createTime(musicState.duration).third

        }

    }
    private fun updateUI(musicState: MusicState)=with(bind){
        this?.let {
            currentMusicState = musicState
            mPrefs.currentDuration = musicState.currentDuration
            // Quitamos esta propiedad de mainSeekBar.max en actualización constante
            // porque genera un mal efecto en la vista al cargar la pista guardada
            // entre otros pequeños inconvenientes, ahora está en onResumen para actualizarse cuando cambiemos de lista
            // a main
            //mainSeekBar.max = musicState.duration.toInt()

            mainSeekBar.progress = musicState.currentDuration.toInt()
            tvSongTimeRest.text = createTime(musicState.currentDuration).third
            //tvSongTimeCompleted.text = createTime(musicState.duration).third
            updateService()
        }
    }
    @SuppressLint("ResourceType")
    private fun setUpListeners()=with(bind){
        this?.let {
            btnMainPlay.setOnClickListener {
                if (songLists.size > 0) {
                    if (!currentMusicState.isPlaying && currentMusicState.duration <= 0) {
                        musicPlayerService?.startPlayer(getSongOfList(currentSelectedPosition),mPrefs.currentPosition.toInt())
                        btnMainPlay.setIconResource(coreRes.drawable.ic_pause)
                    } else {
                        if (isPlaying) {
                            musicPlayerService?.pauseExoPlayer(); btnMainPlay.setIconResource(coreRes.drawable.ic_play)
                            mainViewModel.saveStatePlaying(false)
                        } else {
                            musicPlayerService?.playingExoPlayer(); btnMainPlay.setIconResource(coreRes.drawable.ic_pause)
                            mainViewModel.saveStatePlaying(true)
                        }
                    }

                }
            }
            btnMainPrevious.setOnClickListener {
                if (currentSelectedPosition > 0) {
                    musicPlayerService?.startPlayer(getSongOfList(currentSelectedPosition - 1),currentSelectedPosition -1)
                }
            }
            btnMainNext.setOnClickListener {
                if (currentSelectedPosition < songLists.size - 1) {
                    musicPlayerService?.startPlayer(getSongOfList(currentSelectedPosition + 1),currentSelectedPosition + 1)
                } else {
                    musicPlayerService?.startPlayer(getSongOfList(0),0)
                }
            }
            mainSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                var userSelectPosition = 0
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        tvSongTimeRest.text = createTime(progress.toLong()).third
                        musicPlayerService?.setExoPlayerProgress(progress.toLong())
                        userSelectPosition = progress
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    //isUserSeeking=true
                }
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    //isUserSeeking=false
                    mainSeekBar.progress = userSelectPosition

                }
            })
            btnRepeat.setOnClickListener {

                    when (mPrefs.songMode) {
                        SongMode.RepeatOne.ordinal -> {
                            //  Third: deactivate modes
                            btnRepeat.setIconResource(coreRes.drawable.ic_repeat_all)
                            btnRepeat.backgroundTintList=ColorStateList.valueOf(mColorList(requireContext()).getColor(5,6)
                            )
                            btnShuffle.backgroundTintList=ColorStateList.valueOf(mColorList(requireContext()).getColor(5,6)
                            )
                            mPrefs.songMode = CLEAR_MODE
                        }
                        SongMode.RepeatAll.ordinal -> {
                            // Second: repeat one
                            btnRepeat.setIconResource(coreRes.drawable.ic_repeat_one)
                            btnShuffle.backgroundTintList=ColorStateList.valueOf(mColorList(requireContext()).getColor(5,6)
                            )
                            mPrefs.songMode = REPEAT_ONE

                        }
                        else -> {
                            // First: active repeat All
                            btnRepeat.backgroundTintList=ContextCompat.getColorStateList(requireContext(),coreRes.color.controls_colors)?.withAlpha(128)
                            btnShuffle.backgroundTintList=ColorStateList.valueOf(mColorList(requireContext()).getColor(5,6)
                            )
                            mPrefs.songMode= REPEAT_ALL
                        }
                    }

            }
            btnShuffle.setOnClickListener {
                when(mPrefs.songMode){
                        SongMode.Shuffle.ordinal->{
                        btnShuffle.backgroundTintList=ColorStateList.valueOf(mColorList(requireContext()).getColor(5,6)
                        )
                        mPrefs.songMode= CLEAR_MODE
                    }
                    else->{
                        btnShuffle.backgroundTintList=ContextCompat.getColorStateList(requireContext(),coreRes.color.controls_colors)?.withAlpha(128)
                        btnRepeat.setIconResource(coreRes.drawable.ic_repeat_all)
                        btnRepeat.backgroundTintList=ColorStateList.valueOf(mColorList(requireContext()).getColor(5,6)
                        )
                        mPrefs.songMode= SHUFFLE
                    }
                }
            }
            btnFavorite.setOnClickListener {
                val song=songLists[mPrefs.currentPosition.toInt()]
                if(!isFavorite){
                    mainViewModel.updateSong(song.copy(favorite = true))
                    //isFavorite=true
                }else{
                    mainViewModel.updateSong(song.copy(favorite=false))
                    //isFavorite=false
                }
            }
        }

    }

    private fun getSongOfList(position:Int): SongEntity{
        mPrefs.currentPosition = position.toLong()
        mainViewModel.setCurrentPosition(position)
        val song = songLists[position]
        return song
    }
    private fun updateService(){
        serviceConnection?.let{
        startOrUpdateService(requireContext(),MusicPlayerService::class.java,it,currentMusicState)}

    }

    override fun onResume() {
        super.onResume()
        checkPreferences()
        mainViewModel.checkIfIsFavorite(currentMusicState.idSong)
        if(mPrefs.nextOrPrevFromNotify){
            try {
                val song = songLists[mPrefs.currentPosition.toInt()]
                val songMetadata = getSongMetadata(requireContext(), song.pathLocation)
                val newState = MusicState(
                    songPath = song.pathLocation.toString(),
                    title = songMetadata!!.title,
                    artist = songMetadata!!.artist,
                    album = songMetadata!!.album,
                )
                updateUIOnceTime(newState)
                mainViewModel.saveStatePlaying(mPrefs.isPlaying)
            }catch(ex:Exception){}

        }
        mPrefs.nextOrPrevFromNotify=false
        bind?.mainSeekBar?.max = currentMusicState.duration.toInt()
    }

    override fun onStop() {
        super.onStop()
        serviceConnection?.let{
       }

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