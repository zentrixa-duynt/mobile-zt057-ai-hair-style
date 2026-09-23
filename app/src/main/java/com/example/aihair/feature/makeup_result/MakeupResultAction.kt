package com.example.aihair.feature.makeup_result

sealed class MakeupResultAction {
    data class StartAnalysis(val imageUri: String) : MakeupResultAction()
    object BackClicked : MakeupResultAction()
}
