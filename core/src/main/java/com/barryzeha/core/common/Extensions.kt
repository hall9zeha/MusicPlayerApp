package com.barryzeha.core.common

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.barryzeha.core.R
import com.barryzeha.core.model.entities.MusicState
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.Resource
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlin.math.roundToInt


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 1/5/24.
 * Copyright (c)  All rights reserved.
 **/
 
fun Activity.showSnackBar(view: View, msg:String, duration:Int = Snackbar.LENGTH_SHORT)=
    Snackbar.make(view,msg,duration).show()

@Suppress("DEPRECATION")
fun <T> Context.isServiceRunning(service:Class<T>):Boolean{
    return (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
        .getRunningServices(Integer.MAX_VALUE)
        .any{it.service.className == service.name}
}

fun ImageView.loadImage(bitmap:Bitmap) {
    val slideAnimation = TranslateAnimation(
        Animation.RELATIVE_TO_PARENT, -1f, // Desde fuera de la pantalla a la izquierda
        Animation.RELATIVE_TO_PARENT, 0f,  // Hasta su posición original
        Animation.RELATIVE_TO_PARENT, 0f,  // Sin cambio en la altura
        Animation.RELATIVE_TO_PARENT, 0f   // Sin cambio en la altura
    )
    slideAnimation.duration = 200 // Duración de la animación en milisegundos
    slideAnimation.fillAfter = true // Mantiene la posición final después de la animación

    Glide.with(this.context)
        .load(bitmap)
        .fitCenter()
        .into(this)
    this.startAnimation(slideAnimation)
}
// El placeholder solo se usa para la carátula del fragmento principal
fun ImageView.loadImage(resource:Int)=
    Glide.with(this.context)
        .load(resource)
        .fitCenter()
        .placeholder(R.drawable.placeholder_cover)
        .into(this)
fun Int.adjustAlpha(factor: Float): Int =
    (this.ushr(24) * factor).roundToInt() shl 24 or (0x00FFFFFF and this)

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

