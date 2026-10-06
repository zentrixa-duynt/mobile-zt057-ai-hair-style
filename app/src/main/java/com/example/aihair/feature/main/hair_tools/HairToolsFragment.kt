package com.example.aihair.feature.main.hair_tools

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import com.example.aihair.core.ui.base.BaseFragment
import com.example.aihair.core.ui.click.setDebouncedClickListener
import com.example.aihair.databinding.FragmentHairToolsBinding
import com.example.aihair.feature.photo_editor.PhotoEditorActivity
import dev.zentrixa.common.admob.ZTInterstitialAdUtils
import dev.zentrixa.common.firebase.ZTAnalyticsUtils
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.view.isVisible

@AndroidEntryPoint
class HairToolsFragment : BaseFragment<FragmentHairToolsBinding>(FragmentHairToolsBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        
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
            ZTAnalyticsUtils.logEvent("S_screen_hair_tool")
        }
    }

    private fun setupClickListeners() {
        binding.card1.setDebouncedClickListener { clickedView ->
            ZTAnalyticsUtils.logEvent("D_action_trans_style")
            ZTInterstitialAdUtils.loadAndShowInterstitialAd(requireActivity(), "inter_function", "p_inter_function") {
                clickedView.transitionName = "shared_element_container"
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    requireActivity(),
                    clickedView,
                    "shared_element_container"
                )
                val intent = Intent(requireContext(), PhotoEditorActivity::class.java).apply {
                    putExtra("EXTRA_IS_MORPH", true)
                    putExtra(PhotoEditorActivity.EXTRA_HAIR_TYPE, PhotoEditorActivity.TYPE_HAIR_STYLE)
                }
                startActivity(intent, options.toBundle())
            }
        }

        binding.card2.setDebouncedClickListener { clickedView ->
            ZTAnalyticsUtils.logEvent("D_action_trans_color")
            ZTInterstitialAdUtils.loadAndShowInterstitialAd(requireActivity(), "inter_function", "p_inter_function") {
                clickedView.transitionName = "shared_element_container"
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    requireActivity(),
                    clickedView,
                    "shared_element_container"
                )
                val intent = Intent(requireContext(), PhotoEditorActivity::class.java).apply {
                    putExtra("EXTRA_IS_MORPH", true)
                    putExtra(PhotoEditorActivity.EXTRA_HAIR_TYPE, PhotoEditorActivity.TYPE_HAIR_COLOR)
                }
                startActivity(intent, options.toBundle())
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = HairToolsFragment()
    }
}