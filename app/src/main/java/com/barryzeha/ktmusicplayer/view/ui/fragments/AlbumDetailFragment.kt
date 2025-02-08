package com.barryzeha.ktmusicplayer.view.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.core.R as coreRes
import com.barryzeha.ktmusicplayer.databinding.FragmentAlbumDetailBinding

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class AlbumDetailFragment : BaseFragment(R.layout.fragment_album_detail) {

    private var param1: String? = null
    private var param2: String? = null
    private var _bind:FragmentAlbumDetailBinding?=null
    private val bind:FragmentAlbumDetailBinding get() = _bind!!

    override fun onCreate(savedInstanceState: Bundle?) {
        activity?.setTheme(coreRes.style.Theme_With_Toolbar)
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