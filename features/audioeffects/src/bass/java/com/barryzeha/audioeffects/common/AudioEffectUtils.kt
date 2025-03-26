package com.barryzeha.audioeffects.common


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 31/8/24.
 * Copyright (c)  All rights reserved.
 **/

fun getEqualizerBandPreConfig(effectType:Int,bandLevel:Int):Int{
 return when (effectType) {
  CUSTOM_PRESET -> 15
  ROCK_PRESET -> when (bandLevel) {
   0 -> 13  // -2 dB
   1 -> 12  // -1 dB
   2 -> 14  // 0 dB
   3 -> 15  // 0 dB
   4 -> 13  // -2 dB
   5 -> 15  // 0 dB
   6 -> 16  // +1 dB
   7 -> 17  // +2 dB
   8 -> 15  // 0 dB
   9 -> 14  // -1 dB
   else -> 15
  }
  POP_PRESET -> when (bandLevel) {
   0 -> 10  // -5 dB
   1 -> 12  // -3 dB
   2 -> 14  // 0 dB
   3 -> 15  // 0 dB
   4 -> 14  // +1 dB
   5 -> 15  // 0 dB
   6 -> 17  // +2 dB
   7 -> 18  // +3 dB
   8 -> 17  // +2 dB
   9 -> 15  // 0 dB
   else -> 15
  }
  JAZZ_PRESET -> when (bandLevel) {
   0 -> 9   // -6 dB
   1 -> 11  // -4 dB
   2 -> 14  // 0 dB
   3 -> 15  // 0 dB
   4 -> 16  // +1 dB
   5 -> 15  // 0 dB
   6 -> 16  // +1 dB
   7 -> 17  // +2 dB
   8 -> 15  // 0 dB
   9 -> 14  // 0 dB
   else -> 15
  }
  CLASSICAL_PRESET -> when (bandLevel) {
   0 -> 12  // +1 dB
   1 -> 14  // +2 dB
   2 -> 15  // 0 dB
   3 -> 15  // 0 dB
   4 -> 14  // +1 dB
   5 -> 15  // 0 dB
   6 -> 14  // -1 dB
   7 -> 15  // 0 dB
   8 -> 15  // 0 dB
   9 -> 15  // 0 dB
   else -> 15
  }
  HIP_HOP_PRESET -> when (bandLevel) {
   0 -> 18  // +3 dB
   1 -> 17  // +2 dB
   2 -> 15  // 0 dB
   3 -> 14  // -1 dB
   4 -> 15  // 0 dB
   5 -> 16  // +1 dB
   6 -> 16  // +1 dB
   7 -> 17  // +2 dB
   8 -> 16  // +1 dB
   9 -> 15  // 0 dB
   else -> 15
  }
  BASS_PRESET -> when (bandLevel) {
   0 -> 19  // +4 dB
   1 -> 18  // +3 dB
   2 -> 16  // +1 dB
   3 -> 15  // 0 dB
   4 -> 14  // -1 dB
   5 -> 14  // -1 dB
   6 -> 17  // +2 dB
   7 -> 18  // +3 dB
   8 -> 17  // +2 dB
   9 -> 16  // +1 dB
   else -> 15 // Default
  }
  ELECTRONIC_PRESET -> when (bandLevel) {
   0 -> 14  // +4 dB
   1 -> 16  // +5 dB
   2 -> 15  // +4 dB
   3 -> 15  // 0 dB
   4 -> 14  // +3 dB
   5 -> 15  // 0 dB
   6 -> 18  // +6 dB
   7 -> 19  // +7 dB
   8 -> 17  // +5 dB
   9 -> 15  // 0 dB
   else -> 15
  }
  FULL_SOUND_PRESET -> when (bandLevel) {
   0 -> 18  // +3 dB
   1 -> 17  // +2 dB
   2 -> 16  // +1 dB
   3 -> 15  // 0 dB
   4 -> 15  // 0 dB
   5 -> 16  // +1 dB
   6 -> 16  // +1 dB
   7 -> 17  // +2 dB
   8 -> 17  // +2 dB
   9 -> 18  // +3 dB
   else -> 15
  }
  FULL_BASS_AND_TREBLE_PRESET -> when (bandLevel) {
   0 -> 19  // +4 dB
   1 -> 18  // +3 dB
   2 -> 16  // +1 dB
   3 -> 15  // 0 dB
   4 -> 14  // -1 dB
   5 -> 14  // -1 dB
   6 -> 17  // +2 dB
   7 -> 18  // +3 dB
   8 -> 18  // +3 dB
   9 -> 18  // +3 dB 
   else -> 15
  }
  HEAD_PHONE-> when (bandLevel){
   0 -> 18
   1 -> 17
   2 -> 18
   3 -> 13
   4 -> 12
   5 -> 13
   6 -> 18
   7 -> 19
   8 -> 18
   9 -> 17
   else ->15
  }
  else -> 15
 }
}
