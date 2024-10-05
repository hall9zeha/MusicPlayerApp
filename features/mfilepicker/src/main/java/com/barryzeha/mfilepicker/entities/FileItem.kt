package com.barryzeha.mfilepicker.entities

import android.net.Uri
import com.barryzeha.mfilepicker.interfaces.FileType


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 31/7/24.
 * Copyright (c)  All rights reserved.
 **/

data class FileItem(
 var fileName:String?="",
 var filePath:String?="",
 private var isChecked:Boolean=false,
 var fileType:FileType?=null,
 var uri:Uri?=null,
 var isDir:Boolean=false,
 var storageType:Int=-1

){
 fun getIsChecked():Boolean = isChecked
 fun setCheck(check:Boolean) {isChecked = check}
 override fun equals(other: Any?): Boolean {
  if (this === other) return true
  if (javaClass != other?.javaClass) return false

  other as FileItem

  return filePath == other.filePath
 }

 override fun hashCode(): Int {
  return filePath?.hashCode() ?: 0
 }
}
