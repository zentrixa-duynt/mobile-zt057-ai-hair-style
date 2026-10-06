package com.example.aihair.feature.main.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import com.example.aihair.core.ui.base.BaseFragment
import com.example.aihair.core.ui.click.setDebouncedClickListener
import com.example.aihair.databinding.FragmentHomeBinding
import com.example.aihair.feature.analysis.AnalysisActivity
import com.example.aihair.feature.hair_ai.HairAIActivity
import dev.zentrixa.common.admob.ZTInterstitialAdUtils
import dev.zentrixa.common.firebase.ZTAnalyticsUtils
import androidx.core.view.isVisible

class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ZTAnalyticsUtils.logOneTimeEvent("S_screen_home_first_show")
        ZTAnalyticsUtils.logEvent("S_screen_home_show")

        binding.frNativeAd.viewTreeObserver.addOnGlobalLayoutListener {
            if (binding.cardNativeAd.isVisible != binding.frNativeAd.isVisible) {
                binding.cardNativeAd.isVisible = binding.frNativeAd.isVisible
            }
        }
        
        setupClickListeners()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            ZTAnalyticsUtils.logEvent("S_screen_home_show")
        }
    }

    private fun setupClickListeners() {
        // Slide to Hair AI
        binding.cardHairStyle.setDebouncedClickListener {
            ZTAnalyticsUtils.logEvent("D_action_hair_style")
            ZTInterstitialAdUtils.loadAndShowInterstitialAd(requireActivity(), "inter_function", "p_inter_function") {
                val intent = Intent(requireContext(), HairAIActivity::class.java).apply {
                    putExtra(HairAIActivity.EXTRA_HAIR_TYPE, HairAIActivity.TYPE_HAIR_STYLE)
                }
                startActivity(intent)
            }
        }

        binding.cardHairColor.setDebouncedClickListener {
            ZTAnalyticsUtils.logEvent("D_action_hair_color")
            ZTInterstitialAdUtils.loadAndShowInterstitialAd(requireActivity(), "inter_function", "p_inter_function") {
                val intent = Intent(requireContext(), HairAIActivity::class.java).apply {
                    putExtra(HairAIActivity.EXTRA_HAIR_TYPE, HairAIActivity.TYPE_HAIR_COLOR)
                }
                startActivity(intent)
            }
        }

        // Morph to Analysis (Face)
        // Morph to Analysis (Face)
        binding.cardFaceAnalysis.setDebouncedClickListener { clickedView ->
            ZTAnalyticsUtils.logEvent("D_action_face_analysis")
            ZTInterstitialAdUtils.loadAndShowInterstitialAd(requireActivity(), "inter_function", "p_inter_function") {
                clickedView.transitionName = "shared_element_container"
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    requireActivity(),
                    clickedView,
                    "shared_element_container"
                )
                val intent = Intent(requireContext(), AnalysisActivity::class.java).apply {
                    putExtra(AnalysisActivity.EXTRA_ANALYSIS_TYPE, AnalysisActivity.TYPE_FACE)
                }
                startActivity(intent, options.toBundle())
            }
        }

        // Morph to Analysis (Makeup)
        // Morph to Analysis (Makeup)
        binding.cardMakeupAnalysis.setDebouncedClickListener { clickedView ->
            ZTAnalyticsUtils.logEvent("D_action_make_analysis")
            ZTInterstitialAdUtils.loadAndShowInterstitialAd(requireActivity(), "inter_function", "p_inter_function") {
                clickedView.transitionName = "shared_element_container"
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    requireActivity(),
                    clickedView,
                    "shared_element_container"
                )
                val intent = Intent(requireContext(), AnalysisActivity::class.java).apply {
                    putExtra(AnalysisActivity.EXTRA_ANALYSIS_TYPE, AnalysisActivity.TYPE_MAKEUP)
                }
                startActivity(intent, options.toBundle())
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}