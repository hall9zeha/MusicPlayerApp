package com.barryzeha.ktmusicplayer.view.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.viewModels
import com.barryzeha.audioeffects.ui.activities.MainEqualizerActivity
import com.barryzeha.core.common.CLEAR_MODE
import com.barryzeha.core.common.COLOR_BACKGROUND
import com.barryzeha.core.common.COLOR_TRANSPARENT
import com.barryzeha.core.common.MAIN_FRAGMENT
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.common.REPEAT_ALL
import com.barryzeha.core.common.REPEAT_ONE
import com.barryzeha.core.common.SHUFFLE
import com.barryzeha.core.common.createTime
import com.barryzeha.core.common.getSongMetadata
import com.barryzeha.core.common.loadImage
import com.barryzeha.core.common.mColorList
import com.barryzeha.core.common.startOrUpdateService
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.core.model.entities.SongMode
import com.barryzeha.core.model.entities.SongState
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.databinding.FragmentMainPlayerBinding
import com.barryzeha.ktmusicplayer.service.MusicPlayerService
import com.barryzeha.ktmusicplayer.view.ui.activities.MainActivity
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import com.barryzeha.library.components.DiscCoverView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.barryzeha.core.R as coreRes

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

@AndroidEntryPoint
class MainPlayerFragment : BaseFragment(R.layout.fragment_main_player) {

    @Inject
    lateinit var defaultPrefs:SharedPreferences

    @Inject
    lateinit var mPrefs:MyPreferences

    private var param1: String? = null
    private var param2: String? = null
    private var bind:FragmentMainPlayerBinding ? = null
    private var isPlaying:Boolean = false
    private var currentMusicState = MusicState()

    private var currentSelectedPosition=0

    private val launcherAudioEffectActivity: ActivityResultLauncher<Int> = registerForActivityResult(MainEqualizerActivity.MainEqualizerContract()){}

    private var isFavorite:Boolean = false
    private var serviceConnection:ServiceConnection?=null

