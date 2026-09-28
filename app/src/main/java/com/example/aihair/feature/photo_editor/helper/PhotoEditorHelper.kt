package com.example.aihair.feature.photo_editor.helper

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.Matrix
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import com.example.aihair.R
import com.example.aihair.core.utils.HairColorMap
import com.example.aihair.core.utils.parseColorSafe
import com.example.aihair.databinding.ActivityPhotoEditorBinding
import com.example.aihair.feature.photo_editor.PhotoEditorUiState
import com.github.chrisbanes.photoview.OnMatrixChangedListener

class PhotoEditorHelper(private val binding: ActivityPhotoEditorBinding) {

    private var lastBgMatrix = Matrix()
    var isBgMatrixInitialized = false

    @SuppressLint("ClickableViewAccessibility")
    fun setupZoomAndPanRouting(isColorMode: Boolean = false, currentStateProvider: () -> PhotoEditorUiState.Success?): View.OnTouchListener {
        val stickerTouchListener = StickerTouchListener(
            isSticker = true,
            onUnconsumedTouch = { event ->
                val currentState = currentStateProvider()
                if (currentState != null && currentState.selectedPhotoUri != null) {
                    val activeBg = binding.imgSelectedPhoto
                    activeBg.dispatchTouchEvent(event)
                }
            },
            getPhotoBounds = {
                binding.imgSelectedPhoto.displayRect
            }
        )

        val matrixChangeListener = OnMatrixChangedListener { _ ->
            if (!binding.imgHairOverlay.isVisible) return@OnMatrixChangedListener

            val activeBg = binding.imgSelectedPhoto

            val currentBgMatrix = Matrix()
            activeBg.getSuppMatrix(currentBgMatrix)

            if (!isBgMatrixInitialized) {
                lastBgMatrix.set(currentBgMatrix)
                isBgMatrixInitialized = true
                return@OnMatrixChangedListener
            }

            val inversePrev = Matrix()
            lastBgMatrix.invert(inversePrev)

            val deltaMatrix = Matrix()
            deltaMatrix.set(currentBgMatrix)
            deltaMatrix.preConcat(inversePrev)

            val hairMatrix = Matrix(binding.imgHairOverlay.imageMatrix)
            hairMatrix.postConcat(deltaMatrix)
            if (binding.imgHairOverlay.scaleType != ImageView.ScaleType.MATRIX) {
                binding.imgHairOverlay.scaleType = ImageView.ScaleType.MATRIX
            }
            binding.imgHairOverlay.imageMatrix = hairMatrix

            lastBgMatrix.set(currentBgMatrix)
        }

        binding.imgSelectedPhoto.setOnMatrixChangeListener(matrixChangeListener)
        
        // Remove manual backgroundTouchListener to allow PhotoView to handle zoom natively
        // in all modes.

        
        return stickerTouchListener
    }

    fun updateColorSelection(selectedColorId: String?, customHex: String?) {
        binding.bgColorNone.isVisible = false
        binding.icNoneUnselected.isVisible = true
        binding.icNoneSelected.isVisible = false
        binding.strokeInnerWhite.isVisible = false
        binding.strokeOuterWhite.isVisible = false
        binding.strokeInnerBlack.isVisible = false
        binding.strokeOuterBlack.isVisible = true
        binding.strokeInnerRed.isVisible = false
        binding.strokeInnerPink.isVisible = false
        binding.strokeInnerBlue.isVisible = false
        binding.strokeInnerGreen.isVisible = false
        binding.strokeInnerOrange.isVisible = false
        binding.strokeInnerYellow.isVisible = false
        binding.strokeInnerPurple.isVisible = false
        binding.strokeInnerCustom.isVisible = false
        binding.strokeOuterCustom.isVisible = false

        when (selectedColorId) {
            "none" -> {
                binding.bgColorNone.isVisible = true
                binding.icNoneUnselected.isVisible = false
                binding.icNoneSelected.isVisible = true
            }
            "c_white" -> {
                binding.strokeInnerWhite.isVisible = true
                binding.strokeOuterWhite.isVisible = true
            }
            "c_black" -> {
                binding.strokeInnerBlack.isVisible = true
                binding.strokeOuterBlack.isVisible = true
            }
            "c_red" -> binding.strokeInnerRed.isVisible = true
            "c_pink" -> binding.strokeInnerPink.isVisible = true
            "c_blue" -> binding.strokeInnerBlue.isVisible = true
            "c_green" -> binding.strokeInnerGreen.isVisible = true
            "c_orange" -> binding.strokeInnerOrange.isVisible = true
            "c_yellow" -> binding.strokeInnerYellow.isVisible = true
            "c_purple" -> binding.strokeInnerPurple.isVisible = true
            "custom" -> binding.strokeInnerCustom.isVisible = true
        }

        if (customHex != null) {
            binding.colorCustom.isVisible = true
            val colorInt = try { customHex.toColorInt() } catch (e: Exception) { Color.WHITE }
            binding.cardColorCustom.setCardBackgroundColor(colorInt)

            val isNearWhite = ColorUtils.calculateLuminance(colorInt) > 0.85
            binding.strokeOuterCustom.isVisible = isNearWhite
        } else {
            binding.colorCustom.isVisible = false
        }
    }

