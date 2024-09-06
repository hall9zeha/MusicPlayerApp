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
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.SeekBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.barryzeha.audioeffects.databinding.ActivityMainEqualizerBinding
import com.barryzeha.audioeffects.ui.activities.MainEqualizerActivity
import com.barryzeha.core.common.BY_ALBUM
import com.barryzeha.core.common.BY_ARTIST
import com.barryzeha.core.common.BY_FAVORITE
import com.barryzeha.core.common.BY_GENRE
import com.barryzeha.core.common.CLEAR_MODE
import com.barryzeha.core.common.COLOR_BACKGROUND
import com.barryzeha.core.common.COLOR_TRANSPARENT
import com.barryzeha.core.common.MAIN_FRAGMENT
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.common.REPEAT_ALL
import com.barryzeha.core.common.REPEAT_ONE
import com.barryzeha.core.common.SHUFFLE
import com.barryzeha.core.common.checkPermissions
import com.barryzeha.core.common.createTime
import com.barryzeha.core.common.getSongMetadata
import com.barryzeha.core.common.mColorList
import com.barryzeha.core.common.showDialog
import com.barryzeha.core.common.startOrUpdateService
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.core.model.entities.SongMode
import com.barryzeha.core.model.entities.SongState
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.common.processSongPaths
import com.barryzeha.ktmusicplayer.common.sortPlayList
import com.barryzeha.ktmusicplayer.databinding.FragmentListPlayerBinding
import com.barryzeha.ktmusicplayer.service.MusicPlayerService
import com.barryzeha.ktmusicplayer.view.ui.activities.MainActivity
import com.barryzeha.ktmusicplayer.view.ui.adapters.MusicListAdapter
import com.barryzeha.ktmusicplayer.view.ui.dialog.OrderByDialog
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import com.barryzeha.mfilepicker.ui.views.FilePickerActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.barryzeha.core.R as coreRes


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

@AndroidEntryPoint
class ListPlayerFragment : BaseFragment(R.layout.fragment_list_player){

    @Inject
    lateinit var mPrefs:MyPreferences

    private var param1: String? = null
    private var param2: String? = null
    private var bind:FragmentListPlayerBinding? = null

    private val mainViewModel:MainViewModel by viewModels(ownerProducer = {requireActivity()})

    private lateinit var adapter:MusicListAdapter

    private lateinit var launcherFilePickerActivity:ActivityResultLauncher<Unit>
    private lateinit var launcherPermission:ActivityResultLauncher<String>
    private lateinit var launcherAudioEffectActivity:ActivityResultLauncher<Int>
    private var isPlaying = false
    private var isUserSeeking=false
    private var userSelectPosition=0
    private var serviceConnection:ServiceConnection?=null
    private  var currentSelectedPosition:Int =0

    private var currentMusicState = MusicState()
    private var song:SongEntity?=null
    private var musicPlayerService: MusicPlayerService?=null

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

