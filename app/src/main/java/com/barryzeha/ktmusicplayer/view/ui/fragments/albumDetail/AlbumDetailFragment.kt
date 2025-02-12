package com.barryzeha.ktmusicplayer.view.ui.fragments.albumDetail

import android.os.Bundle
import android.view.View
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.databinding.FragmentAlbumDetailBinding
import com.barryzeha.ktmusicplayer.view.ui.fragments.BaseFragment

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class AlbumDetailFragment : BaseFragment(R.layout.fragment_album_detail) {

    private var param1: String? = null
    private var param2: String? = null
    private var _bind: FragmentAlbumDetailBinding?=null
    private val bind: FragmentAlbumDetailBinding get() = _bind!!

    override fun onCreate(savedInstanceState: Bundle?) {
        activity?.setTheme(com.barryzeha.core.R.style.Theme_With_Toolbar)
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _bind = FragmentAlbumDetailBinding.bind(view)

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