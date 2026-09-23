package com.example.aihair.feature.photo_editor

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aihair.R
import com.example.aihair.core.utils.HairColorMap
import com.example.aihair.feature.hair_ai.HairStyleItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotoEditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<PhotoEditorUiState>(PhotoEditorUiState.Success())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<PhotoEditorEvent>()
    val event = _event.asSharedFlow()

    private val mixedStyles = generateMixedStyles()
    
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

    init {
        _state.update { 
            PhotoEditorUiState.Success()
        }
    }

    fun onAction(action: PhotoEditorAction) {
        val currentState = _state.value as? PhotoEditorUiState.Success ?: return

        when (action) {
            is PhotoEditorAction.PhotoSelected -> {
                _state.update { currentState.copy(selectedPhotoUri = action.uri, isVerticalPhoto = action.isVertical) }
            }
            is PhotoEditorAction.RemovePhoto -> {
                _state.update { currentState.copy(selectedPhotoUri = null) }
            }
            is PhotoEditorAction.SelectStyle -> {
                val resId = mixedStyles.find { it.id == action.styleId }?.imageResId
                val newColorId = if (currentState.selectedColorId == null) "none" else currentState.selectedColorId
                _state.update { 
                    currentState.copy(
                        currentSelectedStyleId = action.styleId, 
                        currentSelectedStyleResId = resId,
                        selectedColorId = newColorId
                    ) 
                }
            }
            is PhotoEditorAction.SelectColor -> {
                if (action.colorId == "custom" && action.hex != null) {
                    val matchingDefaultId = getMatchingDefaultColorId(action.hex)
                    if (matchingDefaultId != null) {
                        _state.update { currentState.copy(selectedColorId = matchingDefaultId) }
                        return
                    }
                }
                
                val newHex = action.hex ?: currentState.customColorHex
                _state.update { currentState.copy(selectedColorId = action.colorId, customColorHex = newHex) }
            }
            is PhotoEditorAction.ContinueClicked -> {
                if (currentState.selectedPhotoUri != null) {
                    val colorToPass = if (currentState.selectedColorId == "custom") {
                        currentState.customColorHex
                    } else if (currentState.selectedColorId != "none") {
                        currentState.selectedColorId
                    } else {
                        null
                    }
                    
                    viewModelScope.launch {
                        _event.emit(
                            PhotoEditorEvent.NavigateToResult(
                                imageUri = action.croppedImageUri,
                                originalImageUri = currentState.selectedPhotoUri.toString(),
                                isVertical = currentState.isVerticalPhoto,
                                styleId = currentState.currentSelectedStyleId,
                                colorHex = colorToPass
                            )
                        )
                    }
                } else {
                    viewModelScope.launch {
                        _event.emit(PhotoEditorEvent.ShowError(context.getString(R.string.msg_error_select_photo_first)))
                    }
                }
            }
        }
    }

    fun getMixedStyles(): List<HairStyleItem> {
        val currentState = _state.value as? PhotoEditorUiState.Success
        val currentStyleId = currentState?.currentSelectedStyleId
        return mixedStyles.map { it.copy(isSelected = it.id == currentStyleId) }
    }

    fun getColorStyles(): List<HairStyleItem> {
        val currentState = _state.value as? PhotoEditorUiState.Success
        val currentColorId = currentState?.selectedColorId
        return colorStyles.map { it.copy(isSelected = it.id == currentColorId) }
    }

    private fun generateMixedStyles(): List<HairStyleItem> {
        val hairs = listOf(
            HairStyleItem("f_blunt_bob", "Blunt Bob", R.drawable.img_hair1),
            HairStyleItem("f_textured_lob", "Textured Lob", R.drawable.img_hair2),
            HairStyleItem("f_wolf_cut", "Wolf Cut", R.drawable.img_hair3),
            HairStyleItem("f_shag_cut", "Shag Cut", R.drawable.img_hair4),
            HairStyleItem("f_curtain_bangs", "Curtain Bangs", R.drawable.img_hair5),
            HairStyleItem("f_butterfly_cut", "Butterfly Cut", R.drawable.img_hair6),
            HairStyleItem("f_pixie_cut", "Pixie Cut", R.drawable.img_hair7),
            HairStyleItem("f_dutch_braids", "Dutch Braids", R.drawable.img_hair8),
            HairStyleItem("f_high_ponytails", "High Ponytails", R.drawable.img_hair9),
            HairStyleItem("f_sleek_low_bun", "Sleek Low Bun", R.drawable.img_hair10),
            HairStyleItem("f_wavy_ponytail", "Wavy Ponytail", R.drawable.img_hair11),
            HairStyleItem("f_french_bob", "French Bob", R.drawable.img_hair12),
            HairStyleItem("m_french_crop", "French Crop", R.drawable.img_hair13),
            HairStyleItem("m_buzz_cut", "Buzz Cut", R.drawable.img_hair14),
            HairStyleItem("m_pompadour", "Pompadour", R.drawable.img_hair15),
            HairStyleItem("m_undercut_slick", "Undercut Slick", R.drawable.img_hair16),
            HairStyleItem("m_two_block", "Two Block", R.drawable.img_hair17),
            HairStyleItem("m_middle_part", "Middle Part", R.drawable.img_hair18),
            HairStyleItem("m_textured_quiff", "Textured Quiff", R.drawable.img_hair19),
            HairStyleItem("m_ivy_league", "Ivy League", R.drawable.img_hair20),
            HairStyleItem("m_messy_fringe", "Messy Fringe", R.drawable.img_hair21),
            HairStyleItem("m_wavy_medium", "Wavy Medium", R.drawable.img_hair22),
            HairStyleItem("m_caesar_cut", "Caesar Cut", R.drawable.img_hair23),
            HairStyleItem("m_modern_mullet", "Modern Mullet", R.drawable.img_hair24)
        )

        val result = mutableListOf<HairStyleItem>()
        result.addAll(hairs)
        return result
    }

    private fun getMatchingDefaultColorId(hex: String): String? {
        return HairColorMap.getIdForHex(hex)
    }
}
