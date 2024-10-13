package com.barryzeha.core.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Animatable
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.AbsSavedState
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.core.os.ParcelableCompat
import androidx.transition.ChangeImageTransform
import androidx.transition.ChangeTransform
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.barryzeha.core.R
import kotlin.math.hypot
import kotlin.math.min


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 13/10/24.
 * Copyright (c)  All rights reserved.
 **/

const val SHAPE_RECTANGLE = 0
const val SHAPE_CIRCLE = 1
const val ALPHA_TRANSPARENT = 0
const val ALPHA_OPAQUE = 255
class DiscCoverView @JvmOverloads constructor(context: Context, attrs: AttributeSet?=null, defStyleAttr:Int=0):androidx.appcompat.widget.AppCompatImageView(context,attrs,defStyleAttr), Animatable {

 private val TRACK_SIZE: Float = 10f
 private val TRACK_WIDTH: Float = 8f
 private val TRACK_COLOR: Int = Color.parseColor("#56FFFFFF")


 //private static final int TRACK_COLOR = Color.TRANSPARENT;
 private val FULL_ANGLE: Float = 360f
 private val HALF_ANGLE: Float = FULL_ANGLE / 2
 private val DURATION: Int = 3500
 private val DURATION_PER_DEGREES: Float = DURATION / FULL_ANGLE

 private var mStartRotateAnimator: ValueAnimator? = null
 private var mEndRotateAnimator: ValueAnimator? = null
 private var mCircleToRectTransition: Transition? = null
 private var mRectToCircleTransition: Transition? = null

 private var mTrackSize = 0f
 private var mTrackPaint: Paint? = null
 private var mTrackAlpha = 0

 private val mClipPath = Path()
 private val mRectPath = Path()
 private val mTrackPath = Path()

 private var mIsMorphing = false
 private var mRadius = 0f

 private var mCallbacks: Callbacks? = null
 private var mShape = 0

 @IntDef(*[SHAPE_CIRCLE, SHAPE_RECTANGLE]) //@Retention(RetentionPolicy.SOURCE)
 annotation class Shape

 interface Callbacks {
  fun onMorphEnd(coverView: DiscCoverView?)

