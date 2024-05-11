package com.barryzeha.ktmusicplayer.view.ui.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.barryzeha.core.common.READ_STORAGE_REQ_CODE
import com.barryzeha.core.common.checkPermissions
import com.barryzeha.core.common.createTime
import com.barryzeha.core.common.getRealPathFromURI
import com.barryzeha.core.common.showSnackBar
import com.barryzeha.core.entities.SongEntity
import com.barryzeha.ktmusicplayer.databinding.FragmentListPlayerBinding
import com.barryzeha.ktmusicplayer.view.ui.adapters.MusicListAdapter
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import com.barryzeha.core.R as coreRes

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

@AndroidEntryPoint
class ListPlayerFragment : Fragment() {

    private var param1: String? = null
    private var param2: String? = null
    private var _bind:FragmentListPlayerBinding? = null
    private val mainViewModel:MainViewModel by viewModels()
    private var uri:Uri?=null
    private lateinit var adapter:MusicListAdapter
    private lateinit var mediaPlayer:MediaPlayer
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
        setUpViews()
        setUpAdapter()
        setUpMediaPlayer()
        setUpListeners()
        setUpObservers()
        initCheckPermission()
    }

    private fun activityResultFile(){
        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result:ActivityResult->
            if(result.resultCode == Activity.RESULT_OK){
                uri = result.data?.data
                val realPathFromFile = getRealPathFromURI(uri!!, requireContext())
                mainViewModel.saveNewSong(SongEntity(
                    pathLocation = realPathFromFile,
                    timestamp = Date().time
                ))
            }
        }
    }

    private fun setUpMediaPlayer(){
        activity?.let {
            mediaPlayer = MediaPlayer()
        }
    }
    private fun activityResultForPermission(){
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply{
            type = "audio/*"
        }
        intent.putExtra("read_storage", READ_STORAGE_REQ_CODE)
       launcherPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()){
            if(it){
                initCheckPermission()
                //launcher.launch(intent)
            }
        }
    }
    private fun setUpAdapter(){
        adapter = MusicListAdapter(::onItemClick)
        bind.rvSongs.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = this@ListPlayerFragment.adapter
        }
    }
    private fun setUpObservers(){
        mainViewModel.fetchAllSong()
        mainViewModel.allSongs.observe(viewLifecycleOwner){
            if(it.isEmpty()){
                Toast.makeText(context, "No hay ninguna canción", Toast.LENGTH_SHORT).show()
            }else{
                adapter.addAll(it)
            }
        }
        mainViewModel.songById.observe(viewLifecycleOwner){song->
            song?.let{
                adapter.add(song)
            }
        }
        mainViewModel.currentTimeOfSong.observe(viewLifecycleOwner){currentTime->
            currentTime?.let{
                bind.bottomPlayerControls.tvInitTime.text = currentTime.third
                bind.bottomPlayerControls.loadSeekBar.progress = mediaPlayer.currentPosition
            }
        }
    }
    private fun setUpViews()=with(bind){

    }
    private fun setUpListeners()= with(bind){
        val chooseFileIntent = Intent(Intent.ACTION_GET_CONTENT).apply{
            type = "audio/*"
        }
        btnAdd.setOnClickListener {
            checkPermissions(bind.root.context,
                listOf( Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO)
            ){ isGranted, permissionsList->
                if(isGranted){
                    launcher.launch(chooseFileIntent)
                }
                else{
                    permissionsList.forEach {permission->
                        if(!permission.second) {
                            launcherPermission.launch(permission.first)

                        }
                    }
                }
            }
        }
        btnLess.setOnClickListener {
            activity?.showSnackBar(it, "Seleccione un archivo de la lista primero")
        }
        bottomPlayerControls.btnPlay.setOnClickListener{
            if(mediaPlayer.isPlaying){ mediaPlayer.pause(); bottomPlayerControls.btnPlay.setIconResource(coreRes.drawable.ic_play)}
            else {mediaPlayer.start(); bottomPlayerControls.btnPlay.setIconResource(coreRes.drawable.ic_pause)}
        }
        mediaPlayer.setOnCompletionListener {
            bind.bottomPlayerControls.btnPlay.setIconResource(coreRes.drawable.ic_play)
        }
    }
    private fun initCheckPermission(){
        checkPermissions(requireContext(),
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO)
        ){isGranted,permissions->
            if(isGranted) Log.e("GRANTED", "Completed granted" )
            else{
                permissions.forEach {permission->
                    if(!permission.second){
                        launcherPermission.launch(permission.first)
                    }
                }

            }
        }
    }
    private fun onItemClick(song:SongEntity){
        Log.e("URI", Uri.parse(song.pathLocation).toString() )
        Log.e("URI", song.pathLocation.toString() )
        activity?.let {context->
            try {
                checkPermissions(context,
                    listOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO)
                ){isGranted,permissionsList->
                    if(isGranted){
                            mediaPlayer.setDataSource(song.pathLocation)
                            mediaPlayer.prepare()
                            mediaPlayer.start()
                            mainViewModel.fetchCurrentTimeOfSong(mediaPlayer)
                            bind.bottomPlayerControls.btnPlay.setIconResource(coreRes.drawable.ic_pause)
                            bind.bottomPlayerControls.tvEndTime.text= createTime(mediaPlayer.duration).third
                            bind.bottomPlayerControls.loadSeekBar.max=mediaPlayer.duration
                        }else{
                            permissionsList.forEach {permission->
                                if(!permission.second) {
                                    launcherPermission.launch(permission.first)
                                }
                            }
                        }
                    }

            }catch (e:Exception){
                Log.e("ERROR_MEDIA_PLAYER", e.message.toString() )
                Toast.makeText(context, "Error al reproducir", Toast.LENGTH_SHORT).show()
            }
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