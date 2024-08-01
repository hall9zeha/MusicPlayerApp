package com.barryzeha.mfilepicker.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.barryzeha.mfilepicker.R
import com.barryzeha.mfilepicker.databinding.FilePickerItemBinding
import com.barryzeha.mfilepicker.entities.FileItem


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 31/7/24.
 * Copyright (c)  All rights reserved.
 **/

class FilePickerAdapter: RecyclerView.Adapter<FilePickerAdapter.FilePickerViewHolder>(){
    private lateinit var context:Context
    private var listItems:MutableList<FileItem> = mutableListOf()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilePickerViewHolder {
        context= parent.context
        val itemView = LayoutInflater.from(context).inflate(R.layout.file_picker_item,parent,false)
        return FilePickerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FilePickerViewHolder, position: Int) {
        val item = listItems[position]
        holder.bind.tvFileDescription.text = item.fileName
        if(item.isDir){
            holder.bind.ivFileType.setImageResource(R.drawable.ic_folder)
        }else{
            holder.bind.ivFileType.setImageResource(item.fileType?.fileIconResId?:R.drawable.ic_unknown_file)
        }
    }

    override fun getItemCount() = listItems.size

    fun addAll(items:List<FileItem>){
        items.forEach { item ->
            if (!listItems.contains(item)){
                listItems.add(item)
                notifyItemInserted(listItems.size -1)
            }
        }
    }
    inner class FilePickerViewHolder(itemView:View):RecyclerView.ViewHolder(itemView) {

        val bind:FilePickerItemBinding = FilePickerItemBinding.bind(itemView)

    }
}