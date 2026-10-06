package com.example.aihair.feature.hair_result

import dev.zentrixa.common.admob.ZTInterstitialAdUtils
import dev.zentrixa.common.firebase.ZTAnalyticsUtils

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.aihair.core.utils.getImageRatio
import com.example.aihair.core.utils.setDimensionRatio
import com.bumptech.glide.Glide
import com.example.aihair.R
import com.example.aihair.core.ui.base.BaseActivity
import com.example.aihair.core.ui.click.setDebouncedClickListener
import com.example.aihair.core.ui.lifecycle.collectFlow
import com.example.aihair.core.utils.NetworkUtils
import com.example.aihair.databinding.ActivityHairResultBinding
import com.example.aihair.feature.hair_result.component.ReportDialog
import com.example.aihair.feature.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class HairResultActivity : BaseActivity<ActivityHairResultBinding>(ActivityHairResultBinding::inflate) {
    
    private val viewModel: HairResultViewModel by viewModels()

    private var croppedImageUriString: String? = null
    private var originalImageUriString: String? = null
    private var historyType: Int = 0
    
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val localImageUriString = intent.getStringExtra("EXTRA_IMAGE_URI")
        originalImageUriString = intent.getStringExtra("EXTRA_ORIGINAL_IMAGE_URI") ?: localImageUriString
        val historyType = intent.getIntExtra("EXTRA_HISTORY_TYPE", 0).also { historyType = it }
        val styleName = intent.getStringExtra("EXTRA_STYLE_NAME")
        val isColorMode = intent.getBooleanExtra("EXTRA_IS_COLOR_MODE", false)
        val isFemale = intent.getBooleanExtra("EXTRA_IS_FEMALE", true)
        
        // 1. Lấy kích thước thực của ảnh gốc/local và set tỉ lệ động cho khung
        getImageRatio(localImageUriString)?.let { ratio ->
            binding.imgResultPhoto.setDimensionRatio(ratio)
            binding.imgProcessingPhoto.setDimensionRatio(ratio)

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
                        binding.cardProcessingImage.layoutParams.height = maxHeight
                        binding.cardResultImage.layoutParams.height = maxHeight
                        binding.cardProcessingImage.requestLayout()
                        binding.cardResultImage.requestLayout()
                    }
                }
            }
        }
        
        // Preload original image to memory cache
        if (!originalImageUriString.isNullOrEmpty()) {
            Glide.with(this).load(Uri.parse(originalImageUriString)).preload()
        }

        val targetImageView = binding.imgResultPhoto

        val splitTouchListener = android.view.View.OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!originalImageUriString.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(Uri.parse(originalImageUriString))
                            .dontAnimate()
                            .placeholder(targetImageView.drawable)
                            .into(targetImageView)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!croppedImageUriString.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(croppedImageUriString)
                            .dontAnimate()
                            .placeholder(targetImageView.drawable)
                            .into(targetImageView)
                    }
                    true
                }
                else -> false
            }
        }

        // 2. Ấn giữ btn_split thì ảnh gốc hiện ra
        binding.btnSplit.setOnTouchListener(splitTouchListener)
        binding.btnSplitHairType.setOnTouchListener(splitTouchListener)
        
        // 3. Share
        binding.btnShare.setDebouncedClickListener {
            viewModel.onAction(HairResultAction.ShareClicked)
        }
        
        // 4. Ấn save lưu về thư viện ảnh
        binding.btnSave.setDebouncedClickListener {
            viewModel.onAction(HairResultAction.SaveClicked)
        }
        
        // 5. btn_create_again hiển thị khi từ Hair AI sang (historyType == 0)
        binding.btnCreateAgain.isVisible = (historyType == 0)
        binding.btnFlag.isVisible = (historyType == 0)
        binding.btnSplit.isVisible = (historyType == 0)
        binding.btnSplitHairType.isVisible = (historyType == 1)
        
        val rewardConfig = dev.zentrixa.common.admob.ZTRewardedAdUtils.getRewardAdsConfig("reward_function_AI")
        val isAdEnabled = rewardConfig?.enabled != false && rewardConfig?.placements?.get("p_reward_create") != false
        binding.imgAdIconCreateAgain.isVisible = isAdEnabled && !dev.zentrixa.common.utils.ZTUtils.isTurnOffAllAds
        
        binding.btnCreateAgain.setDebouncedClickListener {
            viewModel.onAction(HairResultAction.CreateAgainClicked)
        }
        // 6. ấn btn_home về lại main activity
        binding.btnHome.setDebouncedClickListener {
            ZTInterstitialAdUtils.loadAndShowInterstitialAd(this, "inter_back", "p_inter_back") {
                viewModel.onAction(HairResultAction.HomeClicked)
            }
        }
        
        // 7. xử lý btn_back
        binding.btnBack.setDebouncedClickListener {
            ZTInterstitialAdUtils.loadAndShowInterstitialAd(this, "inter_back", "p_inter_back") {
                viewModel.onAction(HairResultAction.BackClicked)
            }
        }
        
        // 8. nút cờ Report
        binding.btnFlag.setDebouncedClickListener {
            ReportDialog(this).show()
        }

        viewModel.onAction(HairResultAction.LoadData(
            localImageUri = localImageUriString,
            originalImageUri = originalImageUriString,
            historyType = historyType,
            styleName = styleName,
            isColorMode = isColorMode,
            isFemale = isFemale
        ))

        collectViewModel()
    }

    private fun collectViewModel() {
        collectFlow(viewModel.usageState) { usage ->
            val currentDate = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
            val todayUsage = if (usage.lastUsageDate == currentDate) usage.usageCount else 0
            val dailyLimit = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance().getLong("limit_daily").toInt().takeIf { it > 0 } ?: 5
            
            binding.txtTodayRemaining.text = "${dailyLimit - todayUsage}".takeIf { (dailyLimit - todayUsage) >= 0 } ?: "0"
            binding.txtTotalRemaining.text = "/$dailyLimit"
        }

        collectFlow(viewModel.uiState) { state ->
            when (state) {
                is HairResultUiState.Loading -> {
                    // Hiển thị màn hình Processing đặc chế
                    binding.layoutProcessing.isVisible = true
                    binding.layoutResult.isVisible = false
                    
                    // Xử lý thanh ProgressBar: chạy fake progress từ 0 đến 95% trong 30 giây (max 10000)
                    binding.progressBar.progress = 0
                    val animator = ObjectAnimator.ofInt(binding.progressBar, "progress", 0, 9500)
                    animator.duration = 30000
                    animator.interpolator = LinearInterpolator()
                    animator.start()
                    // Lưu lại tag để khi cần có thể cancel (hoặc chỉ cần để nguyên)
                    binding.progressBar.setTag(R.id.progress_bar, animator)
                    
                    // Lắng nghe sự kiện mất mạng để pause progress bar
                    lifecycleScope.launch {
                        NetworkUtils.isConnectedFlow.collect { isConnected ->
                            val currentAnim = binding.progressBar.getTag(R.id.progress_bar) as? ObjectAnimator
                            if (isConnected) {
                                if (currentAnim?.isPaused == true) currentAnim.resume()
                            } else {
                                if (currentAnim?.isRunning == true) currentAnim.pause()
                            }
                        }
                    }
                    
                    // Hiển thị ảnh đang xử lý vào khung (nếu có)
                    val localUriStr = intent.getStringExtra("EXTRA_IMAGE_URI")
                    if (!localUriStr.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(localUriStr)
                            .into(binding.imgProcessingPhoto)
                    }
                    
                    // Bật hiệu ứng quét ảnh từ phải sang trái
                    binding.imgSwipeAnalyst.isVisible = true
                    val scanAnim = AnimationUtils.loadAnimation(this, R.anim.anim_scan_right_to_left)
                    var isFlipped = false
                    scanAnim.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                        override fun onAnimationStart(animation: android.view.animation.Animation?) {
                            binding.imgSwipeAnalyst.scaleX = 1f
                        }
                        override fun onAnimationEnd(animation: android.view.animation.Animation?) {}
                        override fun onAnimationRepeat(animation: android.view.animation.Animation?) {
                            isFlipped = !isFlipped
                            binding.imgSwipeAnalyst.scaleX = if (isFlipped) -1f else 1f
                        }
                    })
                    binding.imgSwipeAnalyst.startAnimation(scanAnim)
                }
                is HairResultUiState.Success -> {
                    ZTAnalyticsUtils.logEvent("S_screen_result_view")

                    // Chuyển sang màn hình Kết quả
                    binding.layoutProcessing.isVisible = false
                    binding.layoutResult.isVisible = true
                    // Hủy fake progress và quét ảnh
                    (binding.progressBar.getTag(R.id.progress_bar) as? ObjectAnimator)?.cancel()
                    binding.progressBar.progress = 10000
                    binding.imgSwipeAnalyst.clearAnimation()
                    binding.imgSwipeAnalyst.isVisible = false
                    
                    binding.frNativeAdResult.setConfig("native_function", "p_native_result")
                    
                    croppedImageUriString = state.imageUrl
                    if (!croppedImageUriString.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(croppedImageUriString)
                            .into(binding.imgResultPhoto)
                    }
                }
                is HairResultUiState.Error -> {
                    (binding.progressBar.getTag(R.id.progress_bar) as? ObjectAnimator)?.cancel()
                    binding.imgSwipeAnalyst.clearAnimation()
                    binding.imgSwipeAnalyst.isVisible = false
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        collectFlow(viewModel.event) { event ->
            when (event) {
                is HairResultEvent.ShareImage -> {
                    val uri = Uri.parse(event.imageUrl)
                    val contentUri = if (uri.scheme == "file") {
                        try {
                            FileProvider.getUriForFile(
                                this@HairResultActivity,
                                "${applicationContext.packageName}.provider",
                                File(uri.path ?: "")
                            )
                        } catch (e: Exception) {
                            uri
                        }
                    } else {
                        uri
                    }
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share Image"))
                }
                is HairResultEvent.ShowToast -> {
                    showToast(event.message)
                }
                is HairResultEvent.NavigateToHome -> {
                    val intent = Intent(this@HairResultActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    finish()
                }
                is HairResultEvent.NavigateBack -> {
                    finish()
                }
                is HairResultEvent.RequireRewardAdToCreateAgain -> {
                    var isAdDone = false
                    dev.zentrixa.common.admob.ZTRewardedAdUtils.loadAndShowRewardAd(
                        this@HairResultActivity,
                        "reward_function_AI",
                        "p_reward_create",
                        onAdEarned = {
                            isAdDone = true
                        },
                        onAdClosed = { isEarned ->
                            if (isEarned || isAdDone) {
                                viewModel.onAction(HairResultAction.ProceedCreateAgain)
                            }
                        }
                    )
                }
            }
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}