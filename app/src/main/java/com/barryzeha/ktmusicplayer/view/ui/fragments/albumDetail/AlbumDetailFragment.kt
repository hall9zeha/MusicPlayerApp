package com.barryzeha.ktmusicplayer.view.ui.fragments.albumDetail

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBar.LayoutParams
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.common.createTime
import com.barryzeha.core.common.fetchFileMetadata
import com.barryzeha.core.common.fetchShortFileMetadata
import com.barryzeha.core.common.fetchTimeOfSong
import com.barryzeha.core.common.getBitmap
import com.barryzeha.core.common.getTimeOfSong
import com.barryzeha.core.common.loadImage
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.common.ALBUM_DETAIL_FRAGMENT
import com.barryzeha.ktmusicplayer.databinding.FragmentAlbumDetailBinding
import com.barryzeha.ktmusicplayer.view.ui.adapters.MusicListAdapter
import com.barryzeha.ktmusicplayer.view.ui.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class AlbumDetailFragment : BaseFragment(R.layout.fragment_album_detail) {

    private var param1: String? = null
    private var param2: String? = null
    private var _bind: FragmentAlbumDetailBinding?=null
    private val bind: FragmentAlbumDetailBinding get() = _bind!!
    private var albumAdapter:MusicListAdapter?=null
    private var albumName:String?=null
    private var navController: NavController?= null
    private var albumSongs:List<SongEntity> = listOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        activity?.setTheme(com.barryzeha.core.R.style.Theme_With_Toolbar)
        super.onCreate(savedInstanceState)
        arguments?.let {
            albumName = it.getString("extra_album")
       }
        activity?.onBackPressedDispatcher?.addCallback(this,object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
               //navController?.navigate(R.id.playlistFragment)
                navController?.navigateUp()
            }
        })

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _bind = FragmentAlbumDetailBinding.bind(view)
        navController = findNavController()
        postponeEnterTransition()
        setupAdapter()
        setupObservers(view)
        setupListeners()
    }

    private fun setupAdapter()=with(bind){
        albumAdapter=MusicListAdapter(::onItemClick,::onMenuItemClick)
        rvAlbumDetail.apply{
            setHasFixedSize(true)
            setItemViewCacheSize(10)
            layoutManager = LinearLayoutManager(context)
            adapter = albumAdapter
            /*post {
            }*/
        }
    }
    private fun setupObservers(view:View){
        albumName?.let{mainViewModel.fetchSongsByAlbum(it)}
        mainViewModel.songsByAlbum.observe(viewLifecycleOwner){songs->
            view.doOnPreDraw {
                startPostponedEnterTransition()
            }
            albumSongs = songs
            albumAdapter?.addAll(songs)
            if(songs.isNotEmpty()) setAlbumInfo(songs)
        }
        mainViewModel.currentTrack.observe(viewLifecycleOwner){currentTrack->
            albumAdapter?.changeBackgroundColorSelectedItem(currentTrack.idSong)
        }
    }
    private fun setAlbumInfo(songs:List<SongEntity>)=with(bind){
        val song = songs[0]
        var totalSongTime = 0L
        val songMeta= fetchFileMetadata(requireContext(),song.pathLocation!!)
        ivMusicCover.loadImage(getBitmap(requireContext(),song.pathLocation)!!)
        tvAlbumName.text=song.album
        val stringBuilder = StringBuilder()
        stringBuilder.appendLine(if(songMeta?.albumArtist?.isNotEmpty()!!)songMeta.albumArtist else songMeta.artist)
        stringBuilder.appendLine(songMeta?.year.toString())
        stringBuilder.appendLine("${songs.size} Canciones")
        tvDetailAlbum.text=stringBuilder
        lifecycleScope.launch(Dispatchers.IO) {
            songs.forEach { track->
                totalSongTime += track.duration
            }
            withContext(Dispatchers.Main){
                stringBuilder.appendLine("Duración ${createTime(totalSongTime).third}")
                tvDetailAlbum.text = stringBuilder
            }
        }
        val layoutParams0 = ConstraintLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0
        )
        layoutParams0.topToBottom=bind.btnPlaying.id
        bind.rvAlbumDetail.layoutParams = layoutParams0
    }
    private fun setupListeners()=with(bind){
        btnPlaying.setOnClickListener{
            musicPlayerService?.openQueue(albumSongs,0)
            mPrefs.isOpenQueue = true
        }
        btnShuffle.setOnClickListener {
            val randomPosition = Random.nextInt(0,albumSongs.size-1)
            musicPlayerService?.openQueue(albumSongs,randomPosition)
            //TODO activar la preferencia shuffle
        }
    }
    private fun onMenuItemClick(view: View, position: Int, songEntity: SongEntity) {
        musicPlayerService?.startPlayer(songEntity)
    }
    private fun onItemClick(position: Int, songEntity: SongEntity) {
        musicPlayerService?.openQueue(albumSongs,position-1)
        mPrefs.isOpenQueue = true
    }
    override fun onResume() {
        super.onResume()
        mPrefs.saveFragmentOfNav = ALBUM_DETAIL_FRAGMENT
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mPrefs.isOpenQueue=false
        musicPlayerService?.reloadIndexOfSong()
    }
    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AlbumDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

}