  fun onRotateEnd(coverView: DiscCoverView?)
 }
 init{
  loadAttr(attrs,defStyleAttr)
  getInstance()
 }
 private fun getInstance():DiscCoverView{
   if(_discCoverView==null){
     _discCoverView=this

   }
  return _discCoverView!!
 }
 private fun loadAttr(attrs: AttributeSet?,defStyleAttr: Int){

  // Canvas.clipPath works wrong when running with hardware acceleration on Android N
  setLayerType(LAYER_TYPE_HARDWARE, null)

  val density = resources.displayMetrics.density
  mTrackSize = TRACK_SIZE * density
  mTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
  mTrackPaint?.style = Paint.Style.STROKE
  mTrackPaint?.strokeWidth = TRACK_WIDTH * density

  mStartRotateAnimator = ObjectAnimator.ofFloat(this, ROTATION, 0f, FULL_ANGLE)
  mStartRotateAnimator?.setInterpolator(LinearInterpolator())
  mStartRotateAnimator?.setRepeatCount(Animation.INFINITE)
  mStartRotateAnimator?.setDuration(DURATION.toLong())
  mStartRotateAnimator?.addListener(object : AnimatorListenerAdapter() {
   override fun onAnimationEnd(animation: Animator) {
    val current = rotation
    val target =
     if (current > HALF_ANGLE) FULL_ANGLE else 0f // Choose the shortest distance to 0 rotation
    val diff = if (target > 0)FULL_ANGLE - current else current
    mEndRotateAnimator!!.setFloatValues(current, target)
    mEndRotateAnimator!!.setDuration(
     (DURATION_PER_DEGREES * diff).toInt()
      .toLong()
    )
    mEndRotateAnimator!!.start()
   }
  })

  mEndRotateAnimator = ObjectAnimator.ofFloat<View>(this@DiscCoverView, ROTATION, 0f)
  mEndRotateAnimator!!.interpolator = LinearInterpolator()
  mEndRotateAnimator!!.addListener(object : AnimatorListenerAdapter() {
   override fun onAnimationEnd(animation: Animator) {
    rotation = 0f
    // isRunning method return true if it's called form here.
    // So we need call from post method to get the right returning.
    post {
     if (mCallbacks != null) {
      mCallbacks!!.onRotateEnd(this@DiscCoverView)
     }
    }
   }
  })

  mRectToCircleTransition = _discCoverView?.MorphTransition(SHAPE_RECTANGLE)
  mRectToCircleTransition?.addTarget(this)
  mRectToCircleTransition?.addListener(object : DiscCoverView.TransitionAdapter() {
   override fun onTransitionStart(transition: Transition) {
    mIsMorphing = true
   }

   override fun onTransitionEnd(transition: Transition) {
    mIsMorphing = false
    mShape = AlbumCoverView.SHAPE_CIRCLE
    if (mCallbacks != null) {
     mCallbacks!!.onMorphEnd(this@DiscCoverView)
    }
   }
  })

  mCircleToRectTransition = _discCoverView?.MorphTransition(SHAPE_CIRCLE)
  mCircleToRectTransition?.addTarget(this)
  mCircleToRectTransition?.addListener(object : DiscCoverView.TransitionAdapter() {
   override fun onTransitionStart(transition: Transition) {
    mIsMorphing = true
   }

   override fun onTransitionEnd(transition: Transition) {
    mIsMorphing = false
    mShape = AlbumCoverView.SHAPE_RECTANGLE
    if (mCallbacks != null) {
     mCallbacks!!.onMorphEnd(this@DiscCoverView)
    }
   }
  })

  val a = context.obtainStyledAttributes(attrs, R.styleable.AlbumCoverView)
  @Shape val shape =
   a.getInt(R.styleable.AlbumCoverView_shape, SHAPE_RECTANGLE)
  @ColorInt val trackColor =
   a.getColor(R.styleable.AlbumCoverView_trackColor, TRACK_COLOR)
  a.recycle()

  setShape(shape)
  setTrackColor(trackColor)
  setScaleType()
 }

 fun setCallbacks(callbacks: DiscCoverView.Callbacks) {
  mCallbacks = callbacks
 }

 /**
  * Return the current shape
  */
 fun getShape(): Int {
  return mShape
 }

 /**
  * Set which shape should be drawn by this [DiscCoverView]
  *
  * @param shape The shape as [.SHAPE_CIRCLE] or [.SHAPE_RECTANGLE]
  */
 fun setShape(@Shape shape: Int) {
  if (shape != mShape) {
   mShape = shape
   setScaleType()
   if (!isInLayout && !isLayoutRequested) {
    calculateRadius()
    resetPaths()
   }
  }
 }

 /**
  * Set the color of the music tracks
  *
  * @param trackColor The color int
  */
 fun setTrackColor(@ColorInt trackColor: Int) {
  if (trackColor != getTrackColor()) {
   val alpha =
    if (mShape == SHAPE_CIRCLE) ALPHA_OPAQUE else ALPHA_TRANSPARENT
   mTrackPaint!!.color = trackColor
   mTrackAlpha = Color.alpha(trackColor)
   mTrackPaint?.alpha = alpha * mTrackAlpha / ALPHA_OPAQUE
   invalidate()
  }
 }

 /**
  * Return the current color of the tracks
  */
 fun getTrackColor(): Int {
  return mTrackPaint!!.color
 }

 fun getTransitionRadius(): Float {
  return mRadius
 }

 fun setTransitionRadius(radius: Float) {
  if (radius != mRadius) {
   mRadius = radius
   resetPaths()
   invalidate()
  }
 }

 override fun getTransitionAlpha(): Float {
  return (mTrackPaint!!.alpha * ALPHA_OPAQUE / mTrackAlpha).toFloat()
 }

