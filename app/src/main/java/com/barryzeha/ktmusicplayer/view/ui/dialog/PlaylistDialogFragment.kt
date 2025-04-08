package com.barryzeha.ktmusicplayer.view.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.model.entities.PlaylistEntity
import com.barryzeha.core.model.entities.PlaylistWithSongsCrossRef
import com.barryzeha.ktmusicplayer.common.createNewPlayListDialog
import com.barryzeha.ktmusicplayer.databinding.BottomSheetPlaylistsLayoutBinding
import com.barryzeha.ktmusicplayer.databinding.OrderByDialogLayoutBinding
import com.barryzeha.ktmusicplayer.service.MusicPlayerService
import com.barryzeha.ktmusicplayer.view.ui.activities.MainActivity
import com.barryzeha.ktmusicplayer.view.ui.adapters.PlayListsAdapter

import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 19/2/25.
 * Copyright (c)  All rights reserved.
 **/
private const val ARG_PARAM_1 = "param1"
@AndroidEntryPoint
class PlaylistDialogFragment : DialogFragment() {

    @Inject
    lateinit var mPrefs: MyPreferences

    private var _bind: BottomSheetPlaylistsLayoutBinding? = null

    private val mainViewModel: MainViewModel by activityViewModels()
    private var dialogView: View? = null
    private var playListAdapter: PlayListsAdapter? = null
    private var idSongForSendToPlaylist: Long = 0
    private var musicPlayerService: MusicPlayerService? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = MaterialAlertDialogBuilder(requireContext()).apply {
            dialogView =  onCreateView(LayoutInflater.from(requireContext()), null, savedInstanceState)
            setView(dialogView)
        }.create()
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity?.let {
            _bind = BottomSheetPlaylistsLayoutBinding.inflate(inflater, container, false)
            _bind?.let { bind ->
                return bind.root
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun getView(): View? {
        return dialogView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _bind = BottomSheetPlaylistsLayoutBinding.bind(dialogView!!)
        getBundle()
        setupAdapter()
        setupObservers()
        setupListeners()
    }
    private fun getBundle(){
        arguments?.let{
            idSongForSendToPlaylist = it.getLong(ARG_PARAM_1)
        }
    }
    private fun setupObservers() {
        mainViewModel.serviceInstance.observe(viewLifecycleOwner) { (serviceConn, serviceInst) ->
            musicPlayerService = serviceInst
        }
        // Playlist
        mainViewModel.createdPlayList.observe(viewLifecycleOwner) { insertedRow ->
            if (insertedRow > 0) {
                Toast.makeText(
                    activity,
                    com.barryzeha.core.R.string.playlistCreatedMsg,
                    Toast.LENGTH_SHORT
                ).show()
                mainViewModel.fetchPlaylists()
            }
        }
        mainViewModel.playLists.observe(viewLifecycleOwner) { playLists ->
            playListAdapter?.let {
                it.addAll(playLists)
                (activity as? MainActivity)?.addItemOnMenuDrawer(playLists)
            }
        }
        mainViewModel.playlistWithSongRefInserted.observe(viewLifecycleOwner) { insertedRow ->
            if (insertedRow > 0) {
                Toast.makeText(
                    activity,
                    com.barryzeha.core.R.string.addedToPlaylist,
                    Toast.LENGTH_SHORT
                ).show()

                // We reset the value to avoid inconveniences, we will use it for our logic
                // when it is greater than zero add to list and if it is zero, switch between lists
                idSongForSendToPlaylist = 0
                dismiss()
            }
        }
        mainViewModel.deletePlayList.observe(viewLifecycleOwner){deleteRow->
            deleteRow?.let {
                if (deleteRow > 0){
                    playListAdapter?.remove(deleteRow)
                    mainViewModel.fetchPlaylists()
                }
            }
        }
    }

    private fun setupAdapter() {

        playListAdapter = PlayListsAdapter({ playlistEntity ->
            // We save playlist ids by clicking on an item in the list that represents our created lists.
            mPrefs.playlistId = playlistEntity.idPlaylist.toInt()
            if (idSongForSendToPlaylist > 0) {
                mainViewModel.savePlaylistWithSongRef(
                    PlaylistWithSongsCrossRef(playlistEntity.idPlaylist, idSongForSendToPlaylist)
                )
            } else {
                // We load the selected playlist
                getPlaylist(playlistEntity.idPlaylist.toInt())
                mainViewModel.setPlaylistName(playlistEntity.playListName)
                mPrefs.isPopulateServicePlaylist = true
            }
        }, { playlist ->
            // When deleting an item
            mainViewModel.deletePlayList(playlist.idPlaylist)
            playListAdapter?.remove(playlist)
            (activity as? MainActivity)?.removeMenuItemDrawer(playlist.idPlaylist.toInt())
        }, { playlist ->
            //Rename a playlist
            mainViewModel.updatePlaylist(playlist)
        })
        _bind?.rvPlaylists?.apply {
            setHasFixedSize(true)
            setItemViewCacheSize(10)
            layoutManager = LinearLayoutManager(context)
            adapter = playListAdapter
        }
        playListAdapter!!.add(PlaylistEntity(0, "Default"))
    }

    private fun getPlaylist(playlistId: Int) {
        mPrefs.playlistId = playlistId
        musicPlayerService?.clearPlayList(true)
        mainViewModel.fetchPlaylistWithSongsBy(playlistId, mPrefs.playListSortOption)
        dismiss()
    }

    private fun setupListeners() {
        _bind?.btnAdd?.setOnClickListener {
            createNewPlayListDialog(requireActivity()) { playlistName ->
                mainViewModel.createPlayList(PlaylistEntity(playListName = playlistName))
            }
        }
    }
    companion object{
        @JvmStatic
        fun newInstance(param1:Long)=
            PlaylistDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PARAM_1, param1)
                }
            }
    }
}