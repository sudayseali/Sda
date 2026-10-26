package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.RecordingItem

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
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
) {
    fun toDomainModel(): RecordingItem = RecordingItem(
        id = id,
        title = title,
        filePath = filePath,
        uriString = uriString,
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        resolutionLabel = resolutionLabel,
        fps = fps,
        audioSource = audioSource,
        createdAt = createdAt,
        isFavorite = isFavorite,
        isScreenshot = isScreenshot
    )

    companion object {
        fun fromDomainModel(item: RecordingItem): RecordingEntity = RecordingEntity(
            id = item.id,
            title = item.title,
            filePath = item.filePath,
            uriString = item.uriString,
            durationMs = item.durationMs,
            sizeBytes = item.sizeBytes,
            resolutionLabel = item.resolutionLabel,
            fps = item.fps,
            audioSource = item.audioSource,
            createdAt = item.createdAt,
            isFavorite = item.isFavorite,
            isScreenshot = item.isScreenshot
        )
    }
}
