package com.barryzeha.mfilepicker.ui.views

import android.os.Bundle
import android.os.Environment
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.barryzeha.mfilepicker.databinding.ActivityFilePickerBinding
import com.barryzeha.mfilepicker.entities.FileItem
import com.barryzeha.mfilepicker.filetype.AudioFileType
import com.barryzeha.mfilepicker.interfaces.FileType
import com.barryzeha.mfilepicker.ui.adapters.FilePickerAdapter
import java.io.File
import java.util.Collections

class FilePickerActivity : AppCompatActivity() {
    private lateinit var bind:ActivityFilePickerBinding
    private lateinit var pickerAdapter:FilePickerAdapter
    private var  fileList:MutableList<FileItem> = mutableListOf()
    private lateinit var rootDirectory:File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityFilePickerBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(bind.root)
        ViewCompat.setOnApplyWindowInsetsListener(bind.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupAdapter()
        rootDirectory = Environment.getExternalStorageDirectory()
        loadFiles(rootDirectory)
    }
    private fun setupAdapter(){
        pickerAdapter = FilePickerAdapter()
        bind.rvFilePicker.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@FilePickerActivity)
            adapter= pickerAdapter
        }
    }
    private fun loadFiles(directory:File){
        fileList.clear()
        val files = directory.listFiles()

        if(files != null){
            val filesList = files.sortedBy { it.name.lowercase() }
           filesList.forEach { file->
               if(file.isDirectory) {
                   fileList.add(
                       FileItem(
                           fileName = file.name,
                           isDir = file.isDirectory
                       )
                   )
               }
           }
            filesList.forEach { file->
                if(file.isFile){
                    var type:FileType?=null
                    if(AudioFileType().verify(file.name)){
                        type=AudioFileType()
                    }
                    fileList.add(FileItem(fileName = file.name, isDir = file.isDirectory, fileType = type))
                }
            }
        }
        pickerAdapter.addAll(fileList)

    }
}