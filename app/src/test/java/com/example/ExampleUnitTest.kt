package com.example

import com.example.data.local.RecordingEntity
import com.example.data.model.AudioSourceType
import com.example.data.model.CountdownOption
import com.example.data.model.RecordingSettings
import com.example.data.model.VideoFps
import com.example.data.model.VideoResolution
import com.example.util.StorageUtils
import com.example.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testTimeDurationFormatting() {
        assertEquals("00:00", TimeUtils.formatDuration(0L))
        assertEquals("00:45", TimeUtils.formatDuration(45_000L))
        assertEquals("03:15", TimeUtils.formatDuration(195_000L))
        assertEquals("01:05:20", TimeUtils.formatDuration(3_920_000L))
    }

    @Test
    fun testStorageBytesFormatting() {
        assertEquals("0 B", StorageUtils.formatBytes(0L))
        assertEquals("500 B", StorageUtils.formatBytes(500L))
        assertEquals("100.0 KB", StorageUtils.formatBytes(102400L))
        assertEquals("50.0 MB", StorageUtils.formatBytes(50L * 1024 * 1024))
        assertEquals("2.50 GB", StorageUtils.formatBytes((2.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun testRecordingSettingsDefaults() {
        val settings = RecordingSettings()
        assertEquals(VideoResolution.FHD_1080P, settings.resolution)
        assertEquals(VideoFps.FPS_60, settings.fps)
        assertEquals(AudioSourceType.MIC, settings.audioSource)
        assertEquals(CountdownOption.SEC_3, settings.countdown)
        assertTrue(settings.floatingControlsEnabled)
    }

    @Test
    fun testEntityDomainModelConversion() {
        val entity = RecordingEntity(
            id = 42L,
            title = "Test Screen Recording",
            filePath = "/path/to/test.mp4",
            uriString = "content://test",
            durationMs = 60000L,
            sizeBytes = 10485760L,
            resolutionLabel = "1080p",
            fps = 60,
            audioSource = "mic",
            createdAt = 1700000000L,
            isFavorite = true,
            isScreenshot = false
        )

        val domain = entity.toDomainModel()
        assertEquals(42L, domain.id)
        assertEquals("Test Screen Recording", domain.title)
        assertEquals(60000L, domain.durationMs)
        assertTrue(domain.isFavorite)
        assertFalse(domain.isScreenshot)
        assertFalse(domain.isDeleted)
    }

    @Test
    fun testTrashEntityMapping() {
        val now = System.currentTimeMillis()
        val trashEntity = RecordingEntity(
            id = 99L,
            title = "Trashed Video",
            filePath = "/path/to/trashed.mp4",
            uriString = "content://trashed",
            durationMs = 120000L,
            sizeBytes = 20000000L,
            resolutionLabel = "720p",
            fps = 30,
            audioSource = "internal",
            createdAt = now - 1000000,
            isFavorite = false,
            isScreenshot = false,
            isDeleted = true,
            deletedAt = now
        )

        val domain = trashEntity.toDomainModel()
        assertTrue(domain.isDeleted)
        assertEquals(now, domain.deletedAt)

        val backToEntity = RecordingEntity.fromDomainModel(domain)
        assertTrue(backToEntity.isDeleted)
        assertEquals(now, backToEntity.deletedAt)
    }

    @Test
    fun testAutoStopOptions() {
        val timeOption = com.example.data.model.AutoStopTimeOption.MIN_15
        assertEquals(15, timeOption.minutes)

        val sizeOption = com.example.data.model.AutoStopFileSizeOption.GB_1
        assertEquals(1024L * 1024 * 1024, sizeOption.bytes)
    }
}
