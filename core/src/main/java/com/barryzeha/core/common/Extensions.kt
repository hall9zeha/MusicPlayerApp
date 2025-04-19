package com.barryzeha.core.common

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.IdRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.use
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.barryzeha.core.R
import com.barryzeha.core.model.entities.MusicState
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.Resource
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import kotlin.math.roundToInt


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 1/5/24.
 * Copyright (c)  All rights reserved.
 **/
 
fun Activity.showSnackBar(view: View, msg:String, duration:Int = Snackbar.LENGTH_SHORT)=
    Snackbar.make(view,msg,duration).show()

fun Activity.showSnackBar(view: View, msg:Int, duration:Int = Snackbar.LENGTH_SHORT)=
    Snackbar.make(view,msg,duration).show()
fun Activity.showOrHideKeyboard(show:Boolean,view:View, isShow:(imm:InputMethodManager?)->Unit, isHide:()->Unit){
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
    if(show){
        isShow(imm)
        imm!!.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }else{
        imm!!.hideSoftInputFromWindow(view.windowToken,0)
        isHide()
    }
}

@Suppress("DEPRECATION")
fun <T> Context.isServiceRunning(service:Class<T>):Boolean{
    return (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
        .getRunningServices(Integer.MAX_VALUE)
        .any{it.service.className == service.name}
}

fun ImageView.loadImage(bitmap:Bitmap, animDirection:Int=-1) {
    var direction =0f
    when (animDirection){
        NEXT->direction=1f
        PREVIOUS->direction= -1f
        DEFAULT_DIRECTION->direction=0f
    }
    Glide.with(this.context)
        .load(bitmap)
        .fitCenter()
        .into(this)
    this.translationX = direction * width // Coloca fuera de la pantalla
    this.animate()
        .translationX(0f) // Desliza a su posición original
        .setDuration(200)
        .setListener(null)

}
// El placeholder solo se usa para la carátula del fragmento principal
fun ImageView.loadImage(resource:Int)=
    Glide.with(this.context)
        .load(resource)
        .fitCenter()
        .placeholder(R.mipmap.ic_launcher)
        .into(this)
fun ImageView.loadImage(url:String)=
    Glide.with(this.context)
        .load(url)
        .centerCrop()
        .diskCacheStrategy(DiskCacheStrategy.NONE)
        // Importante para evitar que glide use imágenes almacenadas en memoria,
        // aunque se haya desabilitado con (DiskCacheStrategy.NONE)
        .skipMemoryCache(true)

        .placeholder(R.mipmap.ic_launcher)
        .into(this)
fun Int.adjustAlpha(factor: Float): Int =
    (this.ushr(24) * factor).roundToInt() shl 24 or (0x00FFFFFF and this)

inline val Int.alpha: Int
    get() = (this shr 24) and 0xFF

// Convert MusicState object to JSon
fun  <T> T.toJson():String{
    val gson = Gson()
    return gson.toJson(this)
}

// convert Json of MusicState to Object
fun String.toObject():MusicState{
    val gson = Gson()
    return gson.fromJson(this,MusicState::class.java)
}

fun getPixels(dipValue:Int, context: Context):Int{
    val r:Resources = context.resources
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dipValue.toFloat(),
        r.displayMetrics
    ).toInt()
}

fun RecyclerView.runWhenReady(action:()->Unit){
    val globalLayoutListener = object: ViewTreeObserver.OnGlobalLayoutListener{
        override fun onGlobalLayout() {
            action()
            viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    }
    viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
}
@StyleRes
fun Context.getThemeResValue():Int{
    val globalTheme = MyPreferences(this).globalTheme
    return when(globalTheme){
        SettingsKeys.DEFAULT_THEME.ordinal->{R.style.Theme_KTMusicPlayer_Material3}
        SettingsKeys.MATERIAL_YOU_THEME.ordinal->{R.style.Base_Theme_KTMusicPlayer}
        else->{
            R.style.Base_Theme_KTMusicPlayer
        }
    }
}
@StyleRes
fun Context.getThemeWithActionBarResValue():Int{
    val globalTheme = MyPreferences(this).globalTheme
    return when(globalTheme){
        SettingsKeys.DEFAULT_THEME.ordinal->{R.style.Theme_KTMusicPlayer_ActionBar}
        SettingsKeys.MATERIAL_YOU_THEME.ordinal->{R.style.Base_Theme_KTMusicPlayer_ActionBar}
        else->{
            R.style.Base_Theme_KTMusicPlayer_ActionBar
        }
    }
}
@Suppress("UNCHECKED_CAST")
fun <T> AppCompatActivity.getFragment(@IdRes id: Int): T {
    return supportFragmentManager.findFragmentById(id) as T
}
@Suppress("UNCHECKED_CAST")
fun <T> Fragment.getFragment(@IdRes id: Int): T {
    return childFragmentManager.findFragmentById(id) as T
}
