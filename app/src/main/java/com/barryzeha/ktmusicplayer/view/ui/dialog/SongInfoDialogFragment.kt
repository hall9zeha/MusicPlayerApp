package com.barryzeha.ktmusicplayer.view.ui.dialog

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.get
import androidx.fragment.app.DialogFragment
import com.barryzeha.core.common.SONG_INFO_EXTRA_KEY
import com.barryzeha.core.common.createTime
import com.barryzeha.core.common.fetchFileMetadata
import com.barryzeha.core.common.getSongMetadata
import com.barryzeha.core.common.loadImage
import com.barryzeha.core.common.mColorList
import com.barryzeha.ktmusicplayer.databinding.SongInfoLayoutBinding
import dagger.hilt.android.AndroidEntryPoint
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
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
    private val getImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()){uri: Uri?->
            uri?.let{
                val galleryUri=it
                try{
                    bind.ivSongDetail.loadImage(getPathFromUri(galleryUri)!!)
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

        /*edtTitle.isEnabled=isEnable
        edtArtist.isEnabled=isEnable
        edtAlbum.isEnabled=isEnable
        edtGenre.isEnabled=isEnable
        edtYear.isEnabled=isEnable*/
        if(!isEnable){
            edtTitle.clearFocus()
            edtArtist.clearFocus()
            edtAlbum.clearFocus()
            edtGenre.clearFocus()
            edtYear.clearFocus()
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
            val metadata = fetchFileMetadata(requireContext(),filePath)
            val songInfo = getSongMetadata(requireContext(),filePath)
            metadata?.let{meta->
                toolbarInfo.subtitle=meta.title
                songInfo?.let { ivSongDetail.loadImage(songInfo.albumArt) }
                tvDuration.text = createTime(meta.songLength).third
                tvBitrate.text=getString(coreRes.string.file_meta_info,meta.bitRate,meta.freq,"Stereo")
                tvSizeFile.text=meta.fileSize
                tvFileFormat.text=meta.format
                edtTitle.setText(meta.title)
                edtArtist.setText(meta.artist)
                edtAlbum.setText(meta.album)
                edtGenre.setText(meta.genre)
                edtYear.setText(meta.year)

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
        //TODO, implementar el guardado de metadatos del archivo, aún no funciona
        filePath?.let{
            try{
                val audioFile = AudioFileIO.read(File(filePath))
                val tag =audioFile.tag

                tag.setField(FieldKey.TITLE,bind.edtTitle.text.toString())
                tag.setField(FieldKey.ARTIST,bind.edtArtist.text.toString())
                tag.setField(FieldKey.ALBUM,bind.edtAlbum.text.toString())
                tag.setField(FieldKey.GENRE,bind.edtGenre.text.toString())
                tag.setField(FieldKey.YEAR,bind.edtYear.text.toString())
                audioFile.tag = tag
                AudioFileIO.write(audioFile)

            }catch(ex:Exception){
                ex.printStackTrace()
                Log.e("EDIT-TAG-ERROR", "${ex.message}")
            }
        }

    }
    private fun getPathFromUri(uri: Uri): String? {
        val cursor = activity?.contentResolver?.query(uri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)
        cursor?.moveToFirst()
        val columnIndex = cursor?.getColumnIndex(MediaStore.Images.Media.DATA)
        val filePath = columnIndex?.let { cursor.getString(it) }
        cursor?.close()
        return filePath
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