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
 attrs: AttributeSet?=null
) : AppCompatSeekBar(context, attrs) {

 private var rect: Rect = Rect()
 private var paint: Paint = Paint()
 private var seekbarHeight: Int = 8

 @Synchronized
 override fun onDraw(canvas: Canvas) {
  // for seek bar line
  rect[0 + 40, height / 2 - seekbarHeight / 2, width - 40] = height / 2 + seekbarHeight / 2
  paint.color = Color.DKGRAY
  canvas.drawRect(rect, paint)

  //for right side
  if (this.progress > 15) {
   rect[(width / 2), height / 2 - seekbarHeight / 2, (width / 2 + width / 30 * (progress - 15))-40] =
    height / 2 + seekbarHeight / 2

   paint.color = Color.GREEN
   canvas.drawRect(rect, paint)
  }

  //for left side
  if (this.progress < 15) {
   rect[(width / 2)- ((width / 30 )* (15-progress)-40), height / 2 - seekbarHeight / 2, (width / 2)] =
    height / 2 + seekbarHeight / 2

   paint.color = Color.RED
   canvas.drawRect(rect, paint)
  }
  super.onDraw(canvas)
 }
}