 fun setTransitionAlpha(
  @IntRange(
   from = ALPHA_TRANSPARENT.toLong(),
   to = ALPHA_OPAQUE.toLong()
  ) alpha: Int
 ) {
  if (alpha.toFloat() != transitionAlpha) {
   mTrackPaint!!.alpha = alpha * mTrackAlpha / ALPHA_OPAQUE
   invalidate()
  }
 }

 fun getMinRadius(): Float {
  val w = width
  val h = height
  return (min(w.toDouble(), h.toDouble()) / 2f).toFloat()
 }

 fun getMaxRadius(): Float {
  val w = width
  val h = height
  return hypot((w / 2f).toDouble(), (h / 2f).toDouble()).toFloat()
 }

 override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
  super.onSizeChanged(w, h, oldw, oldh)
  calculateRadius()
  resetPaths()
 }

 private fun calculateRadius() {
  mRadius = if (SHAPE_CIRCLE == mShape) {
   getMinRadius()
  } else {
   getMaxRadius()
  }
 }

 private fun setScaleType() {
  scaleType = if (SHAPE_CIRCLE == mShape) {
   ScaleType.CENTER_INSIDE
  } else {
   ScaleType.CENTER_CROP
  }
 }

 private fun resetPaths() {
  val w = width
  val h = height
  val centerX = w / 2f
  val centerY = h / 2f

  mClipPath.reset()

  // Radio del círculo externo
  mClipPath.addCircle(centerX, centerY, mRadius, Path.Direction.CW)

  // Radio del círculo interno (vacío)
  val innerRadius = mRadius * 0.15f // valor según el tamaño del agujero
  val innerCirclePath = Path()
  innerCirclePath.addCircle(centerX, centerY, innerRadius, Path.Direction.CW)

  // Resta el círculo interno del círculo externo
  // Esto es lo que creará el agujero
  mClipPath.op(innerCirclePath, Path.Op.DIFFERENCE)

  val trackRadius = min(w.toDouble(), h.toDouble()).toInt()
  val trackCount = 2

  mTrackPath.reset()
  for (i in 0 until trackCount) {
   mTrackPath.addCircle(
    centerX,
    centerY,
    trackRadius * (i / trackCount.toFloat()),
    Path.Direction.CW
   )
  }
  // Dibuja las pistas en el borde del círculo interno
  for (i in 0 until trackCount) {
   val innerTrackRadius = innerRadius + (i * mTrackSize) // Ajusta para que sea visible
   mTrackPath.addCircle(centerX, centerY, innerTrackRadius, Path.Direction.CW)
  }
  mRectPath.reset()
  mRectPath.addRect(0f, 0f, w.toFloat(), h.toFloat(), Path.Direction.CW)
 }

 override fun onDraw(canvas: Canvas) {
  canvas.clipPath(mClipPath)
  super.onDraw(canvas)
  canvas.drawPath(mTrackPath, mTrackPaint!!)
 }

 override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
  // Don't need to consume the system window insets
  return insets
 }

 /**
  * Starts the transition morph to rect or circle, depending the current shape.
  */
 fun morph() {
  if (AlbumCoverView.SHAPE_CIRCLE == mShape) {
   morphToRect()
  } else {
   morphToCircle()
  }
 }

 private fun morphToCircle() {
  if (mIsMorphing) {
   return
  }
  TransitionManager.beginDelayedTransition((parent as ViewGroup), mRectToCircleTransition)
  scaleType = ScaleType.CENTER_INSIDE
 }

 private fun morphToRect() {
  if (mIsMorphing) {
   return
  }
  TransitionManager.beginDelayedTransition((parent as ViewGroup), mCircleToRectTransition)
  scaleType = ScaleType.CENTER_CROP
 }

 /**
  * Start the rotate animation
  */
 override fun start() {
  if (AlbumCoverView.SHAPE_RECTANGLE == mShape) { // Only start rotate when shape is a circle
   return
  }
  if (!isRunning) {
   mStartRotateAnimator!!.start()
  }
 }

 /**
  * Stop the rotate animation
  */
 override fun stop() {
  if (mStartRotateAnimator!!.isRunning) {
   mStartRotateAnimator?.end()
  }
 }
 fun pause(){
  if (mStartRotateAnimator!!.isRunning) {
   mStartRotateAnimator?.pause()
  }
 }
 fun resume(){
  if (mStartRotateAnimator!!.isPaused) {
   mStartRotateAnimator?.resume()
  }else if(!mStartRotateAnimator!!.isRunning) {
   mStartRotateAnimator?.start()
  }
 }
 /**
  * Return if the rotate animation is running
  */
 override fun isRunning(): Boolean {
  return mStartRotateAnimator!!.isRunning || mEndRotateAnimator!!.isRunning || mIsMorphing
 }

 inner class MorphTransition(shape: Int) : TransitionSet() {
  init {
   ordering = ORDERING_TOGETHER
   addTransition(DiscCoverViewTransition(context = context,shape=shape))
   addTransition(ChangeImageTransform())
   addTransition(ChangeTransform())
  }
 }

 inner open class TransitionAdapter : Transition.TransitionListener {
  override fun onTransitionStart(transition: Transition) {
  }

  override fun onTransitionEnd(transition: Transition) {
  }

  override fun onTransitionCancel(transition: Transition) {
  }

  override fun onTransitionPause(transition: Transition) {
  }

  override fun onTransitionResume(transition: Transition) {
  }
 }

 /**
  * [SavedState] methods
  */
 override fun onSaveInstanceState(): Parcelable {
  val superState = super.onSaveInstanceState()
  val ss = SavedState(superState!!)
  ss.shape = getShape()
  ss.trackColor = getTrackColor()
  ss.isRotating = mStartRotateAnimator!!.isRunning
  return ss
 }

 override fun onRestoreInstanceState(state: Parcelable) {
  val ss = state as SavedState
  super.onRestoreInstanceState(ss.superState)
  setShape(ss.shape)
  setTrackColor(ss.trackColor)
  if (ss.isRotating) {
   start()
  }
 }


 private  class SavedState  :
  AbsSavedState{
  var shape: Int = 0
  var trackColor: Int = 0
  var isRotating: Boolean = false

  /*constructor(parcel: Parcel, loader: ClassLoader?) : super(parcel, loader) {
   shape = parcel.readInt()
   trackColor = parcel.readInt()
   isRotating = parcel.readValue(Boolean::class.java.classLoader) as Boolean
  }*/
  constructor(parcel: Parcel):super(parcel){}
  constructor(superState: Parcelable) : super(superState) {
   shape = 0 // o un valor por defecto
   trackColor = 0 // o un valor por defecto
   isRotating = false // o un valor por defecto
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
   super.writeToParcel(dest, flags)
   dest.writeInt(shape)
   dest.writeInt(trackColor)
   dest.writeValue(isRotating)
  }

  override fun toString(): String {
   return (DiscCoverView::class.java.simpleName + "." + SavedState::class.java.simpleName + "{"
           + Integer.toHexString(System.identityHashCode(this))
           + " shape=" + shape + ", trackColor=" + trackColor + ", isRotating=" + isRotating + "}")
  }

  var ParcelableCompat: ParcelableCompat? = null
  constructor(parcel: Parcel, loader:ClassLoader) : super(parcel,loader) {
   shape = parcel.readInt()
   trackColor = parcel.readInt()
   isRotating = parcel.readByte() != 0.toByte()
  }

  override fun describeContents(): Int {
   return 0
  }

  companion object { @JvmField final val CREATOR = object : Parcelable.Creator<SavedState>
   {
    override fun createFromParcel(parcel: Parcel): SavedState {
     return SavedState(parcel)
    }

    override fun newArray(size: Int): Array<SavedState?> {
     return arrayOfNulls(size)
    }
   }
  }

 }
 companion object{
   private var _discCoverView:DiscCoverView?=null
   val discCoverView:DiscCoverView  get() = _discCoverView!!


 }
}