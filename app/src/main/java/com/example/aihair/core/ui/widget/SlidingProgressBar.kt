package com.example.aihair.core.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout

class SlidingProgressBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val indicator = View(context)
    private var animator: ValueAnimator? = null

    init {
        // Track (background)
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#99FFFFFF"))
            cornerRadius = 1000f
        }
        
        // Indicator (foreground moving bar)
        indicator.background = GradientDrawable().apply {
            setColor(Color.parseColor("#F6339A"))
            cornerRadius = 1000f
        }
        
        // Cố định thanh indicator dài 54dp
        val indicatorWidth = (54 * resources.displayMetrics.density).toInt()
        addView(indicator, LayoutParams(indicatorWidth, LayoutParams.MATCH_PARENT))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        animator?.cancel()
        
        val indicatorWidth = (54 * resources.displayMetrics.density).toInt()
        val maxTranslation = (w - indicatorWidth).toFloat()
        
        if (maxTranslation > 0) {
            val isRtl = layoutDirection == View.LAYOUT_DIRECTION_RTL
            val endTranslation = if (isRtl) -maxTranslation else maxTranslation
            
            animator = ValueAnimator.ofFloat(0f, endTranslation).apply {
                duration = 1000 // 1s chạy từ trái qua phải hoặc phải qua trái
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener {
                    indicator.translationX = it.animatedValue as Float
                }
                start()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
