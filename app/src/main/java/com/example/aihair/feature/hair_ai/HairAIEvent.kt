package com.example.aihair.feature.hair_ai

sealed interface HairAIEvent {
    data class ShowError(val message: String) : HairAIEvent
    data class NavigateToResult(
        val imageUri: String,
        val styleId: String,
        val isColorMode: Boolean,
        val isVertical: Boolean,
        val isFemale: Boolean
    ) : HairAIEvent
    
    data class RequireRewardAd(
        val imageUri: String,
        val styleId: String,
        val isColorMode: Boolean,
        val isVertical: Boolean,
        val isFemale: Boolean
    ) : HairAIEvent
}
