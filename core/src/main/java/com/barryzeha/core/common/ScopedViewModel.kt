package com.barryzeha.core.common

import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModel


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 11/6/24.
 * Copyright (c)  All rights reserved.
 **/
abstract class ScopedViewModel: ViewModel(), Scope by Scope.Impl(){
 init{
  initScope()
 }

 @CallSuper
 override fun onCleared() {
  destroyScope()
  super.onCleared()
 }
}