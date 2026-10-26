package com.example.data.model

data class RecordingItem(
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val uriString: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val resolutionLabel: String,
    val fps: Int,
    val audioSource: String,
    val createdAt: Long,
    val isFavorite: Boolean = false,
    val isScreenshot: Boolean = false
)
