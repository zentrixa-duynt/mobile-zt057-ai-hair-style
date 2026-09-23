package com.example.aihair.core.data.remote.dto.analysis

import com.google.gson.annotations.SerializedName

data class AnalysisRequest(
    @SerializedName("model")
    val model: String,
    @SerializedName("messages")
    val messages: List<AnalysisMessage>,
    @SerializedName("max_tokens")
    val maxTokens: Int = 4000,
    @SerializedName("stream")
    val stream: Boolean = true
)

data class AnalysisMessage(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: Any // Can be a String (for system prompt) or List<AnalysisContent> (for user prompt)
)

data class AnalysisContent(
    @SerializedName("type")
    val type: String, // "text" or "image_url"
    @SerializedName("text")
    val text: String? = null,
    @SerializedName("image_url")
    val imageUrl: ImageUrl? = null
)

data class ImageUrl(
    @SerializedName("url")
    val url: String
)
