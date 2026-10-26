package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.RecordingEntity
import com.example.data.model.RecordingItem
import com.example.util.MediaUtils
import com.example.util.StorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class RecordingRepository(private val context: Context) {
    private val dao = AppDatabase.getInstance(context).recordingDao()

    val allRecordings: Flow<List<RecordingItem>> = dao.getAllRecordings().map { list ->
        list.map { it.toDomainModel() }
    }

    val videoRecordings: Flow<List<RecordingItem>> = dao.getVideoRecordings().map { list ->
        list.map { it.toDomainModel() }
    }

    val screenshots: Flow<List<RecordingItem>> = dao.getScreenshots().map { list ->
        list.map { it.toDomainModel() }
    }

    suspend fun insertRecording(item: RecordingItem): Long = withContext(Dispatchers.IO) {
        dao.insertRecording(RecordingEntity.fromDomainModel(item))
    }

    suspend fun getRecordingById(id: Long): RecordingItem? = withContext(Dispatchers.IO) {
        dao.getRecordingById(id)?.toDomainModel()
    }

    suspend fun renameRecording(id: Long, newTitle: String): Boolean = withContext(Dispatchers.IO) {
        val entity = dao.getRecordingById(id) ?: return@withContext false
        val oldFile = File(entity.filePath)
        if (!oldFile.exists()) return@withContext false

        // Determine extension
        val ext = if (entity.isScreenshot) ".png" else ".mp4"
        val sanitizedTitle = newTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        val finalFileName = if (sanitizedTitle.endsWith(ext, ignoreCase = true)) sanitizedTitle else "$sanitizedTitle$ext"
        val newFile = File(oldFile.parentFile, finalFileName)

        if (oldFile.renameTo(newFile)) {
            val updated = entity.copy(
                title = sanitizedTitle.removeSuffix(ext),
                filePath = newFile.absolutePath
            )
            dao.updateRecording(updated)
            true
        } else {
            false
        }
    }

    suspend fun deleteRecording(item: RecordingItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(item.filePath)
            if (file.exists()) {
                file.delete()
            }
            dao.deleteById(item.id)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun toggleFavorite(id: Long, isFav: Boolean) = withContext(Dispatchers.IO) {
        dao.updateFavorite(id, isFav)
    }

    suspend fun syncWithFileSystem() = withContext(Dispatchers.IO) {
        try {
            // Check video directory
            val videoDir = StorageUtils.getRecordingsDirectory(context)
            videoDir.listFiles()?.filter { it.isFile && it.name.endsWith(".mp4") }?.forEach { file ->
                val existing = dao.getRecordingByPath(file.absolutePath)
                if (existing == null) {
                    val meta = MediaUtils.getVideoMetadata(file.absolutePath)
                    val title = file.nameWithoutExtension
                    val item = RecordingEntity(
                        title = title,
                        filePath = file.absolutePath,
                        uriString = file.toURI().toString(),
                        durationMs = meta.durationMs,
                        sizeBytes = file.length(),
                        resolutionLabel = if (meta.width > 0) "${meta.width}x${meta.height}" else "1080p",
                        fps = 60,
                        audioSource = "mic",
                        createdAt = file.lastModified(),
                        isScreenshot = false
                    )
                    dao.insertRecording(item)
                }
            }

            // Check screenshot directory
            val screenshotDir = StorageUtils.getScreenshotsDirectory(context)
            screenshotDir.listFiles()?.filter { it.isFile && (it.name.endsWith(".png") || it.name.endsWith(".jpg")) }?.forEach { file ->
                val existing = dao.getRecordingByPath(file.absolutePath)
                if (existing == null) {
                    val title = file.nameWithoutExtension
                    val item = RecordingEntity(
                        title = title,
                        filePath = file.absolutePath,
                        uriString = file.toURI().toString(),
                        durationMs = 0L,
                        sizeBytes = file.length(),
                        resolutionLabel = "Screenshot",
                        fps = 0,
                        audioSource = "none",
                        createdAt = file.lastModified(),
                        isScreenshot = true
                    )
                    dao.insertRecording(item)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
