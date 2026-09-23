package com.example.aihair.feature.makeup_result

sealed class MakeupResultEvent {
    object NavigateBack : MakeupResultEvent()
    data class ShowToast(val message: String) : MakeupResultEvent()
}
