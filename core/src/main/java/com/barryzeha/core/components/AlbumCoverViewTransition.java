package com.barryzeha.core.components;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Property;
import android.view.ViewGroup;

import androidx.transition.Transition;
import androidx.transition.TransitionValues;

import com.barryzeha.core.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 8/10/24.
 * Copyright (c)  All rights reserved.
 **/

public class AlbumCoverViewTransition extends Transition {

    private static final String PROPNAME_RADIUS = AlbumCoverViewTransition.class.getName() + ":radius";
    private static final String PROPNAME_ALPHA = AlbumCoverViewTransition.class.getName() + ":alpha";
    private static final String[] sTransitionProperties = {PROPNAME_RADIUS, PROPNAME_ALPHA};

    private static final Property<AlbumCoverView, Float> RADIUS_PROPERTY =
            new Property<AlbumCoverView, Float>(Float.class, "radius") {
                @Override
                public void set(AlbumCoverView view, Float radius) {
                    view.setTransitionRadius(radius);
                }

                @Override
                public Float get(AlbumCoverView view) {
                    return view.getTransitionRadius();
                }
            };

    private static final Property<AlbumCoverView, Integer> ALPHA_PROPERTY =
            new Property<AlbumCoverView, Integer>(Integer.class, "alpha") {
                @Override
                public void set(AlbumCoverView view, Integer alpha) {
                    view.setTransitionAlpha(alpha);
                }

                @Override
                public Integer get(AlbumCoverView view) {
                    return (int) view.getTransitionAlpha();
                }
            };

    private final int mStartShape;

    public AlbumCoverViewTransition(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AlbumCoverView);
        int shape = a.getInt(R.styleable.AlbumCoverView_shape, AlbumCoverView.SHAPE_RECTANGLE);
        a.recycle();
        mStartShape = shape;
    }

    public AlbumCoverViewTransition(int shape) {
        mStartShape = shape;
    }

    @Override
    public String[] getTransitionProperties() {
        return sTransitionProperties;
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        // Add fake value to force calling of createAnimator method
        captureValues(transitionValues, "start");
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        // Add fake value to force calling of createAnimator method
        captureValues(transitionValues, "end");
    }

    private void captureValues(TransitionValues transitionValues, Object value) {
        if (transitionValues.view instanceof AlbumCoverView) {
            transitionValues.values.put(PROPNAME_RADIUS, value);
            transitionValues.values.put(PROPNAME_ALPHA, value);
        }
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {

        if (endValues == null || !(endValues.view instanceof AlbumCoverView)) {
            return null;
        }

        AlbumCoverView coverView = (AlbumCoverView) endValues.view;
        final float minRadius = coverView.getMinRadius();
        final float maxRadius = coverView.getMaxRadius();

        float startRadius, endRadius;
        int startTrackAlpha, endTrackAlpha;

        if (mStartShape == AlbumCoverView.SHAPE_RECTANGLE) {
            startRadius = maxRadius;
            endRadius = minRadius;
            startTrackAlpha = AlbumCoverView.ALPHA_TRANSPARENT;
            endTrackAlpha = AlbumCoverView.ALPHA_OPAQUE;
        } else {
            startRadius = minRadius;
            endRadius = maxRadius;
            startTrackAlpha = AlbumCoverView.ALPHA_OPAQUE;
            endTrackAlpha = AlbumCoverView.ALPHA_TRANSPARENT;
        }

        List<Animator> animatorList = new ArrayList<>();

        coverView.setTransitionRadius(startRadius);
        animatorList.add(ObjectAnimator.ofFloat(coverView, RADIUS_PROPERTY, startRadius, endRadius));

        coverView.setTransitionAlpha(startTrackAlpha);
        animatorList.add(ObjectAnimator.ofInt(coverView, ALPHA_PROPERTY, startTrackAlpha, endTrackAlpha));

        AnimatorSet animator = new AnimatorSet();
        animator.playTogether(animatorList);
        return animator;
    }

}