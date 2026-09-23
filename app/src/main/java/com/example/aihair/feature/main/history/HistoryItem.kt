package com.example.aihair.feature.main.history

import java.util.UUID

data class HistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val originalImageUri: String?,
    val resultImageUri: String?,
    val type: Int = 0, // 0 = Hair AI, 1 = Hair Tools
    val timestamp: Long = System.currentTimeMillis()
)
