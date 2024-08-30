package com.barryzeha.audioeffects.common

import android.content.Context
import javax.inject.Inject


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 30/8/24.
 * Copyright (c)  All rights reserved.
 **/

private const val EFFECTS_PREFERENCES_FILE="effectsPreferencesFiles"
private const val SEEK_BAND = "_seekBand_"

class Preferences @Inject constructor(private val ctx: Context) {
    private var mPreferences = ctx.getSharedPreferences(EFFECTS_PREFERENCES_FILE,Context.MODE_PRIVATE)

    fun setSeekBandValue(effectType:Int,seekId:Int,seekValue:Int){
        mPreferences.edit().putInt("$effectType$SEEK_BAND$seekId",seekValue).apply()
    }
    fun getSeekBandValue(effectType: Int,seekId: Int):Int{
        return mPreferences.getInt("$effectType$SEEK_BAND$seekId",1500)
    }
}