package com.example.aihair.feature.face_result

sealed class FaceResultEvent {
    object NavigateBack : FaceResultEvent()
    data class ShowToast(val message: String) : FaceResultEvent()
}
