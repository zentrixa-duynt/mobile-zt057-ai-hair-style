package com.example.aihair.feature.photo_editor.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

class ImageProcessor(private val context: Context) {

    suspend fun processColorMode(
        selectedPhotoUri: Uri,
        maskCompositeBitmap: Bitmap?,
        freehandMaskBitmap: Bitmap?,
        colorMatrix: ColorMatrix?
    ): String? = withContext(Dispatchers.IO) {
        try {
            val highResBgBitmap = Glide.with(context)
                .asBitmap()
                .load(selectedPhotoUri)
                .override(2048, 2048)
                .submit()
                .get()

            val resultBitmap = createBitmap(highResBgBitmap.width, highResBgBitmap.height)
            val canvas = Canvas(resultBitmap)
            canvas.drawBitmap(highResBgBitmap, 0f, 0f, null)

            if (maskCompositeBitmap != null && colorMatrix != null && freehandMaskBitmap != null) {
                val scaledMask =
                    maskCompositeBitmap.scale(highResBgBitmap.width, highResBgBitmap.height)
                val maskCanvas = Canvas(scaledMask)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                    colorFilter = ColorMatrixColorFilter(colorMatrix)
                }

                maskCanvas.drawBitmap(highResBgBitmap, 0f, 0f, paint)

                paint.xfermode = null
                paint.colorFilter = null
                canvas.drawBitmap(scaledMask, 0f, 0f, paint)
                scaledMask.recycle()
            }

            saveBitmapToCache(resultBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun processStyleMode(
        selectedPhotoUri: Uri,
        bgBitmap: Bitmap,
        hairBitmap: Bitmap,
        bgMatrix: Matrix,
        hairMatrix: Matrix,
        hairColorFilter: ColorFilter?
    ): String? = withContext(Dispatchers.IO) {
        try {
            val highResBgBitmap = Glide.with(context)
                .asBitmap()
                .load(selectedPhotoUri)
                .override(2048, 2048)
                .submit()
                .get()

            val resultBitmap = createBitmap(highResBgBitmap.width, highResBgBitmap.height)
            val canvas = Canvas(resultBitmap)
            canvas.drawBitmap(highResBgBitmap, 0f, 0f, null)

            val scaleRatio = highResBgBitmap.width.toFloat() / bgBitmap.width.toFloat()
            val inverseBgMatrix = Matrix()
            bgMatrix.invert(inverseBgMatrix)

            val finalHairMatrix = Matrix()
            finalHairMatrix.set(hairMatrix)
            finalHairMatrix.postConcat(inverseBgMatrix)
            finalHairMatrix.postScale(scaleRatio, scaleRatio)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = hairColorFilter
            }
            canvas.drawBitmap(hairBitmap, finalHairMatrix, paint)

            saveBitmapToCache(resultBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveBitmapToCache(bitmap: Bitmap): String {
        val tempFile = File(context.cacheDir, "composite_hair_${System.currentTimeMillis()}.jpg")
        FileOutputStream(tempFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        return Uri.fromFile(tempFile).toString()
    }
}