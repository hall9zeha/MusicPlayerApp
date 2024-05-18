package com.barryzeha.ktmusicplayer.view.ui.fragments

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.startForegroundService
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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
import com.barryzeha.ktmusicplayer.R as appRes


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

@AndroidEntryPoint
class ListPlayerFragment : Fragment(), ServiceConnection {

    private var param1: String? = null
    private var param2: String? = null
    private var _bind:FragmentListPlayerBinding? = null
    private val mainViewModel:MainViewModel by viewModels()
    private var uri:Uri?=null
    private lateinit var adapter:MusicListAdapter
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var launcher:ActivityResultLauncher<Intent>
    private lateinit var launcherPermission:ActivityResultLauncher<String>
    private var isPlaying = false

    private val bind:FragmentListPlayerBinding get() = _bind!!
    private  var currentSelectedPosition:Int =0

    private var currentMusicState = MusicState()
    private var musicPlayerService: MusicPlayerService?=null

    private val songController = object:SongController{
        override fun play() {
            Toast.makeText(context, "Play", Toast.LENGTH_SHORT).show()
            exoPlayer.play()
            bind.bottomPlayerControls.btnPlay.setIconResource(coreRes.drawable.ic_pause)
            updateMediaPlayerNotify()
        }

        override fun pause() {
            exoPlayer.pause()
            bind.bottomPlayerControls.btnPlay.setIconResource(coreRes.drawable.ic_play)
            updateMediaPlayerNotify()
        }

        override fun next() {
            bind.bottomPlayerControls.btnNext.performClick()
            updateMediaPlayerNotify()
        }

        override fun previous() {
            bind.bottomPlayerControls.btnPrevious.performClick()
            updateMediaPlayerNotify()
        }

        override fun stop() {
            exoPlayer.stop()
            updateMediaPlayerNotify()
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
        setUpViews()
        setUpAdapter()
        setUpMediaPlayer()
        setUpListeners()
        setUpObservers()
        initCheckPermission()
        activity?.bindService(
            startOrUpdateService(),
            this,
            BIND_AUTO_CREATE
        )
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


    private fun setUpMediaPlayer(){
        activity?.let {
             exoPlayer=ExoPlayer.Builder(requireContext())
                .build()
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
                Log.e("TIME",currentTime.third.toString() )
                bind.seekbarControl.tvInitTime.text = currentTime.third
                bind.seekbarControl.loadSeekBar.progress = exoPlayer.currentPosition.toInt()
                updateMediaPlayerNotify()
            }

        }
        mainViewModel.currentSongListPosition.observe(viewLifecycleOwner){positionSelected->
            currentSelectedPosition = positionSelected
        }
    }
    private fun setUpViews()=with(bind){

    }
    private fun updateMediaPlayerNotify(){
        // update media player notify info
        currentMusicState = currentMusicState.copy(
            isPlaying = exoPlayer.isPlaying,
            currentDuration = exoPlayer.currentPosition.toLong(),
            duration = exoPlayer.duration.toLong()
        )
        startOrUpdateService()
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
            if(adapter.itemCount>0) {
                if(!isPlaying)getSongOfAdapter(currentSelectedPosition)?.let{song->startSongPlayer(song)}
                else {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause(); bottomPlayerControls.btnPlay.setIconResource(coreRes.drawable.ic_play)
                    } else {
                        exoPlayer.play(); bottomPlayerControls.btnPlay.setIconResource(coreRes.drawable.ic_pause)
                    }
                }
            }
            updateMediaPlayerNotify()
            isPlaying=true
        }

