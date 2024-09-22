package com.barryzeha.audioeffects.common


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 31/8/24.
 * Copyright (c)  All rights reserved.
 **/

fun getEqualizerBandPreConfig(effectType:Int,bandLevel:Int):Int{
 return when (effectType) {
  CUSTOM_PRESET -> when (bandLevel) {
   0 -> 10  // +5 dB
   1 -> 10  // +5 dB
   2 -> 10  // +5 dB
   3 -> 10  // +5 dB
   4 -> 10  // +5 dB
   5 -> 10  // 0 dB
   6 -> 10  // -5 dB
   7 -> 10   // -10 dB
   8 -> 10   // -15 dB
   9 -> 10   // -20 dB
   else -> 10
  }
  ROCK_PRESET -> when (bandLevel) {
   0 -> 12  // +2 dB
   1 -> 13  // +3 dB
   2 -> 10  // 0 dB
   3 -> 8   // -2 dB
   4 -> 7   // -3 dB
   else -> 10
  }
  POP_PRESET -> when (bandLevel) {
   0 -> 10  // 0 dB
   1 -> 13  // +3 dB
   2 -> 12  // +1.5 dB
   3 -> 10  // 0 dB
   4 -> 7   // -3 dB
   else -> 10
  }
  BASS_PRESET -> when (bandLevel) {
   0 -> 12  // +3 dB
   1 -> 14  // +4 dB
   2 -> 11  // +2 dB
   3 -> 10  // 0 dB
   4 -> 7   // -3 dB
   else -> 10
  }
  FLAT_PRESET -> {
   10  // 0 dB para todas las bandas
  }
  JAZZ_PRESET -> when (bandLevel) {
   0 -> 9   // -1 dB
   1 -> 11  // +1 dB
   2 -> 12  // +2 dB
   3 -> 11  // +1 dB
   4 -> 9   // -1 dB
   else -> 10
  }
  CLASSICAL_PRESET -> when (bandLevel) {
   0 -> 10  // 0 dB
   1 -> 11  // +1 dB
   2 -> 12  // +2 dB
   3 -> 11  // +1 dB
   4 -> 10  // 0 dB
   else -> 10
  }
  HIP_HOP_PRESET -> when (bandLevel) {
   0 -> 12  // +2 dB
   1 -> 13  // +3 dB
   2 -> 11  // +1 dB
   3 -> 11  // +1 dB
   4 -> 9   // -1 dB
   else -> 10
  }
  ELECTRONIC_PRESET -> when (bandLevel) {
   0 -> 13  // +3 dB
   1 -> 14  // +4 dB
   2 -> 12  // +2 dB
   3 -> 11  // +1 dB
   4 -> 9   // -1 dB
   else -> 10
  }
  FULL_SOUND_PRESET -> when (bandLevel) {
   0 -> 14  // +4 dB
   1 -> 13  // +3 dB
   2 -> 12  // +2 dB
   3 -> 11  // +1 dB
   4 -> 10  // 0 dB
   else -> 10
  }
  FULL_BASS_AND_TREBLE_PRESET->when(bandLevel){
   0 -> 15  // +5 dB (60 Hz)
   1 -> 14  // +4 dB (230 Hz)
   2 -> 12  // +2 dB (910 Hz)
   3 -> 10  // 0 dB (3600 Hz)
   4 -> 14  // +4 dB (14000 Hz)
   5 -> 10  // 0 dB
   6 -> 12  // +2 dB
   7 -> 14  // +4 dB
   8 -> 15  // +5 dB
   9 -> 15  // +5 dB
   else -> 10
  }
  else -> 10
 }
}
fun getEqualizerConfig(effectType:Int, numOfBand:Int, prefs:EffectsPreferences): List<Short>{
 val listOfBands= arrayListOf<Short>()
 val lowerEqualizerBandLevel = -20
 for(i in 0 until numOfBand){
    val equalizerBandIndex=i
    val bandValue = prefs.getSeekBandValue(effectType,equalizerBandIndex)
    if(bandValue !=20) {
     listOfBands.add((bandValue + lowerEqualizerBandLevel).toShort())
    }else{
     val bandPresetValue = getEqualizerBandPreConfig(effectType, equalizerBandIndex)
     listOfBands.add((bandPresetValue + lowerEqualizerBandLevel).toShort())
    }
 }
 return listOfBands
}