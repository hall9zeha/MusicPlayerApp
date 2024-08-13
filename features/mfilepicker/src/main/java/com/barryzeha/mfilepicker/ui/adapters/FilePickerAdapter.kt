package com.barryzeha.mfilepicker.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.barryzeha.mfilepicker.R
import com.barryzeha.mfilepicker.databinding.FilePickerItemBinding
import com.barryzeha.mfilepicker.entities.FileItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 31/7/24.
 * Copyright (c)  All rights reserved.
 **/

class FilePickerAdapter(private val onItemClick:(position:Int,item:FileItem)->Unit, private val onCheckboxClick:(position:Int,item:FileItem)->Unit): RecyclerView.Adapter<FilePickerAdapter.FilePickerViewHolder>(){
    private lateinit var context:Context
    private var listItems:MutableList<FileItem> = mutableListOf()
    private var listItemsSelected:MutableList<FileItem> = arrayListOf()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilePickerViewHolder {
        context= parent.context
        val itemView = LayoutInflater.from(context).inflate(R.layout.file_picker_item,parent,false)
        return FilePickerViewHolder(itemView)
    }

    override fun onBindViewHolder(
        holder: FilePickerViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        when(val latestPayload = payloads.lastOrNull()){
            is CheckboxChangePayload.isCheckedChk-> holder.bindCheckBox(latestPayload.isChecked)
            else ->onBindViewHolder(holder,position)
        }

    }
    override fun onBindViewHolder(holder: FilePickerViewHolder, position: Int) {
        val item = listItems[position]
       
        holder.bind.tvFileDescription.text = item.fileName
        holder.bind.chkSelected.isChecked = item.getIsChecked()
        if(item.isDir){
            holder.bind.ivFileType.setImageResource(R.drawable.ic_folder)
        }else{
            holder.bind.ivFileType.setImageResource(item.fileType?.fileIconResId?:R.drawable.ic_unknown_file)
        }
        holder.bind.root.setOnClickListener {
            onItemClick(position,item)
        }
        holder.bind.chkSelected.setOnCheckedChangeListener { buttonView, isChecked ->
            //onCheckboxClick(position,item.copy(isChecked = isChecked))
        }
        holder.bind.chkSelected.setOnClickListener{view->
           onCheckboxClick(position,item.copy(isChecked = (view as CheckBox).isChecked))
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
    fun selectAllItemsChecked(isChecked:Boolean){
        CoroutineScope(Dispatchers.IO).launch {
            listItems.forEachIndexed { index, item ->
                if (item.getIsChecked() != isChecked) {
                    listItems[index] = item.copy(isChecked = isChecked)
                    listItemsSelected.add(item)
                    withContext(Dispatchers.Main) {
                        notifyItemChanged(index,CheckboxChangePayload.isCheckedChk(isChecked))
                    }
                }
            }
        }
    }
    fun getSelectedItems():List<FileItem>{
       return listItemsSelected
    }
    fun clearItemsSelected(){
        listItemsSelected.clear()
    }
    @SuppressLint("NotifyDataSetChanged")
    fun clear(){
        listItems.clear()
        notifyDataSetChanged()
    }
    inner class FilePickerViewHolder(itemView:View):RecyclerView.ViewHolder(itemView) {

        val bind:FilePickerItemBinding = FilePickerItemBinding.bind(itemView)
        internal fun bindCheckBox(isChecked:Boolean){
            bind.chkSelected.isChecked = isChecked
        }
    }
    private sealed interface CheckboxChangePayload{
        data class isCheckedChk(val isChecked: Boolean):CheckboxChangePayload
    }
}