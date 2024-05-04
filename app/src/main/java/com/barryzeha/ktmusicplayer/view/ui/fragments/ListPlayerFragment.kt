package com.barryzeha.ktmusicplayer.view.ui.fragments

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
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
import com.barryzeha.core.common.checkPermissions
import com.barryzeha.core.common.showSnackBar
import com.barryzeha.core.entities.SongEntity
import com.barryzeha.ktmusicplayer.databinding.FragmentListPlayerBinding
import com.barryzeha.ktmusicplayer.view.ui.adapters.MusicListAdapter
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Date


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
        setUpAdapter()
        setUpMediaPlayer()
        setUpListeners()
        setUpObservers()
    }

    private fun activityResultFile(){
        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result:ActivityResult->
            if(result.resultCode == Activity.RESULT_OK){
                uri = result.data?.data
                val probe = getRealPathFromURI(uri!!, requireContext())
                mainViewModel.saveNewSong(SongEntity(
                    pathLocation = probe,
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
       launcherPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()){
            if(it){
                launcher.launch(intent)
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

    private fun onItemClick(song:SongEntity){
        Log.e("URI", Uri.parse(song.pathLocation).toString() )
        Log.e("URI", song.pathLocation.toString() )
        activity?.let {context->
            //try {
                checkPermissions(context,Manifest.permission.READ_EXTERNAL_STORAGE){
                    if(it){
                        //val mediaPlayer = MediaPlayer()

                        mediaPlayer.setDataSource(song.pathLocation)
                        mediaPlayer.prepare()
                        mediaPlayer.start()


                    }else{
                        launcherPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }

            /*}catch (e:Exception){
                Log.e("ERROR_MEDIA_PLAYER", e.message.toString() )
                Toast.makeText(context, "Error al reproducir", Toast.LENGTH_SHORT).show()
            }*/
        }
    }
    fun getRealPathFromURI(uri: Uri, context: Context): String? {
        val returnCursor = context.contentResolver.query(uri, null, null, null, null)
        val nameIndex =  returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE)
        returnCursor.moveToFirst()
        val name = returnCursor.getString(nameIndex)
        val size = returnCursor.getLong(sizeIndex).toString()
        val file = File(context.filesDir, name)
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            var read = 0
            val maxBufferSize = 1 * 1024 * 1024
            val bytesAvailable: Int = inputStream?.available() ?: 0
            //int bufferSize = 1024;
            val bufferSize = Math.min(bytesAvailable, maxBufferSize)
            val buffers = ByteArray(bufferSize)
            while (inputStream?.read(buffers).also {
                    if (it != null) {
                        read = it
                    }
                } != -1) {
                outputStream.write(buffers, 0, read)
            }
            Log.e("File Size", "Size " + file.length())
            inputStream?.close()
            outputStream.close()
            Log.e("File Path", "Path " + file.path)

        } catch (e: java.lang.Exception) {
            Log.e("Exception", e.message!!)
        }
        return file.path
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