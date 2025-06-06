package com.barryzeha.core.model.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/4/24.
 * Copyright (c)  All rights reserved.
 **/
@Parcelize
@Entity(indices = [Index(value = ["id"], unique = true)])
data class SongEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("id")
    val id:Long=0,
    val idPlaylistCreator:Long=0,
    val description:String?="",
    val artist:String="",
    val album:String="",
    val genre:String="",
    val pathLocation:String?="",
    val parentDirectory:String?="",
    val timestamp:Long=0,
    val duration:Long=0,
    val bitrate:String?="",
    val format:String?="",
    val favorite:Boolean=false,
    // Para mostrar y ocultar  el checkbox del item  en recyclerview con el modo selección múltiple
    val isSelectShow:Boolean=false,
    // Para saber qué items seleccionados debemos procesar para eliminar u otras acciones
    val isChecked:Boolean=false

):Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SongEntity

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
