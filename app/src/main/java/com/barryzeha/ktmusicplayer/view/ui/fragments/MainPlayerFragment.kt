package com.barryzeha.ktmusicplayer.view.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.barryzeha.core.common.createTime
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.databinding.FragmentMainPlayerBinding
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import dagger.hilt.android.AndroidEntryPoint


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

@AndroidEntryPoint
class MainPlayerFragment : Fragment() {

    private var param1: String? = null
    private var param2: String? = null
    private var _bind:FragmentMainPlayerBinding ? = null
    private val bind:FragmentMainPlayerBinding get() = _bind!!
    private val mainViewModel:MainViewModel by viewModels(ownerProducer = {requireActivity()})

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
        // Important is necessary setSelected to textview for able marquee autoscroll when text is long than textView size
        setUpObservers()
        bind.tvSongDescription.setSelected(true)
        bind.tvSongArtist.setSelected(true)
        bind.tvSongAlbum.setSelected(true)

    }
    private fun setUpObservers(){
        mainViewModel.musicState.observe(viewLifecycleOwner){
            it?.let{musicState->
                setUpSongInfo(musicState)
            }
        }
    }
    private fun setUpSongInfo(musicState: MusicState){
        bind.tvSongAlbum.text=musicState.album
        bind.tvSongArtist.text=musicState.artist
        bind.tvSongDescription.text = musicState.title
        Glide.with(requireContext())
            .load(musicState.albumArt)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(bind.ivMusicCover)
        bind.pbLinear.max=musicState.duration.toInt()
        bind.pbLinear.progress=musicState.currentDuration.toInt()
        bind.tvSongTimeRest.text= createTime(musicState.currentDuration).third
        bind.tvSongTimeCompleted.text = createTime(musicState.duration).third


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