    fun getColorIntFromState(state: PhotoEditorUiState.Success): Int? {
        if (state.selectedColorId == null || state.selectedColorId == "none") return null
        if (state.selectedColorId == "custom" && state.customColorHex != null) {
            return state.customColorHex.parseColorSafe(Color.WHITE)
        }
        val hex = HairColorMap.getHexForId(state.selectedColorId)
        return hex?.parseColorSafe()
    }
    
    fun getHairColorMatrix(colorInt: Int?): ColorMatrix? {
        if (colorInt == null) return null
        val r = Color.red(colorInt)
        val g = Color.green(colorInt)
        val b = Color.blue(colorInt)
        val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b

        val matrix = ColorMatrix()
        // Step 1: Desaturate to remove original hair color
        matrix.setSaturation(0f)

        if (luminance < 40) {
            // Black family: Darken the base
            val scale = 0.5f
            val addR = 20f + (r / 255f) * 50f
            val addG = 20f + (g / 255f) * 50f
            val addB = 20f + (b / 255f) * 50f
            val darken = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, addR,
                0f, scale, 0f, 0f, addG,
                0f, 0f, scale, 0f, addB,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.postConcat(darken)
        } else {
            // All other colors (White, Pastels, Mid-tones, Bright colors)
            // Step 2: "Bleach" the dark hair to a Silver/White base (lift shadows)
            val scale = 0.6f
            val add = 110f
            val bleach = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, add,
                0f, scale, 0f, 0f, add,
                0f, 0f, scale, 0f, add,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.postConcat(bleach)
            
            // Step 3: "Dye" the hair by multiplying with the target color
            val dye = ColorMatrix(floatArrayOf(
                r / 255f, 0f, 0f, 0f, 0f,
                0f, g / 255f, 0f, 0f, 0f,
                0f, 0f, b / 255f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.postConcat(dye)
        }
        return matrix
    }
    
    fun getPhotoHairColorMatrix(colorInt: Int?): ColorMatrix? {
        if (colorInt == null) return null
        val r = Color.red(colorInt)
        val g = Color.green(colorInt)
        val b = Color.blue(colorInt)
        val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b

        val matrix = ColorMatrix()
        // Step 1: Desaturate
        matrix.setSaturation(0f)

        if (luminance < 40) {
            // Black/Dark colors:
            val scale = 0.8f
            val add = 30f
            val darken = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, add,
                0f, scale, 0f, 0f, add,
                0f, 0f, scale, 0f, add,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.postConcat(darken)
        } else {
            // Light colors (Blondes, Reds, Pink, Silver)
            val scale = 0.7f
            val add = 90f
            val bleach = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, add,
                0f, scale, 0f, 0f, add,
                0f, 0f, scale, 0f, add,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.postConcat(bleach)
        }
        
        // Dye step
        val dye = ColorMatrix(floatArrayOf(
            r / 255f, 0f, 0f, 0f, 0f,
            0f, g / 255f, 0f, 0f, 0f,
            0f, 0f, b / 255f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        matrix.postConcat(dye)
        
        return matrix
    }
}