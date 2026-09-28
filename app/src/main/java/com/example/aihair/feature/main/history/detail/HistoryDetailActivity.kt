package com.example.aihair.feature.main.history.detail

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.aihair.core.utils.getImageRatio
import com.example.aihair.core.utils.setDimensionRatio
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.aihair.core.ui.base.BaseActivity
import com.example.aihair.core.ui.click.setDebouncedClickListener
import com.example.aihair.databinding.ActivityHistoryDetailBinding
import com.example.aihair.feature.main.history.data.HistoryRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class HistoryDetailActivity : BaseActivity<ActivityHistoryDetailBinding>(ActivityHistoryDetailBinding::inflate) {

    @Inject
    lateinit var historyRepository: HistoryRepository

    private var historyId: String? = null
    private var resultImageUriStr: String? = null
    private var originalImageUriStr: String? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        historyId = intent.getStringExtra("EXTRA_HISTORY_ID")
        resultImageUriStr = intent.getStringExtra("EXTRA_IMAGE_URI")
        originalImageUriStr = intent.getStringExtra("EXTRA_ORIGINAL_IMAGE_URI")

        // 1. Tính toán tỉ lệ ảnh từ ảnh gốc (vì ảnh kết quả có thể là URL remote)
        val ratioStr = getImageRatio(originalImageUriStr) ?: getImageRatio(resultImageUriStr)
        ratioStr?.let { ratio ->
            binding.imgResultPhoto.setDimensionRatio(ratio)

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
                        binding.cardResultImage.layoutParams.height = maxHeight
                        binding.cardResultImage.requestLayout()
                    }
                }
            }
        }

        val targetImageView = binding.imgResultPhoto

        if (!resultImageUriStr.isNullOrEmpty()) {
            Glide.with(this)
                .load(Uri.parse(resultImageUriStr))
                .into(targetImageView)
        }

        if (!originalImageUriStr.isNullOrEmpty()) {
            Glide.with(this).load(Uri.parse(originalImageUriStr)).preload()
        }

        // 2. Nút Split - Before/After
        binding.btnSplit.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!originalImageUriStr.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(Uri.parse(originalImageUriStr))
                            .dontAnimate()
                            .placeholder(targetImageView.drawable)
                            .into(targetImageView)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!resultImageUriStr.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(Uri.parse(resultImageUriStr))
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
            resultImageUriStr?.let { uriStr ->
                val uri = Uri.parse(uriStr)
                val contentUri = if (uri.scheme == "file") {
                    try {
                        FileProvider.getUriForFile(
                            this,
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
        }

        // 4. Delete
        binding.btnDelete.setDebouncedClickListener {
            historyId?.let { id ->
                lifecycleScope.launch {
                    historyRepository.removeHistoryItem(id)
                    Toast.makeText(
                        this@HistoryDetailActivity,
                        getString(com.example.aihair.R.string.msg_delete_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }

        // 5. Back
        binding.btnBack.setDebouncedClickListener {
            finish()
        }
    }
}