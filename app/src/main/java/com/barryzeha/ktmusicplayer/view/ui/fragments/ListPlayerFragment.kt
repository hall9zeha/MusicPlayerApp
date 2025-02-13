package com.barryzeha.ktmusicplayer.view.ui.fragments

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.barryzeha.audioeffects.ui.activities.MainEqualizerActivity
import com.barryzeha.core.common.MAIN_FRAGMENT
import com.barryzeha.core.common.checkPermissions
import com.barryzeha.core.common.getBitmap
import com.barryzeha.core.common.getFragment
import com.barryzeha.core.common.getSongMetadata
import com.barryzeha.core.common.keepScreenOn
import com.barryzeha.core.common.loadImage
import com.barryzeha.core.common.showOrHideKeyboard
import com.barryzeha.core.common.startOrUpdateService
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.PlaylistEntity
import com.barryzeha.core.model.entities.PlaylistWithSongsCrossRef
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.common.changeBackgroundColor
import com.barryzeha.ktmusicplayer.common.createNewPlayListDialog
import com.barryzeha.ktmusicplayer.common.getPlayListName
import com.barryzeha.ktmusicplayer.common.onMenuActionAddPopup
import com.barryzeha.ktmusicplayer.common.onMenuItemPopup
import com.barryzeha.ktmusicplayer.common.processSongPaths
import com.barryzeha.ktmusicplayer.common.sortPlayList
import com.barryzeha.ktmusicplayer.databinding.FragmentListPlayerBinding
import com.barryzeha.ktmusicplayer.service.MusicPlayerService
import com.barryzeha.ktmusicplayer.view.ui.activities.MainActivity
import com.barryzeha.ktmusicplayer.view.ui.adapters.MusicListAdapter
import com.barryzeha.ktmusicplayer.view.ui.adapters.PlayListsAdapter
import com.barryzeha.ktmusicplayer.view.ui.dialog.OrderByDialog
import com.barryzeha.ktmusicplayer.view.ui.dialog.SongInfoDialogFragment
import com.barryzeha.ktmusicplayer.view.ui.fragments.playerControls.PlaybackControlsFragment
import com.barryzeha.mfilepicker.ui.views.FilePickerActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.barryzeha.core.R as coreRes

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

@AndroidEntryPoint
class ListPlayerFragment : AbsBaseFragment(R.layout.fragment_list_player){
    private var bind:FragmentListPlayerBinding? = null
    private val binding:FragmentListPlayerBinding get() = bind!!

    override val recyclerView: RecyclerView?
        get() = binding.rvSongs

    private var param1: String? = null
    private var param2: String? = null

    private var playbackControlsFragment:PlaybackControlsFragment?=null


    private var onFinisLoadSongsListener:OnFinishedLoadSongs?=null
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
        onFinisLoadSongsListener = MainPlayerFragment.instance
        instance = this
        setupSubFragments()
      setupNavigation()
    }
    private fun setupNavigation(){
        val fragment = childFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = fragment.navController
        val navInflater = navController.navInflater
        val navGraph = navInflater.inflate(R.navigation.main_graph)
        navController.graph = navGraph
    }
    private fun setupSubFragments(){
        playbackControlsFragment = getFragment(R.id.miniPlayerControls)
        playbackControlsFragment?.let{it.setListMusicFragmentInstance(this)}
    }

     companion object {
         var instance: ListPlayerFragment? = null
         @JvmStatic
         fun newInstance(param1: Int, param2: String) =
             ListPlayerFragment().apply {
                 arguments = Bundle().apply {
                     putInt(ARG_PARAM1, param1)
                     putString(ARG_PARAM2, param2)
                 }
             }
    }

    override fun currentTrack(musicState: MusicState?) {
        super.currentTrack(musicState)
        mainViewModel.setCurrentTrack(musicState!!)
    }

    override fun musicState(musicState: MusicState?) {
        super.musicState(musicState)
        mainViewModel.setMusicState(musicState!!)
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

    interface OnFinishedLoadSongs{
        fun onFinishLoad()
    }
}


