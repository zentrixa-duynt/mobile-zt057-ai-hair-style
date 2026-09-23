package com.example.aihair.core.utils

import android.graphics.Color

object HairColorMap {
    val COLOR_MAP = mapOf(
        "c_white" to "#FFFFFF",
        "c_black" to "#000000",
        "c_red" to "#FB2C36",
        "c_pink" to "#F6339A",
        "c_blue" to "#2B7FFF",
        "c_green" to "#00C950",
        "c_orange" to "#FF6900",
        "c_yellow" to "#F0B100",
        "c_purple" to "#AD46FF",
        "c_platinum_blonde" to "#DBCAB7",
        "c_ash_blonde" to "#BAA48F",
        "c_honey_blonde" to "#EFD0A1",
        "c_caramel_brown" to "#C38959",
        "c_chocolate_brown" to "#71513D",
        "c_dark_brown" to "#4C362A",
        "c_copper_red" to "#C45A2B",
        "c_burgundy" to "#6B2C37",
        "c_natural_black" to "#49423D",
        "c_silver_gray" to "#DFDFDF",
        "c_pastel_pink" to "#F4BFBD",
        "c_blue_black" to "#3F5273"
    )

    fun getHexForId(id: String?): String? = COLOR_MAP[id]

    fun getIdForHex(hex: String?): String? {
        if (hex == null) return null
        val upperHex = hex.uppercase()
        return COLOR_MAP.entries.find { it.value.uppercase() == upperHex }?.key
    }
}

fun String.parseColorSafe(defaultColor: Int = Color.TRANSPARENT): Int {
    return try {
        Color.parseColor(this)
    } catch (e: Exception) {
        defaultColor
    }
}
