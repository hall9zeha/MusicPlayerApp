package com.barryzeha.ktmusicplayer.view.ui.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import com.barryzeha.core.common.checkPermissions
import com.barryzeha.core.common.showSnackBar
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.databinding.FragmentListPlayerBinding
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

@AndroidEntryPoint
class ListPlayerFragment : Fragment() {

    private var param1: String? = null
    private var param2: String? = null
    private var _bind:FragmentListPlayerBinding? = null
    private val mainViewModel:MainViewModel by viewModels()
    private var uri:Uri?=null

    private lateinit var launcher:ActivityResultLauncher<Intent>
    private lateinit var launcherPermission:ActivityResultLauncher<String>
    private val bind:FragmentListPlayerBinding get() = _bind!!

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
            _bind = FragmentListPlayerBinding.inflate(inflater,container,false)
            _bind?.let { bind-> return bind.root }
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activityResultFile()
        activityResultForPermission()
        setUpListeners()
        setUpObservers()
    }

    private fun activityResultFile(){
        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result:ActivityResult->
            if(result.resultCode == Activity.RESULT_OK){
                uri = result.data?.data
                Log.e("URI_CONTENT", uri.toString())
            }
        }
    }
    private fun activityResultForPermission(){
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply{
            type = "audio/*"
        }
       launcherPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()){
            if(it){
                launcher.launch(intent)
            }
        }
    }
    private fun setUpObservers(){
        mainViewModel.fetchAllSong()
        mainViewModel.allSongs.observe(viewLifecycleOwner){
            if(it.isEmpty()){
                Toast.makeText(context, "No hay ninguna canción", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun setUpListeners()= with(bind){
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply{
            type = "audio/*"
        }
        btnAdd.setOnClickListener {
            checkPermissions(bind.root.context,Manifest.permission.READ_EXTERNAL_STORAGE){isGranted->
                if(isGranted){
                    launcher.launch(intent)
                }
                else{
                    launcherPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
        btnLess.setOnClickListener {
            activity?.showSnackBar(it, "Seleccione un archivo de la lista primero")
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ListPlayerFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}