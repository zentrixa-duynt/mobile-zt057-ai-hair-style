package com.example.aihair.core.data.remote.dto.analysis

import com.google.gson.annotations.SerializedName

data class ChatCompletionChunk(
    @SerializedName("choices")
    val choices: List<ChatChoice>?
)

data class ChatChoice(
    @SerializedName("delta")
    val delta: ChatDelta?
)

data class ChatDelta(
    @SerializedName("content")
    val content: String?
)

data class OpenAIErrorResponse(
    @SerializedName("error")
    val error: OpenAIError?
)

data class OpenAIError(
    @SerializedName("message")
    val message: String?,
    @SerializedName("type")
    val type: String?,
    @SerializedName("code")
    val code: String?
)
