package com.barryzeha.core.components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Animatable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.AbsSavedState;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.core.os.ParcelableCompatCreatorCallbacks;
import androidx.transition.ChangeImageTransform;
import androidx.transition.ChangeTransform;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.barryzeha.core.R;

/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 8/10/24.
 * Copyright (c)  All rights reserved.
 **/

public class DiscCoverView extends androidx.appcompat.widget.AppCompatImageView implements Animatable {

    public static final int SHAPE_RECTANGLE = 0;
    public static final int SHAPE_CIRCLE = 1;

    static final int ALPHA_TRANSPARENT = 0;
    static final int ALPHA_OPAQUE = 255;

    private static final float TRACK_SIZE = 10;
    private static final float TRACK_WIDTH = 8;
    private static final int TRACK_COLOR = Color.parseColor("#76FFFFFF");
    private static final float FULL_ANGLE = 360;
    private static final float HALF_ANGLE = FULL_ANGLE / 2;
    private static int DURATION = 3500;
    private static final float DURATION_PER_DEGREES = DURATION / FULL_ANGLE;

    private final ValueAnimator mStartRotateAnimator;
    private final ValueAnimator mEndRotateAnimator;
    private final Transition mCircleToRectTransition;
    private final Transition mRectToCircleTransition;

    private final float mTrackSize;
    private final Paint mTrackPaint;
    private int mTrackAlpha;

    private final Path mClipPath = new Path();
    private final Path mRectPath = new Path();
    private final Path mTrackPath = new Path();

    private boolean mIsMorphing;
    private float mRadius = 0;

    private Callbacks mCallbacks;
    private int mShape;

    @IntDef({SHAPE_CIRCLE, SHAPE_RECTANGLE})
    //@Retention(RetentionPolicy.SOURCE)
    public @interface Shape {
    }

    public interface Callbacks {
        void onMorphEnd(DiscCoverView coverView);

        void onRotateEnd(DiscCoverView coverView);
    }

    public DiscCoverView(Context context) {
        this(context, null, 0);
    }

    public DiscCoverView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DiscCoverView(Context context, AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);

        final float density = getResources().getDisplayMetrics().density;
        mTrackSize = TRACK_SIZE * density;
        mTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTrackPaint.setStyle(Paint.Style.STROKE);
        mTrackPaint.setStrokeWidth(TRACK_WIDTH * density);

