package com.example.aihair.core.ui.click

import android.os.SystemClock
import android.view.View
import java.util.WeakHashMap

private const val DEFAULT_DEBOUNCE_INTERVAL_MS = 700L

private val lastClickAtByView = WeakHashMap<View, Long>()

fun View.setDebouncedClickListener(
    intervalMs: Long = DEFAULT_DEBOUNCE_INTERVAL_MS,
    onClick: (View) -> Unit,
) {
    setOnClickListener { view ->
        val now = SystemClock.elapsedRealtime()
        val lastClickAt = lastClickAtByView[view] ?: 0L
        if (now - lastClickAt < intervalMs) {
            return@setOnClickListener
        }
        lastClickAtByView[view] = now
        onClick(view)
    }
}




