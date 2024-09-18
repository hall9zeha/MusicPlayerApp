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


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 10/9/24.
 * Copyright (c)  All rights reserved.
 **/

fun getParentDirectories(path: String): String {
    /*  val emulatedRegex = "emulated/(\\d+)/".toRegex()
      val emulatedMatchResult = emulatedRegex.find(path)

      val storage=   emulatedMatchResult?.groups?.get(1)?.value ?:""*/

    val storage= getStorageIdentifier(path)

    Log.e("STORAGE-POSITION--:", storage.toString())
    // Encontrar la posición del nombre del archivo (última '/' antes del nombre de archivo)
    val lastIndex = path.lastIndexOf('/')
    if (lastIndex == -1) {
        return path.substringBeforeLast("/")  // Si no se encuentra '/', retornar el path completo
    }

    // Encontrar la posición de '0' o '1' en el path
    val primaryIndex = path.indexOf(storage.toString())
    if (primaryIndex == -1) {
        return path.substringBeforeLast("/")  // Si no se encuentra '0', retornar el path completo
    }

    // Encontrar el primer '/' después de 'primary'
    val firstSlashAfterPrimary = path.indexOf('/', primaryIndex)
    if (firstSlashAfterPrimary == -1 || firstSlashAfterPrimary >= lastIndex) {
        return if (primaryIndex == -1) {
            path.substringBeforeLast("/")
        } else {
            return path.substring(primaryIndex + storage.toString().length, lastIndex)
        }

        // Si no se encuentra '/', retornar el path completo
    }

    // Recortar el path desde el primer '/' después de '0' hasta el nombre del archivo
    val directoriosRecortados = path.substring(firstSlashAfterPrimary + 1, lastIndex)
    return directoriosRecortados

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