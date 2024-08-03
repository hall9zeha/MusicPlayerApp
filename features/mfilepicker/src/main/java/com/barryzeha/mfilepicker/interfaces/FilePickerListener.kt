package com.barryzeha.mfilepicker.interfaces

import java.io.File


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 3/8/24.
 * Copyright (c)  All rights reserved.
 **/

interface FilePickerListener {
 fun onFilesSelected(filesOrDirs:List<File>)
}