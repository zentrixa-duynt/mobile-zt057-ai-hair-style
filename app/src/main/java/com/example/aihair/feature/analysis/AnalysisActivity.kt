package com.example.aihair.feature.analysis

import android.graphics.Color
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Window
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.aihair.R
import com.example.aihair.core.ui.base.BaseActivity
import com.example.aihair.core.ui.click.setDebouncedClickListener
import com.example.aihair.core.ui.transition.animateSlideFadeInAfterMorph
import com.example.aihair.core.ui.transition.setupMorphTransition
import com.example.aihair.databinding.ActivityAnalysisBinding
import com.example.aihair.feature.pick_gender.PickGenderActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AnalysisActivity : BaseActivity<ActivityAnalysisBinding>(ActivityAnalysisBinding::inflate) {

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val intent = Intent(this, PickGenderActivity::class.java)
            intent.putExtra("EXTRA_SELECTED_IMAGE_URI", it.toString())
            intent.putExtra(EXTRA_ANALYSIS_TYPE, getIntent().getStringExtra(EXTRA_ANALYSIS_TYPE))
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setupMorphTransition(R.id.main)
        super.onCreate(savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        // Force slide out transition instead of morph return
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        binding.btnBack.setDebouncedClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Micro-animation: Slide and fade in the back button
        binding.btnBack.animateSlideFadeInAfterMorph()

        val analysisType = intent.getStringExtra(EXTRA_ANALYSIS_TYPE)
        if (analysisType == TYPE_MAKEUP) {
            binding.txtTitle.text = getString(R.string.text_ai_makeup_analysis)
            binding.txtDesc.text = getString(R.string.text_provides_detailed_insights_makeup)
        } else {
            binding.txtTitle.text = getString(R.string.text_ai_face_analysis)
            binding.txtDesc.text = getString(R.string.text_provides_detailed_insights_int)
        }

        binding.btnTryItNow.setDebouncedClickListener {
            pickMedia.launch("image/*")
        }

        startMorphAnimation()
    }

    private fun startMorphAnimation() {
        val images = listOf(binding.imgFace1, binding.imgFace2, binding.imgFace3)
        var currentIndex = 0
        
        lifecycleScope.launch {
            while (isActive) {
                delay(1500) // Wait
                
                val nextIndex = (currentIndex + 1) % images.size
                val nextImage = images[nextIndex]
                
                nextImage.bringToFront()
                nextImage.alpha = 0f
                nextImage.animate()
                    .alpha(1f)
                    .setDuration(1000)
                    .start()
                    
                currentIndex = nextIndex
            }
        }
    }

    companion object {
        const val EXTRA_ANALYSIS_TYPE = "EXTRA_ANALYSIS_TYPE"
        const val TYPE_FACE = "TYPE_FACE"
        const val TYPE_MAKEUP = "TYPE_MAKEUP"
    }
}