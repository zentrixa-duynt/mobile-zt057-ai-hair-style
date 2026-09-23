package com.example.aihair.feature.hair_result

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
import com.example.aihair.core.utils.getImageRatio
import com.example.aihair.core.utils.setDimensionRatio
import com.bumptech.glide.Glide
import com.example.aihair.R
import com.example.aihair.core.ui.base.BaseActivity
import com.example.aihair.core.ui.click.setDebouncedClickListener
import com.example.aihair.core.ui.lifecycle.collectFlow
import com.example.aihair.databinding.ActivityHairResultBinding
import com.example.aihair.feature.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
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
        }
        
        // Preload original image to memory cache
        if (!originalImageUriString.isNullOrEmpty()) {
            Glide.with(this).load(Uri.parse(originalImageUriString)).preload()
        }

        val targetImageView = binding.imgResultPhoto

        // 2. Ấn giữ btn_split thì ảnh gốc hiện ra
        binding.btnSplit.setOnTouchListener { _, event ->
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
        binding.btnCreateAgain.setDebouncedClickListener {
            viewModel.onAction(HairResultAction.CreateAgainClicked)
        }
        // 6. ấn btn_home về lại main activity
        binding.btnHome.setDebouncedClickListener {
            viewModel.onAction(HairResultAction.HomeClicked)
        }
        
        // 7. xử lý btn_back
        binding.btnBack.setDebouncedClickListener {
            viewModel.onAction(HairResultAction.BackClicked)
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
                    binding.imgSwipeAnalyst.startAnimation(scanAnim)
                }
                is HairResultUiState.Success -> {
                    // Chuyển sang màn hình Kết quả
                    binding.layoutProcessing.isVisible = false
                    binding.layoutResult.isVisible = true
                    // Hủy fake progress và quét ảnh
                    (binding.progressBar.getTag(R.id.progress_bar) as? ObjectAnimator)?.cancel()
                    binding.progressBar.progress = 10000
                    binding.imgSwipeAnalyst.clearAnimation()
                    binding.imgSwipeAnalyst.isVisible = false
                    
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
            }
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}