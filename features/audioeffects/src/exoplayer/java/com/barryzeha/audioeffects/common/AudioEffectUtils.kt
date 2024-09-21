package com.barryzeha.audioeffects.common

import android.util.Log


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
   FLAT-> {
    when (bandLevel) {
     0 -> 1500  // 0 dB (60 Hz)
     1 -> 1500  // 0 dB (230 Hz)
     2 -> 1500  // 0 dB (910 Hz)
     3 -> 1500  // 0 dB (3600 Hz)
     4 -> 1500  // 0 dB (14000 Hz)
     else -> 1500
    }
   }
   JAZZ -> {
    when (bandLevel) {
     0 -> 1400  // -1 dB (60 Hz)
     1 -> 1600  // +1 dB (230 Hz)
     2 -> 1800  // +2 dB (910 Hz)
     3 -> 1600  // +1 dB (3600 Hz)
     4 -> 1400  // -1 dB (14000 Hz)
     else -> 1500
    }
   }
   CLASSICAL -> {
    when (bandLevel) {
     0 -> 1500  // 0 dB (60 Hz)
     1 -> 1600  // +1 dB (230 Hz)
     2 -> 1700  // +2 dB (910 Hz)
     3 -> 1600  // +1 dB (3600 Hz)
     4 -> 1500  // 0 dB (14000 Hz)
     else -> 1500
    }
   }
   HIP_HOP -> {
    when (bandLevel) {
     0 -> 2000  // +2 dB (60 Hz)
     1 -> 2200  // +3 dB (230 Hz)
     2 -> 1800  // +1 dB (910 Hz)
     3 -> 1600  // +1 dB (3600 Hz)
     4 -> 1300  // -1 dB (14000 Hz)
     else -> 1500
    }
   }
   ELECTRONIC -> {
    when (bandLevel) {
     0 -> 2100  // +3 dB (60 Hz)
     1 -> 2300  // +4 dB (230 Hz)
     2 -> 2000  // +2 dB (910 Hz)
     3 -> 1600  // +1 dB (3600 Hz)
     4 -> 1400  // -1 dB (14000 Hz)
     else -> 1500
    }
   }
   FULL_SOUND -> {
    when (bandLevel) {
     0 -> 2000  // +4 dB (60 Hz)
     1 -> 1900  // +3 dB (230 Hz)
     2 -> 1700  // +2 dB (910 Hz)
     3 -> 1600  // +1 dB (3600 Hz)
     4 -> 1500  // 0 dB (14000 Hz)
     else -> 1500
    }
   }
   else->0
  }

}
fun getEqualizerConfig(effectType:Int, numOfBand:Int, prefs:EffectsPreferences): List<Short>{
 val listOfBands= arrayListOf<Short>()
 val lowerEqualizerBandLevel = -1500
 for(i in 0 until numOfBand){
    val equalizerBandIndex=i
    val bandValue = prefs.getSeekBandValue(effectType,equalizerBandIndex)
    if(bandValue !=1500) {
     listOfBands.add((bandValue + lowerEqualizerBandLevel).toShort())
    }else{
     val bandPresetValue = getEqualizerBandPreConfig(effectType, equalizerBandIndex)
     listOfBands.add((bandPresetValue + lowerEqualizerBandLevel).toShort())
    }
 }
 return listOfBands
}