        mStartRotateAnimator = ObjectAnimator.ofFloat(this, View.ROTATION, 0, FULL_ANGLE);
        mStartRotateAnimator.setInterpolator(new LinearInterpolator());
        mStartRotateAnimator.setRepeatCount(Animation.INFINITE);
        mStartRotateAnimator.setDuration(DURATION);
        mStartRotateAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                float current = getRotation();
                float target = current > HALF_ANGLE ? FULL_ANGLE : 0; // Choose the shortest distance to 0 rotation
                float diff = target > 0 ? FULL_ANGLE - current : current;
                mEndRotateAnimator.setFloatValues(current, target);
                mEndRotateAnimator.setDuration((int) (DURATION_PER_DEGREES * diff));
                mEndRotateAnimator.start();
            }
        });

        mEndRotateAnimator = ObjectAnimator.ofFloat(DiscCoverView.this, View.ROTATION, 0);
        mEndRotateAnimator.setInterpolator(new LinearInterpolator());
        mEndRotateAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setRotation(0);
                // isRunning method return true if it's called form here.
                // So we need call from post method to get the right returning.
                post(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallbacks != null) {
                            mCallbacks.onRotateEnd(DiscCoverView.this);
                        }
                    }
                });
            }
        });

        mRectToCircleTransition = new MorphTransition(SHAPE_RECTANGLE);
        mRectToCircleTransition.addTarget(this);
        mRectToCircleTransition.addListener(new TransitionAdapter() {
            @Override
            public void onTransitionStart(Transition transition) {
                mIsMorphing = true;
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                mIsMorphing = false;
                mShape = SHAPE_CIRCLE;
                if (mCallbacks != null) {
                    mCallbacks.onMorphEnd(DiscCoverView.this);
                }
            }
        });

        mCircleToRectTransition = new MorphTransition(SHAPE_CIRCLE);
        mCircleToRectTransition.addTarget(this);
        mCircleToRectTransition.addListener(new TransitionAdapter() {
            @Override
            public void onTransitionStart(Transition transition) {
                mIsMorphing = true;
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                mIsMorphing = false;
                mShape = SHAPE_RECTANGLE;
                if (mCallbacks != null) {
                    mCallbacks.onMorphEnd(DiscCoverView.this);
                }
            }
        });

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DiscCoverView);
        @Shape int shape = a.getInt(R.styleable.DiscCoverView_shape, SHAPE_CIRCLE);
        @ColorInt int trackColor = a.getColor(R.styleable.DiscCoverView_trackColor, TRACK_COLOR);
        int durationRotate = a.getInt(R.styleable.DiscCoverView_speedRotation, DURATION);
        a.recycle();

        setShape(shape);
        setTrackColor(trackColor);
        setRotateDuration(durationRotate);
        setScaleType();
    }

    public void setCallbacks(Callbacks callbacks) {
        mCallbacks = callbacks;
    }

    public int getShape() {
        return mShape;
    }

    public void setShape(@Shape int shape) {
        if (shape != mShape) {
            mShape = shape;
            setScaleType();
            if (!isInLayout() && !isLayoutRequested()) {
                calculateRadius();
                resetPaths();
            }
        }
    }

    public void setTrackColor(@ColorInt int trackColor) {
        if (trackColor != getTrackColor()) {
            int alpha = mShape == SHAPE_CIRCLE ? ALPHA_OPAQUE : ALPHA_TRANSPARENT;
            mTrackPaint.setColor(trackColor);
            mTrackAlpha = Color.alpha(trackColor);
            mTrackPaint.setAlpha(alpha * mTrackAlpha / ALPHA_OPAQUE);
            invalidate();
        }
    }

    public void  setRotateDuration(int duration){
        DURATION = duration;
        mStartRotateAnimator.setDuration(DURATION);

    }

    public int getTrackColor() {
        return mTrackPaint.getColor();
    }

    float getTransitionRadius() {
        return mRadius;
    }

    void setTransitionRadius(float radius) {
        if (radius != mRadius) {
            mRadius = radius;
            resetPaths();
            invalidate();
        }
    }

    public float getTransitionAlpha() {
        return mTrackPaint.getAlpha() * ALPHA_OPAQUE / mTrackAlpha;
    }

    void setTransitionAlpha(@IntRange(from = ALPHA_TRANSPARENT, to = ALPHA_OPAQUE) int alpha) {
        if (alpha != getTransitionAlpha()) {
            mTrackPaint.setAlpha(alpha * mTrackAlpha / ALPHA_OPAQUE);
            invalidate();
        }
    }

    float getMinRadius() {
        final int w = getWidth();
        final int h = getHeight();
        return Math.min(w, h) / 2f;
    }

    float getMaxRadius() {
        final int w = getWidth();
        final int h = getHeight();
        return (float) Math.hypot(w / 2f, h / 2f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculateRadius();
        resetPaths();
    }

    private void calculateRadius() {
        if (SHAPE_CIRCLE == mShape) {
            mRadius = getMinRadius();
        } else {
            mRadius = getMaxRadius();
        }
    }

    private void setScaleType() {
        if (SHAPE_CIRCLE == mShape) {
            setScaleType(ScaleType.CENTER_INSIDE);
        } else {
            setScaleType(ScaleType.CENTER_CROP);
        }
    }

    private void resetPaths() {
        final int w = getWidth();
        final int h = getHeight();
        final float centerX = w / 2f;
        final float centerY = h / 2f;

        mClipPath.reset();

        // Radio del círculo externo
        mClipPath.addCircle(centerX, centerY, mRadius, Path.Direction.CW);

        // Radio del círculo interno (vacío)
        float innerRadius = mRadius * 0.15f; // valor según el tamaño del agujero
        Path innerCirclePath = new Path();
        innerCirclePath.addCircle(centerX, centerY, innerRadius, Path.Direction.CW);

        // Resta el círculo interno del círculo externo
        // Esto es lo que creará el agujero
        mClipPath.op(innerCirclePath, Path.Op.DIFFERENCE);

        final int trackRadius = Math.min(w, h);
        final int trackCount = 2;

        mTrackPath.reset();
        for (int i = 0; i < trackCount; i++) {
            mTrackPath.addCircle(centerX, centerY, trackRadius * (i / (float) trackCount), Path.Direction.CW);
        }
        // Dibuja las pistas en el borde del círculo interno
        for (int i = 0; i < trackCount; i++) {
            float innerTrackRadius = innerRadius + (i * mTrackSize); // Ajusta para que sea visible
            mTrackPath.addCircle(centerX, centerY, innerTrackRadius, Path.Direction.CW);
        }
        mRectPath.reset();
        mRectPath.addRect(0, 0, w, h, Path.Direction.CW);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.clipPath(mClipPath);
        super.onDraw(canvas);
        canvas.drawPath(mTrackPath, mTrackPaint);
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        // Don't need to consume the system window insets
        return insets;
    }

    public void morph() {
        if (SHAPE_CIRCLE == mShape) {
            morphToRect();
        } else {
            morphToCircle();
        }
    }

    private void morphToCircle() {
        if (mIsMorphing) {
            return;
        }
        TransitionManager.beginDelayedTransition((ViewGroup) getParent(), mRectToCircleTransition);
        setScaleType(ScaleType.CENTER_INSIDE);
    }

    private void morphToRect() {
        if (mIsMorphing) {
            return;
        }
        TransitionManager.beginDelayedTransition((ViewGroup) getParent(), mCircleToRectTransition);
        setScaleType(ScaleType.CENTER_CROP);
    }

    /**
     * Start the rotate animation
     */
    @Override
    public void start() {
        if (SHAPE_RECTANGLE == mShape) { // Only start rotate when shape is a circle
            return;
        }
        if (!isRunning()) {
            mStartRotateAnimator.start();
        }
    }

    /**
     * pause the rotate animation
     */
    public void pause(){
        if (mStartRotateAnimator.isRunning()) {
            mStartRotateAnimator.pause();
        }
    }

    /**
     * resume the rotate animation
     */
    public void  resume(){
        if (mStartRotateAnimator.isPaused()) {
            mStartRotateAnimator.resume();
        }else if(!mStartRotateAnimator.isRunning()) {
            mStartRotateAnimator.start();
        }
    }
    /**
     * Stop the rotate animation
     */
    @Override
    public void stop() {
        if (mStartRotateAnimator.isRunning()) {
            //If is rotation the animation stop with rotation to original position slowly
            mStartRotateAnimator.cancel();
        }
    }

    /**
     * End the rotate animation
     */
    public void end(){
        if(mStartRotateAnimator.isRunning()){
            //If is rotation the animation stop with rotation to original position fastly
            mStartRotateAnimator.end();
        }
    }
    /**
     * Return if the rotate animation is running
     */
    @Override
    public boolean isRunning() {
        return mStartRotateAnimator.isRunning() || mEndRotateAnimator.isRunning() || mIsMorphing;
    }

    private static class MorphTransition extends TransitionSet {
        private MorphTransition(int shape) {
            setOrdering(ORDERING_TOGETHER);
            addTransition(new DiscCoverViewTransition(shape));
            addTransition(new ChangeImageTransform());
            addTransition(new ChangeTransform());
        }
    }

    private static class TransitionAdapter implements Transition.TransitionListener {

        @Override
        public void onTransitionStart(Transition transition) {
        }

        @Override
        public void onTransitionEnd(Transition transition) {
        }

        @Override
        public void onTransitionCancel(Transition transition) {
        }

        @Override
        public void onTransitionPause(Transition transition) {
        }

        @Override
        public void onTransitionResume(Transition transition) {
        }
    }

    /**
     * {@link SavedState} methods
     */

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.shape = getShape();
        ss.trackColor = getTrackColor();
        ss.isRotating = mStartRotateAnimator.isRunning();
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setShape(ss.shape);
        setTrackColor(ss.trackColor);
        if (ss.isRotating) {
            start();
        }
    }

    public static class SavedState extends AbsSavedState {

        private int shape;
        private int trackColor;
        private boolean isRotating;

        private SavedState(Parcel in, ClassLoader loader) {
            super(in, loader);
            shape = in.readInt();
            trackColor = in.readInt();
            isRotating = (boolean) in.readValue(Boolean.class.getClassLoader());
        }

        private SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(shape);
            dest.writeInt(trackColor);
            dest.writeValue(isRotating);
        }

        @Override
        public String toString() {
            return DiscCoverView.class.getSimpleName() + "." + SavedState.class.getSimpleName() + "{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " shape=" + shape + ", trackColor=" + trackColor + ", isRotating=" + isRotating + "}";
        }

        private static androidx.core.os.ParcelableCompat ParcelableCompat;
        public static final Parcelable.Creator<SavedState> CREATOR
                = ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel parcel, ClassLoader loader) {
                return new SavedState(parcel, loader);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        });
    }

}
