package com.barryzeha.ktmusicplayer.view.ui.fragments.albumDetail

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.barryzeha.core.common.SHUFFLE
import com.barryzeha.core.common.createTime
import com.barryzeha.core.common.fetchShortMetadataAlbumInfo
import com.barryzeha.core.common.getBitmap
import com.barryzeha.core.common.loadImage
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.common.ALBUM_DETAIL_FRAGMENT
import com.barryzeha.ktmusicplayer.common.ON_ALBUM_DETAIL_ITEM_MENU
import com.barryzeha.ktmusicplayer.common.onMenuItemPopup
import com.barryzeha.ktmusicplayer.databinding.FragmentAlbumDetailBinding
import com.barryzeha.ktmusicplayer.view.ui.adapters.MusicListAdapter
import com.barryzeha.ktmusicplayer.view.ui.dialog.PlaylistDialogFragment
import com.barryzeha.ktmusicplayer.view.ui.dialog.SongInfoDialogFragment
import com.barryzeha.ktmusicplayer.view.ui.fragments.BaseFragment
import com.barryzeha.ktmusicplayer.view.viewmodel.AlbumDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random
import com.barryzeha.core.R as coreRes

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class AlbumDetailFragment : BaseFragment(R.layout.fragment_album_detail) {

    private var param1: String? = null
    private var param2: String? = null
    private val arguments by navArgs<AlbumDetailFragmentArgs>()
    private val albumDetailViewModel: AlbumDetailViewModel by viewModels()
    private var _bind: FragmentAlbumDetailBinding?=null
    private val bind: FragmentAlbumDetailBinding get() = _bind!!
    private var albumAdapter:MusicListAdapter?=null
    private var albumName:String?=null
    private var navController: NavController?= null
    private var albumSongs:List<SongEntity> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        activity?.setTheme(com.barryzeha.core.R.style.Base_Theme_KTMusicPlayer)
        super.onCreate(savedInstanceState)
        albumDetailViewModel.fetchSongsByAlbum(arguments.extraAlbum)
       /*activity?.onBackPressedDispatcher?.addCallback(this,object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
               //navController?.navigate(R.id.playlistFragment)
                navController?.navigateUp()
            }
        })*/

    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _bind = FragmentAlbumDetailBinding.bind(view)
        //postponeEnterTransition()
        navController = findNavController()
        setupAdapter()
        setupListeners()
        setupObservers(view)
    }

    private fun setupAdapter()=with(bind){
        albumAdapter=MusicListAdapter(::onItemClick,::onMenuItemClick)
        rvAlbumDetail.apply{
            setHasFixedSize(true)
            setItemViewCacheSize(10)
            layoutManager = LinearLayoutManager(context)
            adapter = albumAdapter

        }
    }
    private fun setupObservers(view:View){
        albumDetailViewModel.songsByAlbum.observe(viewLifecycleOwner){songs->
          /*   view.doOnPreDraw {
                 startPostponedEnterTransition()
             }*/
            if(songs.isNotEmpty()) setAlbumInfo(songs)
            albumSongs = songs
            albumAdapter?.addAll(songs)
        }
        mainViewModel.currentTrack.observe(viewLifecycleOwner){currentTrack->
            albumAdapter?.changeBackgroundColorSelectedItem(currentTrack.idSong)
        }
    }
    private fun setAlbumInfo(songs:List<SongEntity>)=with(bind){
        val song = songs[0]
        var totalSongTime = 0L
        ivMusicCover.loadImage(getBitmap(requireContext(),song.pathLocation)!!)
        lifecycleScope.launch(Dispatchers.IO) {
            val songMeta= fetchShortMetadataAlbumInfo(requireContext(),song.pathLocation!!)
            songs.forEach { track->
                totalSongTime += track.duration
            }
            withContext(Dispatchers.Main){

                tvAlbumName.text=song.album
                val stringBuilder = StringBuilder()
                stringBuilder.appendLine(if(songMeta?.albumArtist?.isNotEmpty()!!)songMeta.albumArtist else songMeta.artist)
                stringBuilder.appendLine(songMeta?.year.toString())
                stringBuilder.appendLine("${songs.size} ${getString(coreRes.string.songs)}")
                stringBuilder.appendLine("${getString(coreRes.string.duration)} ${createTime(totalSongTime).third}")
                tvDetailAlbum.text=stringBuilder
            }
        }
        // Ya que guideLine solo está disponible en el diseño Landscape de nuestra vista
        // no necesitamos aplicar un nuevo LayoutParam a nuestro recyclerview, solo en el diseño Portrait de nuestra vista
        guideLine?.let{
            // No need apply new Layout params
        }?:run{
            val newLayoutParams = ConstraintLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0)
            newLayoutParams.topToBottom=bind.btnPlaying.id
            bind.rvAlbumDetail.layoutParams = newLayoutParams
        }
    }

    private fun setupListeners()=with(bind){
        btnPlaying.setOnClickListener{
            musicPlayerService?.openQueue(albumSongs,0)
            mPrefs.isOpenQueue = true
        }
        btnShuffle.setOnClickListener {
            val randomPosition = Random.nextInt(0,albumSongs.size-1)
            musicPlayerService?.openQueue(albumSongs,randomPosition)
            mPrefs.songMode = SHUFFLE
        }
    }
    private fun onMenuItemClick(view: View, position: Int, songEntity: SongEntity) {
        onMenuItemPopup(ON_ALBUM_DETAIL_ITEM_MENU,requireActivity(), mPrefs, view, {
            // Delete item callback
            mainViewModel.deleteSong(songEntity)
        }, { // Delete all items callback disable on this fragment

        }, { // Send to playlist callback
            if (albumAdapter?.itemCount!! > 0) {
                PlaylistDialogFragment.newInstance(songEntity.id).show(parentFragmentManager,
                    PlaylistDialogFragment::class.simpleName)
            }
        }, {//
            mainViewModel.updateSong(songEntity.copy(favorite = true))
            Toast.makeText(context, com.barryzeha.core.R.string.addToFavorite, Toast.LENGTH_SHORT).show()
        }, {// Go to song info
            SongInfoDialogFragment.newInstance(
                SongEntity(
                    id = songEntity.id,
                    pathLocation = songEntity.pathLocation
                )
            )
                .show(parentFragmentManager, SongInfoDialogFragment::class.simpleName)
        },{//Go to album, disable on this fragment
        })

    }
    private fun onItemClick(position: Int, songEntity: SongEntity) {
        musicPlayerService?.openQueue(albumSongs,position-1)
        mPrefs.isOpenQueue = true
    }

    override fun currentTrack(musicState: MusicState?) {
        super.currentTrack(musicState)
        musicState?.let {
            mainViewModel.setCurrentTrack(musicState)
        }
    }

    override fun musicState(musicState: MusicState?) {
        super.musicState(musicState)
        musicState?.let{
            mainViewModel.setMusicState(musicState)
        }
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
               /* arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }*/
            }
    }

}