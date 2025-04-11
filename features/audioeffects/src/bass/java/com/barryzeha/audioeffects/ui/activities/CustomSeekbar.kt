package com.barryzeha.audioeffects.ui.activities

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.content.ContextCompat
import com.barryzeha.core.R as coreRes


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 21/9/24.
 * Copyright (c)  All rights reserved.
 **/

 class  CustomSeekBar @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet?=null,
  private val progressRange:Int = 30
 ) : AppCompatSeekBar(context, attrs) {

  private var rect: Rect = Rect()
  private var paint: Paint = Paint()
  private var seekbarHeight: Int = 4
  init {
   this.max = progressRange
  }
  @Synchronized
  override fun onDraw(canvas: Canvas) {

   // Dibujar la línea base del SeekBar (gris)
   rect[0 + 30, height / 2 - seekbarHeight / 2, width - 30] = height / 2 + seekbarHeight / 2
   paint.color = Color.DKGRAY
   canvas.drawRect(rect, paint)

   // Dibujar la parte verde (si el progreso está a la derecha del centro)
   // restamos 30 a progressWidth para evitar que el thumb sobrepase el seekbar, puede acomodarse a conveniencia

   if (this.progress > (progressRange / 2)) {
    val progressWidth = (width / progressRange.toFloat() * (progress - (progressRange / 2))).toInt() - 30
    rect[width / 2, height / 2 - seekbarHeight / 2, width / 2 + progressWidth] = height / 2 + seekbarHeight / 2
    paint.color = Color.GREEN
    canvas.drawRect(rect, paint)
   }

   // Dibujar la parte roja (si el progreso está a la izquierda del centro)
   // restamos 30 a progressWidth para evitar que el thumb sobrepase el seekbar, puede acomodarse a conveniencia

   if (this.progress < (progressRange / 2)) {
    val progressWidth = (width / progressRange.toFloat() * ((progressRange / 2) - progress)-30).toInt()
    rect[width / 2 - progressWidth, height / 2 - seekbarHeight / 2, width / 2] = height / 2 + seekbarHeight / 2
    paint.color = Color.RED
    canvas.drawRect(rect, paint)
   }
   super.onDraw(canvas)
  }
 }