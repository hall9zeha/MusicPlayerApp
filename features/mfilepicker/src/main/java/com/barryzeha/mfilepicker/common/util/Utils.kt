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

fun getParentDirectories(path: String): String {
    //val storage= getStorageIdentifier(path)
    //val name = Path(path).parent.name
    val file = File(path)
    val parentDir = file.parentFile?.name
    val regex = Regex("^(cd\\d*|disc\\d*)$", RegexOption.IGNORE_CASE)
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
fun getStorageIdentifier(path: String): String? {
    // Expresiones regulares para detectar el número de almacenamiento en rutas emuladas y en tarjetas SD
    val emulatedRegex = "emulated/(\\d+)/".toRegex()
    val sdCardRegex = "storage/(extSdCard|external_sd)/".toRegex()

    // Intentar encontrar una coincidencia para emulated
    val emulatedMatchResult = emulatedRegex.find(path)
    if (emulatedMatchResult != null) {
        // Devolver solo el número de almacenamiento
        return emulatedMatchResult.groups[1]?.value
    }

    // Intentar encontrar una coincidencia para SD card
    val sdCardMatchResult = sdCardRegex.find(path)
    if (sdCardMatchResult != null) {
        // Devolver el nombre de la SD card (extSdCard o external_sd)
        return sdCardMatchResult.groups[1]?.value
    }

    // Si no se encuentra ninguna coincidencia, retornar null
    return null
}
//TODO eliminar lo que queda del provider
fun getUriFromFile(file: File, context: Context): Uri {
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}