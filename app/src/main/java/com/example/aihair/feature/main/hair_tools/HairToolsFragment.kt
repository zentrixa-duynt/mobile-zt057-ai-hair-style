package com.example.aihair.feature.main.hair_tools

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import com.example.aihair.core.ui.base.BaseFragment
import com.example.aihair.core.ui.click.setDebouncedClickListener
import com.example.aihair.databinding.FragmentHairToolsBinding
import com.example.aihair.feature.photo_editor.PhotoEditorActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HairToolsFragment : BaseFragment<FragmentHairToolsBinding>(FragmentHairToolsBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.card1.setDebouncedClickListener { clickedView ->
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

        binding.card2.setDebouncedClickListener { clickedView ->
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

    companion object {
        @JvmStatic
        fun newInstance() = HairToolsFragment()
    }
}