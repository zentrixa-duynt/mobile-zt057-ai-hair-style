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

class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Slide to Hair AI
        binding.cardHairStyle.setDebouncedClickListener {
            val intent = Intent(requireContext(), HairAIActivity::class.java).apply {
                putExtra(HairAIActivity.EXTRA_HAIR_TYPE, HairAIActivity.TYPE_HAIR_STYLE)
            }
            startActivity(intent)
        }
        binding.cardHairColor.setDebouncedClickListener { clickedView ->
            clickedView.transitionName = "shared_element_container"
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                requireActivity(),
                clickedView,
                "shared_element_container"
            )
            val intent = Intent(requireContext(), HairAIActivity::class.java).apply {
                putExtra("EXTRA_IS_MORPH", true)
                putExtra(HairAIActivity.EXTRA_HAIR_TYPE, HairAIActivity.TYPE_HAIR_COLOR)
            }
            startActivity(intent, options.toBundle())
        }

        // Morph to Analysis (Face)
        binding.cardFaceAnalysis.setDebouncedClickListener { clickedView ->
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

        // Morph to Analysis (Makeup)
        binding.cardMakeupAnalysis.setDebouncedClickListener { clickedView ->
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

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}