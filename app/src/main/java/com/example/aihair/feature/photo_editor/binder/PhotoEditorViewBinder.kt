package com.example.aihair.feature.photo_editor.binder

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.transition.Transition
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.example.aihair.R
import com.example.aihair.core.ui.click.setDebouncedClickListener
import com.example.aihair.core.ui.transition.prepareSlideFadeIn
import com.example.aihair.core.ui.transition.startSlideFadeIn
import com.example.aihair.databinding.ActivityPhotoEditorBinding
import com.example.aihair.feature.photo_editor.PhotoEditorAction
import com.example.aihair.feature.photo_editor.PhotoEditorActivity
import com.example.aihair.feature.photo_editor.PhotoEditorUiState
import com.example.aihair.feature.photo_editor.component.ColorPickerDialog
import com.example.aihair.feature.photo_editor.helper.DrawingManager
import com.example.aihair.feature.photo_editor.helper.PhotoEditorHelper
import com.example.aihair.feature.photo_editor.helper.DrawingManager.PaintMode

class PhotoEditorViewBinder(
    private val activity: PhotoEditorActivity,
    private val binding: ActivityPhotoEditorBinding,
    private val helper: PhotoEditorHelper,
    private val drawingManager: DrawingManager,
    private val photoEditorAdapter: RecyclerView.Adapter<*>,
    private val hairType: Int,
    private val isMorph: Boolean,
    private val onAction: (PhotoEditorAction) -> Unit,
    private val onPickMedia: () -> Unit,
    private val onProcessAndContinue: () -> Unit,
    private val onSubmitList: () -> Unit
) {
    var paintMode = PaintMode.BRUSH
        private set

    private var lastPhotoUri: Uri? = null
    private var lastStyleResId: Int? = null
    private var lastColorId: String? = null
    private var lastCustomHex: String? = null
    private var hasShownColorWarningToast = false
    private var isMultiTouchGesture = false
    private var isFirstLoad = true
    private var isTransitionFinished = false
    
    private var currentState: PhotoEditorUiState.Success? = null

    fun setupUI() {
        setupBackNavigationAndTransitions()
        setupRecyclerViewsAndZoom()
        setupTouchListeners()
        setupActionButtons()
        setupColorClicks()
    }

    private fun setupBackNavigationAndTransitions() {
        activity.onBackPressedDispatcher.addCallback(activity, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                activity.finish()
            }
        })

        binding.btnBack.setDebouncedClickListener {
            activity.onBackPressedDispatcher.onBackPressed()
        }

        if (isMorph) {
            activity.postponeEnterTransition()
            binding.btnBack.prepareSlideFadeIn()
        } else {
            isTransitionFinished = true
        }

        if (isMorph && activity.window.sharedElementEnterTransition != null) {
            activity.window.sharedElementEnterTransition.addListener(object : Transition.TransitionListener {
                override fun onTransitionEnd(transition: Transition) {
                    transition.removeListener(this)
                    binding.btnBack.startSlideFadeIn(delay = 0L)
                    isTransitionFinished = true
                    if (isTransitionFinished) {
                        onSubmitList()
                    }
                }
                override fun onTransitionCancel(transition: Transition) {
                    transition.removeListener(this)
                    isTransitionFinished = true
                }
                override fun onTransitionStart(transition: Transition) {}
                override fun onTransitionPause(transition: Transition) {}
                override fun onTransitionResume(transition: Transition) {}
            })
        } else {
            isTransitionFinished = true
        }
    }

    private fun setupRecyclerViewsAndZoom() {
        binding.rvThumbnails.adapter = photoEditorAdapter
        binding.rvThumbnails.itemAnimator = null
        
        binding.rvThumbnails.isVisible = true
        binding.scrollColors.isVisible = true

        // Configure zoom limits
        binding.imgSelectedPhoto.minimumScale = 1.0f
        binding.imgSelectedPhoto.maximumScale = 5.0f
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListeners() {
        val isColorMode = hairType == PhotoEditorActivity.TYPE_HAIR_COLOR

        val stickerTouchListener = helper.setupZoomAndPanRouting(isColorMode) {
            currentState
        }
        
        binding.imgHairOverlay.setOnTouchListener { view, event ->
            val state = currentState
            val hasColor = state?.selectedColorId != null && state.selectedColorId != "none"
            
            if (hairType != PhotoEditorActivity.TYPE_HAIR_COLOR || paintMode == PaintMode.NONE || drawingManager.originalBitmap == null || drawingManager.freehandMaskBitmap == null) {
                // Delegate to StickerTouchListener for zoom/pan
                return@setOnTouchListener stickerTouchListener.onTouch(view, event)
            }
            
            if (!hasColor) {
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    if (!hasShownColorWarningToast) {
                        Toast.makeText(activity, activity.getString(R.string.msg_error_select_color_first), Toast.LENGTH_SHORT).show()
                        hasShownColorWarningToast = true
                    }
                }
                return@setOnTouchListener stickerTouchListener.onTouch(view, event)
            }
            
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                isMultiTouchGesture = false
            }
            if (event.pointerCount > 1) {
                isMultiTouchGesture = true
            }

            if (isMultiTouchGesture) {
                if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN && event.pointerCount == 2) {
                    val colorMatrix = state?.let { helper.getPhotoHairColorMatrix(helper.getColorIntFromState(it)) }
                    drawingManager.recompositeImage(colorMatrix, paintMode)
                    
                    val downEvent = MotionEvent.obtain(event.downTime, event.eventTime, MotionEvent.ACTION_DOWN, event.getX(0), event.getY(0), event.metaState)
                    stickerTouchListener.onTouch(view, downEvent)
                    downEvent.recycle()
                }
                return@setOnTouchListener stickerTouchListener.onTouch(view, event)
            }
            
            drawingManager.onTouchEvent(event, paintMode) {
                val colorMatrix = state?.let { helper.getPhotoHairColorMatrix(helper.getColorIntFromState(it)) }
                drawingManager.recompositeImage(colorMatrix, paintMode)
            }
            true
        }
    }
    


    private fun setupActionButtons() {
        binding.layoutAddPhoto.setDebouncedClickListener {
            if (currentState?.selectedPhotoUri == null) {
                onPickMedia()
            }
        }

        binding.btnRemovePhoto.setDebouncedClickListener {
            onAction(PhotoEditorAction.RemovePhoto)
        }

        binding.btnBrush.setDebouncedClickListener {
            paintMode = if (paintMode == PaintMode.BRUSH) PaintMode.NONE else PaintMode.BRUSH
            updateToolUI()
        }
        
        binding.btnEraser.setDebouncedClickListener {
            paintMode = if (paintMode == PaintMode.ERASER) PaintMode.NONE else PaintMode.ERASER
            updateToolUI()
        }
        
        updateToolUI()
        
        binding.btnColorNone.setDebouncedClickListener {
            drawingManager.clearColor()
            val state = currentState
            val colorMatrix = state?.let { helper.getPhotoHairColorMatrix(helper.getColorIntFromState(it)) }
            drawingManager.recompositeImage(colorMatrix, paintMode)
            onAction(PhotoEditorAction.SelectColor("none"))
        }

        binding.btnContinue.setDebouncedClickListener {
            onProcessAndContinue()
        }
    }

    private fun setupColorClicks() {
        val colors = listOf(
            "none" to binding.btnColorNone,
            "c_white" to binding.colorWhite,
            "c_black" to binding.colorBlack,
            "c_red" to binding.colorRed,
            "c_pink" to binding.colorPink,
            "c_blue" to binding.colorBlue,
            "c_green" to binding.colorGreen,
            "c_orange" to binding.colorOrange,
            "c_yellow" to binding.colorYellow,
            "c_purple" to binding.colorPurple,
            "custom" to binding.colorCustom
        )

        colors.forEach { (colorId, view) ->
            view.setDebouncedClickListener {
                if (colorId != "custom") {
                    onAction(PhotoEditorAction.SelectColor(colorId))
                } else {
                    val hex = currentState?.customColorHex ?: "#7384F9"
                    onAction(PhotoEditorAction.SelectColor("custom", hex))
                }
            }
        }

        binding.btnColorPicker.setDebouncedClickListener {
            ColorPickerDialog(activity, currentState?.customColorHex) { hex ->
                onAction(PhotoEditorAction.SelectColor("custom", hex))
            }.show()
        }
    }

    fun bind(state: PhotoEditorUiState.Success) {
        currentState = state
        
        if (lastPhotoUri != state.selectedPhotoUri) {
            handleNewPhotoSelection(state.selectedPhotoUri)
        }
        
        updatePhotoArea(state)
        updateContinueButton(state)
        handleFirstLoadAndThumbnails()
        updateColors(state)
    }

    private fun handleNewPhotoSelection(selectedPhotoUri: Uri?) {
        lastPhotoUri = selectedPhotoUri
        helper.isBgMatrixInitialized = false
        // Reset hair overlay position for the new photo
        binding.imgHairOverlay.scaleType = ImageView.ScaleType.FIT_CENTER
        
        drawingManager.clearBitmaps()
        
        if (selectedPhotoUri != null) {
            binding.imgSelectedPhoto.isVisible = true
            Glide.with(activity)
                .asBitmap()
                .load(selectedPhotoUri)
                .override(800, 800)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                    ) {
                        drawingManager.initBitmaps(resource)
                        binding.imgSelectedPhoto.setImageBitmap(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        drawingManager.clearBitmaps()
                    }
                })
        }
    }

    private fun updatePhotoArea(state: PhotoEditorUiState.Success) {
        if (state.selectedPhotoUri != null) {
            binding.layoutAddPhotoPlaceholder.isVisible = false
            binding.layoutFilledPhoto.isVisible = true
            binding.btnRemovePhoto.isVisible = true
            
            updateHairOverlay(state)
            
            binding.layoutTools.isVisible = hairType == PhotoEditorActivity.TYPE_HAIR_COLOR
        } else {
            binding.layoutAddPhotoPlaceholder.isVisible = true
            binding.layoutFilledPhoto.isVisible = false
            binding.btnRemovePhoto.isVisible = false
            binding.layoutTools.isVisible = false
            binding.imgHairOverlay.isVisible = false
            lastStyleResId = null
        }
    }

    private fun updateHairOverlay(state: PhotoEditorUiState.Success) {
        if (state.currentSelectedStyleResId != null) {
            binding.imgHairOverlay.isVisible = true
            if (lastStyleResId != state.currentSelectedStyleResId) {
                val isFirstLoadStyle = lastStyleResId == null
                val oldDrawable = binding.imgHairOverlay.drawable
                val oldMatrix = Matrix(binding.imgHairOverlay.imageMatrix)
                
                lastStyleResId = state.currentSelectedStyleResId
                Glide.with(activity)
                    .load(state.currentSelectedStyleResId)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: com.bumptech.glide.request.target.Target<Drawable>, isFirstResource: Boolean): Boolean = false
                        override fun onResourceReady(resource: Drawable, model: Any, target: com.bumptech.glide.request.target.Target<Drawable>?, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                            binding.imgHairOverlay.post {
                                val viewWidth = binding.imgHairOverlay.width.toFloat()
                                val viewHeight = binding.imgHairOverlay.height.toFloat()
                                
                                val newBaseMatrix = Matrix()
                                newBaseMatrix.setRectToRect(
                                    RectF(
                                        0f,
                                        0f,
                                        resource.intrinsicWidth.toFloat(),
                                        resource.intrinsicHeight.toFloat()
                                    ),
                                    RectF(0f, 0f, viewWidth, viewHeight),
                                    Matrix.ScaleToFit.CENTER
                                )

                                val finalMatrix = Matrix()

                                if (isFirstLoadStyle || oldDrawable == null) {
                                    finalMatrix.set(newBaseMatrix)
                                    finalMatrix.postScale(0.5f, 0.5f, viewWidth / 2f, viewHeight / 2f)
                                } else {
                                    val oldBaseMatrix = Matrix()
                                    oldBaseMatrix.setRectToRect(
                                        RectF(0f, 0f, oldDrawable.intrinsicWidth.toFloat(), oldDrawable.intrinsicHeight.toFloat()),
                                        RectF(0f, 0f, viewWidth, viewHeight),
                                        Matrix.ScaleToFit.CENTER
                                    )
                                    
                                    val oldBaseInverse = Matrix()
                                    oldBaseMatrix.invert(oldBaseInverse)
                                    
                                    val userMatrix = Matrix(oldMatrix)
                                    userMatrix.preConcat(oldBaseInverse)
                                    
                                    finalMatrix.set(newBaseMatrix)
                                    finalMatrix.postConcat(userMatrix)
                                }
                                
                                binding.imgHairOverlay.scaleType = ImageView.ScaleType.MATRIX
                                binding.imgHairOverlay.imageMatrix = finalMatrix
                            }
                            return false
                        }
                    })
                    .into(binding.imgHairOverlay)
            }
            
            val colorInt = helper.getColorIntFromState(state)
            val matrix = helper.getHairColorMatrix(colorInt)
            if (matrix != null) {
                binding.imgHairOverlay.colorFilter = ColorMatrixColorFilter(matrix)
            } else {
                binding.imgHairOverlay.clearColorFilter()
            }
            binding.imgHairOverlay.invalidate()
        } else {
            lastStyleResId = null
            val isColorMode = hairType == PhotoEditorActivity.TYPE_HAIR_COLOR
            binding.imgHairOverlay.isVisible = isColorMode // Keep visible for touch intercept in color mode
            binding.imgHairOverlay.setImageDrawable(null)
            binding.imgHairOverlay.clearColorFilter()
        }
    }

    private fun updateContinueButton(state: PhotoEditorUiState.Success) {
        val isColorMode = hairType == PhotoEditorActivity.TYPE_HAIR_COLOR
        val canContinue = state.selectedPhotoUri != null && (isColorMode || state.currentSelectedStyleId != null)
        binding.btnContinue.isEnabled = canContinue
    }

    private fun handleFirstLoadAndThumbnails() {
        if (isFirstLoad) {
            isFirstLoad = false
            if (isMorph) {
                binding.root.doOnPreDraw {
                    activity.startPostponedEnterTransition()
                }
            } else {
                if (isTransitionFinished) {
                    onSubmitList()
                }
            }
        } else {
            if (isTransitionFinished) {
                onSubmitList()
            }
        }
    }

    private fun updateColors(state: PhotoEditorUiState.Success) {
        helper.updateColorSelection(state.selectedColorId, state.customColorHex)
        
        if (lastColorId != state.selectedColorId || lastCustomHex != state.customColorHex) {
            lastColorId = state.selectedColorId
            lastCustomHex = state.customColorHex
            if (hairType == PhotoEditorActivity.TYPE_HAIR_COLOR) {
                if (paintMode == PaintMode.NONE && state.selectedColorId != "none") {
                    paintMode = PaintMode.BRUSH
                }
                updateToolUI()
                val colorMatrix = helper.getPhotoHairColorMatrix(helper.getColorIntFromState(state))
                drawingManager.recompositeImage(colorMatrix, paintMode)
            }
        }
    }

    private fun updateToolUI() {
        val state = currentState
        val selectedColorInt = state?.let { helper.getColorIntFromState(it) } ?: Color.WHITE

        when (paintMode) {
            PaintMode.BRUSH -> {
                binding.btnBrush.setColorFilter(selectedColorInt)
                binding.btnEraser.setColorFilter(Color.WHITE)
            }
            PaintMode.ERASER -> {
                binding.btnBrush.setColorFilter(Color.WHITE)
                binding.btnEraser.setColorFilter(selectedColorInt)
            }
            else -> {
                binding.btnBrush.setColorFilter(Color.WHITE)
                binding.btnEraser.setColorFilter(Color.WHITE)
            }
        }
    }
}
