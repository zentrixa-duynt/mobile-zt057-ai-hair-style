package com.example.aihair.core.data.remote.dto.analysis

import com.google.gson.annotations.SerializedName

data class FaceAnalysisData(
    @SerializedName("hair_style")
    val hairStyle: String,
    @SerializedName("hair_color")
    val hairColor: String,
    @SerializedName("face_shape")
    val faceShape: String,
    @SerializedName("hairstyle_fit_rate")
    val hairstyleFitRate: Int,
    @SerializedName("golden_ratio")
    val goldenRatio: Double,
    @SerializedName("golden_ratio_short_description")
    val goldenRatioShortDescription: String,
    @SerializedName("chin")
    val chin: String,
    @SerializedName("cheekbone")
    val cheekbone: String,
    @SerializedName("temple")
    val temple: String,
    @SerializedName("apple_cheeks")
    val appleCheeks: String,
    @SerializedName("golden_ratio_keywords")
    val goldenRatioKeywords: List<String>,
    @SerializedName("suitable_hair_styles")
    val suitableHairStyles: List<String>
)
