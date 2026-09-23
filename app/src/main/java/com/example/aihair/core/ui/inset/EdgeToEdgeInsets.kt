package com.example.aihair.core.ui.inset

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding

fun View.applyEdgeToEdgeContentInsets(
    applyLeft: Boolean = true,
    applyTop: Boolean = true,
    applyRight: Boolean = true,
    applyBottom: Boolean = true
) {
    val initialPaddingLeft = paddingLeft
    val initialPaddingTop = paddingTop
    val initialPaddingRight = paddingRight
    val initialPaddingBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(
            left = if (applyLeft) initialPaddingLeft + systemBars.left else initialPaddingLeft,
            top = if (applyTop) initialPaddingTop + systemBars.top else initialPaddingTop,
            right = if (applyRight) initialPaddingRight + systemBars.right else initialPaddingRight,
            bottom = if (applyBottom) initialPaddingBottom + systemBars.bottom else initialPaddingBottom,
        )
        insets
    }

    requestApplyInsetsWhenAttached()
}

fun View.applyEdgeToEdgeMarginInsets(
    applyLeft: Boolean = true,
    applyTop: Boolean = true,
    applyRight: Boolean = true,
    applyBottom: Boolean = true
) {
    val initialMarginLeft = (layoutParams as? ViewGroup.MarginLayoutParams)?.leftMargin ?: 0
    val initialMarginTop = (layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin ?: 0
    val initialMarginRight = (layoutParams as? ViewGroup.MarginLayoutParams)?.rightMargin ?: 0
    val initialMarginBottom = (layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            leftMargin = if (applyLeft) initialMarginLeft + systemBars.left else initialMarginLeft
            topMargin = if (applyTop) initialMarginTop + systemBars.top else initialMarginTop
            rightMargin = if (applyRight) initialMarginRight + systemBars.right else initialMarginRight
            bottomMargin = if (applyBottom) initialMarginBottom + systemBars.bottom else initialMarginBottom
        }
        insets
    }

    requestApplyInsetsWhenAttached()
}

private fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        requestApplyInsets()
        return
    }

    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) {
            view.removeOnAttachStateChangeListener(this)
            view.requestApplyInsets()
        }

        override fun onViewDetachedFromWindow(view: View) = Unit
    })
}

