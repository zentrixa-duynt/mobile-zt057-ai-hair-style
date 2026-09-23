package com.example.aihair.core.ui.base

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.graphics.drawable.toDrawable

abstract class BasePopupWindow(context: Context) : PopupWindow(context) {

    init {
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        isFocusable = true
        isOutsideTouchable = true

        // Transparent background so the rounded corners of the XML background show correctly
        setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
    }

    fun show(anchorView: View, xoff: Int = 0, yoff: Int = 0, gravity: Int = Gravity.TOP or Gravity.START) {
        showAsDropDown(anchorView, xoff, yoff, gravity)
    }

    fun setOnDismiss(onDismiss: () -> Unit) {
        setOnDismissListener { onDismiss() }
    }
}
