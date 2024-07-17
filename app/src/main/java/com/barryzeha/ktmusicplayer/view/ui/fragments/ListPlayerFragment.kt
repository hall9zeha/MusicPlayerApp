package com.barryzeha.ktmusicplayer.view.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.SeekBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.barryzeha.core.common.CLEAR_MODE
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.common.REPEAT_ALL
import com.barryzeha.core.common.REPEAT_ONE
import com.barryzeha.core.common.SHUFFLE
import com.barryzeha.core.common.checkPermissions
import com.barryzeha.core.common.createTime
import com.barryzeha.core.common.fetchFileMetadata
import com.barryzeha.core.common.getRealPathFromURI
import com.barryzeha.core.common.getSongMetadata
import com.barryzeha.core.common.mColorList
import com.barryzeha.core.common.startOrUpdateService
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.core.model.entities.SongMode
import com.barryzeha.core.model.entities.SongState
import com.barryzeha.ktmusicplayer.MyApp
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.databinding.FragmentListPlayerBinding
import com.barryzeha.ktmusicplayer.service.MusicPlayerService
import com.barryzeha.ktmusicplayer.view.ui.adapters.MusicListAdapter
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import com.barryzeha.core.R as coreRes


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

@AndroidEntryPoint
class ListPlayerFragment : BaseFragment(R.layout.fragment_list_player){

    private var param1: String? = null
    private var param2: String? = null
    private var bind:FragmentListPlayerBinding? = null

    private val mainViewModel:MainViewModel by viewModels(ownerProducer = {requireActivity()})
    private var uri:Uri?=null
    private lateinit var adapter:MusicListAdapter
    private lateinit var launcher:ActivityResultLauncher<Intent>
    private lateinit var launcherOpenMultipleDocs:ActivityResultLauncher<Array<String>>
    private lateinit var launcherPermission:ActivityResultLauncher<String>
    private var isPlaying = false
    private var isUserSeeking=false
    private var userSelectPosition=0
    private var serviceConnection:ServiceConnection?=null
    private  var currentSelectedPosition:Int =0

