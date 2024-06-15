package com.barryzeha.core.common

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 15/6/24.
 * Copyright (c)  All rights reserved.
 **/

class SingleMutableLiveData<T>:MutableLiveData<T>() {
 override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
  super.observe(owner, Observer{t->
    if(t !=null){
     observer.onChanged(t)
     postValue(null)
    }
  })
 }
}