        bind = FragmentListPlayerBinding.bind(view)
        setUpAdapter()
        setUpObservers()
        setUpPlayListName()
        filePickerActivityResult()
        audioEffectActivityResult()
        activityResultForPermission()
        initCheckPermission()
        setUpListeners()

    }
    private fun audioEffectActivityResult(){
        launcherAudioEffectActivity = registerForActivityResult(MainEqualizerActivity.MainEqualizerContract()){

        }
    }
    private fun filePickerActivityResult(){
        launcherFilePickerActivity = registerForActivityResult(FilePickerActivity.FilePickerContract()) { paths ->
            if(paths.isNotEmpty())bind?.pbLoad?.visibility=View.VISIBLE
            processSongPaths(paths ,{itemsCount->mainViewModel.setItemsCount(itemsCount)},{song->
                CoroutineScope(Dispatchers.Main).launch {
                    bind?.pbLoad?.isIndeterminate=false
                }
                mainViewModel.saveNewSong(song)

            })
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
            post {
                setNumberOfTrack()
            }
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
           setNumberOfTrack()

        }
        mainViewModel.processedRegisterInfo.observe(viewLifecycleOwner){(size, count)->
            bind?.pbLoad?.apply {
                max=size
                progress=count

                setNumberOfTrack(itemCount = count)
            }
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
        mainViewModel.allSongs.observe(viewLifecycleOwner){songList->
            // La actualización del adaptador debe ocurrir en el hilo principal siempre
            // Dará problemas al recrearse la vista cuando rotemos la pantalla si no está en el
            // hilo principal

            //TODO ordenar la lista de media items nuevamente cada vez que hacemos un filtro
            sortPlayList(mPrefs.playListSortOption, songList
            ) { result ->
                // Probando
                musicPlayerService?.populatePlayList(songList)
                // ************
                adapter.addAll(result)
                setNumberOfTrack()
                bind?.pbLoad?.visibility=View.GONE
                bind?.pbLoad?.isIndeterminate=true
            }
        }

        mainViewModel.orderBySelection.observe(viewLifecycleOwner){selectedSort->
            adapter.removeAll()
            // probando
            musicPlayerService?.clearPlayList()
            // ********
            mPrefs.playListSortOption = selectedSort
            mainViewModel.fetchAllSongsBy(selectedSort)
            setUpPlayListName()
            //Todo manejar correctamente la lista de canciones cuando se traiga solamente los favoritos
            // volver a llenar la lista de reproducción u otra mejor opción
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
                adapter.changeBackgroundColorSelectedItem(songId = mPrefs.idSong)

            }
        }
        mainViewModel.deletedRow.observe(viewLifecycleOwner){deletedRow->
            if(deletedRow>0) song?.let{song->
                adapter.remove(song)
                musicPlayerService?.removeMediaItem(song)
                setNumberOfTrack(scrollToPosition = false)
                if(song.id == mPrefs.idSong) mPrefs.clearIdSongInPrefs()
            }
        }
        mainViewModel.deleteAllRows.observe(viewLifecycleOwner){deleteRows->
            if(deleteRows>0){
                adapter.removeAll()
                mPrefs.clearIdSongInPrefs()
                mPrefs.clearCurrentPosition()
                setNumberOfTrack()
                musicPlayerService?.clearPlayList()
            }
        }
        mainViewModel.isFavorite.observe(viewLifecycleOwner){isFavorite->
            this.isFavorite = isFavorite
            bind?.btnFavorite?.setIconResource(if(isFavorite)coreRes.drawable.ic_favorite_fill else coreRes.drawable.ic_favorite)
        }

    }
    private fun setUpPlayListName()=with(bind){
        this?.let{
            when(mPrefs.playListSortOption){
                BY_ALBUM->tvPlayListName.text=getString(coreRes.string.album)
                BY_ARTIST->tvPlayListName.text=getString(coreRes.string.artist)
                BY_GENRE->tvPlayListName.text=getString(coreRes.string.genre)
                BY_FAVORITE->tvPlayListName.text=getString(coreRes.string.favorite)
                else->tvPlayListName.text=getString(coreRes.string.default_title)

            }
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

            adapter.changeBackgroundColorSelectedItem(mPrefs.currentPosition.toInt(), musicState.idSong)

            activity?.let {
                val songMetadata = getSongMetadata(requireActivity(), musicState.songPath)
                songMetadata?.let {
                    ivCover.setImageBitmap(it.albumArt)
                }

            }
            mainViewModel.checkIfIsFavorite(musicState.idSong)

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
        var clicked=false

        val permissionList:List<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,)
        }
        this?.let {
            btnMenu?.setOnClickListener {
                (activity as MainActivity).bind.mainDrawerLayout.openDrawer(GravityCompat.START)
            }
            btnAdd.setOnClickListener {
                checkPermissions(
                    requireContext(),
                    permissionList
                ) { isGranted, permissionsList ->
                    if (isGranted) {
                       launcherFilePickerActivity.launch(Unit)

                    } else {
                        permissionsList.forEach { (permission,granted)->
                            if (!granted) {
                                launcherPermission.launch(permission)

                            }
                        }
                    }
                }
            }
            bottomPlayerControls.btnPlay.setOnClickListener {
                if (adapter.itemCount > 0) {
                    if (!currentMusicState.isPlaying && currentMusicState.duration <= 0) getSongOfAdapter(
                        mPrefs.idSong
                    )?.let { song ->
                        musicPlayerService?.startPlayer(song)
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
                        musicPlayerService?.prevSong()
                }
            }
            bottomPlayerControls.btnNext.setOnClickListener {
                if (currentSelectedPosition < adapter.itemCount - 1) {
                       musicPlayerService?.nextSong()

                    }
                else {
                    getSongOfAdapter(0)?.let { song ->
                        musicPlayerService?.startPlayer(song)

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
            bottomPlayerControls.btnRepeat.setOnClickListener {

                when (mPrefs.songMode) {
                    SongMode.RepeatOne.ordinal -> {
                        //  Third: deactivate modes
                        bottomPlayerControls.btnRepeat.setIconResource(coreRes.drawable.ic_repeat_all)
                        bottomPlayerControls.btnRepeat.backgroundTintList=getColorStateList(COLOR_BACKGROUND,COLOR_TRANSPARENT)
                        bottomPlayerControls.btnShuffle.backgroundTintList=getColorStateList(COLOR_BACKGROUND,COLOR_TRANSPARENT)

                        mPrefs.songMode = CLEAR_MODE
                    }
                    SongMode.RepeatAll.ordinal -> {
                        // Second: repeat one
                        bottomPlayerControls.btnRepeat.setIconResource(coreRes.drawable.ic_repeat_one)
                        bottomPlayerControls.btnShuffle.backgroundTintList=getColorStateList(COLOR_BACKGROUND,COLOR_TRANSPARENT)
                        mPrefs.songMode = REPEAT_ONE

                    }
                    else -> {
                        // First: active repeat All
                        bottomPlayerControls.btnRepeat.backgroundTintList=
                            ContextCompat.getColorStateList(requireContext(),coreRes.color.controls_colors)?.withAlpha(128)
                        bottomPlayerControls.btnShuffle.backgroundTintList=getColorStateList(COLOR_BACKGROUND,COLOR_TRANSPARENT)
                        mPrefs.songMode= REPEAT_ALL
                    }
                }

            }
            bottomPlayerControls.btnShuffle.setOnClickListener {
                when(mPrefs.songMode){
                    SongMode.Shuffle.ordinal->{
                        bottomPlayerControls.btnShuffle.backgroundTintList=getColorStateList(COLOR_BACKGROUND,COLOR_TRANSPARENT)
                        mPrefs.songMode= CLEAR_MODE
                    }
                    else->{
                        bottomPlayerControls.btnShuffle.backgroundTintList=ContextCompat.getColorStateList(requireContext(),coreRes.color.controls_colors)?.withAlpha(128)
                        mPrefs.songMode= SHUFFLE
                        bottomPlayerControls.btnRepeat.setIconResource(coreRes.drawable.ic_repeat_all)
                        bottomPlayerControls.btnRepeat.backgroundTintList=getColorStateList(COLOR_BACKGROUND,COLOR_TRANSPARENT)

                    }
                }
            }
            btnFavorite.setOnClickListener {
                 if (!isFavorite) {mainViewModel.updateFavoriteSong(true, mPrefs.idSong)}
                 else {mainViewModel.updateFavoriteSong(false,mPrefs.idSong) }
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
            btnMultipleSelect?.setOnClickListener{

                if(clicked){
                    adapter.showMultipleSelection(false)
                    btnMultipleSelect.backgroundTintList = getColorStateList(COLOR_BACKGROUND,COLOR_TRANSPARENT)
                    clicked=false
                    visibleOrGoneBottomActions(true)
                    adapter.clearListItemsForDelete()
                }else{
                    adapter.showMultipleSelection(true)
                   btnMultipleSelect.backgroundTintList=ContextCompat.getColorStateList(requireContext(),coreRes.color.controls_colors)?.withAlpha(128)
                    clicked=true
                    visibleOrGoneBottomActions(false)
                }
            }
            btnFilter?.setOnClickListener{
                OrderByDialog().show(parentFragmentManager,OrderByDialog::class.simpleName)
            }
            btnDelete?.setOnClickListener {
                val listForDeleted = adapter.getListItemsForDelete().toList()
                mainViewModel.deleteSong(listForDeleted)
                musicPlayerService?.removeMediaItems(listForDeleted)
                adapter.removeItemsForMultipleSelectedAction()
            }
            btnMainEq?.setOnClickListener{
                launcherAudioEffectActivity.launch(musicPlayerService?.getSessionId()!!)

            }
        }
    }
    private fun getColorStateList(index:Int,defaultIndex:Int):ColorStateList{
        return ColorStateList.valueOf(mColorList(requireContext()).getColor(index,defaultIndex))
    }
    private fun showOrHideSearchbar()=with(bind){
        this?.let{
            if(!isFiltering){
                visibleOrGoneViews(false)
                btnSearch.backgroundTintList=ContextCompat.getColorStateList(requireContext(),coreRes.color.controls_colors)?.withAlpha(128)
                isFiltering=true
                showKeyboard(true)
            }else {
                visibleOrGoneViews(true)
                edtSearch?.setText("")
                btnSearch.backgroundTintList = getColorStateList(COLOR_BACKGROUND,COLOR_TRANSPARENT)
                isFiltering = false
                showKeyboard(false)
            }
        }
    }
    private fun visibleOrGoneViews(isVisible:Boolean)=with(bind){
        this?.let {
            tilSearch?.visibility = if(isVisible)View.GONE else View.VISIBLE
            btnClose?.visibility = if(isVisible)View.GONE else View.VISIBLE

            btnMenu?.visibility = if(isVisible)View.VISIBLE else View.GONE
            btnFilter?.visibility = if(isVisible)View.VISIBLE else View.GONE
            btnMainEq?.visibility = if(isVisible)View.VISIBLE else View.GONE
            tvPlayListName?.visibility = if(isVisible)View.VISIBLE else View.GONE
        }
    }
    private fun visibleOrGoneBottomActions(isVisible:Boolean)=with(bind){
        this?.let{
            btnAdd.visibility = if(isVisible)View.VISIBLE else View.INVISIBLE
            btnFavorite.visibility = if(isVisible)View.VISIBLE else View.INVISIBLE
            btnSearch.visibility = if(isVisible)View.VISIBLE else View.INVISIBLE
            btnMore.visibility = if(isVisible) View.VISIBLE else View.INVISIBLE

            btnDelete?.visibility = if(isVisible)View.GONE else View.VISIBLE
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
                adapter.changeBackgroundColorSelectedItem(mPrefs.currentPosition.toInt(), mPrefs.idSong)
            }
        }
    }

    private fun getSongOfAdapter(idSong: Long):SongEntity?{
        var song:SongEntity?=null
        song = if(idSong>-1){
            adapter.getSongById(idSong)
        }else{
            // En la posición 1 porque primero tendremos un item header en la posición 0
            adapter.getSongByPosition(1)
        }
        song?.let{
            val pos =  adapter.getPositionByItem(it)
            mainViewModel.setCurrentPosition(pos!!.second)
            mPrefs.currentPosition = pos.first.toLong()
            bind?.rvSongs?.scrollToPosition(pos.second)
            return song
        }
        return null
    }
    private fun initCheckPermission(){
        val permissionList:MutableList<String> =  if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            mutableListOf(Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_MEDIA_AUDIO)
        }else{
            mutableListOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.BLUETOOTH_CONNECT)
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

                    bottomPlayerControls.btnRepeat.setIconResource(coreRes.drawable.ic_repeat_one)
                    bottomPlayerControls.btnRepeat.backgroundTintList = ContextCompat.getColorStateList(
                        requireContext(),
                        coreRes.color.controls_colors
                    )?.withAlpha(128)
                }
                SongMode.RepeatAll.ordinal -> {
                    bottomPlayerControls.btnRepeat.backgroundTintList = ContextCompat.getColorStateList(
                        requireContext(),
                        coreRes.color.controls_colors
                    )?.withAlpha(128)
                }
                SongMode.Shuffle.ordinal ->{
                    bottomPlayerControls.btnShuffle.backgroundTintList = ContextCompat.getColorStateList(
                        requireContext(),
                        coreRes.color.controls_colors
                    )?.withAlpha(128)
                }
                else -> {
                    bottomPlayerControls.btnRepeat.setIconResource(coreRes.drawable.ic_repeat_all)
                    bottomPlayerControls.btnRepeat.backgroundTintList = ColorStateList.valueOf(
                        mColorList(requireContext()).getColor(COLOR_BACKGROUND,COLOR_TRANSPARENT)
                    )
                    bottomPlayerControls.btnShuffle.backgroundTintList = ColorStateList.valueOf(
                        mColorList(requireContext()).getColor(COLOR_BACKGROUND,COLOR_TRANSPARENT)
                    )

                }
            }
        }
    }
    private fun onItemClick(position:Int,song: SongEntity){
       adapter.getPositionByItem(song)?.let {pos->
            musicPlayerService?.startPlayer(song)
            mPrefs.idSong = song.id
            mainViewModel.setCurrentPosition(pos.first)
        }
    }
    private fun onMenuItemClick(view:View, position: Int, selectedSong: SongEntity) {
        val popupMenu = PopupMenu(activity,view)
        popupMenu.menuInflater.inflate(coreRes.menu.item_menu,popupMenu.menu)
        popupMenu.setOnMenuItemClickListener {item->
            when(item.itemId){
                coreRes.id.deleteItem->{
                    mainViewModel.deleteSong(selectedSong)
                    this.song=selectedSong

                }
                coreRes.id.deleteAllItem->{
                    showDialog(requireContext(),R.string.delete_all,
                        R.string.delete_all_msg){
                    adapter.removeAll()
                    mainViewModel.deleteAllSongs()
                    }
                }
            }
            true
        }
        popupMenu.show()
    }

    private fun setNumberOfTrack(scrollToPosition:Boolean=true,itemCount:Int=0){
        val itemSong = adapter.getSongById(mPrefs.idSong)

        itemSong?.let{
            val (numberedPos, realPos) = adapter.getPositionByItem(itemSong)
            mPrefs.currentPosition = numberedPos.toLong()
            adapter.changeBackgroundColorSelectedItem(realPos,mPrefs.idSong)
            if(scrollToPosition)bind?.rvSongs?.scrollToPosition(realPos)
        }

        bind?.seekbarControl?.tvNumberSong?.text =
            String.format("#%s/%s", if(mPrefs.currentPosition>-1)mPrefs.currentPosition else 0, (adapter.getSongItemCount() + itemCount))
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
                else if(!musicState.latestPlayed && mPrefs.songMode == SongMode.Shuffle.ordinal){
                    mainViewModel.setCurrentTrack(musicState)
                }
                else{
                    //TODO al usar los controles de next y prev directamente desde el servicio
                    // nos vemos obligados e implementar esta sección, revisar su estabilidad
                    mainViewModel.setCurrentTrack(musicState)
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
        setNumberOfTrack()
        mainViewModel.checkIfIsFavorite(currentMusicState.idSong)
        currentSelectedPosition = mPrefs.currentPosition.toInt()
        adapter.changeBackgroundColorSelectedItem(mPrefs.currentPosition.toInt(),mPrefs.idSong)
        val itemSong = adapter.getSongById(mPrefs.idSong)
        itemSong?.let{
            val position = adapter.getPositionByItem(itemSong)
            bind?.rvSongs?.scrollToPosition(position.second)
        }
        if(mPrefs.controlFromNotify){
            try {
                //val song = getSongOfAdapter(mPrefs.currentPosition.toInt())
                val song = getSongOfAdapter(mPrefs.idSong)
                song?.let {
                    val songMetadata = getSongMetadata(requireContext(), song.pathLocation)
                    val newState = MusicState(
                        songPath = song.pathLocation.toString(),
                        title = songMetadata!!.title,
                        artist = songMetadata!!.artist,
                        album = songMetadata!!.album,
                        duration = songMetadata.duration
                    )
                    mainViewModel.saveStatePlaying(mPrefs.isPlaying)
                    updateUIOnceTime(newState)
                }
            }catch(ex:Exception){}
        }
        mPrefs.controlFromNotify=false
    }
    override fun onStop() {
        mPrefs.currentView = MAIN_FRAGMENT
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


