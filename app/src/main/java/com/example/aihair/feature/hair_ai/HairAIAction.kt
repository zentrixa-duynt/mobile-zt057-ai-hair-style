package com.example.aihair.feature.hair_ai

import android.net.Uri

sealed interface HairAIAction {
    data class Init(val isColorMode: Boolean) : HairAIAction
    data class SelectTab(val isFemale: Boolean) : HairAIAction
    data class SelectStyle(val styleId: String) : HairAIAction
    data class PhotoSelected(val uri: Uri, val isVertical: Boolean) : HairAIAction
    object RemovePhoto : HairAIAction
    object CreateClicked : HairAIAction
}
