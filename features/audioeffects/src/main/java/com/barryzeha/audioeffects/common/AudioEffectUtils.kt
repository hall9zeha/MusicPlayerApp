package com.barryzeha.audioeffects.common


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 31/8/24.
 * Copyright (c)  All rights reserved.
 **/

fun getEqualizerBandPreConfig(effectType:Int,bandLevel:Int):Int{
  return when(effectType){
   CUSTOM->{when (bandLevel) {
    0 -> 1500  // +5 dB (60 Hz)
    1 -> 1500  // +5 dB (230 Hz)
    2 -> 1500  // +5 dB (910 Hz)
    3 -> 1500  // +5 dB (3600 Hz)
    4 -> 1500  // +5 dB (14000 Hz)
    else -> 1500
   }}
   ROCK->{
    when (bandLevel) {
     0 -> 1200  // +2 dB (60 Hz)
     1 -> 1800  // +3 dB (230 Hz)
     2 -> 1500  // 0 dB (910 Hz)
     3 -> 1200  // -2 dB (3600 Hz)
     4 -> 900   // -3 dB (14000 Hz)
     else -> 1500

    }
   }
   POP->{
    when (bandLevel) {
     0 -> 1500  // 0 dB (60 Hz)
     1 -> 2100  // +3 dB (230 Hz)
     2 -> 1800  // +1.5 dB (910 Hz)
     3 -> 1500  // 0 dB (3600 Hz)
     4 -> 1200  // -3 dB (14000 Hz)
     else -> 1500
    }
   }
   BASS->{
    when (bandLevel) {
     0 -> 2400  // +3 dB (60 Hz)
     1 -> 2700  // +4 dB (230 Hz)
     2 -> 2400  // +2 dB (910 Hz)
     3 -> 1500  // 0 dB (3600 Hz)
     4 -> 1200  // -3 dB (14000 Hz)
     else -> 1500
    }
   }
   else->0
  }

}