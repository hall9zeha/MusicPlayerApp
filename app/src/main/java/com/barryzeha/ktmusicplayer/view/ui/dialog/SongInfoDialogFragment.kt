package com.barryzeha.ktmusicplayer.view.ui.dialog

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.get
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.barryzeha.core.common.MyPreferences
import com.barryzeha.core.common.SONG_INFO_EXTRA_KEY
import com.barryzeha.core.common.createTime
import com.barryzeha.core.common.fetchFileMetadata
import com.barryzeha.core.common.getSongMetadata
import com.barryzeha.core.common.loadImage
import com.barryzeha.core.common.mColorList
import com.barryzeha.core.common.showSnackBar
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.ktmusicplayer.databinding.SongInfoLayoutBinding
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import com.barryzeha.mfilepicker.common.util.getParentDirectories
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
    private val openDocumentTreeLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()){uri:Uri?->
        uri?.let{
            handleUriSAFSelection(uri)
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
            songEntity = it.getParcelable(SONG_INFO_EXTRA_KEY)
            songEntity?.pathLocation?.let{path->
                pathFile=path
                setFileInfo(pathFile)
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
            lnMainInfo.visibility=View.VISIBLE
            lnMainInfoEdit.visibility=View.GONE
        }else{
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
    }
    private fun setFileInfo(filePath:String?)=with(bind){
        showEditViews(false)
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

    private fun handleUriSAFSelection(treeUri:Uri){
        // Guardamos la uri del directorio para uso posterior
        mPrefs.directorySAFUri = treeUri.toString()
        // Conceder permisos persistentes para que no sea necesario pedir acceso nuevamente.
        requireContext().contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        saveFileEdited(pathFile!!,{
            isEditing = false
            showEditViews(false)
            activity?.showSnackBar(bind.root, coreRes.string.editFileSuccess)
            pathFile?.let { setFileInfo(it) }
            viewModel.setIsSongTagEdited(songEntity!!)

        },{})
    }

    private fun editAudioFileMetadata(filePath: String?){
        filePath?.let{
            try{

                val audioFile:AudioFile = if(Build.VERSION.SDK_INT<=Build.VERSION_CODES.Q) {
                    AudioFileIO.read(File(filePath))
                }else{
                    AudioFileIO.read(getPathOfAppDirectory(filePath))
                }
                var tag =audioFile.tag
                if(tag==null){
                    // Si el archivo no tiene etiquetas de metadatos las creamos vacías
                    tag = ID3v24Tag()
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
                    tag.deleteArtworkField()
                    val artwork = ArtworkFactory.createArtworkFromFile(File(imagePath!!))
                    tag.setField(artwork)

                }
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                    //openDocumentTreeLauncher.launch(null)
                    if(mPrefs.directorySAFUri?.isEmpty()!!) {
                        openDocumentTreeLauncher.launch(null)
                    }else{
                        audioFile.tag = tag
                        audioFile.commit()
                        enableViews(false)
                        saveFileEdited(filePath,{
                            isEditing = false
                            showEditViews(false)
                            enableViews(true)
                            activity?.showSnackBar(bind.root,coreRes.string.editFileSuccess)
                            pathFile?.let{setFileInfo(it)}
                            viewModel.setIsSongTagEdited(songEntity!!)
                            },{
                    })

                }}else {
                    audioFile.tag = tag
                    audioFile.commit()
                    isEditing = false
                    showEditViews(false)
                    viewModel.setIsSongTagEdited(songEntity!!)
                    activity?.showSnackBar(bind.root,coreRes.string.editFileSuccess, Snackbar.LENGTH_LONG)
                }
                //AudioFileIO.write(audioFile)
            }catch(ex:Exception){
                isEditing = false
                showEditViews(false)
                ex.printStackTrace()
                Log.e("EDIT-TAG-ERROR", "${ex.message}")
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
    private fun saveFileEdited(originalPathFile: String, onSuccess:()->Unit, onError:()->Unit){
        //TODO refactorizar
        CoroutineScope(Dispatchers.IO).launch {
            try {

                val fileName = originalPathFile.substringAfterLast("/")
                val pathWithoutName = originalPathFile.substringBeforeLast("/")
                val songFileInternalPath = File(requireContext().filesDir, fileName)

                val contentResolver: ContentResolver = requireContext().contentResolver

                // Crear un archivo de entrada a partir de tu archivo editado (directorio interno)
                val inputFile = songFileInternalPath
                if (!inputFile.exists()) {
                    throw IOException("El archivo original no existe")
                }
                // Acceder al directorio seleccionado con SAF
                val documentFile =DocumentFile.fromTreeUri(requireContext(), Uri.parse(mPrefs.directorySAFUri))
                // Verificar si el directorio es válido
                if (documentFile != null) {
                    if (!documentFile.canWrite()) {
                        // Solicitar el permiso persistente nuevamente
                        openDocumentTreeLauncher.launch(null)
                    } else {
                        //TODO REFACTORIZAR
                        val uriPath = Uri.parse(mPrefs.directorySAFUri).path
                        val rootSAFDir = uriPath?.substringAfterLast(":")
                        val parentDir = getParentDirectories(originalPathFile)

                        val directory = if(rootSAFDir == parentDir) documentFile
                        else getSubdirectory(documentFile,parentDir.split("/"))

                        // Buscamos el archivo existente para eliminarlo y luego copiar el que tenemos editado

                        if (directory != null && directory.isDirectory) {
                            // Buscar el archivo existente con el mismo nombre
                            val existingFile = directory.findFile(fileName)
                            // Si el archivo existe, eliminarlo
                            existingFile?.delete()

                        }
                        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(songFileInternalPath.extension)

                        val newFile = directory?.createFile(
                            mimeType.toString().lowercase(),
                            fileName
                        )
                        // Comprobar si el archivo fue creado correctamente
                        if (newFile != null) {
                            // Abrir un OutputStream para el nuevo archivo
                            val outputStream = contentResolver.openOutputStream(newFile.uri)

                            // Abrir un InputStream para el archivo original (editado)
                            val inputStream = FileInputStream(inputFile)

                            // Copiar el contenido del archivo original al nuevo archivo
                            inputStream.use { input ->
                                outputStream?.use { output ->
                                    input.copyTo(output)
                                }
                            }
                            withContext(Dispatchers.Main){
                                clearInternalAppFilesDir()
                                onSuccess()

                            }
                        } else {
                            withContext(Dispatchers.Main){
                                clearInternalAppFilesDir()
                                onError()
                            }
                            throw IOException("No se pudo crear el archivo en el directorio seleccionado")
                        }
                    }
                } else {
                   withContext(Dispatchers.Main){
                        clearInternalAppFilesDir()
                        onError()
                    }
                    throw IOException("No se tiene permiso para escribir en el directorio seleccionado")

                }

           } catch (ex: Exception) {
                ex.printStackTrace()

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
    private fun getSubdirectory(root: DocumentFile,subDirs:List<String>): DocumentFile? {
        var currentDir: DocumentFile? = root
        for (subDir in subDirs) {
            currentDir = currentDir?.findFile(subDir)
            if (currentDir == null || !currentDir!!.isDirectory) {
                return null
            }
        }
        return currentDir
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