package com.barryzeha.core.components

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import androidx.transition.Transition
import android.util.AttributeSet
import android.util.Property
import android.view.ViewGroup
import androidx.transition.TransitionValues
import com.barryzeha.core.R


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 13/10/24.
 * Copyright (c)  All rights reserved.
 **/

class DiscCoverViewTransition @JvmOverloads constructor(context: Context, attrs: AttributeSet?=null): Transition(context,attrs!!) {
 private val PROPNAME_RADIUS: String = AlbumCoverViewTransition::class.java.name + ":radius"
 private val PROPNAME_ALPHA: String = AlbumCoverViewTransition::class.java.name + ":alpha"
 private val sTransitionProperties: Array<String> = arrayOf(PROPNAME_RADIUS, PROPNAME_ALPHA)

 private  val RADIUS_PROPERTY: Property<AlbumCoverView, Float> = object : Property<AlbumCoverView, Float>(
  Float::class.java, "radius"
 ) {
  override fun set(view: AlbumCoverView, radius: Float?) {
   view.transitionRadius = radius!!
  }

  override fun get(view: AlbumCoverView): Float? {
   return view.transitionRadius
  }
 }

 private val ALPHA_PROPERTY: Property<AlbumCoverView, Int> = object : Property<AlbumCoverView, Int>(
  Int::class.java, "alpha"
 ) {
  override fun set(view: AlbumCoverView, alpha: Int?) {
   view.setTransitionAlpha(alpha!!)
  }

  override fun get(view: AlbumCoverView): Int? {
   return view.transitionAlpha.toInt()
  }
 }

 private var mStartShape = 0
 init{
  val a = context.obtainStyledAttributes(attrs, R.styleable.AlbumCoverView)
  val shape = a.getInt(R.styleable.AlbumCoverView_shape, AlbumCoverView.SHAPE_RECTANGLE)
  a.recycle()
  mStartShape = shape
 }


 constructor(context: Context, attrs: AttributeSet?=null, shape: Int) : this(context, attrs) {
  mStartShape = shape
 }
 override fun getTransitionProperties(): Array<String> {
  return sTransitionProperties
 }
 override fun captureStartValues(transitionValues: TransitionValues) {
  // Add fake value to force calling of createAnimator method
  captureValues(transitionValues, "start")
 }
 override fun captureEndValues(transitionValues: TransitionValues) {
  // Add fake value to force calling of createAnimator method
  captureValues(transitionValues, "end")
 }
 private fun captureValues(transitionValues: TransitionValues?, value: Any) {
  if (transitionValues?.view is AlbumCoverView) {
   transitionValues.values[PROPNAME_RADIUS] = value
   transitionValues.values[PROPNAME_ALPHA] = value
  }
 }

 override fun createAnimator(
  sceneRoot: ViewGroup,
  startValues: TransitionValues?,
  endValues: TransitionValues?
 ): Animator? {
  if (endValues == null || endValues.view !is AlbumCoverView) {
   return null
  }

  val coverView = endValues.view as AlbumCoverView
  val minRadius = coverView.minRadius
  val maxRadius = coverView.maxRadius

  val startRadius: Float
  val endRadius: Float
  val startTrackAlpha: Int
  val endTrackAlpha: Int

  if (mStartShape == AlbumCoverView.SHAPE_RECTANGLE) {
   startRadius = maxRadius
   endRadius = minRadius
   startTrackAlpha = AlbumCoverView.ALPHA_TRANSPARENT
   endTrackAlpha = AlbumCoverView.ALPHA_OPAQUE
  } else {
   startRadius = minRadius
   endRadius = maxRadius
   startTrackAlpha = AlbumCoverView.ALPHA_OPAQUE
   endTrackAlpha = AlbumCoverView.ALPHA_TRANSPARENT
  }

  val animatorList: MutableList<Animator> = ArrayList()

  coverView.transitionRadius = startRadius
  animatorList.add(ObjectAnimator.ofFloat(coverView, RADIUS_PROPERTY, startRadius, endRadius))

  coverView.setTransitionAlpha(startTrackAlpha)
  animatorList.add(ObjectAnimator.ofInt(coverView, ALPHA_PROPERTY, startTrackAlpha, endTrackAlpha))

  val animator = AnimatorSet()
  animator.playTogether(animatorList)
  return animator
 }
}