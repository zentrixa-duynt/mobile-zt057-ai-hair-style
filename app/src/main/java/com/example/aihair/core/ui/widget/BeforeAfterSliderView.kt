package com.example.aihair.core.ui.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.example.aihair.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BeforeAfterSliderView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var leftBitmap: Bitmap? = null
    private var rightBitmap: Bitmap? = null
    
    private val leftMatrix = Matrix()
    private val rightMatrix = Matrix()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }

    private var sliderPosition = 0.5f // 0f to 1f
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 1f * resources.displayMetrics.density
    }
    
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private var thumbIcon: Drawable? = null
    private var autoPlayAnimatorSet: AnimatorSet? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    init {
        // Load default vector icon
        thumbIcon = ContextCompat.getDrawable(context, R.drawable.ic_convert)?.apply {
            DrawableCompat.setTint(this, Color.parseColor("#030712"))
        }
        
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.BeforeAfterSliderView, 0, 0)
            val leftResId = typedArray.getResourceId(R.styleable.BeforeAfterSliderView_imageLeft, 0)
            val rightResId = typedArray.getResourceId(R.styleable.BeforeAfterSliderView_imageRight, 0)
            
            if (leftResId != 0 || rightResId != 0) {
                setImages(leftResId, rightResId)
            }
            typedArray.recycle()
        }
    }

    fun setImages(leftResId: Int, rightResId: Int) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                if (leftResId != 0) {
                    leftBitmap = BitmapFactory.decodeResource(resources, leftResId, options)
                }
                if (rightResId != 0) {
                    rightBitmap = BitmapFactory.decodeResource(resources, rightResId, options)
                }
            }
            updateMatrices()
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateMatrices()
    }

    private fun updateMatrices() {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth == 0f || viewHeight == 0f) return

        leftBitmap?.let { bmp ->
            calculateCenterCropMatrix(bmp, viewWidth, viewHeight, leftMatrix)
        }
        rightBitmap?.let { bmp ->
            calculateCenterCropMatrix(bmp, viewWidth, viewHeight, rightMatrix)
        }
    }

    private fun calculateCenterCropMatrix(bitmap: Bitmap, viewWidth: Float, viewHeight: Float, matrix: Matrix) {
        val scale: Float
        var dx = 0f
        var dy = 0f

        if (bitmap.width * viewHeight > viewWidth * bitmap.height) {
            scale = viewHeight / bitmap.height.toFloat()
            dx = (viewWidth - bitmap.width * scale) * 0.5f
        } else {
            scale = viewWidth / bitmap.width.toFloat()
            dy = (viewHeight - bitmap.height * scale) * 0.5f
        }

        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, dy)
    }

    override fun onDraw(canvas: Canvas) {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val splitX = viewWidth * sliderPosition

        // Draw left image (left of the split line)
        leftBitmap?.let { bmp ->
            canvas.save()
            canvas.clipRect(0f, 0f, splitX, viewHeight)
            canvas.drawBitmap(bmp, leftMatrix, paint)
            canvas.restore()
        }

        // Draw right image (right of the split line)
        rightBitmap?.let { bmp ->
            canvas.save()
            canvas.clipRect(splitX, 0f, viewWidth, viewHeight)
            canvas.drawBitmap(bmp, rightMatrix, paint)
            canvas.restore()
        }

        // Draw split line
        canvas.drawLine(splitX, 0f, splitX, viewHeight, linePaint)

        // Draw thumb
        // According to CSS, top is 280px on a 328px container.
        // This means it's 48px from the bottom. Center of the 24px thumb is 36px from bottom.
        val thumbYCenter = viewHeight - (36f * resources.displayMetrics.density)
        val radius = 12f * resources.displayMetrics.density
        
        canvas.drawCircle(splitX, thumbYCenter, radius, thumbPaint)
        
        thumbIcon?.let { icon ->
            val iconSize = 16f * resources.displayMetrics.density
            val left = (splitX - iconSize / 2f).toInt()
            val top = (thumbYCenter - iconSize / 2f).toInt()
            val right = (splitX + iconSize / 2f).toInt()
            val bottom = (thumbYCenter + iconSize / 2f).toInt()
            
            icon.setBounds(left, top, right, bottom)
            icon.draw(canvas)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAutoPlay()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        autoPlayAnimatorSet?.cancel()
        scope.cancel()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == View.VISIBLE) {
            autoPlayAnimatorSet?.resume()
        } else {
            autoPlayAnimatorSet?.pause()
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == View.VISIBLE) {
            autoPlayAnimatorSet?.resume()
        } else {
            autoPlayAnimatorSet?.pause()
        }
    }

    private fun createSegment(start: Float, end: Float, durationMs: Long): ValueAnimator {
        return ValueAnimator.ofFloat(start, end).apply {
            duration = durationMs
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                sliderPosition = animator.animatedValue as Float
                invalidate()
            }
        }
    }

    private fun startAutoPlay() {
        autoPlayAnimatorSet?.cancel()
        
        val move1 = createSegment(0.1f, 0.5f, 400)
        val pause1 = createSegment(0.5f, 0.5f, 500)
        val move2 = createSegment(0.5f, 0.9f, 400)
        val pause2 = createSegment(0.9f, 0.9f, 500)
        val move3 = createSegment(0.9f, 0.5f, 400)
        val pause3 = createSegment(0.5f, 0.5f, 500)
        val move4 = createSegment(0.5f, 0.1f, 400)
        val pause4 = createSegment(0.1f, 0.1f, 500)

        autoPlayAnimatorSet = AnimatorSet().apply {
            playSequentially(move1, pause1, move2, pause2, move3, pause3, move4, pause4)
            addListener(object : AnimatorListenerAdapter() {
                var isCancelled = false
                override fun onAnimationCancel(animation: Animator) {
                    isCancelled = true
                }
                override fun onAnimationEnd(animation: Animator) {
                    if (!isCancelled) {
                        animation.start()
                    }
                }
            })
            start()
        }
    }
}
