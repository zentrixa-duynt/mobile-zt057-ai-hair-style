package com.example.aihair.feature.makeup_result

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.aihair.core.utils.parseColorSafe
import androidx.activity.viewModels
import androidx.cardview.widget.CardView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.example.aihair.core.ui.lifecycle.collectFlow
import com.bumptech.glide.Glide
import com.example.aihair.R
import com.example.aihair.core.ui.base.BaseActivity
import com.example.aihair.core.ui.click.setDebouncedClickListener
import com.example.aihair.core.utils.getImageRatio
import com.example.aihair.core.utils.setDimensionRatio
import com.example.aihair.core.utils.parseColorSafe
import com.example.aihair.databinding.ActivityMakeupResultBinding
import com.example.aihair.core.data.remote.dto.analysis.MakeupAnalysisData
import android.widget.LinearLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.core.net.toUri

@AndroidEntryPoint
class MakeupResultActivity : BaseActivity<ActivityMakeupResultBinding>(ActivityMakeupResultBinding::inflate) {

    private val viewModel: MakeupResultViewModel by viewModels()

    private val adapter by lazy {
        createMakeupRecommendationAdapter()
    }

    private var currentData: MakeupAnalysisData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViews()
        observeViewModel()

        val imageUri = intent.getStringExtra("EXTRA_SELECTED_IMAGE_URI") ?: intent.getStringExtra("IMAGE_URI")
        
        getImageRatio(imageUri)?.let { ratio ->
            binding.layoutProcessing.imgProcessingPhoto.setDimensionRatio(ratio)
        }
        
        if (!imageUri.isNullOrEmpty()) {
            Glide.with(this)
                .load(imageUri.toUri())
                .into(binding.layoutScore.imgMakeupProfile)
            Glide.with(this)
                .load(imageUri.toUri())
                .into(binding.layoutProcessing.imgProcessingPhoto)
            viewModel.onAction(MakeupResultAction.StartAnalysis(imageUri))
        } else {
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupViews() {
        binding.btnBack.setDebouncedClickListener {
            viewModel.onAction(MakeupResultAction.BackClicked)
        }

        binding.layoutRecommendation.rvRecommendations.adapter = adapter

        setupTabs()
    }

    private fun setupTabs() {
        val tabs = listOf(
            Triple(binding.layoutRecommendation.tabBrows, binding.layoutRecommendation.txtTabBrows, "brows"),
            Triple(binding.layoutRecommendation.tabEyes, binding.layoutRecommendation.txtTabEyes, "eyes"),
            Triple(binding.layoutRecommendation.tabNose, binding.layoutRecommendation.txtTabNose, "nose"),
            Triple(binding.layoutRecommendation.tabSkin, binding.layoutRecommendation.txtTabSkin, "skin"),
            Triple(binding.layoutRecommendation.tabBlush, binding.layoutRecommendation.txtTabBlush, "blush"),
            Triple(binding.layoutRecommendation.tabLip, binding.layoutRecommendation.txtTabLip, "lips")
        )
        
        val indicators = mapOf(
            "brows" to binding.layoutRecommendation.indicatorBrows,
            "eyes" to binding.layoutRecommendation.indicatorEyes,
            "nose" to binding.layoutRecommendation.indicatorNose,
            "skin" to binding.layoutRecommendation.indicatorSkin,
            "blush" to binding.layoutRecommendation.indicatorBlush,
            "lips" to binding.layoutRecommendation.indicatorLip
        )

        tabs.forEach { (tabView, textView, key) ->
            textView.isSelected = true // Enable marquee effect
            tabView.setDebouncedClickListener {
                // Hide all indicators and reset text color
                tabs.forEach { (_, tv, k) ->
                    indicators[k]?.visibility = View.INVISIBLE
                    tv.setTextColor(Color.parseColor("#6A7282"))
                }
                
                // Show current indicator and set selected text color
                indicators[key]?.visibility = View.VISIBLE
                textView.setTextColor(Color.parseColor("#FFFFFF"))
                
                // Update adapter data
                currentData?.let { data ->
                    val recs = when (key) {
                        "brows" -> data.recommendations.brows
                        "eyes" -> data.recommendations.eyes
                        "nose" -> data.recommendations.nose
                        "skin" -> data.recommendations.skin
                        "blush" -> data.recommendations.blush
                        "lips" -> data.recommendations.lips
                        else -> emptyList()
                    }
                    adapter.submitList(recs)
                }
            }
        }
    }

    private fun observeViewModel() {
        collectFlow(viewModel.state) { state ->
            handleState(state)
        }
        collectFlow(viewModel.event) { event ->
            handleEvent(event)
        }
    }

    private fun handleState(state: MakeupResultUiState) {
        when (state) {
            is MakeupResultUiState.Idle -> {
                binding.layoutProcessing.root.visibility = View.GONE
                binding.layoutResult.visibility = View.GONE
                binding.layoutProcessing.imgSwipeAnalyst.clearAnimation()
                binding.layoutProcessing.imgSwipeAnalyst.visibility = View.INVISIBLE
            }
            is MakeupResultUiState.Loading -> {
                binding.layoutProcessing.root.visibility = View.VISIBLE
                binding.layoutResult.visibility = View.GONE
                binding.layoutProcessing.imgSwipeAnalyst.visibility = View.VISIBLE
                val scanAnim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.anim_scan_right_to_left)
                binding.layoutProcessing.imgSwipeAnalyst.startAnimation(scanAnim)
            }
            is MakeupResultUiState.Success -> {
                binding.layoutProcessing.root.visibility = View.GONE
                binding.layoutResult.visibility = View.VISIBLE
                binding.layoutProcessing.imgSwipeAnalyst.clearAnimation()
                binding.layoutProcessing.imgSwipeAnalyst.visibility = View.INVISIBLE
                currentData = state.data
                bindData(state.data)
            }
            is MakeupResultUiState.Error -> {
                binding.layoutProcessing.root.visibility = View.GONE
                binding.layoutResult.visibility = View.VISIBLE
                binding.layoutProcessing.imgSwipeAnalyst.clearAnimation()
                binding.layoutProcessing.imgSwipeAnalyst.visibility = View.INVISIBLE
            }
        }
    }

