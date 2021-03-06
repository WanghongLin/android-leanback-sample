/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.support.v17.leanback.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.util.Log;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.transition.Visibility;
import android.transition.Transition;
import android.transition.TransitionValues;

/**
 * Slide distance toward/from a edge.  The direction and distance are determined by
 * {@link SlideCallback}.
 */
class Slide extends Visibility {
    private static final String TAG = "Slide";

    /**
     * Move Views in or out of the left edge of the scene.
     * @see #setSlideEdge(int)
     */
    public static final int LEFT = 0;

    /**
     * Move Views in or out of the top edge of the scene.
     * @see #setSlideEdge(int)
     */
    public static final int TOP = 1;

    /**
     * Move Views in or out of the right edge of the scene.
     * @see #setSlideEdge(int)
     */
    public static final int RIGHT = 2;

    /**
     * Move Views in or out of the bottom edge of the scene. This is the
     * default slide direction.
     * @see #setSlideEdge(int)
     */
    public static final int BOTTOM = 3;

    private static final TimeInterpolator sDecelerate = new DecelerateInterpolator();
    private static final TimeInterpolator sAccelerate = new AccelerateInterpolator();

    private int[] mTempLoc = new int[2];
    SlideCallback mCallback;
    private int[] mTempEdge = new int[1];
    private float[] mTempDistance = new float[1];

    private interface CalculateSlide {
        /** Returns the translation value for view when it out of the scene */
        float getGone(float slide, View view);

        /** Returns the translation value for view when it is in the scene */
        float getHere(View view);

        /** Returns the property to animate translation */
        Property<View, Float> getProperty();
    }

    private static abstract class CalculateSlideHorizontal implements CalculateSlide {
        @Override
        public float getHere(View view) {
            return view.getTranslationX();
        }

        @Override
        public Property<View, Float> getProperty() {
            return View.TRANSLATION_X;
        }
    }

    private static abstract class CalculateSlideVertical implements CalculateSlide {
        @Override
        public float getHere(View view) {
            return view.getTranslationY();
        }

        @Override
        public Property<View, Float> getProperty() {
            return View.TRANSLATION_Y;
        }
    }

    private static final CalculateSlide sCalculateLeft = new CalculateSlideHorizontal() {
        @Override
        public float getGone(float distance, View view) {
            return view.getTranslationX() - distance;
        }
    };

    private static final CalculateSlide sCalculateTop = new CalculateSlideVertical() {
        @Override
        public float getGone(float distance, View view) {
            return view.getTranslationY() - distance;
        }
    };

    private static final CalculateSlide sCalculateRight = new CalculateSlideHorizontal() {
        @Override
        public float getGone(float distance, View view) {
            return view.getTranslationX() + distance;
        }
    };

    private static final CalculateSlide sCalculateBottom = new CalculateSlideVertical() {
        @Override
        public float getGone(float distance, View view) {
            return view.getTranslationY() + distance;
        }
    };

    public Slide() {
    }

    public void setCallback(SlideCallback callback) {
        mCallback = callback;
    }

    private CalculateSlide getSlideEdge(int slideEdge) {
        switch (slideEdge) {
            case LEFT:
                return sCalculateLeft;
            case TOP:
                return sCalculateTop;
            case RIGHT:
                return sCalculateRight;
            case BOTTOM:
                return sCalculateBottom;
            default:
                throw new IllegalArgumentException("Invalid slide direction");
        }
    }

    private Animator createAnimation(final View view, Property<View, Float> property,
            float start, float end, float terminalValue, TimeInterpolator interpolator) {
        view.setTranslationY(start);
        if (start == end) {
            return null;
        }
        final ObjectAnimator anim = ObjectAnimator.ofFloat(view, property, start, end);

        SlideAnimatorListener listener = new SlideAnimatorListener(view, terminalValue, end);
        anim.addListener(listener);
        anim.addPauseListener(listener);
        anim.setInterpolator(interpolator);
        return anim;
    }

    @Override
    public Animator onAppear(ViewGroup sceneRoot,
            TransitionValues startValues, int startVisibility,
            TransitionValues endValues, int endVisibility) {
        View view = (endValues != null) ? endValues.view : null;
        if (view == null) {
            return null;
        }
        if (mCallback == null || !mCallback.getSlide(view, true, mTempEdge, mTempDistance)) {
            return null;
        }
        final CalculateSlide slideCalculator = getSlideEdge(mTempEdge[0]);
        float end = slideCalculator.getHere(view);
        float start = slideCalculator.getGone(mTempDistance[0], view);
        return createAnimation(view, slideCalculator.getProperty(), start, end, end, sDecelerate);
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot,
            TransitionValues startValues, int startVisibility,
            TransitionValues endValues, int endVisibility) {
        View view = (startValues != null) ? startValues.view : null;
        if (view == null) {
            return null;
        }
        if (mCallback == null || !mCallback.getSlide(view, false, mTempEdge, mTempDistance)) {
            return null;
        }
        final CalculateSlide slideCalculator = getSlideEdge(mTempEdge[0]);
        float start = slideCalculator.getHere(view);
        float end = slideCalculator.getGone(mTempDistance[0], view);

        return createAnimation(view, slideCalculator.getProperty(), start, end, start,
                sAccelerate);
    }

    private static class SlideAnimatorListener extends AnimatorListenerAdapter {
        private boolean mCanceled = false;
        private float mPausedY;
        private final View mView;
        private final float mEndY;
        private final float mTerminalY;

        public SlideAnimatorListener(View view, float terminalY, float endY) {
            mView = view;
            mTerminalY = terminalY;
            mEndY = endY;
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            mView.setTranslationY(mTerminalY);
            mCanceled = true;
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            if (!mCanceled) {
                mView.setTranslationY(mTerminalY);
            }
        }

        @Override
        public void onAnimationPause(Animator animator) {
            mPausedY = mView.getTranslationY();
            mView.setTranslationY(mEndY);
        }

        @Override
        public void onAnimationResume(Animator animator) {
            mView.setTranslationY(mPausedY);
        }
    }
}