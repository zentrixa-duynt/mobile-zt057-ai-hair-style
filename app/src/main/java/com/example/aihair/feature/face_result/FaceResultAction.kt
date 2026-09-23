package com.example.aihair.feature.face_result

sealed class FaceResultAction {
    data class StartAnalysis(val imageUri: String, val gender: String) : FaceResultAction()
    object BackClicked : FaceResultAction()
}
