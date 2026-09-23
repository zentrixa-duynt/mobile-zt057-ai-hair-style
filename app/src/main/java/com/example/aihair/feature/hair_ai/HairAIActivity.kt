package com.example.aihair.feature.hair_ai

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.transition.Transition
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.aihair.R
import com.example.aihair.core.ui.base.BaseActivity
import com.example.aihair.core.ui.click.setDebouncedClickListener
import com.example.aihair.core.ui.lifecycle.collectFlow
import com.example.aihair.core.ui.transition.prepareSlideFadeIn
import com.example.aihair.core.ui.transition.setupMorphTransition
import com.example.aihair.core.ui.transition.startSlideFadeIn
import com.example.aihair.core.utils.isImageVertical
import com.example.aihair.databinding.ActivityHairAiBinding
import com.example.aihair.databinding.ItemStyleBinding
import com.example.aihair.feature.hair_result.HairResultActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class HairAIActivity : BaseActivity<ActivityHairAiBinding>(ActivityHairAiBinding::inflate) {

    private val viewModel: HairAIViewModel by viewModels()

    private val hairAIAdapter by lazy {
        createHairAIAdapter { styleId ->
            viewModel.onAction(HairAIAction.SelectStyle(styleId))
        }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            lifecycleScope.launch {
                val isVertical = checkImageIsVertical(it)
                viewModel.onAction(HairAIAction.PhotoSelected(it, isVertical))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setupMorphTransition(R.id.main)
        super.onCreate(savedInstanceState)

        setupUI()
        collectViewModel()
    }

    private fun setupUI() {
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        binding.btnBack.setDebouncedClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val isMorph = intent.getBooleanExtra("EXTRA_IS_MORPH", false)
        if (isMorph) {
            postponeEnterTransition()
            binding.btnBack.prepareSlideFadeIn()
        } else {
            isTransitionFinished = true
        }

        val type = intent.getIntExtra(EXTRA_HAIR_TYPE, TYPE_HAIR_STYLE)
        binding.tvTitle.text = if (type == TYPE_HAIR_COLOR) {
            getString(R.string.text_ai_hair_color)
        } else {
            getString(R.string.text_ai_hair_style)
        }
        
        // Set visibility immediately to avoid flashing
        binding.tabBar.isVisible = (type == TYPE_HAIR_STYLE)
        
        binding.rvStyles.adapter = hairAIAdapter
        binding.rvStyles.itemAnimator = null

        binding.layoutEmptyPhoto.setDebouncedClickListener {
            pickMedia.launch("image/*")
        }

        binding.boxAddPhoto.setDebouncedClickListener {
            pickMedia.launch("image/*")
        }

        binding.btnClosePhoto.setDebouncedClickListener {
            viewModel.onAction(HairAIAction.RemovePhoto)
        }

        binding.btnCreate.setDebouncedClickListener {
            viewModel.onAction(HairAIAction.CreateClicked)
        }

        binding.tabBar.applyConfig(tabWidthDp = 64f, tabGapDp = 32f, textSizeSpVal = 14f, tabHeightDp = 28f)
        binding.tabBar.setTabTitles(listOf(getString(R.string.text_female), getString(R.string.text_male)))
        binding.tabBar.onTabSelected = { index ->
            viewModel.onAction(HairAIAction.SelectTab(isFemale = index == 0))
        }

        if (isMorph && window.sharedElementEnterTransition != null) {
            window.sharedElementEnterTransition.addListener(object : Transition.TransitionListener {
                override fun onTransitionEnd(transition: Transition) {
                    transition.removeListener(this)
                    binding.btnBack.startSlideFadeIn(delay = 0L)
                    isTransitionFinished = true
                    val state = viewModel.uiState.value as? HairAIUiState.Success
                    if (state != null) {
                        hairAIAdapter.submitList(state.styles)
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

        viewModel.onAction(HairAIAction.Init(isColorMode = type == TYPE_HAIR_COLOR))
    }

    private var isTransitionFinished = false
    private var hasStartedTransition = false

    private fun collectViewModel() {
        collectFlow(viewModel.uiState, action = ::renderState)
        collectFlow(viewModel.event, action = ::handleEvent)
    }

    private fun renderState(state: HairAIUiState) {
        when (state) {
            is HairAIUiState.Loading -> {
                showLoading()
            }
            is HairAIUiState.Success -> {
                hideLoading()
                
                // Tabs
                binding.tabBar.isVisible = !state.isColorMode
                if (!state.isColorMode) {
                    binding.tabBar.setSelectedIndex(if (state.isFemaleTabSelected) 0 else 1, animate = false)
                }

                // List
                if (!hasStartedTransition && intent.getBooleanExtra("EXTRA_IS_MORPH", false)) {
                    hasStartedTransition = true
                    binding.root.doOnPreDraw {
                        startPostponedEnterTransition()
                    }
                }
                
                if (isTransitionFinished) {
                    hairAIAdapter.submitList(state.styles)
                }

                // Photo
                if (state.selectedPhotoUri != null) {
                    binding.boxAddPhoto.isClickable = false
                    binding.layoutEmptyPhoto.isVisible = false
                    binding.layoutFilledPhoto.isVisible = true
                    
                    binding.imgSelectedPhoto.isVisible = true
                    Glide.with(this@HairAIActivity)
                        .load(state.selectedPhotoUri)
                        .override(800, 800)
                        .into(binding.imgSelectedPhoto)
                } else {
                    binding.boxAddPhoto.isClickable = true
                    binding.layoutEmptyPhoto.isVisible = true
                    binding.layoutFilledPhoto.isVisible = false
                }
                
                binding.btnCreate.isEnabled = state.selectedPhotoUri != null && state.selectedStyleId != null
            }
            is HairAIUiState.Error -> {
                hideLoading()
                Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleEvent(event: HairAIEvent) {
        when (event) {
            is HairAIEvent.ShowError -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }
            is HairAIEvent.NavigateToResult -> {
                val currentStyles = (viewModel.uiState.value as? HairAIUiState.Success)?.styles ?: emptyList()
                val selectedStyle = currentStyles.find { it.id == event.styleId }
                val styleName = selectedStyle?.name?.takeIf { it.isNotEmpty() }
                    ?: selectedStyle?.nameResId?.let { getString(it) }
                    ?: event.styleId

                val intent = Intent(this, HairResultActivity::class.java).apply {
                    putExtra("EXTRA_IMAGE_URI", event.imageUri)
                    putExtra("EXTRA_ORIGINAL_IMAGE_URI", event.imageUri)
                    putExtra("EXTRA_HISTORY_TYPE", 0) // 0 = Hair AI
                    putExtra("EXTRA_STYLE_NAME", styleName)
                    putExtra("EXTRA_IS_COLOR_MODE", event.isColorMode)
                    putExtra("EXTRA_IS_VERTICAL", event.isVertical)
                    putExtra("EXTRA_IS_FEMALE", event.isFemale)
                }
                startActivity(intent)
            }
        }
    }

    private suspend fun checkImageIsVertical(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        isImageVertical(uri.toString())
    }

    companion object {
        const val EXTRA_HAIR_TYPE = "EXTRA_HAIR_TYPE"
        const val TYPE_HAIR_STYLE = 0
        const val TYPE_HAIR_COLOR = 1
    }
}