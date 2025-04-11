package com.barryzeha.audioeffects.common


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 31/8/24.
 * Copyright (c)  All rights reserved.
 **/

fun getEqualizerBandPreConfig(effectType:Int,bandLevel:Int):Float{
 return when (effectType) {
  CUSTOM_PRESET -> 0f
  ROCK_PRESET -> when (bandLevel) {
   0 -> 5.4f
   1 -> 4.5f
   2 -> -3.6f
   3 -> -6.6f
   4 -> -2.7f
   5 ->  2.1f
   6 -> 6.0f
   7 -> 7.5f
   8 -> 7.8f
   9 -> 8.1f
   else -> 0f
  }
  POP_PRESET -> when (bandLevel) {
   0 -> -1.6f
   1 -> 0.6f
   2 -> 5.4f
   3 -> 4.5f
   4 -> 0.9f
   5 -> -1.5f
   6 -> -1.8f
   7 -> -2.1f
   8 -> -2.1f
   9 -> -0.3f
   else -> 0f
  }
  JAZZ_PRESET -> when (bandLevel) {
   0 -> 3f
   1 -> 6.3f
   2 -> 3.6f
   3 -> -3.9f
   4 -> -5.1f
   5 -> 1.2f
   6 -> 9f
   7 -> -1.8f
   8 -> -2.4f
   9 -> 2.5f
   else -> 0f
  }
  CLASSICAL_PRESET -> when (bandLevel) {
   0 -> 0f
   1 -> 0f
   2 -> 0f
   3 -> 0f
   4 -> 0f
   5 -> 0f
   6 -> 0f
   7 -> -2.9f
   8 -> -5.8f
   9 -> -8.1f
   else -> 0f
  }
  HIP_HOP_PRESET -> when (bandLevel) {
   0 -> 5.0f
   1 -> 4.0f
   2 -> 1.5f
   3 -> 2.5f
   4 -> -1.5f
   5 -> -1.5f
   6 -> 1.2f
   7 -> -1f
   8 -> 2f
   9 -> 2.5f
   else -> 0f
  }
  BASS_PRESET -> when (bandLevel) {
   0 -> 7.4f
   1 -> 7.2f
   2 -> 7.2f
   3 -> 4.2f
   4 -> -0.3f
   5 -> -3.0f
   6 -> -4.5f
   7 -> -6.0f
   8 -> -7.2f
   9 -> -9f
   else -> 0f
  }
  ELECTRONIC_PRESET -> when (bandLevel) {
   0 -> 4f
   1 -> 5f
   2 -> 4f
   3 -> 0f
   4 -> 3f
   5 -> 0f
   6 -> 6f
   7 -> 7f
   8 -> 5f
   9 -> 0f
   else -> 0f
  }
  FULL_SOUND_PRESET -> when (bandLevel) {
   0 -> 3.8f
   1 -> 3.2f
   2 -> 2f
   3 -> 1.2f
   4 -> 0.5f
   5 -> 0.5f
   6 -> 1.8f
   7 -> 3f
   8 -> 3.5f
   9 -> 4f
   else -> 0f
  }
  FULL_BASS_AND_TREBLE_PRESET -> when (bandLevel) {
   0 -> 7.4f
   1 -> 4.2f
   2 -> 6.6f
   3 -> 0f
   4 -> -2.1f
   5 -> 0f
   6 -> 2.1f
   7 -> 6.0f
   8 -> 8.4f
   9 -> 9.2f
   else -> 0f
  }
  HEAD_PHONE-> when (bandLevel){
   0 -> 2.2f
   1 -> 4f
   2 -> 1.9f
   3 -> -3.6f
   4 -> -4.5f
   5 -> -3f
   6 -> 2f
   7 -> 1.7f
   8 -> 3.9f
   9 -> 1.8f
   else ->0f
  }
  else -> 0f
 }
}
