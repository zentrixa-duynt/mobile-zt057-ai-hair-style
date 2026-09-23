package com.example.aihair.feature.language

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aihair.R
import com.example.aihair.core.data.local.datastore.AppPreferences
import com.example.aihair.core.language.AppLanguageManager
import com.example.aihair.core.ui.base.BaseActivity
import com.example.aihair.core.ui.click.setDebouncedClickListener
import com.example.aihair.core.ui.lifecycle.collectFlow
import com.example.aihair.databinding.ActivityLanguageBinding
import com.example.aihair.feature.main.MainActivity
import com.example.aihair.feature.settings.SettingsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LanguageActivity : BaseActivity<ActivityLanguageBinding>(ActivityLanguageBinding::inflate) {

    @Inject lateinit var appPreferences: AppPreferences
    private val languageAdapter by lazy {
        createLanguageAdapter(
            onLanguageClick = { languageTag ->
                viewModel.onAction(LanguageAction.SelectLanguage(languageTag))
            },
            currentLanguageTagProvider = {
                viewModel.uiState.value.selectedLanguageTag.takeIf { it.isNotEmpty() }
                    ?: AppLanguageManager.currentLanguageTag()
            }
        )
    }
    private val viewModel: LanguageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupLanguageList()
        
        binding.btnBack.setDebouncedClickListener {
            viewModel.onAction(LanguageAction.ConfirmSelection)
        }

        collectViewModel()
    }

    private fun setupLanguageList() {
        binding.rvLanguages.apply {
            layoutManager = LinearLayoutManager(this@LanguageActivity)
            adapter = languageAdapter
        }
    }

    private fun collectViewModel() {
        collectFlow(viewModel.uiState, action = ::renderState)
        collectFlow(viewModel.event, action = ::handleEvent)
    }

    private fun renderState(state: LanguageUiState) {
        val hasSelection = state.selectedLanguageTag.isNotEmpty()
        val languageTagToUse = if (hasSelection) state.selectedLanguageTag else AppLanguageManager.currentLanguageTag()
        val localizedContext = AppLanguageManager.wrapContext(this, languageTagToUse)
        
        binding.tvTitleLanguages.text = localizedContext.getString(R.string.title_languages)
        binding.root.layoutDirection = localizedContext.resources.configuration.layoutDirection
        binding.btnBack.setImageDrawable(androidx.core.content.ContextCompat.getDrawable(localizedContext, R.drawable.arrow_left))

        languageAdapter.submitList(state.items)
    }

    private fun handleEvent(event: LanguageEvent) {
        when (event) {
            LanguageEvent.LanguageSaved -> {
                lifecycleScope.launch {
                    val launchState = appPreferences.launchState.first()
                    val destinationClassName = when {
                        intent.getBooleanExtra("from_setting", false) -> SettingsActivity::class.java.name
                        else -> MainActivity::class.java.name
                    }
                    navigateToDestination(destinationClassName, launchState.selectedLanguage)
                }
            }
            is LanguageEvent.ShowError -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToDestination(destinationClassName: String, selectedLanguage: String?) {
        AppLanguageManager.setPendingLanguage(selectedLanguage)
        val destination = Class.forName(destinationClassName)
        val intent = Intent(this, destination).apply {
            putExtra(AppLanguageManager.EXTRA_LANGUAGE_TAG, selectedLanguage)
        }
        startActivity(intent)
        finish()
    }
}