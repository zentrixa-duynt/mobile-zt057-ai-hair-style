package com.example.aihair.core.data.remote.dto.generation

import androidx.annotation.Keep

@Keep
data class UploadResponse(
    val success: Boolean,
    val baseUrl: String?,
    val filename: String?,
    val message: String?
)
