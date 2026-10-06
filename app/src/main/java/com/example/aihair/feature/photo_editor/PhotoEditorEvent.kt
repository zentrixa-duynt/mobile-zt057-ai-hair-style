package com.example.aihair.feature.photo_editor

sealed interface PhotoEditorEvent {
    data class ShowError(val message: String) : PhotoEditorEvent
    data class NavigateToResult(
        val imageUri: String,
        val originalImageUri: String,
        val isVertical: Boolean,
        val styleId: String?,
        val colorHex: String?
    ) : PhotoEditorEvent
    data class RequireRewardAdToUnlock(val itemId: String, val isColor: Boolean) : PhotoEditorEvent
}
