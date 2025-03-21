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
   0 -> 13  // -2 dB (Subgraves - para un poco de cuerpo en el bajo)
   1 -> 12  // -1 dB (Graves - para mantener los bajos sólidos)
   2 -> 14  // 0 dB (Graves medios - equilibrio)
   3 -> 15  // 0 dB (Medios - claridad en las guitarras y voces)
   4 -> 13  // -2 dB (Medios altos - evitar que las voces se "pierdan")
   5 -> 15  // 0 dB (Medios altos - balance general)
   6 -> 16  // +1 dB (Agudos - para darle aire sin ser demasiado brillante)
   7 -> 17  // +2 dB (Agudos altos - definición extra en el platillo y detalles)
   8 -> 15  // 0 dB (Super agudos - mantener balance)
   9 -> 14  // -1 dB (Subgraves - refuerzo moderado)
   else -> 15
  }
  POP_PRESET -> when (bandLevel) {
   0 -> 10  // -5 dB (Subgraves - mantén el bajo presente pero no exagerado)
   1 -> 12  // -3 dB (Graves - para que no sobrecargue)
   2 -> 14  // 0 dB (Graves medios - claridad para bajos y percusión)
   3 -> 15  // 0 dB (Medios - vocales bien equilibradas)
   4 -> 14  // +1 dB (Medios altos - aumentar presencia vocal)
   5 -> 15  // 0 dB (Medios altos - claridad balanceada)
   6 -> 17  // +2 dB (Agudos - brillo para instrumentos y voces)
   7 -> 18  // +3 dB (Super agudos - para mayor definición)
   8 -> 17  // +2 dB (Subgraves - un toque extra de cuerpo)
   9 -> 15  // 0 dB (Subgraves - refuerzo moderado)
   else -> 15
  }
  JAZZ_PRESET -> when (bandLevel) {
   0 -> 9   // -6 dB (Subgraves - poco énfasis en el bajo, mantener natural)
   1 -> 11  // -4 dB (Graves - claridad en los instrumentos bajos)
   2 -> 14  // 0 dB (Graves medios - para un sonido natural)
   3 -> 15  // 0 dB (Medios - equilibrar la presencia de instrumentos)
   4 -> 16  // +1 dB (Medios altos - claridad extra)
   5 -> 15  // 0 dB (Medios altos - equilibrio general)
   6 -> 16  // +1 dB (Agudos - para darle brillo sin ser áspero)
   7 -> 17  // +2 dB (Super agudos - darle aire a los instrumentos)
   8 -> 15  // 0 dB (Subgraves - balance)
   9 -> 14  // 0 dB (Subgraves - refuerzo moderado)
   else -> 15
  }
  CLASSICAL_PRESET -> when (bandLevel) {
   0 -> 12  // +1 dB (Subgraves - mantener el bajo natural)
   1 -> 14  // +2 dB (Graves - cuerpo moderado)
   2 -> 15  // 0 dB (Graves medios - no se quiere que los bajos sobresalgan)
   3 -> 15  // 0 dB (Medios - claridad de cuerdas y cuerdas vocales)
   4 -> 14  // +1 dB (Medios altos - para las cuerdas más brillantes)
   5 -> 15  // 0 dB (Medios altos - balance)
   6 -> 14  // -1 dB (Agudos - no demasiado brillante)
   7 -> 15  // 0 dB (Super agudos - equilibrio)
   8 -> 15  // 0 dB (Subgraves - moderado)
   9 -> 15  // 0 dB (Super agudos - claridad sin ser intrusivo)
   else -> 15
  }
  HIP_HOP_PRESET -> when (bandLevel) {
   0 -> 18  // +3 dB (Subgraves - refuerzo fuerte para un bajo potente)
   1 -> 17  // +2 dB (Graves - para darle cuerpo a los bajos)
   2 -> 15  // 0 dB (Graves medios - claridad sin perder peso en el bajo)
   3 -> 14  // -1 dB (Medios - mantener las vocales claras pero no sobrecargar)
   4 -> 15  // 0 dB (Medios altos - claridad en las voces)
   5 -> 16  // +1 dB (Medios altos - aumentar la presencia vocal)
   6 -> 16  // +1 dB (Agudos - para dar algo de brillo a los instrumentos)
   7 -> 17  // +2 dB (Super agudos - para darle un toque brillante a los efectos)
   8 -> 16  // +1 dB (Subgraves - para mantener el golpe)
   9 -> 15  // 0 dB (Subgraves - balance general)
   else -> 15
  }
  BASS_PRESET -> when (bandLevel) {
   0 -> 19  // +4 dB (Subgraves - refuerzo masivo para un bajo profundo)
   1 -> 18  // +3 dB (Graves - refuerzo de graves para mayor cuerpo)
   2 -> 16  // +1 dB (Graves medios - para dar un equilibrio en la respuesta)
   3 -> 15  // 0 dB (Medios - para no sobresaturar las frecuencias medias)
   4 -> 14  // -1 dB (Medios altos - mantener los medios sin que se vuelvan demasiado gruesos)
   5 -> 14  // -1 dB (Medios altos - para mantener la claridad)
   6 -> 17  // +2 dB (Agudos - brillo en los hi-hats y efectos)
   7 -> 18  // +3 dB (Super agudos - definición en detalles y efectos)
   8 -> 17  // +2 dB (Subgraves - refuerzo adicional para asegurar el impacto)
   9 -> 16  // +1 dB (Subgraves - balance general para evitar que el sonido se sature)
   else -> 15 // Default
  }
  ELECTRONIC_PRESET -> when (bandLevel) {
   0 -> 14  // +4 dB (Subgraves - para mucha presencia en el bajo)
   1 -> 16  // +5 dB (Graves - refuerzo de graves)
   2 -> 15  // +4 dB (Graves medios - balance)
   3 -> 15  // 0 dB (Medios - equilibrio para vocales)
   4 -> 14  // +3 dB (Medios altos - claridad)
   5 -> 15  // 0 dB (Medios altos - definición)
   6 -> 18  // +6 dB (Agudos - brillo para efectos electrónicos)
   7 -> 19  // +7 dB (Super agudos - brillantez)
   8 -> 17  // +5 dB (Subgraves - para refuerzo)
   9 -> 15  // 0 dB (Subgraves - moderado)
   else -> 15
  }
  FULL_SOUND_PRESET -> when (bandLevel) {
   0 -> 18  // +3 dB (Subgraves - mejorar la presencia de los bajos sin ser demasiado intrusivos)
   1 -> 17  // +2 dB (Graves - para asegurar que el sonido se sienta lleno)
   2 -> 16  // +1 dB (Graves medios - dar cuerpo sin perder definición)
   3 -> 15  // 0 dB (Medios - claridad en las voces e instrumentos sin distorsión)
   4 -> 15  // 0 dB (Medios - asegurar que las frecuencias medias sean bien equilibradas)
   5 -> 16  // +1 dB (Medios altos - para darle definición y brillo a las voces e instrumentos)
   6 -> 16  // +1 dB (Agudos - brillo general sin ser excesivos)
   7 -> 17  // +2 dB (Super agudos - resaltar detalles como percusión y efectos sin volverse chirriante)
   8 -> 17  // +2 dB (Subgraves - dar impacto sin perder el balance)
   9 -> 18  // +3 dB (Subgraves - para un bajo muy presente y definido)
   else -> 15
  }
  FULL_BASS_AND_TREBLE_PRESET -> when (bandLevel) {
   0 -> 19  // +4 dB (Subgraves - refuerzo para el bajo profundo)
   1 -> 18  // +3 dB (Graves - refuerzo adicional para una mayor presencia de los bajos)
   2 -> 16  // +1 dB (Graves medios - equilibrar sin que se sobrecarguen)
   3 -> 15  // 0 dB (Medios - claridad sin sobrecargar)
   4 -> 14  // -1 dB (Medios altos - mantener la definición de las voces e instrumentos)
   5 -> 14  // -1 dB (Medios altos - para evitar distorsión)
   6 -> 17  // +2 dB (Agudos - para brillo y claridad)
   7 -> 18  // +3 dB (Super agudos - refuerzo para detalles y definición)
   8 -> 18  // +3 dB (Subgraves - para un golpe más fuerte y profundo)
   9 -> 18  // +3 dB (Subgraves - asegurarse de que el bajo no pierda fuerza)
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
