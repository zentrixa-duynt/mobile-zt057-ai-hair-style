package com.example.aihair.feature.photo_editor.helper

import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import com.github.chrisbanes.photoview.PhotoView
import kotlin.math.atan2
import kotlin.math.sqrt

class StickerTouchListener(
    private val isSticker: Boolean = true,
    private val onUnconsumedTouch: ((MotionEvent) -> Unit)? = null,
    private val getPhotoBounds: (() -> RectF?)? = null
) : View.OnTouchListener {
    private val savedMatrix = Matrix()
    private val initialDragMatrix = Matrix()

    private var mode = NONE
    private val start = PointF()
    private val mid = PointF()
    private var oldDist = 1f
    private var oldAngle = 0f
    
    private var isTarget = false

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM_ROTATE = 2
    }
    
    private fun getDisplayRect(view: View): RectF? {
        if (view is PhotoView) return view.displayRect
        if (view is ImageView) {
            val drawable = view.drawable ?: return null
            val rect = RectF(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
            view.imageMatrix.mapRect(rect)
            return rect
        }
        return null
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            if (isSticker && v is ImageView) {
                isTarget = isTouchOnImagePixels(v, event.x, event.y)
            } else {
                isTarget = true
            }
        }
        
        if (!isTarget) {
            onUnconsumedTouch?.invoke(event)
            return true
        }
        
        if (isSticker && v is ImageView && v.scaleType != ImageView.ScaleType.MATRIX) {
            v.scaleType = ImageView.ScaleType.MATRIX
        }

        val currentMatrix = Matrix()
        if (v is PhotoView && !isSticker) {
            v.getSuppMatrix(currentMatrix)
        } else if (v is ImageView) {
            currentMatrix.set(v.imageMatrix)
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(currentMatrix)
                initialDragMatrix.set(currentMatrix)
                start.set(event.x, event.y)
                mode = DRAG
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)
                if (oldDist > 10f) {
                    savedMatrix.set(currentMatrix)
                    if (isSticker) {
                        val displayRect = getDisplayRect(v)
                        if (displayRect != null) {
                            mid.set(displayRect.centerX(), displayRect.centerY())
                        } else {
                            mid.set(v.width / 2f, v.height / 2f)
                        }
                    } else {
                        mid.set(v.width / 2f, v.height / 2f)
                    }
                    if (isSticker) oldAngle = rotation(event)
                    mode = ZOOM_ROTATE
                }
            }
            MotionEvent.ACTION_UP -> {
                mode = NONE
                if (isSticker && v is ImageView) {
                    val stickerRect = getDisplayRect(v)
                    val photoRect = getPhotoBounds?.invoke() ?: RectF(0f, 0f, v.width.toFloat(), v.height.toFloat())
                    if (stickerRect != null) {
                        val intersection = RectF(stickerRect)
                        val hasIntersection = intersection.intersect(photoRect)
                        val percentInside = if (hasIntersection) {
                            (intersection.width() * intersection.height()) / (stickerRect.width() * stickerRect.height())
                        } else {
                            0f
                        }
                        
                        // Reset nếu tóc giả nằm TRONG ảnh ít hơn 20% (tức là bị kéo ra ngoài quá 80%)
                        if (percentInside < 0.2f) {
                            applyMatrix(v, initialDragMatrix)
                        }
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    currentMatrix.set(savedMatrix)
                    currentMatrix.postTranslate(event.x - start.x, event.y - start.y)
                    applyMatrix(v, currentMatrix)
                } else if (mode == ZOOM_ROTATE) {
                    val newDist = spacing(event)
                    if (newDist > 10f) {
                        currentMatrix.set(savedMatrix)
                        
                        val scale = newDist / oldDist
                        currentMatrix.postScale(scale, scale, mid.x, mid.y)
                        
                        if (isSticker) {
                            val newAngle = rotation(event)
                            currentMatrix.postRotate(newAngle - oldAngle, mid.x, mid.y)
                        }
                        
                        applyMatrix(v, currentMatrix)
                    }
                }
            }
        }
        return true
    }
    
    private fun applyMatrix(view: View, matrix: Matrix) {
        if (view is PhotoView && !isSticker) {
            view.setSuppMatrix(matrix)
        } else if (view is ImageView) {
            view.imageMatrix = matrix
        }
    }

    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }

    private fun rotation(event: MotionEvent): Float {
        val deltaX = (event.getX(0) - event.getX(1)).toDouble()
        val deltaY = (event.getY(0) - event.getY(1)).toDouble()
        val radians = atan2(deltaY, deltaX)
        return Math.toDegrees(radians).toFloat()
    }
    
    private fun isTouchOnImagePixels(v: ImageView, x: Float, y: Float): Boolean {
        try {
            val drawable = v.drawable ?: return false
            val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return false
            
            val inverseMatrix = Matrix()
            v.imageMatrix.invert(inverseMatrix)
            val pts = floatArrayOf(x, y)
            inverseMatrix.mapPoints(pts)
            
            val bx = pts[0].toInt()
            val by = pts[1].toInt()
            
            val values = FloatArray(9)
            v.imageMatrix.getValues(values)
            val scale = values[Matrix.MSCALE_X]
            val tolerance = (40f / scale).toInt().coerceIn(5, 200)
            
            if (bx in 0 until bitmap.width && by in 0 until bitmap.height) {
                if (Color.alpha(bitmap.getPixel(bx, by)) > 10) return true
            }
            
            val step = Math.max(1, tolerance / 4)
            for (dy in -tolerance..tolerance step step) {
                for (dx in -tolerance..tolerance step step) {
                    val checkX = bx + dx
                    val checkY = by + dy
                    if (checkX in 0 until bitmap.width && checkY in 0 until bitmap.height) {
                        if (Color.alpha(bitmap.getPixel(checkX, checkY)) > 10) {
                            return true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            val displayRect = getDisplayRect(v)
            if (displayRect != null) {
                return displayRect.contains(x, y)
            }
        }
        return false
    }
}
