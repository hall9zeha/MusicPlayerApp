package com.barryzeha.audioeffects.common

import android.util.Log
import android.widget.SeekBar
import androidx.core.view.get
import com.barryzeha.core.R
import com.un4seen.bass.BASS
import com.un4seen.bass.BASS.BASS_DX8_PARAMEQ
import com.barryzeha.core.R as coreRes

/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 3/9/24.
 * Copyright (c)  All rights reserved.
 **/
private val fxArray:IntArray = IntArray(11)
private var chan=0
private var fxChan=0
private var mPrefs:EffectsPreferences?=null
object EqualizerManager {

 // For bass
 fun initEqualizer(channel:Int, prefs:EffectsPreferences){
   chan=channel
     mPrefs=prefs
   if(prefs.effectsIsEnabled){
      setUpEqValues(prefs)
   }
 }

    private fun setUpEqValues(prefs: EffectsPreferences) {
        setupFX(prefs)
        for (i in 0 until fxArray.size - 1) {
            val seekId = i
            val eqValue = prefs.getSeekBandValue(prefs.effectType, seekId)
            // Si no hay cambios en los valores de una banda de equalizador en preferencias cargar los valores predefinidos
            val bandValue =
                if (eqValue != 20) eqValue else getEqualizerBandPreConfig(prefs.effectType, seekId)
            updateFX(i, bandValue)
        }
        val reverbValue = prefs.getReverbSeekBandValue(prefs.effectType, coreRes.id.reverb)
        updateFX(fxArray.size - 1, reverbValue)
        val volumeValue = prefs.getVolumeSeekBandValue(prefs.effectType, coreRes.id.volume)
        updateFX(11, volumeValue)

    }

    private fun setupFX(prefs: EffectsPreferences) {
        // setup the effects

        for (i in fxArray.indices - 1) {

            fxArray[i] = BASS.BASS_ChannelSetFX(chan, BASS.BASS_FX_DX8_PARAMEQ, 0)
            val p = BASS_DX8_PARAMEQ()
            p.fGain = 0f
            p.fBandwidth = 18f
            p.fCenter =
                (125f * Math.pow(2.0, (i - 1).toDouble())).toFloat() // Frecuencias centradas
            BASS.BASS_FXSetParameters(fxArray[i], p)

        }
        fxArray[fxArray.size - 1] = BASS.BASS_ChannelSetFX(chan, BASS.BASS_FX_DX8_REVERB, 0)
        updateFX(fxArray.size - 1, fxArray[fxArray.size - 1])
        val volumeValue=prefs.getVolumeSeekBandValue(prefs.effectType,coreRes.id.volume)
        BASS.BASS_ChannelSetAttribute(chan, BASS.BASS_ATTRIB_VOL, volumeValue / 10f);

    }

    private fun updateFX(index: Int, value: Int) {
        val n = index
        val v = value;
        if (n < fxArray.size - 1) { // EQ
            val p: BASS.BASS_DX8_PARAMEQ = BASS.BASS_DX8_PARAMEQ()
            BASS.BASS_FXGetParameters(fxArray[n], p);
            p.fGain = (v - 10).toFloat()
            BASS.BASS_FXSetParameters(fxArray[n], p);
        } else if (n == fxArray.size - 1) { // reverb
            val p: BASS.BASS_DX8_REVERB = BASS.BASS_DX8_REVERB()
            BASS.BASS_FXGetParameters(fxArray[n], p);
            p.fReverbMix = (if (v != 0) (Math.log(v / 20.0) * 20).toFloat() else (-96).toFloat())
            BASS.BASS_FXSetParameters(fxArray[n], p);
        } else // volume
            BASS.BASS_ChannelSetAttribute(chan, BASS.BASS_ATTRIB_VOL, v / 10f);

    }
    fun applyEq(){
        mPrefs?.let {prefs->
            for (i in 0 until fxArray.size - 1) {
                val seekId = i
                val eqValue = prefs.getSeekBandValue(prefs.effectType, seekId)
                // Si no hay cambios en los valores de una banda de equalizador en preferencias cargar los valores predefinidos
                val bandValue =
                    if (eqValue != 20) eqValue else getEqualizerBandPreConfig(prefs.effectType, seekId)
                updateFX(i, bandValue)
            }
            val reverbValue = prefs.getReverbSeekBandValue(prefs.effectType, coreRes.id.reverb)
            updateFX(fxArray.size - 1, reverbValue)
            val volumeValue = prefs.getVolumeSeekBandValue(prefs.effectType, coreRes.id.volume)
            updateFX(11, volumeValue)
        }
    }
 fun release() {

 }


}