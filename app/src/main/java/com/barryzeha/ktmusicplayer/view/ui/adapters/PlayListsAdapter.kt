package com.barryzeha.ktmusicplayer.view.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.barryzeha.core.model.entities.PlaylistEntity
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.databinding.PlaylistItemBinding


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/10/24.
 * Copyright (c)  All rights reserved.
 **/

class PlayListsAdapter(private val onItemClick:(PlaylistEntity)->Unit):RecyclerView.Adapter<PlayListsAdapter.ViewHolder>() {
    private var playLists:MutableList<PlaylistEntity> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.playlist_item,parent,false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.onBind(playLists[position])
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        when(val latestPayload=payloads.lastOrNull()){
            is ItemPlaylistChangePayload.playlistName -> holder.setPlaylistName(latestPayload.name)
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
            notifyItemChanged(index,ItemPlaylistChangePayload.playlistName(playList.playListName))
        }
    }
    fun remove(playList: PlaylistEntity){
        if(playLists.contains(playList)){
            val index = playLists.indexOf(playList)
            playLists.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    inner class ViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
        private val bind = PlaylistItemBinding.bind(itemView)
        fun onBind(playList:PlaylistEntity)=with(bind){
            tvPlaylistName.text=playList.playListName
            this.root.setOnClickListener { onItemClick(playList)}
        }
        fun setPlaylistName(name:String){
            bind.tvPlaylistName.text = name
        }
    }
    private sealed interface ItemPlaylistChangePayload{
        data class playlistName(val name:String):ItemPlaylistChangePayload
    }
}