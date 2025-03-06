package com.barryzeha.ktmusicplayer.view.ui.adapters


import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Filter
import android.widget.Filterable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.barryzeha.core.common.COLOR_ACCENT
import com.barryzeha.core.common.COLOR_ON_PRIMARY
import com.barryzeha.core.common.COLOR_PRIMARY
import com.barryzeha.core.common.COLOR_TRANSPARENT
import com.barryzeha.core.common.SettingsKeys
import com.barryzeha.core.common.TEXT_COLOR_PRIMARY_INVERSE_NO_DISABLE
import com.barryzeha.core.common.adjustAlpha
import com.barryzeha.core.common.fetchFileMetadata
import com.barryzeha.core.common.fetchShortFileMetadata
import com.barryzeha.core.common.mColorList
import com.barryzeha.core.model.entities.SongEntity
import com.barryzeha.ktmusicplayer.MyApp
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.common.changeBackgroundColor
import com.barryzeha.ktmusicplayer.databinding.ItemSongBinding
import com.barryzeha.ktmusicplayer.databinding.ListItemHeaderBinding
import com.l4digital.fastscroll.FastScroller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 26/4/24.
 * Copyright (c)  All rights reserved.
 **/

class MusicListAdapter(private val onItemClick:(Int, SongEntity)->Unit ,
                       private val onMenuItemClick:(view:View,Int,SongEntity)->Unit): ListAdapter<Any, RecyclerView.ViewHolder>(CombinedDiffCallback(SongDiffCallback(), HeaderDiffCallback())), Filterable, FastScroller.SectionIndexer {

    private val SONG_ITEM=0
    private val HEADER_ITEM=1
    private var originalList:MutableList<Any> = arrayListOf()
    private var songEntityIndices = mutableListOf<Int>()
    private var itemListForDelete:MutableList<SongEntity> = arrayListOf()
    private var selectedPos = -1
    private var lastSelectedPos = -1
    private  var context:Context = MyApp.context

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
                is ItemSongChangePayload.BackgroundColor -> (holder as MViewHolder).bindBackgroundColor(latestPayload.color)
                is ItemSongChangePayload.CheckBoxVisible -> (holder as MViewHolder).bindCheckboxVisible(latestPayload.isVisible)
                else -> onBindViewHolder(holder, position)
            }
    }
    override fun getSectionText(position: Int) = if(getItem(position) is SongEntity) (getItem(position) as SongEntity).album else ""

    @SuppressLint("ResourceType")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(holder is MViewHolder) {
            try {
                if (selectedPos == position) {
                    (holder as MViewHolder).bind.root.setBackgroundColor(
                        mColorList(context).getColor(COLOR_PRIMARY, 0).adjustAlpha(0.3f)
                    )
                } else {
                    (holder as MViewHolder).bind.root.setBackgroundColor(Color.TRANSPARENT)
                }
            } finally {
                mColorList(context).recycle()
            }

            val songPosition = getOriginalPosition(getItem(position))
            holder.onBind(songPosition, getItem(position) as SongEntity)
        }else if(holder is HeaderViewHolder){
            holder.onBind(getItem(position) as String)
        }

    }
    // Esta función también nos mostrará la numeración original incluso mientras filtramos
    // la lista al buscar una canción
    private fun getOriginalPosition(item: Any): Int {
        // Encuentra la posición del ítem en la lista original
        val itemPos = originalList.indexOf(item)
        // Si el ítem está en la lista original y es un SongEntity
        if (itemPos != -1 && originalList[itemPos] is SongEntity) {
            // Busca la posición en la lista de índices
            return songEntityIndices.indexOf(itemPos) + 1
        }
        return 0 // Retorna 0 si no se encuentra o no es SongEntity
    }

    override fun getItemViewType(position: Int): Int {
        return if(getItem(position) is SongEntity) SONG_ITEM else HEADER_ITEM
    }
    fun showMultipleSelection(visibility:Boolean){
        val currentList = currentList.toMutableList()
       CoroutineScope(Dispatchers.IO).launch{
            currentList.forEachIndexed{index, item->
                if(item is SongEntity){
                    currentList[index]= item.copy(isSelectShow = visibility)
                    withContext(Dispatchers.Main){
                       notifyItemChanged(index,ItemSongChangePayload.CheckBoxVisible(visibility))
                    }
                }
            }
           submitList(currentList)
           originalList=currentList
        }
    }
    @SuppressLint("ResourceType")
    fun changeBackgroundColorSelectedItem(songId:Long){
        // obtenemos la posición del item por su id, ya que tenemos dos tipos de vistas en el recyclerview
        // solo debemos cambiar de color a items SongEntity
        val songItem = originalList.filterIsInstance<SongEntity>().find { songId == it.id }
        songItem?.let {
                val position = originalList.indexOf(songItem)
                selectedPos = originalList.indexOf(songItem)
                if (lastSelectedPos == -1) {
                    lastSelectedPos = selectedPos
                } else {
                    notifyItemChanged(lastSelectedPos, Color.TRANSPARENT)
                    lastSelectedPos = selectedPos
                }
                notifyItemChanged(
                    selectedPos,
                    ItemSongChangePayload.BackgroundColor(
                        if (MyApp.mPrefs.globalTheme == SettingsKeys.MATERIAL_YOU_THEME.ordinal) mColorList(
                            context
                        ).getColor(
                            COLOR_ACCENT, COLOR_TRANSPARENT
                        ).adjustAlpha(0.3f) else ContextCompat.getColor(
                            context,
                            com.barryzeha.core.R.color.primaryColor
                        ).adjustAlpha(0.3f)
                    )
                )
            }
    }
    private fun setUpSongEntitiesIndices(){
        songEntityIndices.clear()
        for(i in originalList.indices){
            if(originalList[i] is SongEntity){
                songEntityIndices.add(i)
            }
        }
    }
    // Al usar DiffUtils o asyncListDiffer para agregar más de un item a la vez a veces solo ingresa el último
    // otras si muestra lo item completos, al parecer la actualización asíncrona en segundo plano es un problema
    // SE SOLUCIONÓ llamando a la lista completa de registros cada vez que se insertaba uno nuevo, parece poco eficiente,
    // pero diff util está diseñado para manejarlo, aún así seguiremos averiguando más.

    fun addAll(songs:List<Any>){
        this.originalList=songs.toMutableList()
        setUpSongEntitiesIndices()
        submitList(songs)
    }

    fun update(song:SongEntity){
        val currentList = currentList.toMutableList()
        val item = getSongById(song.id)
        val positionOnAdapter = currentList.indexOf(item as SongEntity)
        val positionOnOriginalList = originalList.indexOf(item)

        val meta = fetchFileMetadata(context, song.pathLocation!!)
        meta?.let {
            val itemUpdated = item?.copy(
                description = meta.title.toString(),
                artist = meta.artist.toString(),
                album = meta.album.toString(),
                genre = meta.genre.toString()
            )
            originalList[positionOnOriginalList] = itemUpdated as SongEntity
            currentList[positionOnAdapter] = itemUpdated
            submitList(currentList)
            notifyItemChanged(positionOnAdapter)
        }

    }
    //TODO crear otro método para manejar la eliminación de una lista de elementos seleccionados
    // y llamar al método setUpSongEntitiesIndices() al final
    fun remove(song:SongEntity){

        val currentList=currentList.toMutableList()
        if(currentList.contains(song)){
            val position = currentList.indexOf(song)
            // Remove the header if no other songs exist under it
            currentList.removeAt(position)
            val headerPos=shouldRemoveHeaderForSong(song)
            if(headerPos>-1){
                currentList.removeAt(headerPos)
            }
            submitList(currentList)
            originalList = currentList
            setUpSongEntitiesIndices()
        }
    }
    fun removeItemsForMultipleSelectedAction() {
        CoroutineScope(Dispatchers.IO).launch {
            val currentList = currentList.toMutableList()
            val headersToRemove = mutableSetOf<Int>()  // Para almacenar posiciones de encabezados a eliminar

            itemListForDelete.forEach { song ->
                val position = currentList.indexOf(song)
                if (position != -1) {
                    // Primero, elimina la canción
                    currentList.removeAt(position)

                    // Luego, revisa si hay un encabezado asociado

                    val headerPos = shouldRemoveHeaderForSong(position, currentList)
                    if (headerPos != -1) {
                        headersToRemove.add(headerPos)  // Marca el encabezado para eliminación
                    }
                }
            }

            // Ahora elimina los encabezados marcados, de arriba hacia abajo
            val sortedHeadersToRemove = headersToRemove.sortedDescending()
            sortedHeadersToRemove.forEach { headerPos ->
                if (headerPos >= 0 && headerPos < currentList.size) {
                    currentList.removeAt(headerPos)
                }
            }
            submitList(currentList)
            originalList = currentList
        }
    }

    fun removeAll(){
        val currentList = currentList.toMutableList()
        currentList.clear()
        submitList(currentList)
        originalList=currentList

    }
    fun getListItemsForDelete():List<SongEntity> = itemListForDelete
    fun clearListItemsForDelete(){itemListForDelete.clear()}

    private fun shouldRemoveHeaderForSong(song:SongEntity):Int{
        val position = currentList.indexOf(song)
        val aboveItem = currentList[position - 1]
        return if(aboveItem is String ) {
            if(position<currentList.size-1 && currentList[position +1] is String){
                position - 1

            }else if (position>=currentList.size-1){
                position -1
            }else{
                -1
            }
        }
        else if(aboveItem is String && position==currentList.size-1) position - 1
        else -1
    }
    private fun shouldRemoveHeaderForSong(songPosition: Int, list: List<Any>):Int{
        val position = songPosition
        val aboveItem = list[position - 1]
        return if(aboveItem is String ) {
            if(position<list.size-1 && list[position +1] is String){
                position - 1

            }else if (position>=list.size-1){
                position -1
            }else{
                -1
            }
        }
        else if(aboveItem is String && position==list.size-1) position - 1
        else -1
    }

    fun getSongItemCount():Int{
        var itemSong = 0
        originalList.forEach { it ->
            if (it is SongEntity) itemSong++
        }

        return itemSong
    }
    fun getSongByPosition(position: Int): SongEntity?{
        return if(originalList.isNotEmpty()){
            if(originalList[position] is SongEntity) {
                originalList[position] as SongEntity
            }else{
                originalList[position + 1] as SongEntity
            }
        }else{
            null
        }
    }
    // Obtener la posición numerada solo de items SongEntity sin contar itemHeaders
    fun getPositionByItem(item: Any): Pair<Int,Int> {
        val positionNumbered = originalList.indexOf(item) // Index in filteredList
            .let { index -> originalList
                .take(index + 1)
                .count { it is SongEntity } // Count SongEntity items up to the current index
            }
        val realPosition=if (originalList.isNotEmpty()) {
            originalList.indexOf(item)
        } else {
            null
        }
        return Pair(positionNumbered,realPosition!!)
    }
    fun getSongById(idSong:Long):SongEntity?{
        return originalList.filterIsInstance<SongEntity>().find { idSong == it.id }
    }
    inner class MViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
        val bind = ItemSongBinding.bind(itemView)
        fun onBind(position:Int,song: SongEntity) = with(bind){
                CoroutineScope(Dispatchers.IO).launch {
                    val audioTag = fetchShortFileMetadata(root.context,song.pathLocation!!)
                    withContext(Dispatchers.Main) {
                        audioTag?.let {
                            //ivThumbnailCover.loadImage(it.coverArt!!)
                            chkItemSong.visibility = if (song.isSelectShow) View.VISIBLE else View.GONE
                            tvBitrate.text = String.format("%s::kbps", audioTag.bitRate)
                            tvSongDesc.text = String.format(
                                "%s. %s - %s",
                                (position), audioTag.title,
                                audioTag.artist
                            )
                            chkItemSong.isChecked = song.isChecked
                            tvDuration.text = audioTag.songLengthFormatted
                            tvFileFormat.text =
                                String.format(
                                    "::%s",
                                    song.pathLocation?.substringAfterLast(".", "NA")
                                )

                        }?:run{
                            chkItemSong.visibility = if (song.isSelectShow) View.VISIBLE else View.GONE
                            tvBitrate.text = String.format("%s::kbps", song.bitrate)
                            tvSongDesc.text = String.format(
                                "%s. %s - %s",
                                (position), song.description,
                                song.artist
                            )
                            chkItemSong.isChecked = song.isChecked
                            tvDuration.text = "00:00"
                            tvFileFormat.text =
                                String.format(
                                    "::%s",
                                    song.pathLocation?.substringAfterLast(".", "NA")
                                )
                        }
                    }
                    root.setOnClickListener {
                        // La marcación del item se hará cuando se lance current track en nuestra interface
                        onItemClick(position, song)
                    }
                    chkItemSong.setOnClickListener {view->
                        if((view as CheckBox).isChecked){
                            itemListForDelete.add(song)
                        }else{
                            itemListForDelete.remove(song)
                        }
                    }
                    ivOptions.setOnClickListener { onMenuItemClick(it, position, song) }
                }
        }
        internal  fun bindBackgroundColor(color: Int) {
            bind.root.setBackgroundColor(color)
        }
        internal fun bindCheckboxVisible(isVisible: Boolean){
            bind.chkItemSong.visibility=if(isVisible)View.VISIBLE else View.GONE
        }
    }
    inner class HeaderViewHolder(v:View):StickyViewHolder(v){
        val bind = ListItemHeaderBinding.bind(v)
        fun onBind(value:String)=with(bind){
            tvHeaderDescription.text=value
            val alpha = 148
            val colorWithAlpha = (alpha shl 24) or (mColorList(context).getColor(COLOR_PRIMARY,
                COLOR_PRIMARY) and 0x00FFFFFF)
            //this.root.setBackgroundColor(colorWithAlpha)
            val textHeaderColor = if(MyApp.mPrefs.globalTheme == SettingsKeys.MATERIAL_YOU_THEME.ordinal)mColorList(context).getColor(
                COLOR_ACCENT,COLOR_PRIMARY)else mColorList(context).getColor(COLOR_PRIMARY,COLOR_PRIMARY)
            tvHeaderDescription.setTextColor(textHeaderColor)
            divider.dividerColor = textHeaderColor
        }
    }
    // Filter
    private val searchFilter:Filter = object:Filter(){
        override fun performFiltering(input: CharSequence?): FilterResults {
            val filteredList = if (input.toString().isEmpty()) {
                originalList
            } else {
                originalList.filter { item ->
                    if (item is SongEntity) {
                        item.description.toString().lowercase().contains(input!!)
                                || item.album.lowercase().contains(input)
                                || item.artist.lowercase().contains(input)
                    } else {
                        false
                    }
                }
            }
            return FilterResults().apply { values = filteredList }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            @Suppress("UNCHECKED_CAST")
            submitList(results?.values as? List<Any> ?: emptyList())
        }
    }
    override fun getFilter(): Filter {
        return searchFilter
    }
    private class CombinedDiffCallback(
        private val songDiffCallback: SongDiffCallback,
        private val headerDiffCallback: HeaderDiffCallback
    ) : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is SongEntity && newItem is SongEntity -> songDiffCallback.areItemsTheSame(oldItem, newItem)
                oldItem is String && newItem is String -> headerDiffCallback.areItemsTheSame(oldItem, newItem)
                else -> false
            }
        }
        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is SongEntity && newItem is SongEntity -> songDiffCallback.areContentsTheSame(oldItem, newItem)
                oldItem is String && newItem is String -> headerDiffCallback.areContentsTheSame(oldItem, newItem)
                else -> false
            }
        }
    }
    private class SongDiffCallback : DiffUtil.ItemCallback<SongEntity>() {
        override fun areItemsTheSame(oldItem: SongEntity, newItem: SongEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SongEntity, newItem: SongEntity): Boolean {
            return oldItem == newItem
        }
    }
    private class HeaderDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }

    private sealed interface ItemSongChangePayload{
        data class BackgroundColor(val color:Int):ItemSongChangePayload
        data class CheckBoxVisible(val isVisible:Boolean):ItemSongChangePayload
    }
    private sealed interface ItemHeaderChangePayload{
        data class TextHeader(val text:String):ItemHeaderChangePayload
    }


}