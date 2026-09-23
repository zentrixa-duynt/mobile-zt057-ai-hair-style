package com.example.aihair.core.ui.toast

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.widget.TextView

fun showCustomToast(message: String, toastContainer: View, messageView: TextView) {
    messageView.text = message
    toastContainer.apply {
        alpha = 0f
        visibility = View.VISIBLE
        animate()
            .alpha(1f)
            .setDuration(300)
            .setListener(null)
            .withEndAction {
                postDelayed({
                    animate()
                        .alpha(0f)
                        .setDuration(300)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                visibility = View.GONE
                            }
                        })
                }, 2000)
            }
    }
}
