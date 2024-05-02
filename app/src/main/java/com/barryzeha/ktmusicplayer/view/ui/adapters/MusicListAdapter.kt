package com.barryzeha.ktmusicplayer.view.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.barryzeha.core.entities.SongEntity
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.databinding.ItemSongBinding


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 26/4/24.
 * Copyright (c)  All rights reserved.
 **/

class MusicListAdapter(): RecyclerView.Adapter<MusicListAdapter.MViewHolder>() {

    private var songList:MutableList<SongEntity> = arrayListOf()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_song,parent,false)
        return MViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MViewHolder, position: Int) {
        holder.onBind(songList[position])
    }

    override fun getItemCount() = songList.size

    fun addAll(songs:List<SongEntity>){
        songs.forEach {
           add(it)
        }
    }
    fun add(song:SongEntity){
        if(!songList.contains(song)){
            songList.add(song)
            notifyItemInserted(songList.size -1)
        }else{
            val position = songList.indexOf(song)
            songList[position] = song
            notifyItemChanged(position)
        }
    }
    inner class MViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
        private val bind = ItemSongBinding.bind(itemView)
        fun onBind(song:SongEntity) = with(bind){
           tvSongDesc.text=song.pathLocation
        }
    }

}