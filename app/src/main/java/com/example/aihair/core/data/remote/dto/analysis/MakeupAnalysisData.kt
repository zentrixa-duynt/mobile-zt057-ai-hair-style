package com.example.aihair.core.data.remote.dto.analysis

import com.google.gson.annotations.SerializedName

data class MakeupAnalysisData(
    @SerializedName("overall_makeup_score")
    val overallMakeupScore: Int,
    @SerializedName("balance_scores")
    val balanceScores: BalanceScores,
    @SerializedName("recommendations")
    val recommendations: Recommendations,
    @SerializedName("color_palette")
    val colorPalette: ColorPalette
)

data class BalanceScores(
    @SerializedName("brow")
    val brow: Int,
    @SerializedName("eyes")
    val eyes: Int,
    @SerializedName("skin")
    val skin: Int,
    @SerializedName("cheeks")
    val cheeks: Int,
    @SerializedName("lips")
    val lips: Int,
    @SerializedName("harmony")
    val harmony: Int
)

data class Recommendations(
    @SerializedName("brows")
    val brows: List<String>,
    @SerializedName("eyes")
    val eyes: List<String>,
    @SerializedName("nose")
    val nose: List<String>,
    @SerializedName("skin")
    val skin: List<String>,
    @SerializedName("blush")
    val blush: List<String>,
    @SerializedName("lips")
    val lips: List<String>
)

data class ColorPalette(
    @SerializedName("best_neutrals")
    val bestNeutrals: List<String>,
    @SerializedName("best_blush_lip")
    val bestBlushLip: List<String>,
    @SerializedName("best_eyes_shadow")
    val bestEyesShadow: List<String>,
    @SerializedName("best_liner_mascara")
    val bestLinerMascara: List<String>
)
