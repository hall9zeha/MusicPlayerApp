package com.barryzeha.audioeffects.common


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 31/8/24.
 * Copyright (c)  All rights reserved.
 **/

fun getEqualizerBandPreConfig(effectType:Int,bandLevel:Int):Int{
 return when (effectType) {
  CUSTOM_PRESET -> 10
  ROCK_PRESET -> when (bandLevel) {
   0 -> 14 // +4 dB (sub-bajo)
   1 -> 12 // +2 dB
   2 -> 10 // 0 dB
   3 -> 8  // -2 dB
   4 -> 6  // -4 dB (medios)
   5 -> 10 // 0 dB
   6 -> 11 // +1 dB
   7 -> 12 // +2 dB
   8 -> 10 // 0 dB
   9 -> 8  // -2 dB
   else -> 10
  }
  POP_PRESET -> when (bandLevel) {
   0 -> 12 // +2 dB (sub-bajo)
   1 -> 14 // +4 dB
   2 -> 12 // +2 dB
   3 -> 10 // 0 dB
   4 -> 8  // -2 dB
   5 -> 9  // -1 dB
   6 -> 10 // 0 dB
   7 -> 9  // -1 dB
   8 -> 7  // -3 dB
   9 -> 5  // -5 dB
   else -> 10
  }
  JAZZ_PRESET -> when (bandLevel) {
   0 -> 10 // 0 dB
   1 -> 12 // +2 dB
   2 -> 13 // +3 dB
   3 -> 11 // +1 dB
   4 -> 9  // -1 dB
   5 -> 10 // 0 dB
   6 -> 12 // +2 dB
   7 -> 11 // +1 dB
   8 -> 10 // 0 dB
   9 -> 9  // -1 dB
   else -> 10
  }
  CLASSICAL_PRESET -> when (bandLevel) {
   0 -> 10 // 0 dB
   1 -> 12 // +2 dB
   2 -> 13 // +3 dB
   3 -> 12 // +2 dB
   4 -> 11 // +1 dB
   5 -> 10 // 0 dB
   6 -> 9  // -1 dB
   7 -> 10 // 0 dB
   8 -> 10 // 0 dB
   9 -> 10 // 0 dB
   else -> 10
  }
  HIP_HOP_PRESET -> when (bandLevel) {
   0 -> 15 // +5 dB
   1 -> 14 // +4 dB
   2 -> 12 // +2 dB
   3 -> 11 // +1 dB
   4 -> 10 // 0 dB
   5 -> 9  // -1 dB
   6 -> 10 // 0 dB
   7 -> 12 // +2 dB
   8 -> 13 // +3 dB
   9 -> 14 // +4 dB
   else -> 10
  }
  BASS_PRESET -> when (bandLevel) {
   0 -> 15 // +5 dB (sub-bajo)
   1 -> 14 // +4 dB
   2 -> 12 // +2 dB
   3 -> 10 // 0 dB
   4 -> 9  // -1 dB
   5 -> 10 // 0 dB
   6 -> 12 // +2 dB
   7 -> 14 // +4 dB
   8 -> 15 // +5 dB
   9 -> 15 // +5 dB
   else -> 10 // Default
  }
  ELECTRONIC_PRESET -> when (bandLevel) {
   0 -> 14 // +4 dB
   1 -> 13 // +3 dB
   2 -> 12 // +2 dB
   3 -> 11 // +1 dB
   4 -> 10 // 0 dB
   5 -> 9  // -1 dB
   6 -> 11 // +1 dB
   7 -> 13 // +3 dB
   8 -> 14 // +4 dB
   9 -> 15 // +5 dB
   else -> 10
  }
  FULL_SOUND_PRESET -> when (bandLevel) {
   0 -> 14 // +4 dB
   1 -> 13 // +3 dB
   2 -> 12 // +2 dB
   3 -> 11 // +1 dB
   4 -> 10 // 0 dB
   5 -> 12 // +2 dB
   6 -> 13 // +3 dB
   7 -> 14 // +4 dB
   8 -> 15 // +5 dB
   9 -> 15 // +5 dB
   else -> 10
  }
  FULL_BASS_AND_TREBLE_PRESET -> when (bandLevel) {
   0 -> 15 // +5 dB
   1 -> 14 // +4 dB
   2 -> 12 // +2 dB
   3 -> 10 // 0 dB
   4 -> 14 // +4 dB
   5 -> 10 // 0 dB
   6 -> 12 // +2 dB
   7 -> 14 // +4 dB
   8 -> 15 // +5 dB
   9 -> 15 // +5 dB
   else -> 10
  }
  else -> 10
 }
}
