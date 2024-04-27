package com.barryzeha.ktmusicplayer.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.databinding.FragmentMainPlayerBinding


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MainPlayerFragment : Fragment() {

    private var param1: String? = null
    private var param2: String? = null
    private var _bind:FragmentMainPlayerBinding ? = null
    private val bind:FragmentMainPlayerBinding get() = _bind!!

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
        bind.tvSongDescription.setSelected(true)
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