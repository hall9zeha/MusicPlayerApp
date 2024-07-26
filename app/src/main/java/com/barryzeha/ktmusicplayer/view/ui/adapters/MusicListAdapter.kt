package com.barryzeha.ktmusicplayer.view.ui.adapters


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
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
import com.barryzeha.ktmusicplayer.databinding.ListItemHeaderBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 26/4/24.
 * Copyright (c)  All rights reserved.
 **/

class MusicListAdapter(private val onItemClick:(Int, SongEntity)->Unit ,private val onMenuItemClick:(view:View,Int,SongEntity)->Unit): ListAdapter<Any, RecyclerView.ViewHolder>(SongDiffCallback()), Filterable {
//class MusicListAdapter(private val onItemClick:(Int, SongEntity)->Unit ,private val onMenuItemClick:(view:View,Int,SongEntity)->Unit): RecyclerView.Adapter<MusicListAdapter.MViewHolder>(){
    private val SONG_ITEM=0
    private val HEADER_ITEM=1

    private var originalList:MutableList<Any> = arrayListOf()
    //private val asyncListDiffer = AsyncListDiffer(this,SongDiffCallback())

    private var selectedPos = -1
    private var lastSelectedPos = -1
    private  var context:Context = MyApp.context

    init{
        //setHasStableIds(true)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context=parent.context
        return if(viewType == SONG_ITEM) {
            val itemViewSong = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
            MViewHolder(itemViewSong)
        }else{
            val itemViewHeader = LayoutInflater.from(parent.context).inflate(R.layout.list_item_header,parent,false)
            HeaderViewHolder(itemViewHeader)
        }


    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
             when (val latestPayload = payloads.lastOrNull()) {
                is SongChangePayload.BackgroundColor -> (holder as MViewHolder).bindBackgroundColor(latestPayload.color)
                else -> onBindViewHolder(holder, position)
            }

    }
   /* override fun getItemId(position: Int): Long {
        if(currentList[position] is SongEntity) {
            return ((currentList[position] as SongEntity)).id
        }else{
            return 0
        }
    }*/

    @SuppressLint("ResourceType")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(holder is MViewHolder) {
            try {
                if (selectedPos == position) {
                    (holder as MViewHolder).bind.root.setBackgroundColor(
                        mColorList(context).getColor(2, 0).adjustAlpha(0.3f)
                    )
                } else {
                    (holder as MViewHolder).bind.root.setBackgroundColor(Color.TRANSPARENT)
                }

            } finally {
                mColorList(context).recycle()
            }
        }
        if(holder is MViewHolder) {
           holder.onBind(position, getItem(position) as SongEntity)
        }else if(holder is HeaderViewHolder){
            holder.onBind(getItem(position) as String)
        }
        //holder.onBind(position, songList[position])
        //holder.onBind(position, asyncListDiffer.currentList[position])
    }

    override fun getItemViewType(position: Int): Int {
        return if(getItem(position) is SongEntity) SONG_ITEM else HEADER_ITEM
    }
    @SuppressLint("ResourceType")
    fun changeBackgroundColorSelectedItem(position: Int, songId:Long){
        // obtenemos la posición del item por su id, ya que tenemos dos tipos de vistas en el recyclerview
        // solo debemos cambiar de color a items SongEntity
        val songItem = originalList.filterIsInstance<SongEntity>().find {songId == it.id}

        songItem?.let {
            val position = originalList.indexOf(songItem)
            //selectedPos = position
            selectedPos = originalList.indexOf(songItem)
            if (lastSelectedPos == -1) {
                lastSelectedPos = selectedPos
            } else {
                notifyItemChanged(lastSelectedPos, Color.TRANSPARENT)
                lastSelectedPos = selectedPos
            }
            notifyItemChanged(
                selectedPos,
                SongChangePayload.BackgroundColor(
                    mColorList(context).getColor(2, 0).adjustAlpha(0.3f)
                )
            )
        }
    }
    fun addAll(songs:List<Any>){
        this.originalList=songs.toMutableList()
        submitList(originalList)
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
            if(currentList[position] is SongEntity) {
                currentList[position] as SongEntity
            }else{
                currentList[position + 1] as SongEntity
            }
        }else{
            null
        }
    }
    fun getPositionByItem(songItem:SongEntity):Int?{
        return if(originalList.isNotEmpty()){
            originalList.indexOf(songItem)
        }else{
            null
        }
    }
    fun getSongById(idSong:Long):SongEntity?{
        return originalList.filterIsInstance<SongEntity>().find { idSong == it.id }
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
                        changeBackgroundColorSelectedItem(bindingAdapterPosition, song.id)
                        onItemClick(position, song)

                    }
                    ivOptions.setOnClickListener { onMenuItemClick(it, position, song) }
                }
        }
        internal  fun bindBackgroundColor(color: Int) {
            bind.root.setBackgroundColor(color)
        }

    }
    inner class HeaderViewHolder(v:View):StickyViewHolder(v){
        val bind = ListItemHeaderBinding.bind(v)
        fun onBind(value:String)=with(bind){
            tvHeaderDescription.text=value
        }
    }
    // Filter
    private val searchFilter:Filter = object:Filter(){
        override fun performFiltering(input: CharSequence?): FilterResults {
            val filteredList = if(input.toString().isEmpty()){
                originalList
            }else{
                /*originalList.filter{it as SongEntity
                    it.description.toString().lowercase().contains(input!!)}*/
                originalList.filter { item ->
                    // Verifica si el item es una instancia de SongEntity
                    if (item is SongEntity) {
                        item.description.toString().lowercase().contains(input!!)
                    } else {
                        false
                    }
                }
            }
            return FilterResults().apply { values=filteredList }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            @Suppress("UNCHECKED_CAST")
            submitList(results?.values as? List<Any> ?: emptyList())
            //submitList((results?.values is SongEntity) as ArrayList<SongEntity>)
        }
    }
    override fun getFilter(): Filter {
        return searchFilter
    }
    private class SongDiffCallback:DiffUtil.ItemCallback<Any>(){
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return if(oldItem is SongEntity && newItem is SongEntity){((oldItem as SongEntity).id == (newItem as SongEntity).id)} else false
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return if(oldItem is SongEntity && newItem is SongEntity)(oldItem as SongEntity) == (newItem as SongEntity) else false
        }
    }
    private sealed interface SongChangePayload{
        data class BackgroundColor(val color:Int):SongChangePayload
    }


}