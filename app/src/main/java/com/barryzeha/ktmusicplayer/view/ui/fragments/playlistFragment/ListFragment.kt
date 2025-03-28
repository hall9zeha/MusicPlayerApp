package com.barryzeha.ktmusicplayer.view.ui.fragments.playlistFragment

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.barryzeha.audioeffects.ui.activities.MainEqualizerActivity
import com.barryzeha.core.common.MAIN_FRAGMENT
import com.barryzeha.core.common.checkPermissions
import com.barryzeha.core.common.getBitmap
import com.barryzeha.core.common.getThemeResValue
import com.barryzeha.core.common.keepScreenOn
import com.barryzeha.core.common.loadImage
import com.barryzeha.core.common.showOrHideKeyboard
import com.barryzeha.core.common.startOrUpdateService
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.PlaylistEntity
import com.barryzeha.core.model.entities.PlaylistWithSongsCrossRef
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.common.LIST_FRAGMENT
import com.barryzeha.ktmusicplayer.common.ON_MENU_ITEM
import com.barryzeha.ktmusicplayer.common.ON_MINI_PLAYER_MENU
import com.barryzeha.ktmusicplayer.common.changeBackgroundColor
import com.barryzeha.ktmusicplayer.common.createNewPlayListDialog
import com.barryzeha.ktmusicplayer.common.getPlayListName
import com.barryzeha.ktmusicplayer.common.onMenuActionAddPopup
import com.barryzeha.ktmusicplayer.common.onMenuItemPopup
import com.barryzeha.ktmusicplayer.common.processSongPaths
import com.barryzeha.ktmusicplayer.common.sortPlayList
import com.barryzeha.ktmusicplayer.databinding.FragmentPlaylistBinding
import com.barryzeha.ktmusicplayer.service.MusicPlayerService
import com.barryzeha.ktmusicplayer.view.ui.activities.MainActivity
import com.barryzeha.ktmusicplayer.view.ui.adapters.MusicListAdapter
import com.barryzeha.ktmusicplayer.view.ui.adapters.PlayListsAdapter
import com.barryzeha.ktmusicplayer.view.ui.dialog.OrderByDialog
import com.barryzeha.ktmusicplayer.view.ui.dialog.PlaylistDialogFragment
import com.barryzeha.ktmusicplayer.view.ui.dialog.SongInfoDialogFragment
import com.barryzeha.ktmusicplayer.view.ui.fragments.BaseFragment
import com.barryzeha.ktmusicplayer.view.ui.fragments.MainPlayerFragment
import com.barryzeha.ktmusicplayer.view.ui.fragments.albumDetail.AlbumDetailFragmentArgs
import com.barryzeha.ktmusicplayer.view.ui.fragments.playerControls.PlaybackControlsFragment
import com.barryzeha.mfilepicker.ui.views.FilePickerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 13/2/25.
 * Copyright (c)  All rights reserved.
 **/
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ListFragment : BaseFragment(R.layout.fragment_playlist) {
    private var bind: FragmentPlaylistBinding? = null
    private val binding: FragmentPlaylistBinding get() = bind!!
    var musicListAdapter: MusicListAdapter? = null

    private var param1: String? = null
    private var param2: String? = null
    private var playbackControlsFragment: PlaybackControlsFragment? = null

    private lateinit var launcherFilePickerActivity: ActivityResultLauncher<Unit>
    private lateinit var launcherPermission: ActivityResultLauncher<String>
    private lateinit var launcherAudioEffectActivity: ActivityResultLauncher<Int>

    private var currentMusicState = MusicState()
    private var song: SongEntity? = null
    private var isFavorite: Boolean = false

    private var onFinisLoadSongsListener: OnFinishedLoadSongs? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        handleArgumentsSending()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.setTheme(requireContext().getThemeResValue())
        super.onViewCreated(view, savedInstanceState)
        bind = FragmentPlaylistBinding.bind(view)

        onFinisLoadSongsListener = MainPlayerFragment.instance
        instance = this
        navController=findNavController()
        setupSongsAdapter()
        setUpObservers()
        setUpPlayListName()
        filePickerActivityResult()
        audioEffectActivityResult()
        activityResultForPermission()
        setUpListeners()
        mainViewModel.sharedFragmentInstance(this)
    }

    private fun handleArgumentsSending() {
        arguments?.let { arg ->
            val playlistId = arg.getInt(ARG_PARAM1)
            getPlaylist(playlistId)
        }
    }
    private fun audioEffectActivityResult() {
        launcherAudioEffectActivity =
            registerForActivityResult(MainEqualizerActivity.MainEqualizerContract()) { }
    }
    private fun filePickerActivityResult() {
        launcherFilePickerActivity =
            registerForActivityResult(FilePickerActivity.FilePickerContract()) { paths ->
                if (paths.isNotEmpty()) {
                    bind?.pbLoad?.visibility = View.VISIBLE
                    //Mantenemos la pantalla encendida para evitar interrupciones mientras se procesa
                    keepScreenOn(requireActivity(), true)
                    //**********************************
                    processSongPaths(paths,
                        { itemsCount -> mainViewModel.setItemsCount(itemsCount) },
                        { song ->
                            CoroutineScope(Dispatchers.Main).launch {
                                bind?.pbLoad?.isIndeterminate = false
                            }
                            mainViewModel.saveNewSong(song)
                        })
                }
            }
    }
    private fun activityResultForPermission() {
        launcherPermission =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) {
                    //TODO launch file picker activity if read permissions were given
                }
            }
    }
    private fun setupSongsAdapter() {
        musicListAdapter = MusicListAdapter(::onItemClick, ::onMenuItemClick)
        bind?.rvSongs?.apply {
            setHasFixedSize(true)
            setItemViewCacheSize(10)
            layoutManager = LinearLayoutManager(context)
            adapter = musicListAdapter
            post {
                setNumberOfTrack()
            }
        }
    }
    private fun getPlaylist(playlistId: Int) {
        mPrefs.playlistId = playlistId
        musicPlayerService?.clearPlayList(false)
        mainViewModel.fetchPlaylistWithSongsBy(playlistId, mPrefs.playListSortOption)

    }

    private fun setUpObservers() {
        mainViewModel.controlsFragmentInstance.observe(viewLifecycleOwner){instance->
            playbackControlsFragment = instance
        }
        mainViewModel.musicState.observe(viewLifecycleOwner) { musicState ->
            updateUI(musicState)
        }
        mainViewModel.currentTrack.observe(viewLifecycleOwner) { currentTRack ->
            updateUIOnceTime(currentTRack)
            playbackControlsFragment?.setNumberOfTracks()
            setNumberOfTrack(false)
        }
        mainViewModel.progressRegisterSaved.observe(viewLifecycleOwner) { (totalRegisters, count) ->
            bind?.pbLoad?.apply {
                max = totalRegisters
                progress = count
                setNumberOfTrack(itemCount = count)
            }
        }
        mainViewModel.isPlaying.observe(viewLifecycleOwner) { statePlay ->
            playbackControlsFragment?.updatePlayerStateUI(statePlay)
        }
        mainViewModel.allSongs.observe(viewLifecycleOwner) { songList ->
            // We remove the permanently on screen state
            keepScreenOn(requireActivity(), false)

            // The adapter update must occur on the main thread. Otherwise, it will cause problems recreating the view when we rotate the screen.
            sortPlayList(
                mPrefs.playListSortOption, songList
            ) { result ->
                // We fill the list of mediaitems when we select a filter
                if (!mPrefs.firstExecution) musicPlayerService?.populatePlayList(songList)
                // ************
                musicListAdapter?.addAll(result)
                bind?.rvSongs?.post {
                    setNumberOfTrack()
                    //TODO improve getting track number/total tracks in main fragment
                    MainPlayerFragment.instance?.setNumberOfTrack(mPrefs.idSong)
                    onFinisLoadSongsListener?.onFinishLoad()
                    mainViewModel.sharedFragmentInstance(this@ListFragment)
                }
                bind?.pbLoad?.visibility = View.GONE
                bind?.pbLoad?.isIndeterminate = true
                mPrefs.firstExecution = false
            }
        }
        mainViewModel.orderBySelection.observe(viewLifecycleOwner) { selectedSort ->
            musicListAdapter?.removeAll()
            // Delete the media items list to load the new list, as it will have a different order.
            musicPlayerService?.clearPlayList(isSort = true)
            // ********
            mPrefs.playListSortOption = selectedSort
            mainViewModel.fetchPlaylistWithSongsBy(mPrefs.playlistId, selectedSort)
            setUpPlayListName()
        }
        mainViewModel.songById.observe(viewLifecycleOwner) { song ->
            song?.let {
                musicPlayerService?.setNewMediaItem(song)
            }
        }
        mainViewModel.currentSongListPosition.observe(viewLifecycleOwner) { positionSelected ->
            musicPlayerService?.setCurrentSongPosition(positionSelected)
            positionSelected?.let {
                musicListAdapter?.changeBackgroundColorSelectedItem(songId = mPrefs.idSong)
            }
        }
        mainViewModel.deletedRow.observe(viewLifecycleOwner) { deletedRow ->
            if (deletedRow > 0) song?.let { song ->
                musicListAdapter?.remove(song)
                if (song.id == mPrefs.idSong) {
                    mainViewModel.removeSongState(song.id)
                    mPrefs.clearIdSongInPrefs()
                    //If we delete the track that is playing, we move on to the next one
                    musicPlayerService?.nextSong()
                }
                musicPlayerService?.removeMediaItem(song)
                setNumberOfTrack(scrollToPosition = false)
            }
        }
        mainViewModel.deleteAllRows.observe(viewLifecycleOwner) { deleteRows ->
            if (deleteRows > 0) {
                musicListAdapter?.removeAll()
                mPrefs.clearIdSongInPrefs()
                mPrefs.clearCurrentPosition()
                musicPlayerService?.clearPlayList(isSort = false)
                currentMusicState = MusicState()
                setNumberOfTrack()
            }
        }
        mainViewModel.isFavorite.observe(viewLifecycleOwner) { isFavorite ->
            this.isFavorite = isFavorite
            bind?.btnFavorite?.setIconResource(if (isFavorite) com.barryzeha.core.R.drawable.ic_favorite_fill else com.barryzeha.core.R.drawable.ic_favorite)
            //For change state of the favorite action in media notify
            musicPlayerService?.checkIfSongIsFavorite(mPrefs.idSong)
        }
        mainViewModel.isSongTagEdited.observe(viewLifecycleOwner) { song ->
            song?.let {
                musicListAdapter?.update(song)
            }
        }
        mainViewModel.playlistName.observe(viewLifecycleOwner){name->
            bind?.tvPlayListName?.text = name
        }
    }
    private fun setUpPlayListName() = with(bind) {
        this?.let {
            getPlayListName(mPrefs) { headerTextRes ->
                mainViewModel.setPlaylistName(getString(headerTextRes))
                tvPlayListName.text = getString(headerTextRes)
            }
        }
    }
    private fun updateUIOnceTime(musicState: MusicState) = with(bind) {
        this?.let {
            currentMusicState = musicState
            val albumArt = getBitmap(requireContext(), musicState.songPath)
            ivCover.loadImage(albumArt!!)
            playbackControlsFragment?.updateUIOnceTime(musicState)
            musicListAdapter?.changeBackgroundColorSelectedItem(musicState.idSong)
            mainViewModel.checkIfIsFavorite(musicState.idSong)
            mainViewModel.saveStatePlaying(musicPlayerService?.playingState()!!)
            mainViewModel.setCurrentPosition(mPrefs.currentIndexSong.toInt())
            //Move to the beginning of the list when all tracks have finished playing
            if (musicPlayerService?.getCurrentSongPosition()!! >= musicPlayerService?.playListSize()!! - 1) rvSongs.scrollToPosition(
                0
            )
        }
    }
    private fun updateUI(musicState: MusicState) {
        currentMusicState = musicState
        mPrefs.currentPosition = musicState.currentDuration
        updateService()
    }
    private fun setUpListeners() = with(bind) {
        var clicked = false
        val permissionList: List<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOf(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        this?.let {
            tvPlayListName.setOnClickListener {
                PlaylistDialogFragment().show(parentFragmentManager,PlaylistDialogFragment::class.simpleName)
            }
            btnMenu?.setOnClickListener {
                (activity as MainActivity).bind.mainDrawerLayout.openDrawer(GravityCompat.START)
            }
            btnAdd.setOnClickListener {
                checkPermissions(
                    requireContext(),
                    permissionList
                ) { isGranted, permissionsList ->
                    onMenuActionAddPopup(requireActivity(), btnAdd, {
                        if (isGranted) {
                            launcherFilePickerActivity.launch(Unit)

                        } else {
                            permissionsList.forEach { (permission, granted) ->
                                if (!granted) {
                                    launcherPermission.launch(permission)
                                }
                            }
                        }
                    }, {
                        createNewPlayListDialog(requireActivity()) { name ->
                            mainViewModel.createPlayList(PlaylistEntity(playListName = name))
                        }
                    })
                }
            }
            btnFavorite.setOnClickListener {
                if (!isFavorite) {
                    mainViewModel.updateFavoriteSong(true, mPrefs.idSong)
                } else {
                    mainViewModel.updateFavoriteSong(false, mPrefs.idSong)
                }
            }
            btnSearch.setOnClickListener {
                showOrHideSearchbar()
            }
            btnClose.setOnClickListener {
                showOrHideSearchbar()
            }
            edtSearch.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    musicListAdapter?.filter?.filter(s)
                }

                override fun afterTextChanged(s: Editable?) {}
            })
            btnMultipleSelect.setOnClickListener {
                if (clicked) {
                    musicListAdapter?.showMultipleSelection(false)
                    btnMultipleSelect.backgroundTintList =
                        changeBackgroundColor(requireContext(), false)
                    clicked = false
                    visibleOrGoneBottomActions(true)
                    musicListAdapter?.clearListItemsForDelete()
                } else {
                    musicListAdapter?.showMultipleSelection(true)
                    btnMultipleSelect.backgroundTintList =
                        changeBackgroundColor(requireContext(), true)
                    clicked = true
                    visibleOrGoneBottomActions(false)
                }
            }
            btnFilter.setOnClickListener {
                OrderByDialog().show(parentFragmentManager, OrderByDialog::class.simpleName)
            }
            btnDelete?.setOnClickListener {
                val listForDeleted = musicListAdapter?.getListItemsForDelete()?.toList()
                listForDeleted?.let {
                    mainViewModel.deleteSong(listForDeleted)
                    musicPlayerService?.removeMediaItems(listForDeleted)
                    musicListAdapter?.removeItemsForMultipleSelectedAction()
                }
            }
            btnMainEq.setOnClickListener {
                launcherAudioEffectActivity.launch(musicPlayerService?.getSessionOrChannelId()!!)
            }
            btnMore.setOnClickListener { view ->
                onMenuItemPopup(ON_MINI_PLAYER_MENU, requireActivity(), mPrefs,view, {
                    // Delete item callback
                }, {
                    // Delete all items callback
                    musicListAdapter?.removeAll()
                    mainViewModel.deleteAllSongs()
                }, {}, {}, {
                    // Song info callback
                    SongInfoDialogFragment.newInstance(
                        SongEntity(
                            id = currentMusicState.idSong,
                            pathLocation = currentMusicState.songPath
                        )
                    )
                        .show(parentFragmentManager, SongInfoDialogFragment::class.simpleName)
                },{})
            }
        }
    }
    private fun showOrHideSearchbar() = with(bind) {
        this?.let {
            if (!isFiltering) {
                visibleOrGoneViews(false)
                btnSearch.backgroundTintList = changeBackgroundColor(requireContext(), true)
                isFiltering = true
                showKeyboard(true, edtSearch)
            } else {
                visibleOrGoneViews(true)
                edtSearch.setText("")
                btnSearch.backgroundTintList = changeBackgroundColor(requireContext(), false)
                isFiltering = false
                showKeyboard(false, edtSearch)
            }
        }
    }
    fun hideSearchBar() = with(bind) {
        this?.let {
            if (isFiltering) {
                visibleOrGoneViews(true)
                edtSearch.setText("")
                btnSearch.backgroundTintList = changeBackgroundColor(requireContext(), false)
                isFiltering = false
                showKeyboard(false, edtSearch)
            }
        }
    }
    private fun visibleOrGoneViews(isVisible: Boolean) = with(bind) {
        this?.let {
            tilSearch?.visibility = if (isVisible) View.GONE else View.VISIBLE
            btnClose?.visibility = if (isVisible) View.GONE else View.VISIBLE
            btnMenu?.visibility = if (isVisible) View.VISIBLE else View.GONE
            btnFilter?.visibility = if (isVisible) View.VISIBLE else View.GONE
            btnMainEq?.visibility = if (isVisible) View.VISIBLE else View.GONE
            tvPlayListName?.visibility = if (isVisible) View.VISIBLE else View.GONE
        }
    }
    private fun visibleOrGoneBottomActions(isVisible: Boolean) = with(bind) {
        this?.let {
            btnAdd.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
            btnFavorite.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
            btnSearch.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
            btnMore.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
            btnDelete?.visibility = if (isVisible) View.GONE else View.VISIBLE
        }
    }
    private fun showKeyboard(show: Boolean, view: View) {
        activity?.showOrHideKeyboard(show, view, { _ ->// isShown
            view.requestFocus()
        }, { // isHide
            CoroutineScope(Dispatchers.Main).launch {
                delay(1000)
                musicListAdapter?.changeBackgroundColorSelectedItem(mPrefs.idSong)
            }
        })
    }
    private fun onItemClick(position: Int, song: SongEntity) {
        musicListAdapter?.getPositionByItem(song)?.let { pos ->
            musicPlayerService?.startPlayer(song)
            musicPlayerService?.clearABLoopOfPreferences()
            mPrefs.idSong = song.id
            mainViewModel.setCurrentPosition(pos.first)

        }
    }
    private fun onMenuItemClick(view: View, position: Int, selectedSong: SongEntity) {
        onMenuItemPopup(ON_MENU_ITEM,requireActivity(), mPrefs,view, { // Delete item callback
            mainViewModel.deleteSong(selectedSong)
            this.song = selectedSong
        }, { // Delete all items callback
            musicListAdapter?.removeAll()
            mainViewModel.deleteAllSongs()
        }, { // Send to playlist callback
             PlaylistDialogFragment.newInstance(selectedSong.id).show(parentFragmentManager,PlaylistDialogFragment::class.simpleName)

        }, {//
            mainViewModel.updateSong(selectedSong.copy(favorite = true))
        }, {// Go to song info
            SongInfoDialogFragment.newInstance(
                SongEntity(
                    id = selectedSong.id,
                    pathLocation = selectedSong.pathLocation
                )
            )
                .show(parentFragmentManager, SongInfoDialogFragment::class.simpleName)
        },{// Go to album detail
            lifecycleScope.launch {
               withContext(Dispatchers.Main) {
                    delay(300)
                    navController?.navigate(R.id.albumDetailFragment, bundleOf("extra_album" to selectedSong.album))
                }
            }
        })
    }
    fun setNumberOfTrack(scrollToPosition: Boolean = true, itemCount: Int = 0):Pair<Int,Int> {
        val itemSong = musicListAdapter?.getSongById(mPrefs.idSong)
        itemSong?.let {
            val (numberedPos, realPos) = musicListAdapter?.getPositionByItem(itemSong)!!
            mPrefs.currentIndexSong = numberedPos.toLong()
            musicListAdapter?.changeBackgroundColorSelectedItem(mPrefs.idSong)
            if (scrollToPosition) bind?.rvSongs?.scrollToPosition(realPos)
        }
        val numbersOfTrack=Pair((if (mPrefs.currentIndexSong > -1) mPrefs.currentIndexSong else 0).toInt(),(musicListAdapter?.getSongItemCount()!! + itemCount))
        return numbersOfTrack
    }
    private fun updateService() {
        serviceConnection?.let {
            startOrUpdateService(
                requireContext(),
                MusicPlayerService::class.java, it, currentMusicState
            )
        }
    }
    override fun musicState(musicState: MusicState?) {
        super.musicState(musicState)
        musicState?.let { mainViewModel.setMusicState(musicState) }
    }
    override fun currentTrack(musicState: MusicState?) {
        super.currentTrack(musicState)
        musicState?.let { mainViewModel.setCurrentTrack(musicState) }

    }
    // The overridden method onConnectedService is not fired here because it is executed after the first fragment.
    // We obtain the connection to the service through the view model sent from the main activity.
    override fun onServiceDisconnected() {
        super.onServiceDisconnected()
        musicPlayerService = null
    }
    override fun onPause() {
        super.onPause()
        setNumberOfTrack()
    }
    override fun onResume() {
        super.onResume()
        setNumberOfTrack()
        mainViewModel.checkIfIsFavorite(currentMusicState.idSong)
        mainViewModel.reloadSongInfo()
        mPrefs.saveFragmentOfNav = LIST_FRAGMENT
    }
    override fun onStop() {
        mPrefs.currentView = MAIN_FRAGMENT
        if(mPrefs.idSong>-1) mainViewModel.saveCurrentStateSong(currentMusicState)
        super.onStop()
    }

    companion object {
        var instance: ListFragment? = null
        @SuppressLint("StaticFieldLeak")
        var isFiltering: Boolean = false
        var navController:NavController?=null
        @JvmStatic
        fun newInstance(param1: Int, param2: String) =
            ListFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
    interface OnFinishedLoadSongs {
        fun onFinishLoad()
    }
}