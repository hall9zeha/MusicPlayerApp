package com.barryzeha.mfilepicker.entities

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
 var isDir:Boolean=false,

){
 fun getIsChecked():Boolean = isChecked
 fun setCheck(check:Boolean) {isChecked = check}
}
