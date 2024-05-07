package com.barryzeha.ktmusicplayer.view.ui.adapters

import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.barryzeha.core.common.getBitrate
import com.barryzeha.core.common.getTimeOfSong
import com.barryzeha.core.entities.SongEntity
import com.barryzeha.ktmusicplayer.R
import com.barryzeha.ktmusicplayer.databinding.ItemSongBinding


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 26/4/24.
 * Copyright (c)  All rights reserved.
 **/

class MusicListAdapter(private val onItemClick:(SongEntity)->Unit ): RecyclerView.Adapter<MusicListAdapter.MViewHolder>() {

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
           Log.e("Song", it.pathLocation.toString() )
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
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(song.pathLocation)
            mediaPlayer.prepare()
            val info=mediaPlayer.trackInfo
            for(i in info){
                if(i.trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO)
                    tvBitrate.text=String.format("%s::kbps",getBitrate(song.pathLocation!!).toString())

            }
            Log.e("TAG", song.pathLocation.toString() )

            tvSongDesc.text=song.pathLocation?.substringAfterLast("/","No named")
            tvDuration.text= getTimeOfSong( (mediaPlayer.duration).toLong())
            //tvFileFormat.text = String.format("::%s",song.pathLocation?.substring(song.pathLocation?.lastIndexOf(".")!! +1))
            tvFileFormat.text = String.format("::%s",song.pathLocation?.substringAfterLast(".","NA"))
            root.setOnClickListener { onItemClick(song) }
        }

    }

}