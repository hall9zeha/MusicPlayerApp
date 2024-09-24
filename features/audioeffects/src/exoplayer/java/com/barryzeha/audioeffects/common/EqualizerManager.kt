package com.barryzeha.audioeffects.common

import android.media.audiofx.Equalizer


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 3/9/24.
 * Copyright (c)  All rights reserved.
 **/

object EqualizerManager {
 // For exoplayer
 var mEq: Equalizer? = null

 fun initEqualizer(sessionId: Int) {
  if (mEq == null) {
   mEq = Equalizer(1000, sessionId)
  }
 }

 fun setEnabled(enabled: Boolean) {
  mEq?.setEnabled(enabled)
 }
 fun setBand(index:Short, band:Short){
  mEq?.setBandLevel(index.toShort(),band)
 }
 fun release() {
  mEq?.release()
  mEq = null
 }


}