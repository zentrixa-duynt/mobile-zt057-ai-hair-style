package com.example.aihair.feature.main.history

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.aihair.R
import com.example.aihair.core.ui.lifecycle.collectFlow
import com.example.aihair.databinding.FragmentHistoryBinding
import com.example.aihair.feature.main.history.detail.HistoryDetailActivity
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()
    private val adapter by lazy {
        createHistoryAdapter { item ->
            val intent = Intent(requireContext(), HistoryDetailActivity::class.java).apply {
                putExtra("EXTRA_HISTORY_ID", item.id)
                putExtra("EXTRA_IMAGE_URI", item.resultImageUri)
                putExtra("EXTRA_ORIGINAL_IMAGE_URI", item.originalImageUri)
            }
            startActivity(intent)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var currentTabIndex = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvHistory.adapter = adapter

        // Sync tabs with categories: 0 = Hair AI, 1 = Hair Tools
        val tabTitles = listOf(
            getString(R.string.text_hair_ai),
            getString(R.string.text_hair_tools)
        )
        binding.tabBar.setTabTitles(tabTitles)

        binding.tabBar.onTabSelected = { index ->
            currentTabIndex = index
            updateAdapterData(viewModel.history.value)
        }

        collectFlow(viewModel.history) { historyList ->
            updateAdapterData(historyList)
        }
    }

    private fun updateAdapterData(historyList: List<HistoryItem>) {
        val filteredList = historyList.filter { it.type == currentTabIndex }
        adapter.submitList(filteredList) {
            binding.rvHistory.post {
                val canScroll = binding.rvHistory.computeVerticalScrollRange() > binding.rvHistory.height
                val params = binding.tabBarContainer.layoutParams as AppBarLayout.LayoutParams
                if (canScroll) {
                    params.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                } else {
                    binding.appBar.setExpanded(true, false)
                    params.scrollFlags = 0
                }
                binding.tabBarContainer.layoutParams = params
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = HistoryFragment()
    }
}