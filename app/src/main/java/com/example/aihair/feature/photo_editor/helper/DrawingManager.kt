package com.example.aihair.feature.photo_editor.helper

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.MotionEvent
import androidx.core.graphics.createBitmap
import com.example.aihair.feature.photo_editor.PhotoEditorActivity
import com.github.chrisbanes.photoview.PhotoView

class DrawingManager(private val imageView: PhotoView) {

    enum class PaintMode { BRUSH, ERASER, NONE }

    var originalBitmap: Bitmap? = null
        private set
    var maskCompositeBitmap: Bitmap? = null
        private set
    var freehandMaskBitmap: Bitmap? = null
        private set

    private var compositeBitmap: Bitmap? = null
    private var freehandCanvas: Canvas? = null

    private val currentDrawPath = Path()
    private val freehandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 30f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
    }

    fun initBitmaps(bitmap: Bitmap) {
        originalBitmap = bitmap
        freehandMaskBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        freehandCanvas = Canvas(freehandMaskBitmap!!)
        compositeBitmap = null
        maskCompositeBitmap = null
        currentDrawPath.reset()
    }

    fun clearBitmaps() {
        originalBitmap = null
        freehandMaskBitmap = null
        freehandCanvas = null
        compositeBitmap = null
        maskCompositeBitmap = null
        currentDrawPath.reset()
    }

    fun clearColor() {
        freehandMaskBitmap?.eraseColor(Color.TRANSPARENT)
    }

    fun onTouchEvent(
        event: MotionEvent,
        paintMode: PaintMode,
        onRecompositeNeeded: () -> Unit
    ): Boolean {
        if (paintMode == PaintMode.NONE || originalBitmap == null) return false

        val inverseMatrix = Matrix()
        imageView.imageMatrix.invert(inverseMatrix)
        val point = floatArrayOf(event.x, event.y)
        inverseMatrix.mapPoints(point)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val values = FloatArray(9)
                imageView.imageMatrix.getValues(values)
                val currentScale = values[Matrix.MSCALE_X]

                val dynamicStrokeWidth = 40f / currentScale
                val dynamicBlurRadius = (15f / currentScale).coerceAtLeast(1f)

                freehandPaint.strokeWidth = dynamicStrokeWidth
                freehandPaint.maskFilter =
                    BlurMaskFilter(dynamicBlurRadius, BlurMaskFilter.Blur.NORMAL)

                currentDrawPath.reset()
                currentDrawPath.moveTo(point[0], point[1])
            }
            MotionEvent.ACTION_MOVE -> {
                currentDrawPath.lineTo(point[0], point[1])
                onRecompositeNeeded()
            }
            MotionEvent.ACTION_UP -> {
                currentDrawPath.lineTo(point[0], point[1])
                if (paintMode == PaintMode.BRUSH) {
                    freehandPaint.xfermode = null
                    freehandPaint.color = Color.BLACK
                } else if (paintMode == PaintMode.ERASER) {
                    freehandPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                    freehandPaint.color = Color.TRANSPARENT
                }
                freehandCanvas?.drawPath(currentDrawPath, freehandPaint)
                currentDrawPath.reset()

                onRecompositeNeeded()
            }
        }
        return true
    }

    fun recompositeImage(colorMatrix: ColorMatrix?, paintMode: PaintMode) {
        val base = originalBitmap ?: return

        if (compositeBitmap == null || compositeBitmap?.width != base.width || compositeBitmap?.height != base.height) {
            compositeBitmap =
                createBitmap(base.width, base.height, base.config ?: Bitmap.Config.ARGB_8888)
        }
        if (maskCompositeBitmap == null || maskCompositeBitmap?.width != base.width || maskCompositeBitmap?.height != base.height) {
            maskCompositeBitmap = createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
        }

        val composite = compositeBitmap!!
        val maskComposite = maskCompositeBitmap!!

        val canvas = Canvas(composite)
        val maskCanvas = Canvas(maskComposite)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Clear previous mask composite
        maskCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        // Draw the freehand mask
        freehandMaskBitmap?.let { maskCanvas.drawBitmap(it, 0f, 0f, paint) }

        // Draw the current unbaked drawing path
        if (paintMode != PaintMode.NONE && !currentDrawPath.isEmpty) {
            val originalColor = freehandPaint.color
            val originalXfermode = freehandPaint.xfermode

            if (paintMode == PaintMode.BRUSH) {
                freehandPaint.xfermode = null
                freehandPaint.color = Color.BLACK
            } else if (paintMode == PaintMode.ERASER) {
                freehandPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                freehandPaint.color = Color.TRANSPARENT
            }
            maskCanvas.drawPath(currentDrawPath, freehandPaint)

            freehandPaint.color = originalColor
            freehandPaint.xfermode = originalXfermode
        }

        if (colorMatrix != null) {
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)

            // Draw base onto maskCanvas using SRC_IN and ColorMatrix
            maskCanvas.drawBitmap(base, 0f, 0f, paint)

            // Reset paint for composite
            paint.xfermode = null
            paint.colorFilter = null
            paint.alpha = 255
        } else {
            // Fallback
            paint.alpha = 100
        }

        // Draw base image
        canvas.drawBitmap(base, 0f, 0f, paint)

        // Draw the unified tinted mask over base
        canvas.drawBitmap(maskComposite, 0f, 0f, paint)

        val currentMatrix = Matrix()
        imageView.getSuppMatrix(currentMatrix)

        imageView.setImageBitmap(composite)

        imageView.setSuppMatrix(currentMatrix)
        imageView.invalidate()
    }
}