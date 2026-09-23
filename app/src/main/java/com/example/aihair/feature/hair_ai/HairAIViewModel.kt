package com.example.aihair.feature.hair_ai

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aihair.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HairAIViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<HairAIUiState>(HairAIUiState.Success())
    val uiState: StateFlow<HairAIUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<HairAIEvent>()
    val event: SharedFlow<HairAIEvent> = _event.asSharedFlow()

    private val femaleStyles = listOf(
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
        HairStyleItem("f_french_bob", "French Bob", R.drawable.img_female_frenchbob)
    )

    private val maleStyles = listOf(
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

    private val colorStyles = listOf(
        HairStyleItem(id = "c_platinum_blonde", nameResId = R.string.color_platinum_blonde, imageResId = R.drawable.img_color_platinumblonde),
        HairStyleItem(id = "c_ash_blonde", nameResId = R.string.color_ash_blonde, imageResId = R.drawable.img_color_ashblonde),
        HairStyleItem(id = "c_honey_blonde", nameResId = R.string.color_honey_blonde, imageResId = R.drawable.img_color_honeyblonde),
        HairStyleItem(id = "c_caramel_brown", nameResId = R.string.color_caramel_brown, imageResId = R.drawable.img_color_caramelbrown),
        HairStyleItem(id = "c_chocolate_brown", nameResId = R.string.color_chocolate_brown, imageResId = R.drawable.img_color_chocolatebrown),
        HairStyleItem(id = "c_dark_brown", nameResId = R.string.color_dark_brown, imageResId = R.drawable.img_color_darkbrown),
        HairStyleItem(id = "c_copper_red", nameResId = R.string.color_copper_red, imageResId = R.drawable.img_color_copperred),
        HairStyleItem(id = "c_burgundy", nameResId = R.string.color_burgundy, imageResId = R.drawable.img_color_burgundy),
        HairStyleItem(id = "c_natural_black", nameResId = R.string.color_natural_black, imageResId = R.drawable.img_color_naturalblack),
        HairStyleItem(id = "c_silver_gray", nameResId = R.string.color_silver_gray, imageResId = R.drawable.img_color_silvergray),
        HairStyleItem(id = "c_pastel_pink", nameResId = R.string.color_pastel_pink, imageResId = R.drawable.img_color_pastelpink),
        HairStyleItem(id = "c_blue_black", nameResId = R.string.color_blue_black, imageResId = R.drawable.img_color_blueblack)
    )

    private var currentSelectedStyleId: String? = null

    fun onAction(action: HairAIAction) {
        val currentState = _uiState.value
        if (currentState !is HairAIUiState.Success) return

        when (action) {
            is HairAIAction.Init -> {
                updateStyles(
                    isColorMode = action.isColorMode,
                    isFemale = currentState.isFemaleTabSelected,
                    selectedId = currentSelectedStyleId
                )
            }
            is HairAIAction.SelectTab -> {
                if (currentState.isFemaleTabSelected != action.isFemale && !currentState.isColorMode) {
                    updateStyles(isColorMode = currentState.isColorMode, isFemale = action.isFemale, selectedId = currentSelectedStyleId)
                }
            }
            is HairAIAction.SelectStyle -> {
                currentSelectedStyleId = action.styleId
                updateStyles(isColorMode = currentState.isColorMode, isFemale = currentState.isFemaleTabSelected, selectedId = action.styleId)
            }
            is HairAIAction.PhotoSelected -> {
                _uiState.value = currentState.copy(
                    selectedPhotoUri = action.uri,
                    isPhotoVertical = action.isVertical
                )
            }
            HairAIAction.RemovePhoto -> {
                _uiState.value = currentState.copy(
                    selectedPhotoUri = null
                )
            }
            HairAIAction.CreateClicked -> {
                val photoUri = currentState.selectedPhotoUri?.toString()
                val styleId = currentSelectedStyleId
                
                if (photoUri != null && styleId != null) {
                    val resolvedStyleName = getStyleName(styleId, currentState.isColorMode, currentState.isFemaleTabSelected) ?: styleId
                    viewModelScope.launch {
                        _event.emit(HairAIEvent.NavigateToResult(
                            imageUri = photoUri,
                            styleId = resolvedStyleName,
                            isColorMode = currentState.isColorMode,
                            isVertical = currentState.isPhotoVertical,
                            isFemale = currentState.isFemaleTabSelected
                        ))
                    }
                } else {
                    viewModelScope.launch {
                        _event.emit(HairAIEvent.ShowError(context.getString(R.string.msg_error_select_photo_style_first)))
                    }
                }
            }
        }
    }

    private fun updateStyles(isColorMode: Boolean, isFemale: Boolean, selectedId: String?) {
        val currentPhotoUri = (_uiState.value as? HairAIUiState.Success)?.selectedPhotoUri
        val isPhotoVertical = (_uiState.value as? HairAIUiState.Success)?.isPhotoVertical ?: false
        
        val baseList = if (isColorMode) {
            colorStyles
        } else if (isFemale) {
            femaleStyles
        } else {
            maleStyles
        }
        
        val updatedList = baseList.map { item ->
            item.copy(isSelected = item.id == selectedId)
        }

        _uiState.value = HairAIUiState.Success(
            isColorMode = isColorMode,
            isFemaleTabSelected = isFemale,
            styles = updatedList,
            selectedStyleId = selectedId,
            selectedPhotoUri = currentPhotoUri,
            isPhotoVertical = isPhotoVertical
        )
    }

    private fun getStyleName(styleId: String, isColorMode: Boolean, isFemale: Boolean): String? {
        val baseList = if (isColorMode) {
            colorStyles
        } else if (isFemale) {
            femaleStyles
        } else {
            maleStyles
        }
        val item = baseList.find { it.id == styleId } ?: return null
        return if (item.nameResId != null) {
            context.getString(item.nameResId)
        } else {
            item.name
        }
    }
}