    private fun bindData(data: MakeupAnalysisData) {
        binding.layoutScore.txtOverallScoreTitle.isSelected = true
        binding.layoutScore.txtMakeupScore.text = "${data.overallMakeupScore}%"
        binding.layoutScore.progressMakeupScore.progress = data.overallMakeupScore

        binding.layoutBalance.txtFacialBalanceTitle.isSelected = true
        binding.layoutBalance.txtScoreBrow.text = data.balanceScores.brow.toString()
        binding.layoutBalance.progressBrow.progress = data.balanceScores.brow

        binding.layoutBalance.txtScoreEyes.text = data.balanceScores.eyes.toString()
        binding.layoutBalance.progressEyes.progress = data.balanceScores.eyes

        binding.layoutBalance.txtScoreSkin.text = data.balanceScores.skin.toString()
        binding.layoutBalance.progressSkin.progress = data.balanceScores.skin

        binding.layoutBalance.txtScoreCheeks.text = data.balanceScores.cheeks.toString()
        binding.layoutBalance.progressCheeks.progress = data.balanceScores.cheeks

        binding.layoutBalance.txtScoreLips.text = data.balanceScores.lips.toString()
        binding.layoutBalance.progressLips.progress = data.balanceScores.lips

        binding.layoutBalance.txtScoreHarmony.text = data.balanceScores.harmony.toString()
        binding.layoutBalance.progressHarmony.progress = data.balanceScores.harmony

        // Default tab is brows
        adapter.submitList(data.recommendations.brows)

        // Palette
        populatePalette(binding.layoutPalette.cgBestNeutrals, data.colorPalette.bestNeutrals)
        populatePalette(binding.layoutPalette.cgBestBlushLip, data.colorPalette.bestBlushLip)
        populatePalette(binding.layoutPalette.cgBestEyesShadow, data.colorPalette.bestEyesShadow)
        populatePalette(binding.layoutPalette.cgBestLinerMascara, data.colorPalette.bestLinerMascara)
    }

    private fun populatePalette(group: LinearLayout, hexColors: List<String>) {
        group.removeAllViews()
        hexColors.forEach { hex ->
            try {
                val card = layoutInflater.inflate(R.layout.item_palette_color, group, false) as CardView
                card.setCardBackgroundColor(hex.parseColorSafe(Color.TRANSPARENT))
                group.addView(card)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleEvent(event: MakeupResultEvent) {
        when (event) {
            is MakeupResultEvent.NavigateBack -> finish()
            is MakeupResultEvent.ShowToast -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}