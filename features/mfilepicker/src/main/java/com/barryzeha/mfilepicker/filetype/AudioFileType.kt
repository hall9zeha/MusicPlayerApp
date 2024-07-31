package com.barryzeha.mfilepicker.filetype

import com.barryzeha.mfilepicker.R
import com.barryzeha.mfilepicker.interfaces.FileType


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 31/7/24.
 * Copyright (c)  All rights reserved.
 **/

class AudioFileType:FileType {
 override val fileType: String
  get() = "Audio"
 override val fileIconResId: Int
  get() = R.drawable.ic_audio_file

 override fun verify(fileName: String): Boolean {
   val isHasSuffix = fileName.contains(".")
   if(!isHasSuffix) return false
   val suffix = fileName.substring(fileName.lastIndexOf(".") + 1)
   return when(suffix) {
    "aif", "iff", "m3u", "m4a", "mid", "mp3", "mpa", "wav", "wma", "ogg", "flac", "ape", "alac" -> {
     true
    }
    else-> false
   }
 }
}