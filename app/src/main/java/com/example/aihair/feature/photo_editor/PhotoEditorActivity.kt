package com.example.aihair.feature.photo_editor

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.aihair.R
import com.example.aihair.core.ui.base.BaseActivity
import com.example.aihair.core.ui.lifecycle.collectFlow
import com.example.aihair.core.utils.isImageVertical
import com.example.aihair.core.ui.transition.setupMorphTransition
import com.example.aihair.databinding.ActivityPhotoEditorBinding
import com.example.aihair.feature.hair_result.HairResultActivity
import com.example.aihair.feature.photo_editor.binder.PhotoEditorViewBinder
import com.example.aihair.feature.photo_editor.helper.DrawingManager
import com.example.aihair.feature.photo_editor.helper.ImageProcessor
import com.example.aihair.feature.photo_editor.helper.PhotoEditorHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class PhotoEditorActivity : BaseActivity<ActivityPhotoEditorBinding>(ActivityPhotoEditorBinding::inflate) {

    companion object {
        const val EXTRA_HAIR_TYPE = "EXTRA_HAIR_TYPE"
        const val TYPE_HAIR_STYLE = 0
        const val TYPE_HAIR_COLOR = 1
    }

    private val viewModel: PhotoEditorViewModel by viewModels()

    private val photoEditorAdapter by lazy {
        createPhotoEditorAdapter(
            onStyleClick = { styleId ->
                val hairType = intent.getIntExtra(EXTRA_HAIR_TYPE, TYPE_HAIR_STYLE)
                if (hairType == TYPE_HAIR_COLOR) {
                    viewModel.onAction(PhotoEditorAction.SelectColor(styleId))
                } else {
                    viewModel.onAction(PhotoEditorAction.SelectStyle(styleId))
                }
            }
        )
    }

    private fun submitListToAdapter() {
        val hairType = intent.getIntExtra(EXTRA_HAIR_TYPE, TYPE_HAIR_STYLE)
        if (hairType == TYPE_HAIR_COLOR) {
            photoEditorAdapter.submitList(viewModel.getColorStyles())
        } else {
            photoEditorAdapter.submitList(viewModel.getMixedStyles())
        }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            lifecycleScope.launch {
                val isVertical = checkImageIsVertical(it)
                viewModel.onAction(PhotoEditorAction.PhotoSelected(it, isVertical))
            }
        }
    }

    private lateinit var helper: PhotoEditorHelper
    private val imageProcessor by lazy { ImageProcessor(this) }
    private val drawingManager by lazy { DrawingManager(binding.imgSelectedPhoto) }
    
    private lateinit var viewBinder: PhotoEditorViewBinder

    private suspend fun checkImageIsVertical(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        isImageVertical(uri.toString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setupMorphTransition(R.id.main)
        super.onCreate(savedInstanceState)
        
        helper = PhotoEditorHelper(binding)
        
        viewBinder = PhotoEditorViewBinder(
            activity = this,
            binding = binding,
            helper = helper,
            drawingManager = drawingManager,
            photoEditorAdapter = photoEditorAdapter,
            hairType = intent.getIntExtra(EXTRA_HAIR_TYPE, TYPE_HAIR_STYLE),
            isMorph = intent.getBooleanExtra("EXTRA_IS_MORPH", false),
            onAction = { action -> viewModel.onAction(action) },
            onPickMedia = { pickMedia.launch("image/*") },
            onProcessAndContinue = { processAndContinue() },
            onSubmitList = { submitListToAdapter() }
        )
        
        viewBinder.setupUI()
        collectViewModel()
    }

    private fun processAndContinue() {
        val hairType = intent.getIntExtra(EXTRA_HAIR_TYPE, TYPE_HAIR_STYLE)
        if (hairType == TYPE_HAIR_COLOR) {
            processColorModeAndContinue()
        } else {
            processStyleModeAndContinue()
        }
    }

    private fun processColorModeAndContinue() {
        val state = viewModel.state.value as? PhotoEditorUiState.Success ?: return
        val base = drawingManager.originalBitmap
        val selectedUri = state.selectedPhotoUri
        if (base == null || selectedUri == null) {
            viewModel.onAction(PhotoEditorAction.ContinueClicked(state.selectedPhotoUri?.toString() ?: ""))
            return
        }
        
        showLoading()
        // Grab UI parameters on Main Thread
        val maskComp = drawingManager.maskCompositeBitmap
        val colorMatrix = helper.getHairColorMatrix(helper.getColorIntFromState(state))
        val currentFreehandMaskBitmap = drawingManager.freehandMaskBitmap
        
        lifecycleScope.launch {
            val resultUri = imageProcessor.processColorMode(
                selectedPhotoUri = selectedUri,
                maskCompositeBitmap = maskComp,
                freehandMaskBitmap = currentFreehandMaskBitmap,
                colorMatrix = colorMatrix
            )
            hideLoading()
            handleProcessorResult(resultUri)
        }
    }

    private fun processStyleModeAndContinue() {
        val state = viewModel.state.value as? PhotoEditorUiState.Success ?: return
        val bgImageView = binding.imgSelectedPhoto
        val bgDrawable = bgImageView.drawable as? BitmapDrawable
        val hairDrawable = binding.imgHairOverlay.drawable as? BitmapDrawable
        val selectedUri = state.selectedPhotoUri
        
        if (bgDrawable == null || hairDrawable == null || selectedUri == null) {
            viewModel.onAction(PhotoEditorAction.ContinueClicked(state.selectedPhotoUri?.toString() ?: ""))
            return
        }

        // Grab UI parameters on Main Thread
        val bgBitmap = bgDrawable.bitmap
        val hairBitmap = hairDrawable.bitmap
        val bgMatrix = Matrix()
        bgImageView.getDisplayMatrix(bgMatrix)
        val hairMatrix = Matrix(binding.imgHairOverlay.imageMatrix)
        val hairColorFilter = binding.imgHairOverlay.colorFilter
        
        showLoading()

        lifecycleScope.launch {
            val resultUri = imageProcessor.processStyleMode(
                selectedPhotoUri = selectedUri,
                bgBitmap = bgBitmap,
                hairBitmap = hairBitmap,
                bgMatrix = bgMatrix,
                hairMatrix = hairMatrix,
                hairColorFilter = hairColorFilter
            )
            hideLoading()
            handleProcessorResult(resultUri)
        }
    }

    private fun handleProcessorResult(resultUri: String?) {
        if (resultUri != null) {
            viewModel.onAction(PhotoEditorAction.ContinueClicked(resultUri))
        } else {
            Toast.makeText(this@PhotoEditorActivity, getString(R.string.msg_error_processing_image), Toast.LENGTH_SHORT).show()
        }
    }

    private fun collectViewModel() {
        collectFlow(viewModel.state) { state ->
            when (state) {
                is PhotoEditorUiState.Loading -> {
                    showLoading()
                }
                is PhotoEditorUiState.Error -> {
                    hideLoading()
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                is PhotoEditorUiState.Success -> {
                    hideLoading()
                    viewBinder.bind(state)
                }
            }
        }

        collectFlow(viewModel.event) { event ->
            handleEvent(event)
        }
    }

    private fun handleEvent(event: PhotoEditorEvent) {
        when (event) {
            is PhotoEditorEvent.ShowError -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }
            is PhotoEditorEvent.NavigateToResult -> {
                val intent = Intent(this, HairResultActivity::class.java).apply {
                    putExtra("EXTRA_IMAGE_URI", event.imageUri)
                    putExtra("EXTRA_HISTORY_TYPE", 1) // 1 = Hair Tools
                    putExtra("EXTRA_ORIGINAL_IMAGE_URI", event.originalImageUri)
                    putExtra("EXTRA_IS_VERTICAL", event.isVertical)
                    putExtra("EXTRA_STYLE_ID", event.styleId)
                    val isColorMode = this@PhotoEditorActivity.intent.getIntExtra(EXTRA_HAIR_TYPE, TYPE_HAIR_STYLE) == TYPE_HAIR_COLOR
                    putExtra("EXTRA_IS_COLOR_MODE", isColorMode)
                    putExtra("EXTRA_COLOR_HEX", event.colorHex)
                }
                startActivity(intent)
            }
        }
    }
}