package com.example.aihair.core.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.content.res.ResourcesCompat
import com.example.aihair.R
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

class SlimeLineTabBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var tabTitles = listOf(
        context.getString(R.string.text_hair_ai),
        context.getString(R.string.text_hair_tools)
    )

    fun setTabTitles(titles: List<String>) {
        this.tabTitles = titles
        requestLayout()
        post {
            indicatorLeft = getTabLeft(selectedIndex)
            indicatorRight = getTabRight(selectedIndex)
            invalidate()
        }
    }
    
    fun applyConfig(tabWidthDp: Float, tabGapDp: Float, textSizeSpVal: Float, tabHeightDp: Float = 48f) {
        this.tabWidth = tabWidthDp.dpToPx()
        this.tabGap = tabGapDp.dpToPx()
        this.textSizeSp = textSizeSpVal.spToPx()
        this.tabHeight = tabHeightDp.dpToPx()
        
        inactiveTextPaint.textSize = this.textSizeSp
        activeTextPaint.textSize = this.textSizeSp
        
        requestLayout()
        post {
            indicatorLeft = getTabLeft(selectedIndex)
            indicatorRight = getTabRight(selectedIndex)
            invalidate()
        }
    }

    // Configs
    private var tabWidth = 116f.dpToPx()
    private var tabGap = 42f.dpToPx()
    private var textSizeSp = 16f.spToPx()
    private var tabHeight = 48f.dpToPx()
    private val indicatorHeight = 1f.dpToPx()

    // Paints
    private val inactiveTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = textSizeSp
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, R.font.poppins_medium)
    }

    private val activeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F6339A")
        textSize = textSizeSp
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, R.font.poppins_medium)
    }

    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F6339A")
        style = Paint.Style.FILL
    }

    // State
    private var selectedIndex = 0
    private var indicatorLeft = 0f
    private var indicatorRight = 0f

    // Animations
    private var leftAnimator: ValueAnimator? = null
    private var rightAnimator: ValueAnimator? = null

    init {
        // Initialize bounds without animation
        post {
            indicatorLeft = getTabLeft(selectedIndex)
            indicatorRight = getTabRight(selectedIndex)
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = (tabWidth * tabTitles.size) + (tabGap * (tabTitles.size - 1))
        val h = tabHeight
        setMeasuredDimension(w.toInt(), h.toInt())
    }

    private var needsMarqueeAnimation = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerY = height / 2f - (inactiveTextPaint.descent() + inactiveTextPaint.ascent()) / 2f
        val bottomY = height.toFloat()

        needsMarqueeAnimation = false

        // 1. Draw inactive text for all tabs (Base layer)
        for (i in tabTitles.indices) {
            val title = tabTitles[i]
            val textWidth = inactiveTextPaint.measureText(title)
            val cx = getTabDrawX(i, textWidth)
            
            canvas.save()
            canvas.clipRect(getTabLeft(i), 0f, getTabRight(i), bottomY)
            canvas.drawText(title, cx, centerY, inactiveTextPaint)
            canvas.restore()
        }

        // 2. Draw active text with clipping (Wipe / Spreading Fungus effect)
        canvas.save()
        canvas.clipRect(indicatorLeft, 0f, indicatorRight, bottomY)
        for (i in tabTitles.indices) {
            val title = tabTitles[i]
            val textWidth = activeTextPaint.measureText(title)
            val cx = getTabDrawX(i, textWidth)
            
            canvas.save()
            canvas.clipRect(getTabLeft(i), 0f, getTabRight(i), bottomY)
            canvas.drawText(title, cx, centerY, activeTextPaint)
            canvas.restore()
        }
        canvas.restore()

        // 3. Draw the slime indicator line at the bottom
        canvas.drawRect(indicatorLeft, bottomY - indicatorHeight, indicatorRight, bottomY, indicatorPaint)

        if (needsMarqueeAnimation) {
            invalidate()
        }
    }

    private fun getTabDrawX(index: Int, textWidth: Float): Float {
        if (textWidth <= tabWidth) {
            return getTabCenterX(index)
        }
        
        needsMarqueeAnimation = true
        val overflow = textWidth - tabWidth + 16f.dpToPx()
        val speed = 25f.dpToPx() // px per second
        val timeToScroll = (overflow / speed * 1000).toLong()
        val pauseTime = 1500L // 1.5 second pause
        val totalCycle = (timeToScroll + pauseTime) * 2
        
        val currentTime = System.currentTimeMillis() % totalCycle
        
        val offset = when {
            currentTime < pauseTime -> 0f
            currentTime < pauseTime + timeToScroll -> {
                val dt = currentTime - pauseTime
                (dt * speed / 1000f)
            }
            currentTime < pauseTime * 2 + timeToScroll -> overflow
            else -> {
                val dt = currentTime - (pauseTime * 2 + timeToScroll)
                overflow - (dt * speed / 1000f)
            }
        }
        
        val tabLeft = getTabLeft(index)
        val startX = tabLeft + textWidth / 2f + 8f.dpToPx()
        return startX - offset
    }

    private fun getTabLeft(index: Int): Float {
        return index * (tabWidth + tabGap)
    }

    private fun getTabRight(index: Int): Float {
        return getTabLeft(index) + tabWidth
    }

    private fun getTabCenterX(index: Int): Float {
        return getTabLeft(index) + tabWidth / 2f
    }

    var onTabSelected: ((Int) -> Unit)? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            // Expanded hit area calculation for better UX
            for (i in tabTitles.indices) {
                val cx = getTabCenterX(i)
                if (Math.abs(x - cx) <= (tabWidth + tabGap) / 2f) {
                    setSelectedIndex(i, animate = true)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    fun setSelectedIndex(index: Int, animate: Boolean = true) {
        if (index == selectedIndex || index !in tabTitles.indices) return
        
        val isMovingRight = index > selectedIndex
        selectedIndex = index
        onTabSelected?.invoke(index)

        val targetLeft = getTabLeft(index)
        val targetRight = getTabRight(index)

        leftAnimator?.cancel()
        rightAnimator?.cancel()

        if (!animate) {
            indicatorLeft = targetLeft
            indicatorRight = targetRight
            invalidate()
            return
        }

        // Animate left edge
        leftAnimator = ValueAnimator.ofFloat(indicatorLeft, targetLeft).apply {
            duration = 450
            if (isMovingRight) {
                // Moving right: left edge delays and rubberbands
                startDelay = 100
                interpolator = OvershootInterpolator(1.2f)
            } else {
                // Moving left: left edge leads
                startDelay = 0
                interpolator = FastOutSlowInInterpolator()
            }
            addUpdateListener { 
                indicatorLeft = it.animatedValue as Float
                invalidate()
            }
        }

        // Animate right edge
        rightAnimator = ValueAnimator.ofFloat(indicatorRight, targetRight).apply {
            duration = 450
            if (isMovingRight) {
                // Moving right: right edge leads
                startDelay = 0
                interpolator = FastOutSlowInInterpolator()
            } else {
                // Moving left: right edge delays and rubberbands
                startDelay = 100
                interpolator = OvershootInterpolator(1.2f)
            }
            addUpdateListener { 
                indicatorRight = it.animatedValue as Float
                invalidate()
            }
        }

        leftAnimator?.start()
        rightAnimator?.start()
    }

    private fun Float.spToPx(): Float = this * resources.displayMetrics.scaledDensity
    private fun Float.dpToPx(): Float = this * resources.displayMetrics.density
    private fun Int.dpToPx(): Float = this * resources.displayMetrics.density
}
