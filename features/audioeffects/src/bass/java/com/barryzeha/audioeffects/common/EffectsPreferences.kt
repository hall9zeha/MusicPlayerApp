package com.barryzeha.audioeffects.common

import android.content.Context
import javax.inject.Inject
import androidx.core.content.edit


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/8/24.
 * Copyright (c)  All rights reserved.
 **/

private const val EFFECTS_PREFERENCES_FILE="bassEffectsPreferencesFiles"
private const val SEEK_BAND = "_seekBand_"
private const val EFFECT_TYPE = "effectType"
private const val ENABLE_EFFECTS="enableEffects"
private const val REVERB="REVERB"

class EffectsPreferences @Inject constructor(private val ctx: Context) {
    private var mPreferences = ctx.getSharedPreferences(EFFECTS_PREFERENCES_FILE,Context.MODE_PRIVATE)
    fun clearPreference(){
        mPreferences.edit { clear() }
    }
    fun setSeekBandValue(effectType:Int,seekId:Int,seekValue:Float){
        mPreferences.edit().putFloat("$effectType$SEEK_BAND$seekId",seekValue).apply()
    }
    fun getSeekBandValue(effectType: Int,seekId: Int):Float{
        return mPreferences.getFloat("$effectType$SEEK_BAND$seekId",30f)
    }
    var effectsIsEnabled:Boolean
        get()=mPreferences.getBoolean(ENABLE_EFFECTS,false)
        set(value)=mPreferences.edit().putBoolean(ENABLE_EFFECTS,value).apply()

    var effectType:Int
        get() = mPreferences.getInt(EFFECT_TYPE,0)
        set(value)=mPreferences.edit().putInt(EFFECT_TYPE,value).apply()

    fun getReverbSeekBandValue(effectType: Int,seekId: Int):Float{
        return mPreferences.getFloat("$effectType$SEEK_BAND$seekId",0f)
    }
    fun getVolumeSeekBandValue(effectType: Int,seekId: Int):Float{
        return mPreferences.getFloat("$effectType$SEEK_BAND$seekId",15f)
    }
    fun clearSeekBandPreferences(){
        val allPrefs = mPreferences.all
        val editor = mPreferences.edit()
        for((key,_) in allPrefs){
            if(key.contains(SEEK_BAND)){
                editor.remove(key)
            }
        }
        mPreferences.edit().remove(EFFECT_TYPE).apply()
        editor.apply()
    }
}