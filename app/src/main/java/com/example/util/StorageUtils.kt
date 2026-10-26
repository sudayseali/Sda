package com.example.util

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.util.Locale

object StorageUtils {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    data class StorageInfo(
        val freeBytes: Long,
        val totalBytes: Long,
        val usedBytesByApp: Long,
        val estimatedMinutesRemaining: Int
    )

    fun getStorageInfo(context: Context, bitrateBps: Int, appRecordingsTotalBytes: Long): StorageInfo {
        return try {
            val path = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
            val stat = StatFs(path.path)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            val totalBytes = stat.blockCountLong * stat.blockSizeLong

            // Calculate estimated minutes remaining based on current bitrate
            // bitrate is bits per second, so bytes per second = bitrateBps / 8
            val bytesPerSecond = if (bitrateBps > 0) (bitrateBps / 8).coerceAtLeast(100_000) else 1_000_000
            val estimatedSeconds = (availableBytes / bytesPerSecond).toInt()
            val estimatedMinutes = (estimatedSeconds / 60).coerceAtLeast(0)

            StorageInfo(
                freeBytes = availableBytes,
                totalBytes = totalBytes,
                usedBytesByApp = appRecordingsTotalBytes,
                estimatedMinutesRemaining = estimatedMinutes
            )
        } catch (e: Exception) {
            StorageInfo(
                freeBytes = 10L * 1024 * 1024 * 1024,
                totalBytes = 64L * 1024 * 1024 * 1024,
                usedBytesByApp = appRecordingsTotalBytes,
                estimatedMinutesRemaining = 120
            )
        }
    }

    fun getRecordingsDirectory(context: Context): File {
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: File(context.filesDir, "Recordings")
        val recordingsDir = File(moviesDir, "ScreenRecordings")
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }
        return recordingsDir
    }

    fun getScreenshotsDirectory(context: Context): File {
        val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: File(context.filesDir, "Screenshots")
        val screenshotsDir = File(picturesDir, "ScreenRecorder")
        if (!screenshotsDir.exists()) {
            screenshotsDir.mkdirs()
        }
        return screenshotsDir
    }
}
