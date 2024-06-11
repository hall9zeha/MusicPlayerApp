package com.barryzeha.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 11/6/24.
 * Copyright (c)  All rights reserved.
 **/

interface Scope: CoroutineScope {
 class Impl:Scope{
  override lateinit var job: Job
 }
 var job:Job
 override val coroutineContext: CoroutineContext
  get() = Dispatchers.Main + job
 fun initScope(){
  job= SupervisorJob()
 }
 fun destroyScope(){
  job.cancel()
 }

}