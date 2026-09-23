package com.example.aihair.core.data.remote.dto.generation

import com.google.gson.annotations.SerializedName

data class GenerationRequest(
    @SerializedName("model")
    val model: String,
    @SerializedName("prompt")
    val prompt: String,
    @SerializedName("config")
    val config: GenerationConfig
)

data class GenerationConfig(
    @SerializedName("background")
    val background: String,
    @SerializedName("quality")
    val quality: String,
    @SerializedName("size")
    val size: String,
    @SerializedName("images")
    val images: List<String>
)
