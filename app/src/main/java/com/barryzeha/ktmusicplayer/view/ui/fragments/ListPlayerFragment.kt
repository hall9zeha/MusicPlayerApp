package com.barryzeha.ktmusicplayer.view.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.barryzeha.core.common.getFragment
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.databinding.FragmentListPlayerBinding
import com.barryzeha.ktmusicplayer.view.ui.fragments.playerControls.PlaybackControlsFragment
import dagger.hilt.android.AndroidEntryPoint

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

@AndroidEntryPoint
class ListPlayerFragment : BaseFragment(R.layout.fragment_list_player){
    private var bind:FragmentListPlayerBinding? = null
    private val binding:FragmentListPlayerBinding get() = bind!!
    private var navController:NavController?=null
    private var navHostFragment:NavHostFragment?=null

    private var _playbackControlsFragmentInstance:PlaybackControlsFragment?=null
    private var param1: String? = null
    private var param2: String? = null

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
        instance = this
        setupNavigation()
        setupSubFragments()
        setupObservers()
    }
    private fun setupNavigation(){
        navHostFragment = childFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment?.navController
        val navInflater = navController?.navInflater
        val navGraph = navInflater?.inflate(R.navigation.main_graph)
        navController?.graph = navGraph!!
        mainViewModel.saveNavControllerInstance(navController!!)
    }
    private fun setupSubFragments(){
        _playbackControlsFragmentInstance = getFragment(R.id.miniPlayerControls)
    }
    private fun setupObservers(){
        mainViewModel.navControllerInstance.observe(viewLifecycleOwner){instance->
            navController = instance
        }
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

}


