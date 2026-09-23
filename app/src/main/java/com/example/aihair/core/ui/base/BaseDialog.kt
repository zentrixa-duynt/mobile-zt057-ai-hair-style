package com.example.aihair.core.ui.base

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.core.graphics.drawable.toDrawable

abstract class BaseDialog(context: Context) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val marginPx = (16 * context.resources.displayMetrics.density).toInt()
        val inset = InsetDrawable(Color.TRANSPARENT.toDrawable(), marginPx, 0, marginPx, 0)
        window?.setBackgroundDrawable(inset)

        setupView()

        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window?.setDimAmount(0.5f)
    }

    abstract fun setupView()
}
