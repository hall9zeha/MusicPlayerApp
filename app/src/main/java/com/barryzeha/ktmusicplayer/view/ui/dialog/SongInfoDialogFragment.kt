package com.barryzeha.ktmusicplayer.view.ui.dialog

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.core.view.get
import androidx.fragment.app.DialogFragment
import com.barryzeha.core.common.SONG_INFO_EXTRA_KEY
import com.barryzeha.core.common.createTime
import com.barryzeha.core.common.fetchFileMetadata
import com.barryzeha.core.common.getSongMetadata
import com.barryzeha.core.common.loadImage
import com.barryzeha.core.common.mColorList
import com.barryzeha.core.common.showSnackBar
import com.barryzeha.ktmusicplayer.databinding.SongInfoLayoutBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.id3.ID3v23Tag
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import com.barryzeha.core.R as coreRes


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 8/11/24.
 * Copyright (c)  All rights reserved.
 **/
@AndroidEntryPoint
class SongInfoDialogFragment : DialogFragment() {
    private var _bind: SongInfoLayoutBinding? = null
    private val bind: SongInfoLayoutBinding get() = _bind!!
    private var isEditing: Boolean = false
    private var idSong:Long=-1
    private var pathFile:String?=null
    private var imagePath:String?=null
    private val getImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()){uri: Uri?->
            uri?.let{
                val galleryUri=it
                try{
                     getPathFromUri(galleryUri){path->
                         imagePath = path
                         bind.ivSongDetail.loadImage(imagePath!!)
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
                b.toolbarInfo.title = "Song info"
                b.toolbarInfo.subtitle = "Song name"
                b.toolbarInfo.setNavigationOnClickListener {
                    dismiss()
                }

                dialog?.window?.apply {
                    statusBarColor = mColorList(context).getColor(4, 1)
                }
                // Listeners of header icons
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
    }
    private fun getIntentExtras(){
        arguments?.let{
            pathFile= it.getString(SONG_INFO_EXTRA_KEY)
            setFileInfo(pathFile)

        }
    }
    private fun enableViews(isEnable:Boolean)=with(bind){
        edtTitle.isFocusableInTouchMode=isEnable
        edtArtist.isFocusableInTouchMode=isEnable
        edtAlbum.isFocusableInTouchMode=isEnable
        edtGenre.isFocusableInTouchMode=isEnable
        edtYear.isFocusableInTouchMode=isEnable
        ivSongDetail.isEnabled = isEnable

        if(!isEnable){
            edtTitle.clearFocus()
            edtArtist.clearFocus()
            edtAlbum.clearFocus()
            edtGenre.clearFocus()
            edtYear.clearFocus()
            lnMainInfo.visibility=View.VISIBLE
            lnMainInfoEdit.visibility=View.GONE
        }else{
            lnMainInfo.visibility=View.GONE
            lnMainInfoEdit.visibility=View.VISIBLE
        }

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
    }
    private fun setFileInfo(filePath:String?)=with(bind){
        enableViews(false)
        filePath?.let {
            val inputFile = File(filePath)
            val metadata = fetchFileMetadata(requireContext(),filePath)
            val songInfo = getSongMetadata(requireContext(),filePath)
            metadata?.let{meta->
                toolbarInfo.subtitle=meta.title
                songInfo?.let { ivSongDetail.loadImage(songInfo.albumArt) }
                tvDuration.text = createTime(meta.songLength).third
                tvBitrate.text=getString(coreRes.string.file_meta_info,meta.bitRate,meta.freq,"Stereo")
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

            }
        }
    }
    private fun setupMenuProvider() {
        //TODO Al usar android.widget.Toolbar nos infla el menú pero sin íconos
        // Por el momento androidx.appcompat.widget.Toolbar nos permite usar este tipo de configuración
        // averiguar más sobre el asunto
        bind.toolbarInfo.inflateMenu(coreRes.menu.song_info_menu)
        val menu=bind.toolbarInfo.menu
        menu[1].setVisible(false)
        bind.toolbarInfo.setOnMenuItemClickListener {menuItem->
            when(menuItem.itemId){
                coreRes.id.itemEdit->{
                    menu[0].setVisible(false)
                    menu[1].setVisible(true)
                    isEditing = true
                    enableViews(true)
                }
                coreRes.id.itemSave->{
                    menu[0].setVisible(true)
                    menu[1].setVisible(false)
                    pathFile?.let{editAudioFileMetadata(it)}
                    isEditing = false
                    enableViews(false)
                    pathFile?.let{setFileInfo(it)}
                }
            }
            true
        }

        // No funciona correctamente para nuestros propósitos

        /* val menuHost:MenuHost = requireActivity()
       menuHost.addMenuProvider(object:MenuProvider{
           override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
               menuInflater.inflate(R.menu.note_menu,menu)
           }
           override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
               return true
           }

       },viewLifecycleOwner, Lifecycle.State.RESUMED)*/
    }
    private fun editAudioFileMetadata(filePath: String?){
        //TODO, implementar el guardado de metadatos para android >=12

        filePath?.let{
            try{

                val audioFile:AudioFile = if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q) {
                    AudioFileIO.read(File(filePath))
                }else{
                    //AudioFileIO.read(getPathOfAppDirectory(filePath))
                    AudioFileIO.read(getAudioUriByName(filePath)?.toFile()!!)

                }
                var tag =audioFile.tag
                if(tag==null){
                    // Si el archivo no tiene etiquetas de metadatos las creamos vacías
                    tag = ID3v23Tag()
                }

                val title = bind.edtTitle.text.toString()
                val artist =bind.edtArtist.text.toString()
                val album = bind.edtAlbum.text.toString()
                val genre = bind.edtGenre.text.toString()
                val year = bind.edtYear.text.toString()
                val numTrack = bind.edtNumTrack.text.toString()

                if(title.isNotEmpty()){
                    try{tag.getFirst(FieldKey.TITLE)
                        tag.setField(FieldKey.TITLE,title)
                    }catch(ex:Exception){
                       tag.createField(FieldKey.TITLE,title)
                    }
                }
                if(artist.isNotEmpty()) {
                    tag.setField(FieldKey.ARTIST, artist)
                }
                if(album.isNotEmpty()) {
                    tag.setField(FieldKey.ALBUM, album)
                }
                if(genre.isNotEmpty()) {
                    tag.setField(FieldKey.GENRE, genre)
                }
                if(year.isNotEmpty()) {
                    tag.setField(FieldKey.YEAR, year)
                }
                if(numTrack.isNotEmpty()){
                    tag.setField(FieldKey.TRACK,numTrack)
                }
                if(imagePath !=null){
                    val artwork = ArtworkFactory.createArtworkFromFile(File(imagePath!!))
                    tag.setField(artwork)
                }
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                    audioFile.tag = tag
                    audioFile.commit()
                    activity?.showSnackBar(bind.root,coreRes.string.editFileSuccess, Snackbar.LENGTH_LONG)
                }else {
                    audioFile.tag = tag
                    audioFile.commit()
                    activity?.showSnackBar(bind.root,coreRes.string.editFileSuccess, Snackbar.LENGTH_LONG)
                }
                //AudioFileIO.write(audioFile)
            }catch(ex:Exception){
                ex.printStackTrace()
                Log.e("EDIT-TAG-ERROR", "${ex.message}")
                activity?.showSnackBar(bind.root,coreRes.string.editFileMsgError, Snackbar.LENGTH_LONG)
            }
        }

    }
    fun getAudioUriByName(originalPath: String): Uri? {
        val fileName = originalPath.substringAfterLast("/")
        val resolver = requireContext().contentResolver
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,  // El ID del archivo en MediaStore
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.MIME_TYPE
        )
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        // Buscar el archivo en MediaStore
        val queryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cursor = resolver.query(
            queryUri, projection, selection, selectionArgs, null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val idColumnIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val audioId = it.getLong(idColumnIndex)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioId
                )
                return contentUri
            }
        }

        return null
    }
    private fun getPathFromUri(uri: Uri,pathFile:(path:String?)->Unit ){
        try {
            CoroutineScope(Dispatchers.IO).launch{
            val oldFile = File(requireContext().filesDir, "cover.jpg")
            if(oldFile.exists())oldFile.delete()
            delay(200)

                val inputStream = requireContext().contentResolver?.openInputStream(uri)
                val file = File(requireContext().filesDir, "cover.jpg") // Usamos un archivo temporal en la caché
                val outputStream = FileOutputStream(file)

                // Copiar los datos de InputStream a OutputStream

                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()

                withContext(Dispatchers.Main) {
                    pathFile(file.absolutePath) // Devolvemos la ruta del archivo temporal
                }
            }


        } catch (e: Exception) {
            e.printStackTrace()
            return pathFile(null)
        }
        return pathFile(null)
    }
    private fun getPathOfAppDirectory(originalPath:String):File{
        val songName = pathFile?.substringAfterLast("/")
        val inputFile = File(pathFile!!)
        val outputFile = File(requireContext().filesDir, songName!!)
        val inputStream = FileInputStream(inputFile)
        val outputStream = FileOutputStream(outputFile)

        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()

        return outputFile
    }
    private fun saveFileEdited(originalPathFile:String){
        try{

            val fileName = originalPathFile.substringAfterLast("/")
            val pathWithoutName = originalPathFile.substringBeforeLast("/")
            val internalPathAppDir = File(requireContext().filesDir, fileName)

            val inputFile = File(originalPathFile)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(inputFile.extension)

            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, pathWithoutName)

            val  resolver = requireContext().contentResolver
            val collection = MediaStore.Files.getContentUri("external")
            val item = resolver.insert(collection,values)

            val outputStream = resolver.openOutputStream(item!!)

            val bufferedInputStream = BufferedInputStream(FileInputStream(inputFile))

            // Abrimos el BufferedOutputStream para escribir al archivo en el MediaStore
            val bufferedOutputStream = BufferedOutputStream(outputStream)

            // Crear un buffer de lectura y escritura
            val buffer = ByteArray(8192)  // Buffer de 8KB
            var bytesRead: Int

            // Leer del inputStream y escribir en el outputStream en bloques
            while (bufferedInputStream.read(buffer).also { bytesRead = it } != -1) {
                bufferedOutputStream.write(buffer, 0, bytesRead)
            }

            // Asegurarse de que todos los datos se escriban
            bufferedOutputStream.flush()

            // Cerrar ambos streams
            bufferedInputStream.close()
            bufferedOutputStream.close()

            /* val outputFile = File(requireContext().filesDir,fileName!!)
            val inputStream = FileInputStream(inputFile)
            val outputStream = FileOutputStream(outputFile)

            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()*/


        }catch(ex:Exception){
            ex.printStackTrace()
            activity?.showSnackBar(bind.root,coreRes.string.editFileMsgError,Snackbar.LENGTH_LONG)
        }
    }

    companion object{
        @JvmStatic
        fun newInstance(filePath:String)=SongInfoDialogFragment().apply {
            arguments = Bundle().apply {
                putString(SONG_INFO_EXTRA_KEY,filePath)
            }
        }
    }

}