package com.example.aihair.core.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.net.toUri

/**
 * Lấy kích thước (width, height) của ảnh từ Uri string mà không cần load toàn bộ ảnh vào bộ nhớ.
 */
fun Context.getImageDimensions(uriString: String?): Pair<Int, Int>? {
    if (uriString.isNullOrEmpty()) return null
    return try {
        val uri = uriString.toUri()
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            if (options.outWidth > 0 && options.outHeight > 0) {
                Pair(options.outWidth, options.outHeight)
            } else {
                null
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Lấy tỉ lệ (width:height) của ảnh từ Uri string.
 */
fun Context.getImageRatio(uriString: String?): String? {
    val (width, height) = getImageDimensions(uriString) ?: return null
    return "$width:$height"
}

/**
 * Kiểm tra xem ảnh có phải là ảnh dọc không (height >= width).
 */
fun Context.isImageVertical(uriString: String?): Boolean {
    val (width, height) = getImageDimensions(uriString) ?: return false
    return height >= width
}

/**
 * Extension function để set dimensionRatio cho các view nằm trong ConstraintLayout.
 */
fun View.setDimensionRatio(ratio: String) {
    val params = layoutParams as? ConstraintLayout.LayoutParams ?: return
    params.dimensionRatio = ratio
    layoutParams = params
}

/**
 * Lưu ảnh (từ mạng hoặc local file) vào thư viện (Gallery) an toàn.
 */
suspend fun Context.saveImageToGallery(uriStr: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val file = if (uriStr.startsWith("http")) {
            val bitmap = Glide.with(this@saveImageToGallery)
                .asBitmap()
                .load(uriStr)
                .submit()
                .get()
            val tempFile = File(cacheDir, "temp_save_${System.currentTimeMillis()}.jpg")
            tempFile.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            tempFile
        } else {
            val uri = Uri.parse(uriStr)
            File(uri.path!!)
        }

        if (!file.exists()) return@withContext false

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "AIHair_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        
        val resolver = contentResolver
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (imageUri != null) {
            resolver.openOutputStream(imageUri).use { out ->
                file.inputStream().use { input ->
                    input.copyTo(out!!)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }
            return@withContext true
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return@withContext false
}

/**
 * Nén và scale ảnh để giảm dung lượng trước khi upload.
 */
suspend fun Context.compressImageFile(uriStr: String, maxWidth: Int = 1024, maxHeight: Int = 1024, quality: Int = 85): File? = withContext(Dispatchers.IO) {
    try {
        val uri = Uri.parse(uriStr)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        
        contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }

        var inSampleSize = 1
        if (options.outHeight > maxHeight || options.outWidth > maxWidth) {
            val halfHeight: Int = options.outHeight / 2
            val halfWidth: Int = options.outWidth / 2
            while (halfHeight / inSampleSize >= maxHeight && halfWidth / inSampleSize >= maxWidth) {
                inSampleSize *= 2
            }
        }

        options.inSampleSize = inSampleSize
        options.inJustDecodeBounds = false
        
        val bitmap = contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: return@withContext null

        val tempFile = File(cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
        tempFile.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        bitmap.recycle()
        return@withContext tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}