        bottomPlayerControls.btnPrevious.setOnClickListener{
             if (currentSelectedPosition > 0) {
                getSongOfAdapter(currentSelectedPosition - 1)?.let{song->startSongPlayer(song)}
             }

        }
        bottomPlayerControls.btnNext.setOnClickListener {
           if(currentSelectedPosition<adapter.itemCount-1){
               getSongOfAdapter(currentSelectedPosition +1)?.let{song->startSongPlayer(song)}
           }
        }
        bind.seekbarControl.loadSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                exoPlayer.seekTo(bind.seekbarControl.loadSeekBar.progress.toLong())
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
        isPlaying=true
        mainViewModel.setCurrentPosition(position)
        startSongPlayer(song)
    }
    private fun startSongPlayer(song: SongEntity){
        activity?.let {context->
           // try {
                checkPermissions(context,
                    listOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO)
                ){isGranted,permissionsList->
                    if(isGranted){
                        if(exoPlayer.isPlaying){
                            exoPlayer.stop()
                            exoPlayer= ExoPlayer.Builder(requireContext())
                                .build()
                            bind.bottomPlayerControls.btnPlay.setIconResource(coreRes.drawable.ic_play)
                        }
                        exoPlayer.addListener(object: Player.Listener{
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                super.onPlaybackStateChanged(playbackState)
                                if (playbackState == Player.STATE_READY && exoPlayer.duration > 0) {
                                    val durationInMillis = exoPlayer.duration
                                    val formattedDuration = createTime(durationInMillis).third
                                    bind.seekbarControl.tvEndTime.text = formattedDuration
                                    bind.seekbarControl.loadSeekBar.max = durationInMillis.toInt()
                                    bind.seekbarControl.loadSeekBar.max=exoPlayer.duration.toInt()
                                    val songMetadata=getSongCover(activity!!,song.pathLocation)

                                    // Set info currentSongEntity

                                    currentMusicState = MusicState(
                                        //isPlaying=mediaPlayer.isPlaying,
                                        isPlaying=exoPlayer.isPlaying,
                                        title = song.pathLocation?.substringAfterLast("/","No named")!!,
                                        artist = songMetadata!!.artist,
                                        album = songMetadata!!.album,
                                        //duration =(mediaPlayer.duration).toLong()
                                        albumArt = songMetadata!!.albumArt,
                                        duration =(exoPlayer.duration).toLong()

                                    )
                                    bind.ivCover.setImageBitmap(songMetadata!!.albumArt)

                                    mainViewModel.fetchCurrentTimeOfSong(exoPlayer)

                                }
                                if(playbackState == Player.STATE_ENDED){
                                    if(currentSelectedPosition < adapter.itemCount){
                                        bind.bottomPlayerControls.btnNext.performClick()
                                    }else {
                                        bind.bottomPlayerControls.btnPlay.setIconResource(coreRes.drawable.ic_play)
                                    }
                                }
                            }
                        })

                        exoPlayer.addMediaItem(MediaItem.fromUri(song.pathLocation!!))
                        exoPlayer.prepare()
                        exoPlayer.play()
                        bind.bottomPlayerControls.btnPlay.setIconResource(coreRes.drawable.ic_pause)

                    }else{
                        permissionsList.forEach {permission->
                            if(!permission.second) {
                                launcherPermission.launch(permission.first)
                            }
                        }
                    }
              }
            /*   }catch (e:Exception){
                 Log.e("ERROR_MEDIA_PLAYER", e.message.toString() )
                 Toast.makeText(context, "Error al reproducir", Toast.LENGTH_SHORT).show()
             }*/
        }

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
        //musicPlayerService!!.setActivity(context as AppCompatActivity)
        musicPlayerService!!.setSongController(songController)

    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicPlayerService = null
    }
/*
    override fun onStart() {
        super.onStart()
        if(!requireContext().isServiceRunning(MyBackgroundService::class.java)) {
            requireContext().bindService(Intent(requireContext(), MyBackgroundService::class.java),
                this,
                Context.BIND_AUTO_CREATE
            )
            myService?.registerOnPomodoroListener(callBackTimer)
        }
    }*/
    override fun onDestroy() {
        super.onDestroy()
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