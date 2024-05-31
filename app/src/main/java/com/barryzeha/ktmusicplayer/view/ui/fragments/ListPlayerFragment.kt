package com.barryzeha.ktmusicplayer.view.ui.fragments

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.startForegroundService
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.barryzeha.core.common.READ_STORAGE_REQ_CODE
import com.barryzeha.core.common.checkPermissions
import com.barryzeha.core.common.createTime
import com.barryzeha.core.common.getRealPathFromURI
import com.barryzeha.core.common.getSongCover
import com.barryzeha.core.common.showSnackBar
import com.barryzeha.core.model.SongController
import com.barryzeha.core.model.entities.MusicState
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.ktmusicplayer.databinding.FragmentListPlayerBinding
import com.barryzeha.ktmusicplayer.service.MusicPlayerService
import com.barryzeha.ktmusicplayer.view.ui.adapters.MusicListAdapter
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import com.barryzeha.core.R as coreRes


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

@AndroidEntryPoint
class ListPlayerFragment : Fragment(), ServiceConnection {

    private var param1: String? = null
    private var param2: String? = null
    private var _bind:FragmentListPlayerBinding? = null
    private val mainViewModel:MainViewModel by viewModels(ownerProducer = {requireActivity()})
    private var uri:Uri?=null
    private lateinit var adapter:MusicListAdapter
    private lateinit var launcher:ActivityResultLauncher<Intent>
    private lateinit var launcherPermission:ActivityResultLauncher<String>
    private var isPlaying = false
    private var isUserSeeking=false
    private var userSelectPosition=0
    private val bind:FragmentListPlayerBinding get() = _bind!!
    private  var currentSelectedPosition:Int =0

    private var currentMusicState = MusicState()
    private var musicPlayerService: MusicPlayerService?=null

