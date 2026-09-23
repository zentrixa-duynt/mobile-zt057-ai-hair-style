package com.example.aihair.core.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var drawPath: Path = Path()
    private var drawPaint: Paint = Paint()
    private var canvasPaint: Paint = Paint(Paint.DITHER_FLAG)
    private var drawCanvas: Canvas? = null
    private var canvasBitmap: Bitmap? = null
    
    // Store paths for undo functionality if needed
    private val paths = ArrayList<Pair<Path, Paint>>()

    var isDrawingEnabled = false
        set(value) {
            field = value
            invalidate()
        }

    init {
        setupDrawing()
    }

    private fun setupDrawing() {
        drawPaint.color = Color.TRANSPARENT
        drawPaint.isAntiAlias = true
        drawPaint.strokeWidth = 60f // Default brush size
        drawPaint.style = Paint.Style.STROKE
        drawPaint.strokeJoin = Paint.Join.ROUND
        drawPaint.strokeCap = Paint.Cap.ROUND
        // Set alpha to simulate coloring (e.g., 40%)
        drawPaint.alpha = 100 
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            drawCanvas = Canvas(canvasBitmap!!)
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvasBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, canvasPaint)
        }
        canvas.drawPath(drawPath, drawPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isDrawingEnabled) return false

        val touchX = event.x
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                drawPath.moveTo(touchX, touchY)
            }
            MotionEvent.ACTION_MOVE -> {
                drawPath.lineTo(touchX, touchY)
            }
            MotionEvent.ACTION_UP -> {
                drawPath.lineTo(touchX, touchY)
                drawCanvas?.drawPath(drawPath, drawPaint)
                
                // Save path for undo
                val newPaint = Paint(drawPaint)
                paths.add(Pair(Path(drawPath), newPaint))
                
                drawPath.reset()
            }
            else -> return false
        }
        invalidate()
        return true
    }

    fun setPaintColor(color: Int) {
        drawPaint.color = color
        // Use a blend mode or just transparency to simulate hair color overlay
        drawPaint.alpha = 100 // about 40% transparency
        drawPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }

    fun setBrushSize(newSize: Float) {
        drawPaint.strokeWidth = newSize
    }

    fun clearDrawing() {
        paths.clear()
        drawPath.reset()
        canvasBitmap?.eraseColor(Color.TRANSPARENT)
        invalidate()
    }

    fun undo() {
        if (paths.isNotEmpty()) {
            paths.removeAt(paths.size - 1)
            canvasBitmap?.eraseColor(Color.TRANSPARENT)
            paths.forEach { (path, paint) ->
                drawCanvas?.drawPath(path, paint)
            }
            invalidate()
        }
    }
}
