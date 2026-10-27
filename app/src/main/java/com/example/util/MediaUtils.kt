package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object MediaUtils {

    data class VideoMeta(
        val durationMs: Long,
        val width: Int,
        val height: Int
    )

    fun getVideoMetadata(filePath: String): VideoMeta {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)

            val duration = durationStr?.toLongOrNull() ?: 0L
            val width = widthStr?.toIntOrNull() ?: 0
            val height = heightStr?.toIntOrNull() ?: 0
            VideoMeta(duration, width, height)
        } catch (e: Exception) {
            VideoMeta(0L, 0, 0)
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }
    }

    fun getVideoThumbnail(context: Context, file: File): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }
    }

    fun shareMedia(context: Context, filePath: String, isScreenshot: Boolean) {
        try {
            val file = File(filePath)
            if (!file.exists()) return

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val mimeType = if (isScreenshot) "image/png" else "video/mp4"

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share recording via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Saves a recorded video into the system Gallery / MediaStore (Movies/ScreenRecorder)
     * so it immediately appears in Google Photos / Gallery apps.
     */
    fun saveVideoToGallery(context: Context, videoFile: File, title: String? = null): Uri? {
        if (!videoFile.exists() || videoFile.length() == 0L) {
            Log.w("MediaUtils", "Cannot save to gallery: file is missing or empty")
            return null
        }

        val resolver = context.contentResolver
        val baseName = if (!title.isNullOrBlank()) title.trim() else videoFile.nameWithoutExtension
        val displayName = if (baseName.endsWith(".mp4", ignoreCase = true)) baseName else "$baseName.mp4"

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.TITLE, baseName.removeSuffix(".mp4"))
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/ScreenRecorder")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        var insertedUri: Uri? = null
        return try {
            insertedUri = resolver.insert(collection, contentValues)
            if (insertedUri != null) {
                resolver.openOutputStream(insertedUri)?.use { outStream ->
                    FileInputStream(videoFile).use { inStream ->
                        inStream.copyTo(outStream)
                    }
                } ?: throw IOException("Could not open output stream for $insertedUri")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(insertedUri, contentValues, null, null)
                }

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(videoFile.absolutePath),
                    arrayOf("video/mp4"),
                    null
                )
            }
            insertedUri
        } catch (e: Exception) {
            Log.e("MediaUtils", "Error saving video to gallery", e)
            if (insertedUri != null) {
                try {
                    resolver.delete(insertedUri, null, null)
                } catch (delEx: Exception) {}
            }
            null
        }
    }

    /**
     * Saves a screenshot into the system Gallery / MediaStore (Pictures/ScreenRecorder)
     */
    fun saveImageToGallery(context: Context, imageFile: File, title: String? = null): Uri? {
        if (!imageFile.exists() || imageFile.length() == 0L) {
            Log.w("MediaUtils", "Cannot save to gallery: image file missing or empty")
            return null
        }

        val resolver = context.contentResolver
        val baseName = if (!title.isNullOrBlank()) title.trim() else imageFile.nameWithoutExtension
        val displayName = if (baseName.endsWith(".png", ignoreCase = true)) baseName else "$baseName.png"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.TITLE, baseName.removeSuffix(".png"))
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ScreenRecorder")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        var insertedUri: Uri? = null
        return try {
            insertedUri = resolver.insert(collection, contentValues)
            if (insertedUri != null) {
                resolver.openOutputStream(insertedUri)?.use { outStream ->
                    FileInputStream(imageFile).use { inStream ->
                        inStream.copyTo(outStream)
                    }
                } ?: throw IOException("Could not open output stream for $insertedUri")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(insertedUri, contentValues, null, null)
                }

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(imageFile.absolutePath),
                    arrayOf("image/png"),
                    null
                )
            }
            insertedUri
        } catch (e: Exception) {
            Log.e("MediaUtils", "Error saving image to gallery", e)
            if (insertedUri != null) {
                try {
                    resolver.delete(insertedUri, null, null)
                } catch (delEx: Exception) {}
            }
            null
        }
    }
}

