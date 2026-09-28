package com.example.aihair.feature.face_result

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.aihair.R
import com.example.aihair.core.ui.base.BaseActivity
import com.example.aihair.core.ui.click.setDebouncedClickListener
import com.example.aihair.core.utils.getImageRatio
import com.example.aihair.core.utils.setDimensionRatio
import com.example.aihair.databinding.ActivityFaceResultBinding
import com.example.aihair.feature.hair_result.HairResultActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.core.net.toUri

@AndroidEntryPoint
class FaceResultActivity : BaseActivity<ActivityFaceResultBinding>(ActivityFaceResultBinding::inflate) {

    private val viewModel: FaceResultViewModel by viewModels()

    private val adapter by lazy {
        createHairstyleSuitAdapter { item ->
            val imageUri = intent.getStringExtra("EXTRA_SELECTED_IMAGE_URI") ?: intent.getStringExtra("IMAGE_URI")
            val gender = intent.getStringExtra("EXTRA_SELECTED_GENDER") ?: "FEMALE"
            val nextIntent = Intent(this, HairResultActivity::class.java).apply {
                putExtra("EXTRA_IMAGE_URI", imageUri)
                putExtra("EXTRA_STYLE_NAME", item.name)
                putExtra("EXTRA_IS_FEMALE", gender.equals("FEMALE", ignoreCase = true))
                putExtra("EXTRA_IS_COLOR_MODE", false)
            }
            startActivity(nextIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupViews()
        observeViewModel()

        // Extract imageUri from Intent
        val imageUri = intent.getStringExtra("EXTRA_SELECTED_IMAGE_URI") ?: intent.getStringExtra("IMAGE_URI")
        val gender = intent.getStringExtra("EXTRA_SELECTED_GENDER") ?: "FEMALE"

        getImageRatio(imageUri)?.let { ratio ->
            binding.layoutProcessing.imgProcessingPhoto.setDimensionRatio(ratio)

            val displayMetrics = resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels
            val screenWidth = displayMetrics.widthPixels
            val cardWidth = screenWidth - (60 * displayMetrics.density).toInt()

            val parts = ratio.split(":")
            if (parts.size == 2) {
                val w = parts[0].toFloatOrNull() ?: 1f
                val h = parts[1].toFloatOrNull() ?: 1f
                if (w > 0) {
                    val imgHeight = cardWidth * (h / w)
                    if (imgHeight > screenHeight * 0.6f) {
                        val maxHeight = (screenHeight * 0.6f).toInt()
                        binding.layoutProcessing.cardProcessingImage.layoutParams.height = maxHeight
                        binding.layoutProcessing.cardProcessingImage.requestLayout()
                    }
                }
            }
        }

        if (!imageUri.isNullOrEmpty()) {
            Glide.with(this)
                .load(imageUri.toUri())
                .into(binding.imgProfile)
            Glide.with(this)
                .load(imageUri.toUri())
                .into(binding.layoutProcessing.imgProcessingPhoto)
            viewModel.onAction(FaceResultAction.StartAnalysis(imageUri, gender))
        } else {
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupViews() {
        binding.btnBack.setDebouncedClickListener {
            viewModel.onAction(FaceResultAction.BackClicked)
        }

        binding.rvHairstyles.adapter = adapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
                        handleState(state)
                    }
                }
                launch {
                    viewModel.event.collect { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }

    private fun handleState(state: FaceResultUiState) {
        when (state) {
            is FaceResultUiState.Idle -> {
                binding.layoutProcessing.root.visibility = View.GONE
                binding.layoutResult.visibility = View.GONE
                binding.layoutProcessing.imgSwipeAnalyst.clearAnimation()
                binding.layoutProcessing.imgSwipeAnalyst.visibility = View.INVISIBLE
            }
            is FaceResultUiState.Loading -> {
                binding.layoutProcessing.root.visibility = View.VISIBLE
                binding.layoutResult.visibility = View.GONE
                binding.layoutProcessing.imgSwipeAnalyst.visibility = View.VISIBLE
                val scanAnim = AnimationUtils.loadAnimation(this, R.anim.anim_scan_right_to_left)
                binding.layoutProcessing.imgSwipeAnalyst.startAnimation(scanAnim)
            }
            is FaceResultUiState.Success -> {
                binding.layoutProcessing.root.visibility = View.GONE
                binding.layoutResult.visibility = View.VISIBLE
                binding.layoutProcessing.imgSwipeAnalyst.clearAnimation()
                binding.layoutProcessing.imgSwipeAnalyst.visibility = View.INVISIBLE
                bindData(state)
            }
            is FaceResultUiState.Error -> {
                binding.layoutProcessing.root.visibility = View.GONE
                binding.layoutResult.visibility = View.VISIBLE
                binding.layoutProcessing.imgSwipeAnalyst.clearAnimation()
                binding.layoutProcessing.imgSwipeAnalyst.visibility = View.INVISIBLE
                // Error handled by event (Toast)
            }
        }
    }

    private fun bindData(state: FaceResultUiState.Success) {
        val data = state.data
        binding.layoutGrid.txtHairStyle.text = data.hairStyle
        binding.layoutGrid.txtHairStyle.isSelected = true
        binding.layoutGrid.txtHairColor.text = data.hairColor
        binding.layoutGrid.txtHairColor.isSelected = true
        binding.layoutGrid.txtFaceShape.text = data.faceShape
        binding.layoutGrid.txtFaceShape.isSelected = true
        binding.layoutGrid.txtFitRate.text = "${data.hairstyleFitRate}%"
        binding.layoutGrid.txtFitRate.isSelected = true

        binding.layoutGoldenRatio.txtGoldenScore.text = data.goldenRatio.toString()
        binding.layoutGoldenRatio.progressGoldenRatio.progress = (data.goldenRatio * 10).toInt()
        binding.layoutGoldenRatio.txtGoldenDesc.text = data.goldenRatioShortDescription

        binding.layoutAttributes.txtChin.text = data.chin
        binding.layoutAttributes.txtCheekbone.text = data.cheekbone
        binding.layoutAttributes.txtTemple.text = data.temple
        binding.layoutAttributes.txtAppleCheeks.text = data.appleCheeks

        binding.layoutTags.cgTags.removeAllViews()
        data.goldenRatioKeywords.forEach { keyword ->
            val tagView = layoutInflater.inflate(R.layout.item_result_tag, binding.layoutTags.cgTags, false) as TextView
            tagView.text = keyword
            binding.layoutTags.cgTags.addView(tagView)
        }

        adapter.submitList(state.recommendedHairstyles)
    }

    private fun handleEvent(event: FaceResultEvent) {
        when (event) {
            is FaceResultEvent.NavigateBack -> finish()
            is FaceResultEvent.ShowToast -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}