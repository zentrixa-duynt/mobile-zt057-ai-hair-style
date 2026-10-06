package com.example.aihair.feature.makeup_result

import dev.zentrixa.common.admob.ZTInterstitialAdUtils
import dev.zentrixa.common.firebase.ZTAnalyticsUtils

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
            ZTInterstitialAdUtils.loadAndShowInterstitialAd(this, "inter_back", "p_inter_back") {
                viewModel.onAction(MakeupResultAction.BackClicked)
            }
        }

        binding.layoutProcessing.txtProcessingDesc.text = getString(R.string.text_analyzing_makeup_report)

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
        
        val fakeTabs = listOf(
            Triple(binding.layoutRecommendation.fakeTabBrows, binding.layoutRecommendation.fakeTxtTabBrows, "brows"),
            Triple(binding.layoutRecommendation.fakeTabEyes, binding.layoutRecommendation.fakeTxtTabEyes, "eyes"),
            Triple(binding.layoutRecommendation.fakeTabNose, binding.layoutRecommendation.fakeTxtTabNose, "nose"),
            Triple(binding.layoutRecommendation.fakeTabSkin, binding.layoutRecommendation.fakeTxtTabSkin, "skin"),
            Triple(binding.layoutRecommendation.fakeTabBlush, binding.layoutRecommendation.fakeTxtTabBlush, "blush"),
            Triple(binding.layoutRecommendation.fakeTabLip, binding.layoutRecommendation.fakeTxtTabLip, "lips")
        )
        
        val allTabs = tabs + fakeTabs
        
        val indicators = mapOf(
            "brows" to listOf(binding.layoutRecommendation.indicatorBrows, binding.layoutRecommendation.fakeIndicatorBrows),
            "eyes" to listOf(binding.layoutRecommendation.indicatorEyes, binding.layoutRecommendation.fakeIndicatorEyes),
            "nose" to listOf(binding.layoutRecommendation.indicatorNose, binding.layoutRecommendation.fakeIndicatorNose),
            "skin" to listOf(binding.layoutRecommendation.indicatorSkin, binding.layoutRecommendation.fakeIndicatorSkin),
            "blush" to listOf(binding.layoutRecommendation.indicatorBlush, binding.layoutRecommendation.fakeIndicatorBlush),
            "lips" to listOf(binding.layoutRecommendation.indicatorLip, binding.layoutRecommendation.fakeIndicatorLip)
        )

        allTabs.forEach { (tabView, textView, key) ->
            textView.isSelected = true // Enable marquee effect
            tabView.setDebouncedClickListener {
                // Hide all indicators and reset text color
                allTabs.forEach { (_, tv, k) ->
                    indicators[k]?.forEach { it.visibility = View.INVISIBLE }
                    tv.setTextColor(Color.parseColor("#6A7282"))
                }
                
                // Show current indicator and set selected text color
                indicators[key]?.forEach { it.visibility = View.VISIBLE }
                allTabs.filter { it.third == key }.forEach { it.second.setTextColor(Color.parseColor("#FFFFFF")) }
                
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

        val rewardConfig = dev.zentrixa.common.admob.ZTRewardedAdUtils.getRewardAdsConfig("reward_unlock_infor")
        val isPlacementEnabled = rewardConfig?.placements?.get("p_reward_unlock_make") ?: true
        val isAdEnabledByConfig = rewardConfig?.enabled != false && isPlacementEnabled

        if (!isAdEnabledByConfig || dev.zentrixa.common.utils.ZTUtils.isTurnOffAllAds) {
            binding.layoutPalette.blurView.visibility = View.GONE
            binding.layoutPalette.txtTitlePalette.visibility = View.VISIBLE
            binding.layoutBalance.blurViewBalance.visibility = View.GONE
            binding.layoutBalance.txtFacialBalanceTitle.visibility = View.VISIBLE
            binding.layoutRecommendation.blurViewRecommendations.visibility = View.GONE
            binding.layoutRecommendation.layoutTabsReal.visibility = View.VISIBLE
            binding.layoutBottom.visibility = View.GONE
        } else {
            binding.layoutPalette.blurView.visibility = View.VISIBLE
            binding.layoutPalette.txtTitlePalette.visibility = View.INVISIBLE
            binding.layoutPalette.blurView.setupWith(binding.layoutPalette.blurTarget).setBlurRadius(8f)
            
            binding.layoutBalance.blurViewBalance.visibility = View.VISIBLE
            binding.layoutBalance.txtFacialBalanceTitle.visibility = View.INVISIBLE

            binding.layoutRecommendation.blurViewRecommendations.visibility = View.VISIBLE
            binding.layoutRecommendation.layoutTabsReal.visibility = View.INVISIBLE
            binding.layoutRecommendation.blurViewRecommendations.setupWith(binding.layoutRecommendation.blurTargetRecommendations).setBlurRadius(8f)
            
            binding.layoutBottom.visibility = View.VISIBLE

            binding.btnWatchVideo.setDebouncedClickListener {
                var isAdDone = false
                dev.zentrixa.common.admob.ZTRewardedAdUtils.loadAndShowRewardAd(
                    this@MakeupResultActivity,
                    "reward_unlock_infor",
                    "p_reward_unlock_make",
                    onAdEarned = {
                        isAdDone = true
                    },
                    onAdClosed = { isEarned ->
                        if (isEarned || isAdDone) {
                            binding.layoutPalette.blurView.visibility = View.GONE
                            binding.layoutPalette.txtTitlePalette.visibility = View.VISIBLE
                            binding.layoutBalance.blurViewBalance.visibility = View.GONE
                            binding.layoutBalance.txtFacialBalanceTitle.visibility = View.VISIBLE
                            binding.layoutRecommendation.blurViewRecommendations.visibility = View.GONE
                            binding.layoutRecommendation.layoutTabsReal.visibility = View.VISIBLE
                            binding.layoutBottom.visibility = View.GONE
                        }
                    }
                )
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
                var isFlipped = false
                scanAnim.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(animation: android.view.animation.Animation?) {
                        binding.layoutProcessing.imgSwipeAnalyst.scaleX = 1f
                    }
                    override fun onAnimationEnd(animation: android.view.animation.Animation?) {}
                    override fun onAnimationRepeat(animation: android.view.animation.Animation?) {
                        isFlipped = !isFlipped
                        binding.layoutProcessing.imgSwipeAnalyst.scaleX = if (isFlipped) -1f else 1f
                    }
                })
                binding.layoutProcessing.imgSwipeAnalyst.startAnimation(scanAnim)
            }
            is MakeupResultUiState.Success -> {
                ZTAnalyticsUtils.logEvent("S_screen_analyze_make")

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