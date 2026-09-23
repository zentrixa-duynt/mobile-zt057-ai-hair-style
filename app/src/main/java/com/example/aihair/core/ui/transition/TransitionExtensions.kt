package com.example.aihair.core.ui.transition

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.PathInterpolator
import androidx.annotation.IdRes
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback

/**
 * Setup Material Container Transform (Morph transition) for Activity enter transition.
 * 
 * @param targetId The layout root ID that has the transitionName (e.g. R.id.main)
 * @param duration Duration of the transition in milliseconds (default is 400ms)
 */
fun Activity.setupMorphTransition(@IdRes targetId: Int, duration: Long = 400L) {
    setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())
    window.sharedElementEnterTransition = MaterialContainerTransform().apply {
        addTarget(targetId)
        this.duration = duration
        interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
        scrimColor = Color.TRANSPARENT
        containerColor = Color.parseColor("#030712") // Match background
    }
}

/**
 * Hides the view and shifts it left, preparing it for the slide-in animation.
 * Call this in onCreate before the transition starts.
 */
fun View.prepareSlideFadeIn() {
    this.alpha = 0f
    this.translationX = -50f
}

/**
 * Applies a micro-animation (Slide and Fade-in) for views like Back Button
 * Call this in onTransitionEnd.
 */
fun View.startSlideFadeIn(delay: Long = 0L) {
    this.animate()
        .alpha(1f)
        .translationX(0f)
        .setDuration(400)
        .setStartDelay(delay)
        .setInterpolator(DecelerateInterpolator())
        .start()
}

/**
 * Legacy method for older activities.
 */
fun View.animateSlideFadeInAfterMorph() {
    prepareSlideFadeIn()
    startSlideFadeIn(400L)
}