    private val songController = object:SongController{
        override fun play() {
            bind.bottomPlayerControls.btnPlay.setIconResource(coreRes.drawable.ic_pause)
            musicPlayerService?.playingExoPlayer()
            mainViewModel.saveStatePlaying(true)

       }
        override fun pause() {
            bind.bottomPlayerControls.btnPlay.setIconResource(coreRes.drawable.ic_play)
            musicPlayerService?.pauseExoPlayer()
            mainViewModel.saveStatePlaying(false)
        }
        override fun next() {
            bind.bottomPlayerControls.btnNext.performClick()
        }
        override fun previous() {
            bind.bottomPlayerControls.btnPrevious.performClick()
        }
        override fun stop() {
            startOrUpdateService()
        }
        override fun musicState(musicState: MusicState?) {
            musicState?.let {
                mainViewModel.setMusicState(musicState)
               //setUpViews(musicState)
            }
        }
        override fun currentTrack(musicState: MusicState?) {
            musicState?.let{
                setUpViews(musicState)
                if(!musicState.isPlaying){
                    if((adapter.itemCount -1)  == currentSelectedPosition) {
                        bind.bottomPlayerControls.btnPlay.setIconResource(coreRes.drawable.ic_play)
                        mainViewModel.saveStatePlaying(false)
                        mainViewModel.setCurrentPosition(0)
                    }
                    else {
                        mainViewModel.saveStatePlaying(true)
                        bind.bottomPlayerControls.btnNext.performClick()}
                }else{
                    mainViewModel.saveStatePlaying(true)
                }
            }
        }
    }
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
        initCheckPermission()
        setUpAdapter()
        setUpObservers()
        setUpListeners()
    }
    private fun activityResultFile(){
        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result:ActivityResult->
            if(result.resultCode == Activity.RESULT_OK){
                uri = result.data?.data
                val realPathFromFile = getRealPathFromURI(uri!!, requireContext())
                mainViewModel.saveNewSong(
                    SongEntity(
                    pathLocation = realPathFromFile,
                    timestamp = Date().time
                )
                )
            }
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
        adapter = MusicListAdapter(::onItemClick,::onMenuItemClick)
        bind.rvSongs.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = this@ListPlayerFragment.adapter
        }
    }
    private fun setUpObservers(){
        mainViewModel.fetchAllSong()
        mainViewModel.musicState.observe(viewLifecycleOwner){savedMusicState->
            currentMusicState = savedMusicState
            bind.ivCover.setImageBitmap(savedMusicState.albumArt)
            val durationInMillis = savedMusicState.duration
            val formattedDuration = createTime(durationInMillis).third
            bind.seekbarControl.loadSeekBar.max = savedMusicState.duration.toInt()
            bind.seekbarControl.tvEndTime.text = formattedDuration
            bind.seekbarControl.tvInitTime.text = createTime(savedMusicState.currentDuration).third
            bind.seekbarControl.loadSeekBar.progress = savedMusicState.currentDuration.toInt()
            startOrUpdateService()
        }
        mainViewModel.isPlaying.observe(viewLifecycleOwner){statePlay->
            isPlaying=statePlay
            if (statePlay) {
                isPlaying = true
                bind.bottomPlayerControls.btnPlay.setIconResource(coreRes.drawable.ic_pause)
            }else{
                bind.bottomPlayerControls.btnPlay.setIconResource(coreRes.drawable.ic_play)
            }
        }
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
        mainViewModel.currentSongListPosition.observe(viewLifecycleOwner){positionSelected->
            currentSelectedPosition = positionSelected
            positionSelected?.let{
                adapter.changeBackgroundColorSelectedItem(positionSelected)
            }
        }
    }
    private fun setUpViews(musicState:MusicState)=with(bind){

            val durationInMillis = musicState.duration
            val formattedDuration = createTime(durationInMillis).third
            seekbarControl.tvEndTime.text = formattedDuration
            seekbarControl.loadSeekBar.max = durationInMillis.toInt()
            seekbarControl.tvInitTime.text = createTime(musicState.currentDuration).third

        activity?.let {
            val songMetadata = getSongCover(requireActivity(), musicState.songPath)
            mainViewModel.setCurrentTrack(musicState)
            songMetadata?.let {
                ivCover.setImageBitmap(it.albumArt)
            }

        }
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
       bottomPlayerControls.btnPlay.setOnClickListener{
            if(adapter.itemCount>0) {
                if(!currentMusicState.isPlaying && currentMusicState.duration<=0)getSongOfAdapter(currentSelectedPosition)?.let{song->
                    musicPlayerService?.startPlayer(song.pathLocation.toString())

                }
                else {
                    if (isPlaying) {
                        musicPlayerService?.pauseExoPlayer(); bottomPlayerControls.btnPlay.setIconResource(coreRes.drawable.ic_play)
                        mainViewModel.saveStatePlaying(false)

                    } else {
                        musicPlayerService?.playingExoPlayer(); bottomPlayerControls.btnPlay.setIconResource(coreRes.drawable.ic_pause)
                        mainViewModel.saveStatePlaying(true)
                    }
                }
            }
        }
        bottomPlayerControls.btnPrevious.setOnClickListener{
             if (currentSelectedPosition > 0) {
                getSongOfAdapter(currentSelectedPosition - 1)?.let{song->
                    musicPlayerService?.startPlayer(song.pathLocation.toString())

                }
            }
        }
        bottomPlayerControls.btnNext.setOnClickListener {
           if(currentSelectedPosition<adapter.itemCount-1){
               getSongOfAdapter(currentSelectedPosition +1)?.let{song->
                   musicPlayerService?.startPlayer(song.pathLocation.toString())

               }
           }
        }
        bind.seekbarControl.loadSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    bind.seekbarControl.tvInitTime.text = createTime(progress.toLong()).third
                    musicPlayerService?.setExoPlayerProgress(progress.toLong())
                    userSelectPosition=progress
                    seekBar?.progress=progress
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking=true
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking=false
                bind.seekbarControl.loadSeekBar.progress=userSelectPosition
            }
        })
    }
    private fun getSongOfAdapter(position:Int): SongEntity?{
        mainViewModel.setCurrentPosition(position)
        val song = adapter.getSongByPosition(currentSelectedPosition)
        return song
    }
    private fun initCheckPermission(){
        val permissionList:MutableList<String> = mutableListOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            permissionList.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        checkPermissions(requireContext(),permissionList){isGranted,permissions->
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
    private fun onItemClick(position:Int,song: SongEntity){
        musicPlayerService?.startPlayer(song.pathLocation.toString())
        mainViewModel.setCurrentPosition(position)
    }
    private fun onMenuItemClick(view:View,position: Int, song: SongEntity) {
        val popupMenu = PopupMenu(activity,view)
        popupMenu.menuInflater.inflate(coreRes.menu.item_menu,popupMenu.menu)
        popupMenu.setOnMenuItemClickListener {
            Toast.makeText(context, position.toString(), Toast.LENGTH_SHORT).show()
            true
        }
        popupMenu.show()
    }
    private fun startOrUpdateService():Intent{
        val serviceIntent = Intent (context, MusicPlayerService::class.java).apply {
            putExtra("musicState", currentMusicState)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(requireContext(),serviceIntent)
        } else activity?.startService(serviceIntent)

        return serviceIntent
    }
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as MusicPlayerService.MusicPlayerServiceBinder
        musicPlayerService = binder.getService()
        musicPlayerService!!.setSongController(songController)
    }
    override fun onServiceDisconnected(name: ComponentName?) {
        musicPlayerService = null
    }
    override fun onStart() {
        super.onStart()
        //if(!requireContext().isServiceRunning(MusicPlayerService::class.java)) {
            requireContext().bindService(startOrUpdateService(),
                this,
                Context.BIND_AUTO_CREATE
            )
            musicPlayerService?.setSongController(songController)
        //}
    }
    override fun onResume() {
        super.onResume()
        musicPlayerService?.setSongController(songController)
    }
    override fun onPause() {
        super.onPause()
        musicPlayerService?.unregisterController()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _bind=null
        try {
            activity?.unbindService(this)
        } catch (e: IllegalArgumentException) {
            Log.e("STOP_SERVICE", "Service not registered")
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


