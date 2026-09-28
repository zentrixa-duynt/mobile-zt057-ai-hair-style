package com.example.aihair.core.data.remote.dto.generation

import com.google.gson.annotations.SerializedName

data class GenerationResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("progress")
    val progress: String?,
    @SerializedName("task_id")
    val taskId: String?,
    @SerializedName("task_result")
    val taskResult: GenerationTaskResult?,
    @SerializedName("err_reason")
    val errorReason: GenerationErrorReason?
)

data class GenerationTaskResult(
    @SerializedName("url")
    val url: String,
    @SerializedName("note")
    val note: String?,
    @SerializedName("url_expires_at")
    val urlExpiresAt: String?
)

data class GenerationErrorReason(
    @SerializedName("code")
    val code: String?,
    @SerializedName("message")
    val message: String
)
