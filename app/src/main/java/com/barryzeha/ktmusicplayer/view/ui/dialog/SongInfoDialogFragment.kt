package com.barryzeha.ktmusicplayer.view.ui.dialog

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.get
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.common.SONG_INFO_EXTRA_KEY
import com.barryzeha.core.common.createTime
import com.barryzeha.core.common.fetchCompleteFileMetadata
import com.barryzeha.core.common.getSongMetadata
import com.barryzeha.core.common.loadImage
import com.barryzeha.core.common.mColorList
import com.barryzeha.core.common.showSnackBar
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.ktmusicplayer.common.processSongPaths
import com.barryzeha.ktmusicplayer.databinding.SongInfoLayoutBinding
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.id3.ID3v24Tag
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import com.barryzeha.core.R as coreRes


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 8/11/24.
 * Copyright (c)  All rights reserved.
 **/
@AndroidEntryPoint
class SongInfoDialogFragment : DialogFragment() {

    @Inject
    lateinit var mPrefs:MyPreferences
    private val viewModel:MainViewModel by activityViewModels()
    private var _bind: SongInfoLayoutBinding? = null
    private val bind: SongInfoLayoutBinding get() = _bind!!
    private var isEditing: Boolean = false
    private var idSong:Long=-1
    private var pathFile:String?=null
    private var imagePath:String?=null
    private var songEntity:SongEntity? = null
    private var songIfFavorite:Boolean=false
    private val getImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()){uri: Uri?->
            uri?.let{
                val galleryUri=it
                try{
                     getPathOfImageCoverFromUri(galleryUri){ path->
                         imagePath = path
                         imagePath?.let{bind.ivSongDetail.loadImage(imagePath!!)}
                     }
                }catch(ex:Exception){
                    ex.printStackTrace()
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, coreRes.style.myFullScreenDialog)
    }
    @SuppressLint("ResourceType")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity?.let {
            _bind = SongInfoLayoutBinding.inflate(inflater, container, false)
            _bind?.let { b ->
                b.toolbarInfo.setNavigationIcon(coreRes.drawable.ic_arrow_back)
                b.toolbarInfo.title = getString(coreRes.string.songInfo)
                b.toolbarInfo.subtitle = "Song name"
                b.toolbarInfo.setNavigationOnClickListener {
                    dismiss()
                }

                dialog?.window?.apply {
                    statusBarColor = mColorList(context).getColor(4, 1)
                }
                return b.root
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenuProvider()
        clearInternalAppFilesDir()
        getIntentExtras()
        setupListeners()
        setupObservers()
    }
    private fun getIntentExtras(){
        arguments?.let{
            songEntity = it.getParcelable(SONG_INFO_EXTRA_KEY)
            songEntity?.pathLocation?.let{path->
                pathFile=path
                setFileInfo(pathFile)
            }
        }
    }
    private fun setupObservers(){
        songEntity?.let {song->
            viewModel.checkSongInfoIfIsFavorite(song.id)
            viewModel.songInfoIsFavorite.observe(viewLifecycleOwner){isFav->
                songIfFavorite=isFav
            }
        }
    }
    private fun showEditViews(isEnable:Boolean)=with(bind){
        ivSongDetail.isEnabled = isEnable

        if(!isEnable){
            edtTitle.clearFocus()
            edtArtist.clearFocus()
            edtAlbum.clearFocus()
            edtGenre.clearFocus()
            edtYear.clearFocus()
            imgButtonPickImage.visibility = View.GONE
            frmShadow.visibility = View.GONE
            lnMainInfo.visibility=View.VISIBLE
            lnMainInfoEdit.visibility=View.GONE
        }else{
            imgButtonPickImage.visibility = View.VISIBLE
            frmShadow.visibility = View.VISIBLE
            lnMainInfo.visibility=View.GONE
            lnMainInfoEdit.visibility=View.VISIBLE
        }
    }
    private fun enableViews(isEnable:Boolean)=with(bind){
        tilTitle.isEnabled=isEnable
        tilArtist.isEnabled=isEnable
        tilAlbum.isEnabled=isEnable
        tilGenre.isEnabled=isEnable
        tilYear.isEnabled=isEnable
        tilNumTrack.isEnabled=isEnable
        pbEdit.visibility = if(isEnable) View.GONE else View.VISIBLE
    }
    private fun clearInternalAppFilesDir(){
        try{
           val files = requireContext().filesDir
            files.listFiles()?.forEach { file->
                file.delete()
            }
        }catch(ex:Exception){
            ex.printStackTrace()
        }
    }
    private fun setupListeners()=with(bind){
        ivSongDetail.setOnClickListener {
            getImageLauncher.launch("image/*")
        }
        imgButtonPickImage.setOnClickListener {
            getImageLauncher.launch("image/*")
        }
    }
    private fun setFileInfo(filePath:String?)=with(bind){
        showEditViews(false)
        filePath?.let {
            val inputFile = File(filePath)
            val metadata = fetchCompleteFileMetadata(requireContext(),filePath)
            val songInfo = getSongMetadata(requireContext(),filePath, withBitmap = true)
            metadata?.let{meta->
                toolbarInfo.subtitle=meta.title
                songInfo?.let { ivSongDetail.loadImage(songInfo.albumArt) }
                tvDuration.text = createTime(meta.songLength).third
                tvBitrate.text=getString(coreRes.string.file_meta_info,meta.bitRate)
                tvFreq.text = "${meta.freq} Khz"
                tvChannel.text= meta.channels
                tvSizeFile.text=meta.fileSize
                tvFileFormat.text=inputFile.extension.uppercase()
                edtTitle.setText(meta.title)
                edtArtist.setText(meta.artist)
                edtAlbum.setText(meta.album)
                edtGenre.setText(meta.genre)
                edtYear.setText(meta.year)
                edtNumTrack.setText(meta.track)

                //
                tvTitle.text=meta.title
                tvArtist.text=meta.artist
                tvAlbum.text=meta.album
                tvGenre.text=meta.genre
                tvYear.text=meta.year
                tvNumberTrack.text= meta.track
                tvFileLocation.text= filePath
            }
        }
    }
    private fun setupMenuProvider() {
        //TODO When using android.widget.Toolbar it inflates the menu but without icons.At the moment androidx.appcompat.widget.Toolbar
        // allows us to use this type of configuration. Find out more about the issue.
        bind.toolbarInfo.inflateMenu(coreRes.menu.song_info_menu)
        val menu=bind.toolbarInfo.menu
        menu[1].setVisible(false)
        bind.toolbarInfo.setOnMenuItemClickListener {menuItem->
            when(menuItem.itemId){
                coreRes.id.itemEdit->{
                    menu[0].setVisible(false)
                    menu[1].setVisible(true)
                    isEditing = true
                    showEditViews(true)
                }
                coreRes.id.itemSave->{
                    menu[0].setVisible(true)
                    menu[1].setVisible(false)
                    pathFile?.let{editAudioFileMetadata(it)}
                }
            }
            true
        }
    }
     private fun editAudioFileMetadata(filePath: String?){
         val title = bind.edtTitle.text.toString()
         val artist =bind.edtArtist.text.toString()
         val album = bind.edtAlbum.text.toString()
         val genre = bind.edtGenre.text.toString()
         val year = bind.edtYear.text.toString()
         val numTrack = bind.edtNumTrack.text.toString()

         filePath?.let{
            try{
                lifecycleScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        enableViews(false)
                    }
                    val audioFile: AudioFile = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                        AudioFileIO.read(File(filePath))
                    } else {
                        AudioFileIO.read(getPathOfAppDirectory(filePath))
                    }
                    var tag = audioFile.tag
                    if (tag == null) {
                        // If the file has no metadata tags, we create them empty.
                        tag = ID3v24Tag()
                    }

                    if (title.isNotEmpty()) {
                        try {
                            tag.getFirst(FieldKey.TITLE)
                            tag.setField(FieldKey.TITLE, title)
                        } catch (ex: Exception) {
                            tag.createField(FieldKey.TITLE, title)
                        }
                    }
                    if (artist.isNotEmpty()) {
                        tag.setField(FieldKey.ARTIST, artist)
                    }
                    if (album.isNotEmpty()) {
                        tag.setField(FieldKey.ALBUM, album)
                    }
                    if (genre.isNotEmpty()) {
                        tag.setField(FieldKey.GENRE, genre)
                    }
                    if (year.isNotEmpty()) {
                        tag.setField(FieldKey.YEAR, year)
                    }
                    if (numTrack.isNotEmpty()) {
                        tag.setField(FieldKey.TRACK, numTrack)
                    }
                    if (imagePath != null) {
                        tag.deleteArtworkField()
                        val artwork = ArtworkFactory.createArtworkFromFile(File(imagePath!!))
                        tag.setField(artwork)

                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        audioFile.tag = tag
                        audioFile.commit()
                        saveFileEdited(filePath, {
                            isEditing = false
                            showEditViews(false)
                            enableViews(true)
                            activity?.showSnackBar(bind.root, coreRes.string.editFileSuccess)
                            pathFile?.let { setFileInfo(it) }
                            processSongPaths(listOf(pathFile.toString()), {}, { song ->
                                viewModel.setIsSongTagEdited(
                                    song.copy(
                                        id = songEntity!!.id,
                                        favorite = songIfFavorite
                                    )
                                )
                            })
                        }, {
                        })
                    } else {
                        audioFile.tag = tag
                        audioFile.commit()
                        isEditing = false
                        withContext(Dispatchers.Main) {
                            showEditViews(false)
                            enableViews(true)
                            processSongPaths(listOf(pathFile.toString()), {}, { song ->
                                viewModel.setIsSongTagEdited(
                                    song.copy(
                                        id = songEntity!!.id,
                                        favorite = songIfFavorite
                                    )
                                )
                            })
                            activity?.showSnackBar(
                                bind.root,
                                coreRes.string.editFileSuccess,
                                Snackbar.LENGTH_LONG
                            )
                        }
                    }
                }

            }catch(ex:Exception){
                isEditing = false
                showEditViews(false)
                enableViews(true)
                ex.printStackTrace()
                Log.e("EDIT_TAG_ERROR", "${ex.message}")
                activity?.showSnackBar(bind.root,coreRes.string.editFileMsgError, Snackbar.LENGTH_LONG)
            }
        }

    }

    private fun getPathOfImageCoverFromUri(uri: Uri, pathFile:(path:String?)->Unit ){
        try {
            CoroutineScope(Dispatchers.IO).launch{
            val oldFile = File(requireContext().filesDir, "cover.jpg")
            if(oldFile.exists())oldFile.delete()
            val inputStream = requireContext().contentResolver?.openInputStream(uri)
            val file = File(requireContext().filesDir, "cover.jpg") // We use a temporary file in the cache
            val outputStream = FileOutputStream(file)

            //Copy data from InputStream to OutputStream
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            withContext(Dispatchers.Main) {
                pathFile(file.absolutePath) // We return the path of the temporary file
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return pathFile(null)
        }
        return pathFile(null)
    }
    private fun getPathOfAppDirectory(originalPath:String):File{
        val songName = originalPath.substringAfterLast("/")
        val inputFile = File(originalPath)
        val outputFile = File(requireContext().filesDir, songName)
        val inputStream = FileInputStream(inputFile)
        val outputStream = FileOutputStream(outputFile)

        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()

        return outputFile
    }
   private fun saveFileEdited(
       originalPathFile: String,
       onSuccess: () -> Unit,
       onError: () -> Unit
   ) {
       CoroutineScope(Dispatchers.IO).launch {
           try {
               val fileName = originalPathFile.substringAfterLast("/")

               //File edited in the application's temporary directory
               val editedFile = File(requireContext().filesDir, fileName)

               if (!editedFile.exists()) {
                   throw IOException("Edited file does not exist")
               }

               // Actual file with original address
               val originalFile = File(originalPathFile)

               if (!originalFile.exists()) {
                   throw IOException("Original file does not exist")
               }

               //Verify writing permit
               if (!originalFile.canWrite()) {
                   throw IOException("No write permission for: $originalPathFile")
               }

               // Directly overwrite
               editedFile.copyTo(originalFile, overwrite = true)
               withContext(Dispatchers.Main) {
                   clearInternalAppFilesDir()
                   onSuccess()
               }

           } catch (ex: Exception) {
               ex.printStackTrace()
               Log.e("EDIT_TAG_ERROR", ex.message ?: "Unknown error")
               withContext(Dispatchers.Main) {
                   clearInternalAppFilesDir()
                   onError()

                   activity?.showSnackBar(
                       bind.root,
                       coreRes.string.editFileMsgError,
                       Snackbar.LENGTH_LONG
                   )
               }
           }
       }
   }
   companion object{
        @JvmStatic
        fun newInstance(songEntity:SongEntity)=SongInfoDialogFragment().apply {
            arguments = Bundle().apply {
                putParcelable(SONG_INFO_EXTRA_KEY,songEntity)
            }
        }
    }

}