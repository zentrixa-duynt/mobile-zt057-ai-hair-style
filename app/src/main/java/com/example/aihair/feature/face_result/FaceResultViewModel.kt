package com.example.aihair.feature.face_result

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aihair.R
import com.example.aihair.core.data.local.datastore.AppPreferences
import com.example.aihair.core.data.remote.dto.analysis.AnalysisState
import com.example.aihair.core.data.repository.AnalysisRepository
import com.example.aihair.feature.hair_ai.HairStyleItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FaceResultViewModel @Inject constructor(
    private val analysisRepository: AnalysisRepository,
    private val appPreferences: AppPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<FaceResultUiState>(FaceResultUiState.Idle)
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<FaceResultEvent>()
    val event = _event.asSharedFlow()

    // Assuming we can get the master list. For now, hardcoding based on earlier findings
    private val masterHairStyles = listOf(
        HairStyleItem("f_blunt_bob", "Blunt Bob", R.drawable.img_female_bluntbob),
        HairStyleItem("f_textured_lob", "Textured Lob", R.drawable.img_female_texturedlob),
        HairStyleItem("f_wolf_cut", "Wolf Cut", R.drawable.img_female_wolfcut),
        HairStyleItem("f_shag_cut", "Shag Cut", R.drawable.img_female_shagcut),
        HairStyleItem("f_curtain_bangs", "Curtain Bangs", R.drawable.img_female_curtainbangs),
        HairStyleItem("f_butterfly_cut", "Butterfly Cut", R.drawable.img_female_butterflycut),
        HairStyleItem("f_pixie_cut", "Pixie Cut", R.drawable.img_female_pixiecut),
        HairStyleItem("f_dutch_braids", "Dutch Braids", R.drawable.img_female_dutchbraids),
        HairStyleItem("f_high_ponytails", "High Ponytails", R.drawable.img_female_highponytails),
        HairStyleItem("f_sleek_low_bun", "Sleek Low Bun", R.drawable.img_female_sleeklowbun),
        HairStyleItem("f_wavy_ponytail", "Wavy Ponytail", R.drawable.img_female_wavyponytail),
        HairStyleItem("f_french_bob", "French Bob", R.drawable.img_female_frenchbob),
        HairStyleItem("m_french_crop", "French Crop", R.drawable.img_male_frenchcrop),
        HairStyleItem("m_buzz_cut", "Buzz Cut", R.drawable.img_male_buzzcut),
        HairStyleItem("m_pompadour", "Pompadour", R.drawable.img_male_pompadour),
        HairStyleItem("m_undercut_slick", "Undercut Slick", R.drawable.img_male_undercutslick),
        HairStyleItem("m_two_block", "Two Block", R.drawable.img_male_twoblock),
        HairStyleItem("m_middle_part", "Middle Part", R.drawable.img_male_middlepart),
        HairStyleItem("m_textured_quiff", "Textured Quiff", R.drawable.img_male_texturedquiff),
        HairStyleItem("m_ivy_league", "Ivy League", R.drawable.img_male_ivyleague),
        HairStyleItem("m_messy_fringe", "Messy Fringe", R.drawable.img_male_messyfringe),
        HairStyleItem("m_wavy_medium", "Wavy Medium", R.drawable.img_male_wavymedium),
        HairStyleItem("m_caesar_cut", "Caesar Cut", R.drawable.img_male_caesarcut),
        HairStyleItem("m_modern_mullet", "Modern Mullet", R.drawable.img_male_modernmullet)
    )

    fun onAction(action: FaceResultAction) {
        when (action) {
            is FaceResultAction.StartAnalysis -> {
                startAnalysis(action.imageUri, action.gender)
            }
            is FaceResultAction.BackClicked -> {
                viewModelScope.launch {
                    _event.emit(FaceResultEvent.NavigateBack)
                }
            }
        }
    }

    private fun startAnalysis(imageUri: String, gender: String) {
        viewModelScope.launch {
            val filteredStyles = masterHairStyles.filter { 
                when (gender) {
                    "MALE" -> it.id.startsWith("m_")
                    "FEMALE" -> it.id.startsWith("f_")
                    else -> true
                }
            }
            val localNames = filteredStyles.map { it.name }
            val launchState = appPreferences.launchState.first()
            val languageCode = launchState.selectedLanguage ?: "en"
            val targetLanguage = when (languageCode) {
                "vi" -> "Vietnamese"
                "es" -> "Spanish"
                "fr" -> "French"
                "de" -> "German"
                "ja" -> "Japanese"
                "ko" -> "Korean"
                "ru" -> "Russian"
                "hi" -> "Hindi"
                "pt" -> "Portuguese"
                "tr" -> "Turkish"
                "hr" -> "Croatian"
                "hu" -> "Hungarian"
                "id" -> "Indonesian"
                "it" -> "Italian"
                "ne" -> "Nepali"
                "th" -> "Thai"
                "uk" -> "Ukrainian"
                "zh" -> "Chinese"
                "ar" -> "Arabic"
                "ur" -> "Urdu"
                "bn" -> "Bengali"
                "tl" -> "Filipino"
                "af" -> "Afrikaans"
                "nl" -> "Dutch"
                else -> "English"
            }
            analysisRepository.startFaceAnalysis(imageUri, targetLanguage, localNames).collect { analysisState ->
                when (analysisState) {
                    is AnalysisState.Uploading,
                    is AnalysisState.Submitting,
                    is AnalysisState.Processing -> {
                        _state.update { FaceResultUiState.Loading }
                    }
                    is AnalysisState.FaceSuccess -> {
                        val currentDate = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
                        try {
                            appPreferences.incrementUsageCount(currentDate)
                        } catch (e: Exception) { e.printStackTrace() }
                        val recommendedStyles = filteredStyles.filter { item ->
                            analysisState.data.suitableHairStyles.contains(item.name)
                        }
                        _state.update { FaceResultUiState.Success(analysisState.data, recommendedStyles) }
                    }
                    is AnalysisState.Error -> {
                        val message = when (val err = analysisState.error) {
                            is AnalysisState.AnalysisError.NetworkError -> err.message
                            is AnalysisState.AnalysisError.StorageError -> err.message
                            is AnalysisState.AnalysisError.SubmitError -> err.message
                            is AnalysisState.AnalysisError.PollingError -> err.message
                            is AnalysisState.AnalysisError.TimeoutError -> err.message
                            is AnalysisState.AnalysisError.TaskFailed -> err.message
                            is AnalysisState.AnalysisError.ParseError -> err.message
                        }
                        _state.update { FaceResultUiState.Error(message) }
                        _event.emit(FaceResultEvent.ShowToast(message))
                        _event.emit(FaceResultEvent.NavigateBack)
                    }
                    else -> {}
                }
            }
        }
    }
}
