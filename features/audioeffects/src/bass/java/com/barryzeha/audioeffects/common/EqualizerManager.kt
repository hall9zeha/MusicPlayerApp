package com.barryzeha.audioeffects.common

import android.util.Log
import com.un4seen.bass.BASS
import com.un4seen.bass.BASS.BASS_DX8_PARAMEQ
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ln
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
private var channelIntent=0
object EqualizerManager {

    // For bass
    fun applyEqualizer(channel:Int, prefs:EffectsPreferences){
        channelIntent=channel
        chan= channelIntent
        mPrefs=prefs
        if(prefs.effectsIsEnabled){
            setEffect(true)
            setUpEqValues(prefs)
        }else{
            //enableOrDisableEffects(false){}
        }
    }
    private fun setUpEqValues(prefs: EffectsPreferences) {
        setupFX() { seekId ->
            val eqValue = prefs.getSeekBandValue(prefs.effectType, seekId)

            // Si no hay cambios en los valores de una banda de ecualizador en preferencias cargar los valores predefinidos
            val bandValue = if (eqValue.toInt() != 30) eqValue else getEqualizerBandPreConfig(
                prefs.effectType,
                seekId
            )
            updateFX(seekId, bandValue)
        }
        val reverbValue = prefs.getReverbSeekBandValue(prefs.effectType, coreRes.id.reverb)
        updateFX(fxArray.size - 1, reverbValue)
        val volumeValue = prefs.getVolumeSeekBandValue(prefs.effectType, coreRes.id.volume)
        updateFX(11, volumeValue)
    }

    fun setupFX(fxIndex:(index:Int)->Unit) {
        // setup the effects
        val chan = if(fxChan !=0) fxChan else chan
        for (i in 0 until  fxArray.size - 1) {
            fxArray[i] = BASS.BASS_ChannelSetFX(chan, BASS.BASS_FX_DX8_PARAMEQ, 0)
            val p = BASS_DX8_PARAMEQ()
            p.fGain = 0f
            p.fBandwidth = 18f
            p.fCenter =(125f * Math.pow(2.0, (i - 1).toDouble())).toFloat() // Frecuencias centradas
            BASS.BASS_FXSetParameters(fxArray[i], p)
            // Enviamos el índice donde lo necesitemos
            fxIndex(i)
        }
        fxArray[fxArray.size - 1] = BASS.BASS_ChannelSetFX(chan, BASS.BASS_FX_DX8_REVERB, 0)
        updateFX(fxArray.size - 1, 0f)
        mPrefs?.let {prefs->
            val volumeValue = prefs.getVolumeSeekBandValue(prefs.effectType, coreRes.id.volume)
            BASS.BASS_ChannelSetAttribute(chan, BASS.BASS_ATTRIB_VOL, volumeValue / 15f)
        }

    }

    fun updateFX(index: Int, value: Float) {
        val n = index
        val v = value
        if (n < fxArray.size - 1) { // EQ
            val p: BASS.BASS_DX8_PARAMEQ = BASS.BASS_DX8_PARAMEQ()
            BASS.BASS_FXGetParameters(fxArray[n], p)
            p.fGain = v.coerceIn(-15f, 15f).takeIf { it.isFinite() } ?: 0f // para evitar valores infinitos si usamos decimales
            BASS.BASS_FXSetParameters(fxArray[n], p)
            val check = BASS.BASS_FXSetParameters(fxArray[n], p)
            if (!check) {
                Log.e("BASS_ERROR", "SetParameters failed on FX[$n]: ${BASS.BASS_ErrorGetCode()}")
            }
        } else if (n == fxArray.size - 1) { // reverb
            val p: BASS.BASS_DX8_REVERB = BASS.BASS_DX8_REVERB()
            BASS.BASS_FXGetParameters(fxArray[n], p)
            p.fReverbMix = (if (v.toInt() != 0) (Math.log(v.toInt() / 30.0) * 30).toFloat() else (-96).toFloat())
            BASS.BASS_FXSetParameters(fxArray[n], p)
        } else // volume
            BASS.BASS_ChannelSetAttribute(chan, BASS.BASS_ATTRIB_VOL, v / 15f)

    }
    fun setEffect(isEnable: Boolean){
        val ch = if (fxChan != 0) fxChan else chan
        for (i in fxArray.indices) {
            BASS.BASS_ChannelRemoveFX(ch, fxArray[i])
        }
        if (isEnable) {
            fxChan= BASS.BASS_StreamCreate(0, 0, 0, BASS.STREAMPROC_DEVICE, null)
        } else {
            fxChan=0
        }
    }
    fun enableOrDisableEffects(isEnable:Boolean,setEnabled:(isEnable:Boolean)->Unit){
        val ch = if (fxChan != 0) fxChan else chan
        for (i in  fxArray.indices) {
            BASS.BASS_ChannelRemoveFX(ch, fxArray[i])
        }
        if (isEnable) {
            fxChan= BASS.BASS_StreamCreate(0, 0, 0, BASS.STREAMPROC_DEVICE, null)
            chan=channelIntent
            mPrefs?.effectsIsEnabled = true
            setEnabled(true)

        } else {
            fxChan=0
            chan=0
            mPrefs?.effectsIsEnabled = false
            BASS.BASS_ChannelSetAttribute(chan, BASS.BASS_ATTRIB_VOL, 1f)
            setEnabled(false)

        }
    }

}