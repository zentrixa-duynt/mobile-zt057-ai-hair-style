package com.example.aihair.feature.photo_editor

import android.net.Uri

sealed interface PhotoEditorAction {
    data class PhotoSelected(val uri: Uri, val isVertical: Boolean) : PhotoEditorAction
    object RemovePhoto : PhotoEditorAction
    data class SelectStyle(val styleId: String) : PhotoEditorAction
    data class SelectColor(val colorId: String, val hex: String? = null) : PhotoEditorAction
    data class ContinueClicked(val croppedImageUri: String) : PhotoEditorAction
}
