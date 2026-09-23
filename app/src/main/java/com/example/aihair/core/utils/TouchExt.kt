package com.example.aihair.core.utils

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager

fun Activity.hideSoftKeyboard() {
    val view = currentFocus ?: View(this)
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

fun View.isTouchInsideClickableView(ev: MotionEvent, ignoredIds: List<Int> = emptyList()): Boolean {
    if (visibility != View.VISIBLE) return false
    
    val location = IntArray(2)
    getLocationOnScreen(location)
    val rect = Rect(
        location[0],
        location[1],
        location[0] + width,
        location[1] + height
    )
    
    if (!rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) return false

    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            if (getChildAt(i).isTouchInsideClickableView(ev, ignoredIds)) {
                return true
            }
        }
    }

    if (isClickable && !ignoredIds.contains(id)) return true

    return false
}