    private val mainViewModel:MainViewModel by viewModels(ownerProducer = {requireActivity()})
    //private val mainViewModel:MainViewModel by activityViewModels()
    private var musicPlayerService: MusicPlayerService?=null
    private var listener: OnFragmentReadyListener? = null

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
            currentSelectedPosition = mPrefs.currentIndexSong.toInt()
            // Important is necessary setSelected to textview for able marquee autoscroll when text is long than textView size
            setUpObservers()
            setUpListeners()
            setUpScrollOnTextViews()
            listener?.onFragmentReady()

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnFragmentReadyListener
    }
    private fun setUpScrollOnTextViews()=with(bind){
        this?.let{
            tvSongArtist.setSelected(true)
            tvSongAlbum.setSelected(true)
            tvSongDescription.setSelected(true);
        }
    }
    private fun discCoverViewIsEnable():Boolean{
        return defaultPrefs.getBoolean("coverStyle",false)
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
                        mColorList(requireContext()).getColor(COLOR_BACKGROUND,COLOR_TRANSPARENT)
                    )
                    btnShuffle.backgroundTintList = ColorStateList.valueOf(
                        mColorList(requireContext()).getColor(COLOR_BACKGROUND,COLOR_TRANSPARENT)
                    )

                }
            }
        }
    }
    private fun setUpObservers(){
        //bind?.ivMusicCover?.loadImage(coreRes.drawable.placeholder_cover)
        (bind?.ivDiscMusicCover as ImageView)?.loadImage(coreRes.drawable.placeholder_cover)
        (bind?.ivMusicCover as ImageView)?.loadImage(coreRes.drawable.placeholder_cover)
        mainViewModel.fetchAllSongFromMain()
        mainViewModel.serviceInstance.observe(viewLifecycleOwner){instance->
            serviceConnection= instance.first
            musicPlayerService= instance.second

        }
        mainViewModel.allSongFromMain.observe(viewLifecycleOwner){songs->
            CoroutineScope(Dispatchers.IO).launch {
                /*if (songs.isNotEmpty()) {
                    songs.forEach {
                        if (!songLists.contains(it)) songLists.add(it)
                    }
                }*/
                withContext(Dispatchers.Main) {
                    setNumberOfTrack(mPrefs.idSong)
                }
            }
        }

        mainViewModel.currentTrack.observe(viewLifecycleOwner){
           it?.let{currentTrack->
               mainViewModel.checkIfIsFavorite(currentTrack.idSong)
               updateUIOnceTime(currentTrack)
               setNumberOfTrack(currentTrack.idSong)

           }
        }
        mainViewModel.musicState.observe(viewLifecycleOwner){
            updateUI(it)
        }
        mainViewModel.isPlaying.observe(viewLifecycleOwner){statePlay->
            checkIfDiscCoverViewIsRotating(statePlay)
            isPlaying=statePlay
            if (statePlay) {
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
                //if(!songLists.contains(song)) songLists.add(song)
            }
        }
        mainViewModel.isFavorite.observe(viewLifecycleOwner){isFavorite->
            this.isFavorite = isFavorite
            bind?.btnFavorite?.setIconResource(if(isFavorite)coreRes.drawable.ic_favorite_fill else coreRes.drawable.ic_favorite)
        }
        mainViewModel.deletedRow.observe(viewLifecycleOwner){deleteRow->
            if(deleteRow>0){
                setNumberOfTrack(currentMusicState.idSong)
            }
        }
        mainViewModel.deleteAllRows.observe(viewLifecycleOwner){deleteAllRows->
            if(deleteAllRows>0){

                setNumberOfTrack(currentMusicState.idSong)
            }
        }
    }

    override fun play() {
        super.play()
        bind?.btnMainPlay?.setIconResource(coreRes.drawable.ic_pause)
        musicPlayerService?.resumePlayer()
        mainViewModel.saveStatePlaying(true)
    }

    override fun pause() {
        super.pause()
        bind?.btnMainPlay?.setIconResource(coreRes.drawable.ic_play)
        musicPlayerService?.pausePlayer()
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
                if((musicPlayerService?.getSongsList()!!.size -1)  == mPrefs.currentIndexSong.toInt() && !musicState.latestPlayed) {
                    bind?.btnMainPlay?.setIconResource(coreRes.drawable.ic_play)
                    mainViewModel.saveStatePlaying(false)
                    //mainViewModel.setCurrentPosition(0)
                    Log.e("CASO 1", "ACTIVO" )
                }

                else if(musicState.duration>0 && musicState.latestPlayed){
                    bind?.btnMainPlay?.setIconResource(coreRes.drawable.ic_play)
                    mainViewModel.saveStatePlaying(false)
                    mainViewModel.setCurrentTrack(musicState)
                    // Obtenemos el número de la pista y el tamaño de la lista la primera vez desde el servicio
                    val (songNumber, listSize) = musicPlayerService?.getNumberOfTrack()!!
                    bind?.tvNumberSong?.text = String.format( "#%s/%s",songNumber,listSize)

                }
                else if(!musicState.latestPlayed && (mPrefs.songMode == SongMode.Shuffle.ordinal)){
                     mainViewModel.setCurrentTrack(musicState)
                 }
                else {
                    //TODO al usar los controles de next y prev directamente desde el servicio
                    // nos vemos obligados e implementar esta sección, revisar su estabilidad
                    mainViewModel.setCurrentTrack(musicState)
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
            val albumArt = getSongMetadata(requireContext(), musicState.songPath)?.albumArt
            tvSongAlbum.text = musicState.album
            tvSongArtist.text = musicState.artist
            tvSongDescription.text = musicState.title
            //ivMusicCover.loadImage(albumArt!!,musicState.nextOrPrev)

            (ivDiscMusicCover as ImageView).loadImage(albumArt!!,musicState.nextOrPrev)
            (ivMusicCover as ImageView).loadImage(albumArt!!,musicState.nextOrPrev)

            mainSeekBar.max = musicState.duration.toInt()
            tvSongTimeRest.text = createTime(musicState.currentDuration).third
            tvSongTimeCompleted.text = createTime(musicState.duration).third
            currentMusicState = musicState
            mainViewModel.saveStatePlaying(mPrefs.isPlaying)
            updateService()
            if(discCoverViewIsEnable()) {
                // Detenemos la animación para cada cambio de canción para que la imágen
                // aparezca correctamente y no rotada
                (bind?.ivDiscMusicCover as DiscCoverView).end()
                CoroutineScope(Dispatchers.IO).launch{
                    delay(500)
                    withContext(Dispatchers.Main) {
                        // Si se estaba reproduciendo iniciamos animacion nuevamente al cambiar de canción
                        if (mPrefs.isPlaying) (bind?.ivDiscMusicCover as DiscCoverView).start()
                    }
                }
            }

        }

    }
    private fun checkIfDiscCoverViewIsRotating(isPlaying:Boolean){
        if(discCoverViewIsEnable()) {
                    if (isPlaying) (bind?.ivDiscMusicCover as DiscCoverView).resume()
                    else (bind?.ivDiscMusicCover as DiscCoverView).pause()
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
        }
    }
    private fun setNumberOfTrack(songId:Long? = null){
        /*bind?.tvNumberSong?.text =
        String.format("#%s/%s", if(mPrefs.currentPosition>-1)position else 0, songLists.count())*/
        CoroutineScope(Dispatchers.IO).launch {
        if(songId != null && songId >-1) {
                val song = ListPlayerFragment.listAdapter?.getSongById(songId.toLong())
            song?.let {
               val (itemNumOnList, _) = ListPlayerFragment.listAdapter?.getPositionByItem(song as SongEntity)
                    ?: Pair(0, 0)
                withContext(Dispatchers.Main) {
                    bind?.tvNumberSong?.text = String.format(
                        "#%s/%s",
                        if (mPrefs.currentIndexSong > -1) itemNumOnList else 0,
                        musicPlayerService?.getSongsList()?.count()
                    )
                }
            }
        }
        }
    }
    private fun checkCoverViewStyle()=with(bind){
        this?.let {
            if (discCoverViewIsEnable()) {
                ivMusicCover.visibility = View.GONE
                ivDiscMusicCover.visibility = View.VISIBLE
                checkIfDiscCoverViewIsRotating(mPrefs.isPlaying)
            } else {
                ivDiscMusicCover.visibility = View.GONE
                ivMusicCover.visibility = View.VISIBLE
            }
        }
    }

    @SuppressLint("ResourceType")
    private fun setUpListeners()=with(bind){
        this?.let {
            bind?.tvSongDescription?.setSelected(true)
            bind?.tvSongArtist?.setSelected(true)
            bind?.tvSongAlbum?.setSelected(true)
            checkCoverViewStyle()
            btnMainMenu?.setOnClickListener {

                (activity as MainActivity).bind.mainDrawerLayout.openDrawer(GravityCompat.START)
            }
            btnMainPlay.setOnClickListener {
                if (musicPlayerService?.getSongsList()?.size!! > 0) {
                    if (!currentMusicState.isPlaying && currentMusicState.duration <= 0) {
                        getSongOfList(currentSelectedPosition)?.let{song->
                            musicPlayerService?.startPlayer(song)
                            btnMainPlay.setIconResource(coreRes.drawable.ic_pause)
                        }

                    } else {
                        if (isPlaying) {
                            musicPlayerService?.pausePlayer(); btnMainPlay.setIconResource(coreRes.drawable.ic_play)
                            mainViewModel.saveStatePlaying(false)
                            if(discCoverViewIsEnable()) (bind?.ivDiscMusicCover as DiscCoverView).pause()

                        } else {
                            musicPlayerService?.resumePlayer(); btnMainPlay.setIconResource(coreRes.drawable.ic_pause)
                            mainViewModel.saveStatePlaying(true)
                            if(discCoverViewIsEnable()) (bind?.ivDiscMusicCover as DiscCoverView).resume()
                        }
                    }

                }
            }
            btnMainPrevious.setOnClickListener {
                checkCoverViewStyle()
                if (currentSelectedPosition > 0) {
                       musicPlayerService?.prevSong()

                }
            }
            btnMainNext.setOnClickListener {
                checkCoverViewStyle()
                if (currentSelectedPosition < ListPlayerFragment.listAdapter?.itemCount!! - 1) {
                      musicPlayerService?.nextSong()
                } else {
                    getSongOfList(0)?.let{song->
                        musicPlayerService?.startPlayer(song)
                    }
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
                        userSelectPosition = progress
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    musicPlayerService?.setPlayerProgress(seekBar?.progress?.toLong()!!)
                    mainSeekBar.progress = userSelectPosition

                }
            })
            btnRepeat.setOnClickListener {

                    when (mPrefs.songMode) {
                        SongMode.RepeatOne.ordinal -> {
                            //  Third: deactivate modes
                            btnRepeat.setIconResource(coreRes.drawable.ic_repeat_all)
                            btnRepeat.backgroundTintList=ColorStateList.valueOf(mColorList(requireContext()).getColor(COLOR_BACKGROUND,COLOR_TRANSPARENT)
                            )
                            btnShuffle.backgroundTintList=ColorStateList.valueOf(mColorList(requireContext()).getColor(COLOR_BACKGROUND,COLOR_TRANSPARENT)
                            )
                            mPrefs.songMode = CLEAR_MODE
                            // For bass flavor
                            musicPlayerService?.sortList()
                        }
                        SongMode.RepeatAll.ordinal -> {
                            // Second: repeat one
                            btnRepeat.setIconResource(coreRes.drawable.ic_repeat_one)
                            btnShuffle.backgroundTintList=ColorStateList.valueOf(mColorList(requireContext()).getColor(COLOR_BACKGROUND,COLOR_TRANSPARENT)
                            )
                            mPrefs.songMode = REPEAT_ONE
                            // For bass flavor
                            musicPlayerService?.sortList()
                        }
                        else -> {
                            // First: active repeat All
                            btnRepeat.backgroundTintList=ContextCompat.getColorStateList(requireContext(),coreRes.color.controls_colors)?.withAlpha(128)
                            btnShuffle.backgroundTintList=ColorStateList.valueOf(mColorList(requireContext()).getColor(COLOR_BACKGROUND,COLOR_TRANSPARENT)
                            )
                            mPrefs.songMode= REPEAT_ALL
                            // For bass flavor
                            musicPlayerService?.sortList()
                        }
                    }

            }
            btnShuffle.setOnClickListener {
                when(mPrefs.songMode){
                        SongMode.Shuffle.ordinal->{
                        btnShuffle.backgroundTintList=ColorStateList.valueOf(mColorList(requireContext()).getColor(COLOR_BACKGROUND,COLOR_TRANSPARENT)
                        )
                            mPrefs.songMode= CLEAR_MODE
                            // For bass flavor
                            musicPlayerService?.sortList()

                    }
                    else->{
                        btnShuffle.backgroundTintList=ContextCompat.getColorStateList(requireContext(),coreRes.color.controls_colors)?.withAlpha(128)
                        btnRepeat.setIconResource(coreRes.drawable.ic_repeat_all)
                        btnRepeat.backgroundTintList=ColorStateList.valueOf(mColorList(requireContext()).getColor(COLOR_BACKGROUND,COLOR_TRANSPARENT)
                        )
                        mPrefs.songMode= SHUFFLE
                        // For bass flavor
                        musicPlayerService?.shuffleList()

                    }
                }
            }
            btnFavorite.setOnClickListener {

                if(!isFavorite){
                    mainViewModel.updateFavoriteSong(true,mPrefs.idSong)
                    //isFavorite=true
                }else{
                    mainViewModel.updateFavoriteSong(false,mPrefs.idSong)
                    //isFavorite=false
                }
            }
            btnMainEq?.setOnClickListener{
                launcherAudioEffectActivity.launch(musicPlayerService?.getSessionOrChannelId()!!)
            }
        }

    }

    private fun getSongOfList(position:Int): SongEntity?{
        if(mPrefs.currentIndexSong>-1) {
            mPrefs.currentIndexSong = position.toLong()
            mainViewModel.setCurrentPosition(position)
            musicPlayerService?.getSongsList()?.let{songsList->
               return songsList[position]
           }?:run{
                return null
            }

        }else{
            //mPrefs.currentPosition = 1
            mainViewModel.setCurrentPosition(0)
            musicPlayerService?.getSongsList()?.let{songsList->
                return songsList[0]
            }?:run { return null }

       }

    }
    private fun updateService(){
        serviceConnection?.let{
        startOrUpdateService(requireContext(),MusicPlayerService::class.java,it,currentMusicState)}

    }
    override fun onResume() {
        super.onResume()
        setNumberOfTrack(mPrefs.idSong)
        checkCoverViewStyle()
        checkPreferences()
        mainViewModel.checkIfIsFavorite(currentMusicState.idSong)
        if(mPrefs.nextOrPrevFromNotify){
            try {

                val song = musicPlayerService?.getSongsList()?.find { mPrefs.idSong == it.id }
                song?.let {

                    val songMetadata = getSongMetadata(requireContext(), song.pathLocation)
                    val newState = MusicState(
                        songPath = song.pathLocation.toString(),
                        title = songMetadata!!.title,
                        artist = songMetadata!!.artist,
                        album = songMetadata!!.album,
                        duration = songMetadata.duration
                    )
                    updateUIOnceTime(newState)
                    mainViewModel.saveStatePlaying(mPrefs.isPlaying)
                }
            }catch(ex:Exception){}

        }
        mPrefs.nextOrPrevFromNotify=false
        bind?.mainSeekBar?.max = currentMusicState.duration.toInt()
    }

    override fun onStop() {
        super.onStop()
        mPrefs.currentView = MAIN_FRAGMENT
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
    interface OnFragmentReadyListener{
        fun onFragmentReady()
    }
}