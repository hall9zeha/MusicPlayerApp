package com.barryzeha.ktmusicplayer.view.ui.adapters


import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.media.MediaPlayer
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.barryzeha.core.common.adjustAlpha
import com.barryzeha.core.common.getBitrate
import com.barryzeha.core.common.getTimeOfSong
import com.barryzeha.core.common.mColorList
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.databinding.ItemSongBinding


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 26/4/24.
 * Copyright (c)  All rights reserved.
 **/

class MusicListAdapter(private val onItemClick:(Int, SongEntity)->Unit ,private val onMenuItemClick:(view:View,Int,SongEntity)->Unit): ListAdapter<SongEntity,MusicListAdapter.MViewHolder>(SongDiffCallback()) {

    private var selectedPos = -1
    private var lastSelectedPos = -1
    private lateinit  var context:Context
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MViewHolder {
        context=parent.context
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_song,parent,false)
        return MViewHolder(itemView)
    }

    @SuppressLint("ResourceType")
    override fun onBindViewHolder(holder: MViewHolder, position: Int) {
        if(selectedPos == position){
            holder.bind.root.setBackgroundColor(mColorList(context).getColor(2,0).adjustAlpha(0.3f))
        }else{
            holder.bind.root.setBackgroundColor(Color.TRANSPARENT)
        }
        holder.onBind(position, getItem(position))
    }
    fun changeBackgroundColorSelectedItem(position: Int){
        selectedPos = position
        if(lastSelectedPos == -1){
            lastSelectedPos = selectedPos
        }else{
            notifyItemChanged(lastSelectedPos)
            lastSelectedPos = selectedPos
        }
        notifyItemChanged(selectedPos)
    }
    fun addAll(songs:List<SongEntity>){
       submitList(songs)

    }
    fun add(song: SongEntity) {
        val currentList = currentList.toMutableList()
        if (!currentList.contains(song)) {
            currentList.add(song)
            submitList(currentList)
        }

    }
    fun remove(song:SongEntity){
        val currentList=currentList.toMutableList()
        if(currentList.contains(song)){
            val position = currentList.indexOf(song)
            currentList.removeAt(position)
            submitList(currentList)
        }
    }
    fun getSongByPosition(position: Int): SongEntity?{
        return if(currentList.isNotEmpty()){
            currentList[position]
        }else{
            null
        }
    }

    inner class MViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
        val bind = ItemSongBinding.bind(itemView)

        fun onBind(position:Int,song: SongEntity) = with(bind){
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(song.pathLocation)
            mediaPlayer.prepare()
            val info=mediaPlayer.trackInfo
            for(i in info){
                if(i.trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO)
                    tvBitrate.text=String.format("%s::kbps",getBitrate(song.pathLocation!!).toString())

            }
            tvSongDesc.text=String.format("%s. %s",(position+1),song.pathLocation?.substringAfterLast("/","No named"))
            tvDuration.text= getTimeOfSong( (mediaPlayer.duration).toLong())
            //tvFileFormat.text = String.format("::%s",song.pathLocation?.substring(song.pathLocation?.lastIndexOf(".")!! +1))
            tvFileFormat.text = String.format("::%s",song.pathLocation?.substringAfterLast(".","NA"))
            root.setOnClickListener {
                changeBackgroundColorSelectedItem(bindingAdapterPosition)
                onItemClick(position,song)

            }
            ivOptions.setOnClickListener { onMenuItemClick(it,position,song) }

        }

    }
    private class SongDiffCallback:DiffUtil.ItemCallback<SongEntity>(){
        override fun areItemsTheSame(oldItem: SongEntity, newItem: SongEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SongEntity, newItem: SongEntity): Boolean {
            return oldItem == newItem
        }
    }

}