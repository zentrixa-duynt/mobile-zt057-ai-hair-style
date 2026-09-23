package com.example.aihair.core.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.aihair.R
import kotlin.math.sqrt

class ColorWheelView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * resources.displayMetrics.density
        color = Color.parseColor("#030712")
    }
    
    private var cx = 0f
    private var cy = 0f
    private var radius = 0f

    private var thumbX = 0f
    private var thumbY = 0f
    private val thumbRadius = 12f * resources.displayMetrics.density // 24dp diameter

    var onColorChangedListener: ((Int, String) -> Unit)? = null
    
    var currentColor = Color.WHITE
        private set

    private var initialColorHex: String? = null

    fun setInitialColor(hex: String) {
        initialColorHex = hex
        if (paletteBitmap != null) {
            updateThumbPositionFromInitialColor()
        }
    }

    private var originalBitmap: Bitmap? = null
    private var paletteBitmap: Bitmap? = null
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }
    private val bitmapRect = RectF()

    init {
        originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.img_palette)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cx = w / 2f
        cy = h / 2f
        // Add padding equal to thumbRadius to prevent the thumb from being clipped at the edges
        radius = Math.min(w, h) / 2f - thumbRadius

        val size = (radius * 2).toInt()
        if (size > 0 && originalBitmap != null) {
            paletteBitmap = Bitmap.createScaledBitmap(originalBitmap!!, size, size, true)
        }
        
        bitmapRect.set(cx - radius, cy - radius, cx + radius, cy + radius)

        // Initialize thumb based on initial color or center
        if (initialColorHex != null) {
            updateThumbPositionFromInitialColor()
        } else if (thumbX == 0f && thumbY == 0f) {
            thumbX = cx
            thumbY = cy
        }
    }

    private fun updateThumbPositionFromInitialColor() {
        val hex = initialColorHex ?: return
        val targetColor = try { Color.parseColor(hex) } catch (e: Exception) { return }
        val bmp = paletteBitmap ?: return
        
        val targetR = Color.red(targetColor)
        val targetG = Color.green(targetColor)
        val targetB = Color.blue(targetColor)
        
        var minDiff = Int.MAX_VALUE
        var bestX = bmp.width / 2
        var bestY = bmp.height / 2
        
        val step = 4 // Subsample to maintain fast performance
        for (y in 0 until bmp.height step step) {
            for (x in 0 until bmp.width step step) {
                val pixel = bmp.getPixel(x, y)
                if (Color.alpha(pixel) > 200) {
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    val diff = (r - targetR) * (r - targetR) + 
                               (g - targetG) * (g - targetG) + 
                               (b - targetB) * (b - targetB)
                    if (diff < minDiff) {
                        minDiff = diff
                        bestX = x
                        bestY = y
                    }
                }
            }
        }
        
        thumbX = bitmapRect.left + (bestX.toFloat() / bmp.width) * bitmapRect.width()
        thumbY = bitmapRect.top + (bestY.toFloat() / bmp.height) * bitmapRect.height()
        
        initialColorHex = null 
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        // Draw the static design image
        paletteBitmap?.let {
            canvas.drawBitmap(it, null, bitmapRect, bitmapPaint)
        }
        
        // Draw the selector thumb
        canvas.drawCircle(thumbX, thumbY, thumbRadius, thumbPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                updateColorFromTouch(event.x, event.y)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateColorFromTouch(x: Float, y: Float) {
        val dx = x - cx
        val dy = y - cy
        val d = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        if (d > radius) {
            thumbX = cx + dx * radius / d
            thumbY = cy + dy * radius / d
        } else {
            thumbX = x
            thumbY = y
        }

        // Extract color precisely from the design bitmap
        paletteBitmap?.let { bmp ->
            // Map the thumb coordinates to the bitmap's coordinates
            val mappedX = ((thumbX - bitmapRect.left) / bitmapRect.width() * bmp.width).toInt()
            val mappedY = ((thumbY - bitmapRect.top) / bitmapRect.height() * bmp.height).toInt()
            
            // Clamp strictly to prevent OutOfBounds
            var safeX = mappedX.coerceIn(0, bmp.width - 1)
            var safeY = mappedY.coerceIn(0, bmp.height - 1)
            
            var pixelColor = bmp.getPixel(safeX, safeY)
            
            // If pixel is semi-transparent (edge anti-aliasing), inch towards center
            var attempts = 0
            val bmpCx = bmp.width / 2
            val bmpCy = bmp.height / 2
            while (Color.alpha(pixelColor) < 250 && attempts < 20) {
                if (safeX < bmpCx) safeX++ else if (safeX > bmpCx) safeX--
                if (safeY < bmpCy) safeY++ else if (safeY > bmpCy) safeY--
                pixelColor = bmp.getPixel(safeX, safeY)
                attempts++
            }
            
            // Only update if we found a reasonably opaque pixel
            if (Color.alpha(pixelColor) > 200) {
                // Remove any remaining transparency
                currentColor = Color.rgb(Color.red(pixelColor), Color.green(pixelColor), Color.blue(pixelColor))
                
                val hex = String.format("#%06X", 0xFFFFFF and currentColor)
                onColorChangedListener?.invoke(currentColor, hex)
            }
        }

        invalidate()
    }
}
