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

public class DiscCoverViewTransition extends Transition {

    private static final String PROPNAME_RADIUS = DiscCoverViewTransition.class.getName() + ":radius";
    private static final String PROPNAME_ALPHA = DiscCoverViewTransition.class.getName() + ":alpha";
    private static final String[] sTransitionProperties = {PROPNAME_RADIUS, PROPNAME_ALPHA};

    private static final Property<DiscCoverView, Float> RADIUS_PROPERTY =
            new Property<DiscCoverView, Float>(Float.class, "radius") {
                @Override
                public void set(DiscCoverView view, Float radius) {
                    view.setTransitionRadius(radius);
                }

                @Override
                public Float get(DiscCoverView view) {
                    return view.getTransitionRadius();
                }
            };

    private static final Property<DiscCoverView, Integer> ALPHA_PROPERTY =
            new Property<DiscCoverView, Integer>(Integer.class, "alpha") {
                @Override
                public void set(DiscCoverView view, Integer alpha) {
                    view.setTransitionAlpha(alpha);
                }

                @Override
                public Integer get(DiscCoverView view) {
                    return (int) view.getTransitionAlpha();
                }
            };

    private final int mStartShape;

    public DiscCoverViewTransition(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DiscCoverView);
        int shape = a.getInt(R.styleable.DiscCoverView_shape, DiscCoverView.SHAPE_RECTANGLE);
        a.recycle();
        mStartShape = shape;
    }

    public DiscCoverViewTransition(int shape) {
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
        if (transitionValues.view instanceof DiscCoverView) {
            transitionValues.values.put(PROPNAME_RADIUS, value);
            transitionValues.values.put(PROPNAME_ALPHA, value);
        }
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {

        if (endValues == null || !(endValues.view instanceof DiscCoverView)) {
            return null;
        }

        DiscCoverView coverView = (DiscCoverView) endValues.view;
        final float minRadius = coverView.getMinRadius();
        final float maxRadius = coverView.getMaxRadius();

        float startRadius, endRadius;
        int startTrackAlpha, endTrackAlpha;

        if (mStartShape == DiscCoverView.SHAPE_RECTANGLE) {
            startRadius = maxRadius;
            endRadius = minRadius;
            startTrackAlpha = DiscCoverView.ALPHA_TRANSPARENT;
            endTrackAlpha = DiscCoverView.ALPHA_OPAQUE;
        } else {
            startRadius = minRadius;
            endRadius = maxRadius;
            startTrackAlpha = DiscCoverView.ALPHA_OPAQUE;
            endTrackAlpha = DiscCoverView.ALPHA_TRANSPARENT;
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