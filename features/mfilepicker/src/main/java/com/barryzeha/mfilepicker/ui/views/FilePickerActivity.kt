package com.barryzeha.mfilepicker.ui.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.barryzeha.core.common.getThemeResValue
import com.barryzeha.mfilepicker.R
import com.barryzeha.mfilepicker.common.Preferences
import com.barryzeha.mfilepicker.common.util.COMMON_DIR
import com.barryzeha.mfilepicker.common.util.ROOT_STORAGE
import com.barryzeha.mfilepicker.common.util.SD_STORAGE
import com.barryzeha.mfilepicker.databinding.ActivityFilePickerBinding
import com.barryzeha.mfilepicker.entities.FileItem
import com.barryzeha.mfilepicker.filetype.AudioFileType
import com.barryzeha.mfilepicker.interfaces.FileType
import com.barryzeha.mfilepicker.ui.adapters.FilePickerAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import com.barryzeha.core.R as coreRes

@AndroidEntryPoint
class FilePickerActivity : AppCompatActivity() {
    @Inject
    lateinit var mPrefs:Preferences
    private var storagePaths = mutableListOf<File>()
    private lateinit var bind:ActivityFilePickerBinding
    private lateinit var pickerAdapter:FilePickerAdapter
    private var  fileList:MutableList<FileItem> = mutableListOf()
    private lateinit var rootDirectory:File
    private var listTreeOfNav:MutableList<Pair<Int,File?>> = arrayListOf()
    private var selectedItemsList:MutableList<FileItem> = arrayListOf()
    private var toolbarMenu:Menu?=null
    private var isSelectedAll:Boolean=false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeResValue())
        super.onCreate(savedInstanceState)
        bind = ActivityFilePickerBinding.inflate(layoutInflater)

        //Para mostrar correctamente el toolbar debemos de comentar la configuración de ViewCompat
        //enableEdgeToEdge()
        setContentView(bind.root)
        /*ViewCompat.setOnApplyWindowInsetsListener(bind.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        */
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setUpMenuProvider()
        setupAdapter()
        setUpLoadFiles()

    }
    private fun setUpLoadFiles(){
        rootDirectory = Environment.getExternalStorageDirectory()

        storagePaths.add(rootDirectory)

        // Listar todos los volúmenes de almacenamiento, esto nos listará los directorios correspondientes a nuestra app
        // filesDir tanto externos como internos, pero solo queremos el path de la tarjeta DS si esta existe.
        // por eso cortaremos el path manteniendo la dirección a la memoria SD ejm: /storage/4566-4556/
        val externalStorageVolumes = getExternalFilesDirs(null)
        if(externalStorageVolumes.size>1) {
            val sdCardStorage = externalStorageVolumes[1]
            sdCardStorage?.let { filePath ->
                val cutPath = filePath.path.substringBefore("Android")
                storagePaths.add(File(cutPath))
            }
        }

        if(mPrefs.lastDirs?.isNotEmpty()!!){
            val lastDirsVisited:MutableList<String?> = arrayListOf()
            lastDirsVisited.addAll(mPrefs.lastDirs!!)

            val lastDir= File(lastDirsVisited[lastDirsVisited.size-1])
            for( i in 0 until  lastDirsVisited.size  - 1 ){
                lastDirsVisited[i]?.let{
                    listTreeOfNav.add(Pair(0,File(lastDirsVisited[i])))
                }?:run{
                    listTreeOfNav.add(Pair(0,null))
                }
            }
            val savedDirs= arrayListOf(lastDir)
            if(lastDir.exists())loadFiles(dirs=savedDirs)else loadFiles(dirs=storagePaths)
        }else {
            loadFiles(dirs=storagePaths)
        }
    }
    private fun setupAdapter(){
        pickerAdapter = FilePickerAdapter(::onItemClick, ::onCheckboxClick)
        bind.rvFilePicker.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@FilePickerActivity)
            adapter= pickerAdapter
        }
    }

    private fun loadFiles(position:Int =0 ,/*directory:File*/ dirs:List<File?>){
        fileList.clear()
        if(dirs.size>1){
            setTitle(applicationInfo.nonLocalizedLabel.toString())
            listTreeOfNav.add(Pair(position, null))
            dirs.forEach { file ->
                val storageType=if(file?.name== ROOT_STORAGE.toString()) ROOT_STORAGE else SD_STORAGE
                file?.let {
                    if (file.isDirectory) {
                        fileList.add(
                            FileItem(
                                filePath = file.absolutePath,
                                fileName = file.path,
                                uri = Uri.fromFile(file),
                                isDir = file.isDirectory,
                                storageType = storageType
                            )
                        )
                    }
                    pickerAdapter.addAll(fileList)
                }
            }
        }else {
            val directory = dirs[0]

            listTreeOfNav.add(Pair(position, directory))
            // Para mostrar la nueva lista desde el inicio cuando navegamos en los directorios
            // internos
            if (listTreeOfNav[listTreeOfNav.size - 1].first > listTreeOfNav.size - 1) {
                bind.rvFilePicker.scrollToPosition(0)
            }
            val files = directory?.listFiles()

            if (files != null) {
                //
                if (checkIfRootDir(directory)) setTitle(directory.parent)
                else setTitle(directory.name)

                val filesList = files.sortedBy { it.name.lowercase() }
                filesList.forEach { file ->

                    if (file.isDirectory) {
                        fileList.add(
                            FileItem(
                                filePath = file.absolutePath,
                                fileName = file.name,
                                uri = Uri.fromFile(file),
                                isDir = file.isDirectory,
                                storageType= COMMON_DIR
                            )
                        )
                    }
                }
                filesList.forEach { file ->
                    if (file.isFile) {
                        var type: FileType? = null
                        if (AudioFileType().verify(file.name)) {
                            type = AudioFileType()
                            fileList.add(
                                FileItem(
                                    filePath = file.absolutePath,
                                    fileName = file.name,
                                    uri = Uri.fromFile(file),
                                    isDir = file.isDirectory,
                                    fileType = type
                                )
                            )
                        }
                    }
                }
            }
            pickerAdapter.addAll(fileList)
        }

    }
    private fun onItemClick(position:Int,item:FileItem){
        val file = File(item.filePath.toString())

        if(item.isDir) {
            pickerAdapter.clear()
            val paths = listOf(file)
            //loadFiles(position,file)
            loadFiles(position,paths)
            toolbarMenu?.getItem(0)?.setVisible(false)
            selectedItemsList.clear()
        }

    }
    private fun onCheckboxClick(position:Int, item:FileItem){
        if(item.getIsChecked()){
            selectedItemsList.add(item)
        }else{
            if(selectedItemsList.contains(item)){
                selectedItemsList.remove(item)
            }
        }
        if(selectedItemsList.size>0) toolbarMenu?.getItem(0)?.setVisible(true)
        else toolbarMenu?.getItem(0)?.setVisible(false)

    }
    private fun checkIfRootDir(directory: File):Boolean{
        val internalRoot = File("/").canonicalFile
        if(directory.canonicalFile == internalRoot) return true

        val externalRoot = getExternalRoot()
        return externalRoot.any{directory.canonicalFile == it.canonicalFile}
    }
    private fun getExternalRoot():List<File>{
        val externalRoots = mutableListOf<File>()
        val externalRootPrimary = Environment.getExternalStorageDirectory().canonicalFile
        externalRoots.add(externalRootPrimary)
        return externalRoots
    }
    private fun setUpMenuProvider(){
        val menuHost:MenuHost = this
        menuHost.addMenuProvider(object:MenuProvider{
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                toolbarMenu=menu
                menuInflater.inflate(R.menu.file_picker_menu,menu)
                menu.getItem(0)?.setVisible(false)

            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when(menuItem.itemId){
                    android.R.id.home->{
                        if( listTreeOfNav.size>1){
                            navigationDirList(listTreeOfNav)

                        }else{
                            mPrefs.clearLastDirs()
                            finish()
                        }
                    }
                    R.id.itemConfirm->{
                        val fileList:MutableList<String> = arrayListOf()
                        selectedItemsList.forEach {item->
                                fileList.add(item.filePath.toString())
                        }
                        val resultIntent = Intent().apply {
                            putStringArrayListExtra("paths", ArrayList(fileList))
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        saveNavigationTree(listTreeOfNav)

                        finish()

                    }
                    R.id.itemSelectAll->{
                        if(!isSelectedAll) {
                            isSelectedAll=true
                            toolbarMenu?.getItem(1)?.setIcon(com.barryzeha.core.R.drawable.ic_deselected_all)
                            selectedItemsList=pickerAdapter.getSelectedItems().toMutableList()
                        }else{
                            isSelectedAll=false
                            toolbarMenu?.getItem(1)?.setIcon(com.barryzeha.core.R.drawable.ic_select_all)
                            selectedItemsList.clear()
                            pickerAdapter.clearItemsSelected()

                        }
                        toolbarMenu?.getItem(0)?.setVisible(isSelectedAll)
                        pickerAdapter.selectAllItemsChecked(isSelectedAll)

                    }
                }
               return true
            }
        })
    }
    private fun saveNavigationTree(listOfNavigation:List<Pair<Int,File?>>){
        val listOfDirPath:ArrayList<String?> = arrayListOf()
        listOfNavigation.forEach { pair->
            val (_,directory) = pair
            directory?.let {
               listOfDirPath.add(directory.absolutePath)
            }?:run{
                listOfDirPath.add(null)
            }
        }
        mPrefs.lastDirs = listOfDirPath
    }

    private fun navigationDirList(dirList:MutableList<Pair<Int,File?>>){
        pickerAdapter.clear()
        val (_,directory) = dirList[dirList.size-2]
        val navPaths = arrayListOf(directory)
        if(directory==null){
            loadFiles(dirs=storagePaths)
        }else{
            loadFiles(dirs=navPaths)
        }
        //loadFiles(directory = dirList[(dirList.size-1)-1].second)

        // Para volver a mostrar la posición del directorio padre cuando naveguemos hacia atrás
        bind.rvFilePicker.scrollToPosition(dirList[(dirList.size - 1) - 1].first)

        dirList.removeAt(dirList.size - 1)
        dirList.removeAt(dirList.size - 1)
        toolbarMenu?.getItem(0)?.setVisible(false)
        selectedItemsList.clear()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if(listTreeOfNav.size>1){
            navigationDirList(listTreeOfNav)
        }else{
            mPrefs.clearLastDirs()
            super.onBackPressed()
        }
  }
    class FilePickerContract:ActivityResultContract<Unit,List<String>>(){
        override fun createIntent(context: Context, input: Unit): Intent {
            return Intent(context,FilePickerActivity::class.java)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): List<String> {
            return intent?.getStringArrayListExtra("paths")?: emptyList()
        }
    }

}