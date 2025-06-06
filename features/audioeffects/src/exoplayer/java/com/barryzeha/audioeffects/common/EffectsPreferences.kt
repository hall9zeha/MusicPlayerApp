package com.barryzeha.audioeffects.common

import android.content.Context
import javax.inject.Inject
import androidx.core.content.edit


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/8/24.
 * Copyright (c)  All rights reserved.
 **/

private const val EFFECTS_PREFERENCES_FILE="effectsPreferencesFiles"
private const val SEEK_BAND = "_seekBand_"
private const val EFFECT_TYPE = "effectType"
private const val ENABLE_EFFECTS="enableEffects"

class EffectsPreferences @Inject constructor(private val ctx: Context) {
    private var mPreferences = ctx.getSharedPreferences(EFFECTS_PREFERENCES_FILE,Context.MODE_PRIVATE)
    fun clearPreference(){
        mPreferences.edit().clear().apply()
    }
    fun setSeekBandValue(effectType:Int,seekId:Int,seekValue:Int){
        mPreferences.edit().putInt("$effectType$SEEK_BAND$seekId",seekValue).apply()
    }
    fun getSeekBandValue(effectType: Int,seekId: Int):Int{
        return mPreferences.getInt("$effectType$SEEK_BAND$seekId",1500)
    }
    var effectsIsEnabled:Boolean
        get()=mPreferences.getBoolean(ENABLE_EFFECTS,false)
        set(value)=mPreferences.edit().putBoolean(ENABLE_EFFECTS,value).apply()

    var effectType:Int
        get() = mPreferences.getInt(EFFECT_TYPE,0)
        set(value)=mPreferences.edit().putInt(EFFECT_TYPE,value).apply()

    fun clearSeekBandPreferences(){
        val allPrefs = mPreferences.all
        val editor = mPreferences.edit()

        for((key,_) in allPrefs){
            if(key.contains(SEEK_BAND)) editor.remove(key)
        }
        mPreferences.edit() { remove(EFFECT_TYPE) }
        editor.apply()
    }

}