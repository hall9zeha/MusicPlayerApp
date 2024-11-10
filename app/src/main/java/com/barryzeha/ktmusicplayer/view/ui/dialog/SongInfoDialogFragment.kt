package com.barryzeha.ktmusicplayer.view.ui.dialog

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.Layout.Alignment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.get
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import com.barryzeha.core.common.mColorList
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.core.R as coreRes
import com.barryzeha.ktmusicplayer.databinding.SongInfoLayoutBinding


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 8/11/24.
 * Copyright (c)  All rights reserved.
 **/

class SongInfoDialogFragment : DialogFragment() {
    private var _bind: SongInfoLayoutBinding? = null
    private val bind: SongInfoLayoutBinding get() = _bind!!
    private var isEditing: Boolean = false

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
                }
                coreRes.id.itemSave->{
                    menu[0].setVisible(true)
                    menu[1].setVisible(false)
                    isEditing = false
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
}