    private var currentMusicState = MusicState()
    private var song:SongEntity?=null
    private var musicPlayerService: MusicPlayerService?=null
    private lateinit var mPrefs:MyPreferences
    private var isFavorite:Boolean=false
    private var isFiltering:Boolean=false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mPrefs = MyApp.mPrefs
        bind = FragmentListPlayerBinding.bind(view)
        activityResultFile()
        activityResultForPermission()
        initCheckPermission()
        setUpAdapter()
        setUpObservers()
        setUpListeners()
    }
    private fun activityResultFile(){
        launcherOpenMultipleDocs= registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){uris->
            uris.forEach { uri ->
                CoroutineScope(Dispatchers.IO).launch {
                    val realPathFromFile = getRealPathFromURI(uri!!, requireContext())
                    val metadata = fetchFileMetadata(requireContext(), realPathFromFile!!)
                    val nameFile = realPathFromFile?.substringAfterLast("/", "No named")
                    withContext(Dispatchers.Main) {
                        mainViewModel.saveNewSong(
                            SongEntity(
                                pathLocation = realPathFromFile,
                                description = metadata.title,
                                artist = metadata.artist!!,
                                album = metadata.album!!,
                                genre = metadata.genre!!,
                                timestamp = Date().time
                            )
                        )
                    }
                }
            }
        }

    }
    private fun activityResultForPermission(){
      launcherPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()){
            if(it){
                initCheckPermission()
            }
        }
    }
    private fun setUpAdapter(){
        adapter = MusicListAdapter(::onItemClick,::onMenuItemClick)
        bind?.rvSongs?.apply {
            setHasFixedSize(true)
            setItemViewCacheSize(10)
            layoutManager = LinearLayoutManager(context)
            adapter = this@ListPlayerFragment.adapter
        }
    }
    private fun setUpObservers(){
        //mainViewModel.fetchAllSong()
        mainViewModel.serviceInstance.observe(viewLifecycleOwner){instance->
            serviceConnection=instance.first
            musicPlayerService=instance.second
        }
        mainViewModel.musicState.observe(viewLifecycleOwner){musicState->
           updateUI(musicState)
        }
        mainViewModel.currentTrack.observe(viewLifecycleOwner){currentTRack->
            updateUIOnceTime(currentTRack)
            bind?.seekbarControl?.tvNumberSong?.text =String.format("#%s/%s", mPrefs.currentPosition + 1, adapter.itemCount)

        }
        mainViewModel.isPlaying.observe(viewLifecycleOwner){statePlay->
            isPlaying=statePlay
            if (statePlay) {
                isPlaying = true
                bind?.bottomPlayerControls?.btnPlay?.setIconResource(coreRes.drawable.ic_pause)
            }else{
                bind?.bottomPlayerControls?.btnPlay?.setIconResource(coreRes.drawable.ic_play)
            }
        }
        mainViewModel.allSongs.observe(viewLifecycleOwner){
            if (it.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    adapter.addAll(it)
                    withContext(Dispatchers.Main) {
                        bind?.seekbarControl?.tvNumberSong?.text =String.format("#%s/%s", mPrefs.currentPosition + 1, it.size)
                    }
                }
            }
        }
        mainViewModel.songById.observe(viewLifecycleOwner){song->
            song?.let{
                //adapter.add(song)
                musicPlayerService?.setNewMediaItem(song)
            }
        }
        mainViewModel.currentSongListPosition.observe(viewLifecycleOwner){positionSelected->
            currentSelectedPosition = positionSelected
            positionSelected?.let{
                adapter.changeBackgroundColorSelectedItem(positionSelected)

            }
        }
        mainViewModel.deletedRow.observe(viewLifecycleOwner){deletedRow->
            if(deletedRow>0) song?.let{song->
                adapter.remove(song)
                musicPlayerService?.removeMediaItem(song)
                bind?.seekbarControl?.tvNumberSong?.text=String.format("#%s/%s",mPrefs.currentPosition + 1, adapter.itemCount)
            }
        }
        mainViewModel.isFavorite.observe(viewLifecycleOwner){isFavorite->
            this.isFavorite = isFavorite
            bind?.btnFavorite?.setIconResource(if(isFavorite)coreRes.drawable.ic_favorite_fill else coreRes.drawable.ic_favorite)
        }
    }
    private fun updateUIOnceTime(musicState:MusicState)=with(bind){
        this?.let {
            currentMusicState = musicState

            bind?.ivCover?.setImageBitmap(
                getSongMetadata(
                    requireContext(),
                    musicState.songPath
                )?.albumArt
            )

            seekbarControl.tvEndTime.text = createTime(musicState.duration).third
            seekbarControl.loadSeekBar.max = musicState.duration.toInt()
            seekbarControl.tvInitTime.text = createTime(musicState.currentDuration).third
            adapter.changeBackgroundColorSelectedItem(mPrefs.currentPosition.toInt())

            activity?.let {
                val songMetadata = getSongMetadata(requireActivity(), musicState.songPath)
                songMetadata?.let {
                    ivCover.setImageBitmap(it.albumArt)
                }

            }
            mainViewModel.checkIfIsFavorite(musicState.idSong)
            updateService()
        }
    }
    private fun updateUI(musicState: MusicState){
        currentMusicState = musicState
        mPrefs.currentDuration = musicState.currentDuration
        //bind?.ivCover?.setImageBitmap(getSongCover(requireContext(), musicState.songPath)?.albumArt)
        bind?.seekbarControl?.loadSeekBar?.max = musicState.duration.toInt()
        bind?.seekbarControl?.tvEndTime?.text = createTime(musicState.duration).third
        bind?.seekbarControl?.tvInitTime?.text = createTime(musicState.currentDuration).third
        bind?.seekbarControl?.loadSeekBar?.progress = musicState.currentDuration.toInt()
        //mainViewModel.saveStatePlaying(musicState.isPlaying)
        updateService()
    }
    private fun setUpListeners()= with(bind){
        this?.let {
            // val chooseFileIntent = Intent(Intent.ACTION_GET_CONTENT).apply{
            //     type = "audio/*"
            // }
            btnAdd.setOnClickListener {
                checkPermissions(
                    requireContext(),
                    listOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO
                    )
                ) { isGranted, permissionsList ->
                    if (isGranted) {
                        val mimeTypes = arrayOf("audio/*")
                        launcherOpenMultipleDocs.launch(mimeTypes)
                    } else {
                        permissionsList.forEach { permission ->
                            if (!permission.second) {
                                launcherPermission.launch(permission.first)

                            }
                        }
                    }
                }
            }
            bottomPlayerControls.btnPlay.setOnClickListener {
                if (adapter.itemCount > 0) {
                    if (!currentMusicState.isPlaying && currentMusicState.duration <= 0) getSongOfAdapter(
                        currentSelectedPosition
                    )?.let { song ->
                        musicPlayerService?.startPlayer(song,currentSelectedPosition)
                    }
                    else {
                        if (isPlaying) {
                            musicPlayerService?.pauseExoPlayer(); bottomPlayerControls.btnPlay.setIconResource(
                                coreRes.drawable.ic_play
                            )
                            mainViewModel.saveStatePlaying(false)

                        } else {
                            musicPlayerService?.playingExoPlayer(); bottomPlayerControls.btnPlay.setIconResource(
                                coreRes.drawable.ic_pause
                            )
                            mainViewModel.saveStatePlaying(true)
                        }
                    }
                }
            }
            bottomPlayerControls.btnPrevious.setOnClickListener {
                if (currentSelectedPosition > 0) {
                    getSongOfAdapter(currentSelectedPosition - 1)?.let { song ->
                        musicPlayerService?.startPlayer(song,currentSelectedPosition -1)

                    }
                }
            }
            bottomPlayerControls.btnNext.setOnClickListener {
                if (currentSelectedPosition < adapter.itemCount - 1) {
                    getSongOfAdapter(currentSelectedPosition + 1)?.let { song ->
                        musicPlayerService?.startPlayer(song,currentSelectedPosition + 1)

                    }
                } else {
                    getSongOfAdapter(0)?.let { song ->
                        musicPlayerService?.startPlayer(song,currentSelectedPosition)

                    }
                }
            }
            seekbarControl.loadSeekBar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        seekbarControl.tvInitTime.text = createTime(progress.toLong()).third
                        musicPlayerService?.setExoPlayerProgress(progress.toLong())
                        userSelectPosition = progress
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isUserSeeking = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    isUserSeeking = false
                    seekbarControl.loadSeekBar.progress = userSelectPosition
                }
            })
            btnRepeat.setOnClickListener {

                when (mPrefs.songMode) {
                    SongMode.RepeatOne.ordinal -> {
                        //  Third: deactivate modes
                        btnRepeat.setIconResource(coreRes.drawable.ic_repeat_all)
                        btnRepeat.backgroundTintList=
                            ColorStateList.valueOf(mColorList(requireContext()).getColor(5,6)
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
                        btnRepeat.backgroundTintList=
                            ContextCompat.getColorStateList(requireContext(),coreRes.color.controls_colors)?.withAlpha(128)
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
                        mPrefs.songMode= SHUFFLE
                        btnRepeat.setIconResource(coreRes.drawable.ic_repeat_all)
                        btnRepeat.backgroundTintList=ColorStateList.valueOf(mColorList(requireContext()).getColor(5,6)
                        )
                    }
                }
            }
            btnFavorite.setOnClickListener {
                val song=getSongOfAdapter(mPrefs.currentPosition.toInt())
                song?.let {
                    if (!isFavorite) {
                        mainViewModel.updateSong(song.copy(favorite = true))
                        //isFavorite=true
                    } else {
                        mainViewModel.updateSong(song.copy(favorite = false))
                        //isFavorite=false
                    }
                }
            }
            btnSearch.setOnClickListener{
               showOrHideSearchbar()
            }
            btnClose?.setOnClickListener {
               showOrHideSearchbar()
            }
            edtSearch?.addTextChangedListener (object: TextWatcher{
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    adapter.filter.filter(s)
                }

                override fun afterTextChanged(s: Editable?) {

                }
            })
        }
    }
    private fun showOrHideSearchbar()=with(bind){
        this?.let{
            if(!isFiltering){

                btnMenu?.visibility=View.GONE
                btnFilter?.visibility=View.GONE
                tvPlayListName?.visibility=View.GONE
                tilSearch?.visibility=View.VISIBLE
                btnClose?.visibility=View.VISIBLE
                btnSearch.backgroundTintList=ContextCompat.getColorStateList(requireContext(),coreRes.color.controls_colors)?.withAlpha(128)
                searchBar?.visibility=View.VISIBLE
                isFiltering=true
                showKeyboard(true)
            }else {
                edtSearch?.setText("")
                btnSearch.backgroundTintList =
                    ColorStateList.valueOf(mColorList(requireContext()).getColor(5, 6))
                isFiltering = false
                showKeyboard(false)
                tilSearch?.visibility=View.GONE
                btnClose?.visibility=View.GONE
                btnMenu?.visibility=View.VISIBLE
                btnFilter?.visibility=View.VISIBLE
                tvPlayListName?.visibility=View.VISIBLE
            }
        }
    }
    private fun showKeyboard(show:Boolean){
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        if(show) {
            bind?.edtSearch?.requestFocus()
            imm!!.showSoftInput(bind?.edtSearch, InputMethodManager.SHOW_IMPLICIT)

        }else{
            imm!!.hideSoftInputFromWindow(bind?.edtSearch?.windowToken,0)
            CoroutineScope(Dispatchers.Main).launch {
                delay(1000)
                adapter.changeBackgroundColorSelectedItem(mPrefs.currentPosition.toInt())
            }
        }
    }
    private fun getSongOfAdapter(position:Int): SongEntity?{
        mainViewModel.setCurrentPosition(position)
        mPrefs.currentPosition = position.toLong()
        val song = adapter.getSongByPosition(position)
        bind?.rvSongs?.scrollToPosition(position)
        return song
    }
    private fun initCheckPermission(){
        val permissionList:MutableList<String> = mutableListOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BLUETOOTH_CONNECT)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            permissionList.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        checkPermissions(requireContext(),permissionList){isGranted,permissions->
            if(isGranted) Log.e("GRANTED", "Completed granted" )
            else{
                permissions.forEach {permission->
                    if(!permission.second){
                        launcherPermission.launch(permission.first)
                    }
                }
            }
        }
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
    private fun onItemClick(position:Int,song: SongEntity){
        if(!isFiltering) {
            musicPlayerService?.startPlayer(song, position)
            mainViewModel.setCurrentPosition(position)
        }else{
            adapter.getPositionByItem(song)?.let {pos->
                musicPlayerService?.startPlayer(song,pos)
                mainViewModel.setCurrentPosition(pos)
                mPrefs.currentPosition=pos.toLong()
            }
        }

    }
    private fun onMenuItemClick(view:View, position: Int, selectedSong: SongEntity) {
        val popupMenu = PopupMenu(activity,view)
        popupMenu.menuInflater.inflate(coreRes.menu.item_menu,popupMenu.menu)
        popupMenu.setOnMenuItemClickListener {
            mainViewModel.deleteSong(selectedSong)
            this.song=selectedSong
            true
        }
        popupMenu.show()
    }
   private fun updateService(){
        serviceConnection?.let{
       startOrUpdateService(requireContext(),MusicPlayerService::class.java,it,currentMusicState)}

    }
    override fun play() {
        super.play()
        bind?.bottomPlayerControls?.btnPlay?.setIconResource(coreRes.drawable.ic_pause)
        musicPlayerService?.playingExoPlayer()
        mainViewModel.saveStatePlaying(true)
    }
    override fun pause() {
        super.pause()
        bind?.bottomPlayerControls?.btnPlay?.setIconResource(coreRes.drawable.ic_play)
        musicPlayerService?.pauseExoPlayer()
        mainViewModel.saveStatePlaying(false)
    }

    override fun next() {
        super.next()
        bind?.bottomPlayerControls?.btnNext?.performClick()
    }
    override fun previous() {
        super.previous()
        bind?.bottomPlayerControls?.btnPrevious?.performClick()
    }
    override fun stop() {
        super.stop()
        activity?.finish()

    }
    override fun musicState(musicState: MusicState?) {
        super.musicState(musicState)
        musicState?.let {
            mainViewModel.setMusicState(musicState)
        }
    }
    override fun currentTrack(musicState: MusicState?) {
        super.currentTrack(musicState)
        musicState?.let{
            if(!musicState.isPlaying){
                if((adapter.itemCount -1)  == currentSelectedPosition && !musicState.latestPlayed) {
                    bind?.bottomPlayerControls?.btnPlay?.setIconResource(coreRes.drawable.ic_play)
                    mainViewModel.saveStatePlaying(false)

                }
                else if(musicState.currentDuration>0 && musicState.latestPlayed){
                    bind?.bottomPlayerControls?.btnPlay?.setIconResource(coreRes.drawable.ic_play)
                    mainViewModel.saveStatePlaying(false)
                    mainViewModel.setCurrentTrack(musicState)
                }
                else {

                }
            }else{
                mainViewModel.saveStatePlaying(true)
                mainViewModel.setCurrentTrack(musicState)
            }
        }
    }
    // El método sobreescrito onConnectedService no se dispara aquí debido a que se ejecuta después del primer fragmento
    // La conexión al servicio la obtenemos a través del view model enviado desde main activity
    override fun onServiceDisconnected() {
        super.onServiceDisconnected()
        musicPlayerService = null
    }

    override fun onResume() {
        super.onResume()
        checkPreferences()
        mainViewModel.checkIfIsFavorite(currentMusicState.idSong)
        currentSelectedPosition = mPrefs.currentPosition.toInt()
        adapter.changeBackgroundColorSelectedItem(mPrefs.currentPosition.toInt())

        bind?.rvSongs?.scrollToPosition(currentSelectedPosition)
        if(mPrefs.controlFromNotify){
            try {
                val song = getSongOfAdapter(mPrefs.currentPosition.toInt())
                song?.let {
                    val songMetadata = getSongMetadata(requireContext(), song.pathLocation)
                    val newState = MusicState(
                        songPath = song.pathLocation.toString(),
                        title = songMetadata!!.title,
                        artist = songMetadata!!.artist,
                        album = songMetadata!!.album
                    )
                    mainViewModel.saveStatePlaying(mPrefs.isPlaying)
                    updateUIOnceTime(newState)
                }

            }catch(ex:Exception){}
        }
        mPrefs.controlFromNotify=false


    }
    override fun onStop() {
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
        super.onStop()
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ListPlayerFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}


