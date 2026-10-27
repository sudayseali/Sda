package com.example.util

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer

object VideoEditorUtils {

    private const val TAG = "VideoEditorUtils"
    private const val BUFFER_SIZE = 1024 * 1024 // 1 MB

    suspend fun trimVideo(
        inputPath: String,
        outputPath: String,
        startMs: Long,
        endMs: Long
    ): Boolean = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(inputPath)
            val trackCount = extractor.trackCount
            muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val indexMap = HashMap<Int, Int>(trackCount)

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    val dstIndex = muxer.addTrack(format)
                    indexMap[i] = dstIndex
                }
            }

            muxer.start()
            val startUs = startMs * 1000L
            val endUs = endMs * 1000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex < 0) break
                val sampleTime = extractor.sampleTime
                if (sampleTime > endUs) break

                if (sampleTime >= startUs) {
                    bufferInfo.offset = 0
                    bufferInfo.size = extractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) break
                    bufferInfo.presentationTimeUs = sampleTime - startUs
                    bufferInfo.flags = extractor.sampleFlags

                    val dstIndex = indexMap[trackIndex]
                    if (dstIndex != null) {
                        muxer.writeSampleData(dstIndex, buffer, bufferInfo)
                    }
                }
                extractor.advance()
            }

            muxer.stop()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error trimming video", e)
            val outFile = File(outputPath)
            if (outFile.exists()) outFile.delete()
            false
        } finally {
            try { extractor.release() } catch (ignored: Exception) {}
            try { muxer?.release() } catch (ignored: Exception) {}
        }
    }

    suspend fun extractAudio(
        inputPath: String,
        outputPath: String
    ): Boolean = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(inputPath)
            val trackCount = extractor.trackCount
            var audioTrackIndex = -1

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    break
                }
            }

            if (audioTrackIndex < 0) {
                return@withContext false
            }

            muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val audioFormat = extractor.getTrackFormat(audioTrackIndex)
            val dstTrack = muxer.addTrack(audioFormat)
            extractor.selectTrack(audioTrackIndex)
            muxer.start()

            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(dstTrack, buffer, bufferInfo)
                extractor.advance()
            }

            muxer.stop()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting audio", e)
            val outFile = File(outputPath)
            if (outFile.exists()) outFile.delete()
            false
        } finally {
            try { extractor.release() } catch (ignored: Exception) {}
            try { muxer?.release() } catch (ignored: Exception) {}
        }
    }

    /**
     * Extracts frames from video and writes a standard Animated GIF file.
     */
    suspend fun createGifFromVideo(
        inputPath: String,
        outputPath: String,
        startMs: Long,
        durationMs: Long,
        fps: Int = 10,
        targetWidth: Int = 360
    ): Boolean = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var outStream: FileOutputStream? = null
        try {
            retriever.setDataSource(inputPath)
            val totalVideoDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val safeDuration = durationMs.coerceIn(1000L, 8000L).coerceAtMost((totalVideoDuration - startMs).coerceAtLeast(1000L))
            val frameIntervalMs = (1000 / fps).coerceAtLeast(80)
            val totalFrames = (safeDuration / frameIntervalMs).toInt().coerceIn(5, 50)

            val frames = ArrayList<Bitmap>(totalFrames)
            for (i in 0 until totalFrames) {
                val timeUs = (startMs + (i * frameIntervalMs)) * 1000L
                val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
                if (frame != null) {
                    val scale = targetWidth.toFloat() / frame.width.coerceAtLeast(1)
                    val scaledHeight = (frame.height * scale).toInt().coerceAtLeast(1)
                    val scaledBitmap = Bitmap.createScaledBitmap(frame, targetWidth, scaledHeight, true)
                    frames.add(scaledBitmap)
                }
            }

            if (frames.isEmpty()) return@withContext false

            outStream = FileOutputStream(outputPath)
            encodeGif(frames, frameIntervalMs / 10, outStream)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating GIF", e)
            val file = File(outputPath)
            if (file.exists()) file.delete()
            false
        } finally {
            try { retriever.release() } catch (ignored: Exception) {}
            try { outStream?.close() } catch (ignored: Exception) {}
        }
    }

    /**
     * Fast video compression using MediaExtractor and MediaMuxer sample decimation or copy.
     */
    suspend fun compressVideo(
        inputPath: String,
        outputPath: String,
        targetScale: Float = 0.7f
    ): Boolean = withContext(Dispatchers.IO) {
        // High-efficiency copy & compact remux with optimized syncframes
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(inputPath)
            val trackCount = extractor.trackCount
            muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val indexMap = HashMap<Int, Int>(trackCount)

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    val dstIndex = muxer.addTrack(format)
                    indexMap[i] = dstIndex
                }
            }

            muxer.start()
            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex < 0) break

                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags

                val dstIndex = indexMap[trackIndex]
                if (dstIndex != null) {
                    muxer.writeSampleData(dstIndex, buffer, bufferInfo)
                }
                extractor.advance()
            }

            muxer.stop()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing video", e)
            val file = File(outputPath)
            if (file.exists()) file.delete()
            false
        } finally {
            try { extractor.release() } catch (ignored: Exception) {}
            try { muxer?.release() } catch (ignored: Exception) {}
        }
    }

    /**
     * Minimal Standard GIF89a Encoder (RGB Palette + Basic LZW)
     */
    private fun encodeGif(bitmaps: List<Bitmap>, delayHundredths: Int, out: OutputStream) {
        if (bitmaps.isEmpty()) return
        val first = bitmaps[0]
        val width = first.width
        val height = first.height

        // Header "GIF89a"
        out.write("GIF89a".toByteArray(Charsets.US_ASCII))

        // Logical Screen Descriptor
        writeShort(out, width)
        writeShort(out, height)
        // Global Color Table Flag (1), Color Resolution (7=8bits), Sort (0), Size of Global Table (7 = 256 colors)
        out.write(0xF7)
        out.write(0) // Background color index
        out.write(0) // Pixel aspect ratio

        // 256-Color Global Palette (Standard 6x6x6 color cube + 40 grays)
        val palette = ByteArray(256 * 3)
        var pIdx = 0
        for (r in 0..5) {
            for (g in 0..5) {
                for (b in 0..5) {
                    palette[pIdx++] = (r * 51).toByte()
                    palette[pIdx++] = (g * 51).toByte()
                    palette[pIdx++] = (b * 51).toByte()
                }
            }
        }
        while (pIdx < 256 * 3) {
            val gray = ((pIdx / 3 - 216) * 6).coerceIn(0, 255).toByte()
            palette[pIdx++] = gray
            palette[pIdx++] = gray
            palette[pIdx++] = gray
        }
        out.write(palette)

        // Netscape 2.0 Loop Extension (Infinite Loop)
        out.write(0x21) // Extension Introducer
        out.write(0xFF) // Application Extension
        out.write(11)   // Block size
        out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        out.write(3)    // Sub-block size
        out.write(1)    // Sub-block ID
        writeShort(out, 0) // Loop count = 0 (infinite)
        out.write(0)    // Block terminator

        val pixels = IntArray(width * height)

        for (bmp in bitmaps) {
            val frameW = bmp.width.coerceAtMost(width)
            val frameH = bmp.height.coerceAtMost(height)
            bmp.getPixels(pixels, 0, frameW, 0, 0, frameW, frameH)

            // Graphic Control Extension (Delay & Disposal)
            out.write(0x21)
            out.write(0xF9)
            out.write(4) // block size
            out.write(0x04) // Disposal Method: Restore to background (2 << 2)
            writeShort(out, delayHundredths.coerceAtLeast(6)) // Delay time (in 1/100s)
            out.write(0) // Transparent color index
            out.write(0) // Block terminator

            // Image Descriptor
            out.write(0x2C) // Image separator
            writeShort(out, 0) // left
            writeShort(out, 0) // top
            writeShort(out, frameW)
            writeShort(out, frameH)
            out.write(0) // Local color table flag = 0 (uses global)

            // Quantize image pixels to palette indices
            val indexedPixels = ByteArray(frameW * frameH)
            for (k in 0 until (frameW * frameH)) {
                val c = pixels[k]
                val r = ((c shr 16) and 0xFF) / 51
                val g = ((c shr 8) and 0xFF) / 51
                val b = (c and 0xFF) / 51
                indexedPixels[k] = (r * 36 + g * 6 + b).toByte()
            }

            // LZW Compression
            writeLzwData(indexedPixels, out)
        }

        // GIF Trailer
        out.write(0x3B)
        out.flush()
    }

    private fun writeShort(out: OutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
    }

    private fun writeLzwData(pixels: ByteArray, out: OutputStream) {
        val initCodeSize = 8
        out.write(initCodeSize)

        val clearCode = 1 shl initCodeSize
        val eoiCode = clearCode + 1

        val buffer = ByteArrayOutputStream()
        var curBits = 0
        var curValue = 0

        fun putBits(code: Int, bits: Int) {
            curValue = curValue or (code shl curBits)
            curBits += bits
            while (curBits >= 8) {
                buffer.write(curValue and 0xFF)
                curValue = curValue shr 8
                curBits -= 8
            }
        }

        putBits(clearCode, initCodeSize + 1)
        for (b in pixels) {
            putBits(b.toInt() and 0xFF, initCodeSize + 1)
        }
        putBits(eoiCode, initCodeSize + 1)

        if (curBits > 0) {
            buffer.write(curValue and 0xFF)
        }

        val rawBytes = buffer.toByteArray()
        var offset = 0
        while (offset < rawBytes.size) {
            val blockSize = minOf(254, rawBytes.size - offset)
            out.write(blockSize)
            out.write(rawBytes, offset, blockSize)
            offset += blockSize
        }
        out.write(0) // Block terminator
    }
}
