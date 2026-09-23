package com.example.aihair.feature.hair_result

sealed class HairResultAction {
    data class LoadData(
        val localImageUri: String?,
        val originalImageUri: String?,
        val historyType: Int,
        val styleName: String?,
        val isColorMode: Boolean,
        val isFemale: Boolean
    ) : HairResultAction()
    
    data object ShareClicked : HairResultAction()
    data object SaveClicked : HairResultAction()
    data object HomeClicked : HairResultAction()
    data object BackClicked : HairResultAction()
    data object CreateAgainClicked : HairResultAction()
}
