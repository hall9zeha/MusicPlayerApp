package com.barryzeha.mfilepicker.common.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import com.barryzeha.core.common.getTimeOfSong
import com.barryzeha.core.model.entities.AudioMetadata
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.io.path.Path
import kotlin.io.path.name


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 10/9/24.
 * Copyright (c)  All rights reserved.
 **/
const val ROOT_STORAGE=0
const val SD_STORAGE=1
const val COMMON_DIR=-1
fun getParentDirectories(path: String): String {
    //val storage= getStorageIdentifier(path)
    //val name = Path(path).parent.name
    val file = File(path)
    val parentDir = file.parentFile?.name
    val regex = Regex("^\\s*(cd\\s*\\d*|disc\\s*\\d*)$", RegexOption.IGNORE_CASE)
    if(parentDir !=null) {
        if (regex.matches(parentDir)) {
            val absolutePath = file.absolutePath
            val pathParts = absolutePath.split('/').filter { it.isNotEmpty() }
            if (pathParts.size >= 2) {
                val lastDir=pathParts[pathParts.size -2]
                val beforeLastDir = pathParts[pathParts.size-3]
                Log.e("PARENT-NAME->", "$beforeLastDir/$lastDir")
                return  "$beforeLastDir/$lastDir"
            }else{
                Log.e("PARENT-NAME->",pathParts[pathParts.size -2] )
                return parentDir
            }
        }else{
            return parentDir
        }
    }
    return ""
}