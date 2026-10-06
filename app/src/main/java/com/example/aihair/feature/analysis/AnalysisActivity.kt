package com.example.aihair.feature.analysis

import dev.zentrixa.common.admob.ZTInterstitialAdUtils

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
import dev.zentrixa.common.admob.ZTAppOpenResume
import dev.zentrixa.common.firebase.ZTAnalyticsUtils
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
            ZTInterstitialAdUtils.loadAndShowInterstitialAd(this, "inter_back", "p_inter_back") {
                onBackPressedDispatcher.onBackPressed()
            }
        }

        // Micro-animation: Slide and fade in the back button
        binding.btnBack.animateSlideFadeInAfterMorph()

        val analysisType = intent.getStringExtra(EXTRA_ANALYSIS_TYPE)
        if (analysisType == TYPE_MAKEUP) {
            ZTAnalyticsUtils.logEvent("S_screen_ai_make")
            binding.txtTitle.text = getString(R.string.text_ai_makeup_analysis)
            binding.txtDesc.text = getString(R.string.text_provides_detailed_insights_makeup)
        } else {
            ZTAnalyticsUtils.logEvent("S_screen_ai_face")
            binding.txtTitle.text = getString(R.string.text_ai_face_analysis)
            binding.txtDesc.text = getString(R.string.text_provides_detailed_insights_int)
        }

        binding.btnTryItNow.setDebouncedClickListener {
            if (analysisType == TYPE_MAKEUP) {
                ZTAnalyticsUtils.logEvent("D_action_try_make")
            } else {
                ZTAnalyticsUtils.logEvent("D_action_try_face")
            }
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                ZTAppOpenResume.disableOpenAdResumeOneTime()
            }
            pickMedia.launch("image/*")
        }

        startMorphAnimation()

        val transition = window.sharedElementEnterTransition
        if (transition != null) {
            transition.addListener(object : android.transition.Transition.TransitionListener {
                override fun onTransitionEnd(transition: android.transition.Transition) {
                    transition.removeListener(this)
                    binding.scrollContent.post {
                        binding.scrollContent.smoothScrollTo(0, binding.scrollContent.getChildAt(0).height)
                    }
                }
                override fun onTransitionCancel(transition: android.transition.Transition) {
                    transition.removeListener(this)
                }
                override fun onTransitionStart(transition: android.transition.Transition) {}
                override fun onTransitionPause(transition: android.transition.Transition) {}
                override fun onTransitionResume(transition: android.transition.Transition) {}
            })
        } else {
            binding.scrollContent.postDelayed({
                binding.scrollContent.smoothScrollTo(0, binding.scrollContent.getChildAt(0).height)
            }, 300)
        }
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