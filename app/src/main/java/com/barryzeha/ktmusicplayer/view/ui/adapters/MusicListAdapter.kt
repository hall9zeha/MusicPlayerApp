package com.barryzeha.ktmusicplayer.view.ui.adapters


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.barryzeha.core.common.adjustAlpha
import com.barryzeha.core.common.fetchFileMetadata
import com.barryzeha.core.common.mColorList
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.ktmusicplayer.MyApp
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.databinding.ItemSongBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 26/4/24.
 * Copyright (c)  All rights reserved.
 **/

class MusicListAdapter(private val onItemClick:(Int, SongEntity)->Unit ,private val onMenuItemClick:(view:View,Int,SongEntity)->Unit): ListAdapter<SongEntity, MusicListAdapter.MViewHolder>(SongDiffCallback()) {
//class MusicListAdapter(private val onItemClick:(Int, SongEntity)->Unit ,private val onMenuItemClick:(view:View,Int,SongEntity)->Unit): RecyclerView.Adapter<MusicListAdapter.MViewHolder>(){
    //private var songList:MutableList<SongEntity> = arrayListOf()
    //private val asyncListDiffer = AsyncListDiffer(this,SongDiffCallback())
    private var selectedPos = -1
    private var lastSelectedPos = -1
    private  var context:Context = MyApp.context

    init{
        setHasStableIds(true)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MViewHolder {
        context=parent.context
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_song,parent,false)
        return MViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MViewHolder, position: Int, payloads: MutableList<Any>) {
             when (val latestPayload = payloads.lastOrNull()) {
                is SongChangePayload.BackgroundColor -> holder.bindBackgroundColor(latestPayload.color)
                else -> onBindViewHolder(holder, position)
            }

    }
    override fun getItemId(position: Int): Long = currentList[position].id

    @SuppressLint("ResourceType")
    override fun onBindViewHolder(holder: MViewHolder, position: Int) {
        try {
            if (selectedPos == position) {
                holder.bind.root.setBackgroundColor(
                    mColorList(context).getColor(2, 0).adjustAlpha(0.3f)
                )
            } else {
                holder.bind.root.setBackgroundColor(Color.TRANSPARENT)
            }

        }finally {
            mColorList(context).recycle()
        }
        holder.onBind(position, getItem(position))
        //holder.onBind(position, songList[position])
        //holder.onBind(position, asyncListDiffer.currentList[position])
    }

    @SuppressLint("ResourceType")
    fun changeBackgroundColorSelectedItem(position: Int){
        selectedPos = position
        if(lastSelectedPos == -1){
            lastSelectedPos = selectedPos
        }else{
            notifyItemChanged(lastSelectedPos,Color.TRANSPARENT)
            lastSelectedPos = selectedPos
        }
        notifyItemChanged(selectedPos,SongChangePayload.BackgroundColor(mColorList(context).getColor(2,0).adjustAlpha(0.3f)))
    }
    fun addAll(songs:List<SongEntity>){
        submitList(songs)
        //asyncListDiffer.submitList(songs)
       /* songs.forEach {
            add(it)
        }*/
    }
    // Al usar DiffUtils o asyncListDiffer para agregar mas de un item a la vez a veces solo ingresa el último
    // otras si muestra lo item completos, al parecer la actualización asíncrona en segundo plano es un problema
    // SE SOLUCIONÓ llamando a la lista completa de registros cada vez que se insertaba uno nuevo, parece poco eficiente,
    // pero diff util está diseñado para manejarlo, aún así seguiremos averiguando más.
    fun add(song: SongEntity) {
        /*val updateList = asyncListDiffer.currentList.toMutableList()
        if (!updateList.contains(song)) {
            updateList.add(song)
            asyncListDiffer.submitList(updateList.toList())
        }*/
        /*if (!songList.contains(song)) {
            songList.add(song)
            notifyItemInserted(songList.size - 1)

            }*/
     }
    fun remove(song:SongEntity){
        //val currentList=asyncListDiffer.currentList.toMutableList()
        val currentList=currentList.toMutableList()
        if(currentList.contains(song)){
            val position = currentList.indexOf(song)
            currentList.removeAt(position)
            submitList(currentList)

        }
/*
        if(songList.contains(song)){
            val position = songList.indexOf(song)
            songList.removeAt(position)
            notifyItemRemoved(position)
        }*/
    }
    fun getSongByPosition(position: Int): SongEntity?{
        return if(currentList.isNotEmpty()){
        //return if(songList.isNotEmpty()){
            currentList[position]
            //songList[position]
        }else{
            null
        }
    }
    inner class MViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
        val bind = ItemSongBinding.bind(itemView)
        fun onBind(position:Int,song: SongEntity) = with(bind){
                CoroutineScope(Dispatchers.IO).launch {
                    val audioTag = fetchFileMetadata(context, song.pathLocation!!)
                    withContext(Dispatchers.Main) {
                        tvBitrate.text = String.format("%s::kbps", audioTag.bitRate)
                        tvSongDesc.text = String.format(
                            "%s. %s - %s",
                            (position + 1), audioTag.title,
                            audioTag.artist
                        )
                        tvDuration.text = audioTag.songLengthFormatted
                        tvFileFormat.text =
                            String.format("::%s", song.pathLocation?.substringAfterLast(".", "NA"))
                    }
                    root.setOnClickListener {
                        changeBackgroundColorSelectedItem(bindingAdapterPosition)
                        onItemClick(position, song)

                    }
                    ivOptions.setOnClickListener { onMenuItemClick(it, position, song) }
                }
        }
        internal  fun bindBackgroundColor(color: Int) {
            bind.root.setBackgroundColor(color)
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
    private sealed interface SongChangePayload{
        data class BackgroundColor(val color:Int):SongChangePayload
    }

}