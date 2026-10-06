package com.example.aihair.feature.hair_result

sealed interface HairResultEvent {
    data class ShareImage(val imageUrl: String) : HairResultEvent
    data class ShowToast(val message: String) : HairResultEvent
    data object NavigateToHome : HairResultEvent
    data object NavigateBack : HairResultEvent
    data object RequireRewardAdToCreateAgain : HairResultEvent
}
