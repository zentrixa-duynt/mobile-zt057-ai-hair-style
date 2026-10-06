package com.example.aihair.feature.face_result

import dev.zentrixa.common.admob.ZTInterstitialAdUtils
import dev.zentrixa.common.firebase.ZTAnalyticsUtils

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
import com.example.aihair.core.data.local.datastore.AppPreferences

import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class FaceResultActivity : BaseActivity<ActivityFaceResultBinding>(ActivityFaceResultBinding::inflate) {

    private val viewModel: FaceResultViewModel by viewModels()

    @Inject
    lateinit var appPreferences: AppPreferences

    private var isUnlocked = false

    private val adapter by lazy {
        createHairstyleSuitAdapter { item ->
            lifecycleScope.launch {
                val usage = appPreferences.usageState.first()
                val currentDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                val todayUsage = if (usage.lastUsageDate == currentDate) usage.usageCount else 0
                val dailyLimit = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance().getLong("limit_daily").toInt().takeIf { it > 0 } ?: 5
                
                if (todayUsage >= dailyLimit) {
                    Toast.makeText(this@FaceResultActivity, getString(R.string.msg_daily_limit_reached), Toast.LENGTH_SHORT).show()
                    return@launch
                }

                var isAdDone = false
                dev.zentrixa.common.admob.ZTRewardedAdUtils.loadAndShowRewardAd(
                    this@FaceResultActivity,
                    "reward_function_AI",
                    "p_reward_apply",
                    onAdEarned = {
                        isAdDone = true
                    },
                    onAdClosed = { isEarned ->
                        if (isEarned || isAdDone) {
                            lifecycleScope.launch {
                                val imageUri = intent.getStringExtra("EXTRA_SELECTED_IMAGE_URI") ?: intent.getStringExtra("IMAGE_URI")
                                val gender = intent.getStringExtra("EXTRA_SELECTED_GENDER") ?: "FEMALE"
                                val nextIntent = Intent(this@FaceResultActivity, HairResultActivity::class.java).apply {
                                    putExtra("EXTRA_IMAGE_URI", imageUri)
                                    putExtra("EXTRA_STYLE_NAME", item.name)
                                    putExtra("EXTRA_IS_FEMALE", gender.equals("FEMALE", ignoreCase = true))
                                    putExtra("EXTRA_IS_COLOR_MODE", false)
                                }
                                startActivity(nextIntent)
                            }
                        }
                    }
                )
            }
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
            ZTInterstitialAdUtils.loadAndShowInterstitialAd(this, "inter_back", "p_inter_back") {
                viewModel.onAction(FaceResultAction.BackClicked)
            }
        }

        binding.rvHairstyles.adapter = adapter

        val rewardConfig = dev.zentrixa.common.admob.ZTRewardedAdUtils.getRewardAdsConfig("reward_unlock_infor")
        val isPlacementEnabled = rewardConfig?.placements?.get("p_reward_unlock_face") ?: true
        val isAdEnabledByConfig = rewardConfig?.enabled != false && isPlacementEnabled

        if (!isAdEnabledByConfig || dev.zentrixa.common.utils.ZTUtils.isTurnOffAllAds) {
            unlockContent()
        } else {
            binding.layoutGoldenRatio.blurView.visibility = View.VISIBLE
            binding.layoutGoldenRatio.txtTitleGoldenRatio.visibility = View.INVISIBLE
            binding.layoutGoldenRatio.blurView.setupWith(binding.layoutGoldenRatio.blurTarget).setBlurRadius(8f)

            binding.layoutAttributes.blurViewChin.visibility = View.VISIBLE
            binding.layoutAttributes.blurViewChin.setupWith(binding.layoutAttributes.blurTargetChin).setBlurRadius(8f)
            binding.layoutAttributes.blurViewCheekbone.visibility = View.VISIBLE
            binding.layoutAttributes.blurViewCheekbone.setupWith(binding.layoutAttributes.blurTargetCheekbone).setBlurRadius(8f)
            binding.layoutAttributes.blurViewTemple.visibility = View.VISIBLE
            binding.layoutAttributes.blurViewTemple.setupWith(binding.layoutAttributes.blurTargetTemple).setBlurRadius(8f)
            binding.layoutAttributes.blurViewAppleCheeks.visibility = View.VISIBLE
            binding.layoutAttributes.blurViewAppleCheeks.setupWith(binding.layoutAttributes.blurTargetAppleCheeks).setBlurRadius(8f)
            
            binding.layoutBottom.visibility = View.VISIBLE

            binding.btnWatchVideo.setDebouncedClickListener {
                var isAdDone = false
                dev.zentrixa.common.admob.ZTRewardedAdUtils.loadAndShowRewardAd(
                    this@FaceResultActivity,
                    "reward_unlock_infor",
                    "p_reward_unlock_face",
                    onAdEarned = {
                        isAdDone = true
                    },
                    onAdClosed = { isEarned ->
                        if (isEarned || isAdDone) {
                            unlockContent()
                        }
                    }
                )
            }
        }
    }

    private fun unlockContent() {
        isUnlocked = true
        binding.layoutGoldenRatio.blurView.visibility = View.GONE
        binding.layoutGoldenRatio.txtTitleGoldenRatio.visibility = View.VISIBLE
        
        binding.layoutAttributes.blurViewChin.visibility = View.GONE
        binding.layoutAttributes.blurViewCheekbone.visibility = View.GONE
        binding.layoutAttributes.blurViewTemple.visibility = View.GONE
        binding.layoutAttributes.blurViewAppleCheeks.visibility = View.GONE
        
        binding.layoutBottom.visibility = View.GONE
        
        // Hide tag blur views if they are already populated
        for (i in 0 until binding.layoutTags.cgTags.childCount) {
            val child = binding.layoutTags.cgTags.getChildAt(i)
            val blurView = child.findViewById<View>(R.id.blur_view_tag)
            blurView?.visibility = View.GONE
        }
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
            is FaceResultUiState.Success -> {
                ZTAnalyticsUtils.logEvent("S_screen_analyze_face")

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
        binding.layoutGrid.txtTitleHairStyle.isSelected = true
        binding.layoutGrid.txtHairColor.text = data.hairColor
        binding.layoutGrid.txtHairColor.isSelected = true
        binding.layoutGrid.txtTitleHairColor.isSelected = true
        binding.layoutGrid.txtFaceShape.text = data.faceShape
        binding.layoutGrid.txtFaceShape.isSelected = true
        binding.layoutGrid.txtTitleFaceShape.isSelected = true
        binding.layoutGrid.txtFitRate.text = "${data.hairstyleFitRate}%"
        binding.layoutGrid.txtFitRate.isSelected = true
        binding.layoutGrid.txtTitleFitRate.isSelected = true

        binding.layoutGoldenRatio.txtGoldenScore.text = data.goldenRatio.toString()
        binding.layoutGoldenRatio.progressGoldenRatio.progress = (data.goldenRatio * 10).toInt()
        binding.layoutGoldenRatio.txtGoldenDesc.text = data.goldenRatioShortDescription

        binding.layoutAttributes.txtChin.text = data.chin
        binding.layoutAttributes.txtCheekbone.text = data.cheekbone
        binding.layoutAttributes.txtTemple.text = data.temple
        binding.layoutAttributes.txtAppleCheeks.text = data.appleCheeks

        binding.layoutTags.cgTags.removeAllViews()
        data.goldenRatioKeywords.forEach { keyword ->
            val view = layoutInflater.inflate(R.layout.item_result_tag, binding.layoutTags.cgTags, false)
            val tagView = view.findViewById<TextView>(R.id.txt_tag)
            tagView.text = keyword
            
            val blurView = view.findViewById<eightbitlab.com.blurview.BlurView>(R.id.blur_view_tag)
            if (isUnlocked) {
                blurView?.visibility = View.GONE
            } else {
                val blurTarget = view.findViewById<eightbitlab.com.blurview.BlurTarget>(R.id.blur_target_tag)
                if (blurView != null && blurTarget != null) {
                    blurView.setupWith(blurTarget).setBlurRadius(8f)
                }
            }
            
            binding.layoutTags.cgTags.addView(view)
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