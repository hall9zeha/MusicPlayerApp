package com.barryzeha.ktmusicplayer.view.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.barryzeha.core.model.entities.PlaylistEntity
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.databinding.PlaylistItemBinding


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/10/24.
 * Copyright (c)  All rights reserved.
 **/

class PlayListsAdapter(private val onItemClick:(PlaylistEntity)->Unit,
                       private val deletePlaylistCallback:(PlaylistEntity)->Unit,
                       private val editedNameCallback:(PlaylistEntity)->Unit):RecyclerView.Adapter<PlayListsAdapter.ViewHolder>() {
    private var playLists:MutableList<PlaylistEntity> = arrayListOf()
    private var isEdit:Boolean = false
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.playlist_item,parent,false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.onBind(playLists[position])
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        when(val latestPayload=payloads.lastOrNull()){
            is ItemPlaylistChangePayload.PlaylistName -> holder.setPlaylistName(latestPayload.name)
            else->onBindViewHolder(holder,position)
        }
    }

    override fun getItemCount(): Int = playLists.size

    fun addAll(playlists:List<PlaylistEntity>){
        playlists.forEach {
            add(it)
        }
    }
    fun add(playList:PlaylistEntity){
        if(!playLists.contains(playList)){
            playLists.add(playList)
            notifyItemInserted(playLists.size -1)
        }else{
            update(playList)
        }

    }
    fun update(playList: PlaylistEntity){
        if(playLists.contains(playList)){
            val index = playLists.indexOf(playList)
            playLists[index] = playList
            notifyItemChanged(index,ItemPlaylistChangePayload.PlaylistName(playList.playListName))
        }
    }
    fun remove(playList: PlaylistEntity){
        if(playLists.contains(playList)){
            val index = playLists.indexOf(playList)
            playLists.removeAt(index)
            notifyItemRemoved(index)
        }
    }
    fun remove(idPlaylist:Int){
        try{
        val playlistIndex = playLists.indexOfFirst { it.idPlaylist == idPlaylist.toLong() }
        playLists.removeAt(playlistIndex)
        notifyItemRemoved(playlistIndex)
        }catch(e:Exception){
            Log.e("REMOVE_PLAYLIST_BY_INDEX_ERR", e.message.toString())
        }
    }

    private fun updateNameOfPlaylist(oldEntity: PlaylistEntity, newEntity:PlaylistEntity){
        if(playLists.contains(oldEntity)){
            val index = playLists.indexOf(oldEntity)
            notifyItemChanged(index, ItemPlaylistChangePayload.PlaylistName(newEntity.playListName))

        }
    }
    inner class ViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
        private val bind = PlaylistItemBinding.bind(itemView)
        fun onBind(playList:PlaylistEntity)=with(bind){
            if(playList.idPlaylist.toInt() ==0){
                btnDelete.visibility=View.INVISIBLE
                btnEdit.visibility=View.INVISIBLE
            }
            edtPlaylistName.setText(playList.playListName)
            edtPlaylistName.setOnClickListener {
                if(!isEdit) {
                    onItemClick(playList)
                }}
            btnDelete.setOnClickListener {
                deletePlaylistCallback(playList)
            }
            btnEdit.setOnClickListener{

                if(!isEdit) {
                    btnEdit.setIconResource(com.barryzeha.mfilepicker.R.drawable.ic_check)
                    edtPlaylistName.isFocusableInTouchMode=true
                    edtPlaylistName.requestFocus()
                    edtPlaylistName.setSelection(edtPlaylistName.length())

                    isEdit=true
                }else{
                    btnEdit.setIconResource(com.barryzeha.core.R.drawable.ic_edit)
                    val playListsUpdated= playList.copy(playListName = edtPlaylistName.text.toString())
                    updateNameOfPlaylist(playList,playListsUpdated)
                    edtPlaylistName.isFocusableInTouchMode=false
                    edtPlaylistName.clearFocus()
                    editedNameCallback(playListsUpdated)
                    edtPlaylistName.onEditorAction(EditorInfo.IME_ACTION_DONE)
                    isEdit = false
                }
            }
        }
        fun setPlaylistName(name:String){
            bind.edtPlaylistName.setText(name)
        }
    }
    private sealed interface ItemPlaylistChangePayload{
        data class PlaylistName(val name:String):ItemPlaylistChangePayload
    }
}