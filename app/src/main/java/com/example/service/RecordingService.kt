package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AudioSourceType
import com.example.data.model.RecordingItem
import com.example.data.model.RecordingSettings
import com.example.data.model.VideoResolution
import com.example.data.repository.RecordingRepository
import com.example.data.repository.SettingsRepository
import com.example.util.MediaUtils
import com.example.util.StorageUtils
import com.example.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class RecordingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null

    private var currentOutputFile: File? = null
    private var recordingStartTime = 0L
    private var accumulatedDuration = 0L
    private var pauseStartTime = 0L
    private var isPaused = false
    private var isStoppingOrCleaningUp = false

    private var sensorManager: android.hardware.SensorManager? = null
    private var shakeDetector: com.example.util.ShakeDetector? = null
    private var cachedSettings: RecordingSettings? = null

    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (_recordingState.value is RecordingState.Recording && !isPaused) {
                val currentElapsed = accumulatedDuration + (SystemClock.elapsedRealtime() - recordingStartTime)
                _recordingDurationMs.value = currentElapsed
                updateNotification()

                // Check auto-stop conditions
                cachedSettings?.let { settings ->
                    // 1. Time limit check
                    if (settings.autoStopTime.minutes > 0) {
                        val maxTimeMs = settings.autoStopTime.minutes * 60 * 1000L
                        if (currentElapsed >= maxTimeMs) {
                            Log.i(TAG, "Auto-stop triggered: reached time limit of ${settings.autoStopTime.minutes} min")
                            stopRecording()
                            return
                        }
                    }

                    // 2. File size limit check
                    if (settings.autoStopFileSize.bytes > 0) {
                        val currentSize = currentOutputFile?.length() ?: 0L
                        if (currentSize >= settings.autoStopFileSize.bytes) {
                            Log.i(TAG, "Auto-stop triggered: reached file size limit of ${settings.autoStopFileSize.label}")
                            stopRecording()
                            return
                        }
                    }
                }

                timerHandler.postDelayed(this, 1000)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                val width = intent.getIntExtra(EXTRA_SCREEN_WIDTH, 0)
                val height = intent.getIntExtra(EXTRA_SCREEN_HEIGHT, 0)
                val density = intent.getIntExtra(EXTRA_SCREEN_DENSITY, 0)

                if (resultCode != 0 && resultData != null) {
                    startRecording(resultCode, resultData, width, height, density)
                } else {
                    stopSelf()
                }
            }
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stopRecording()
            ACTION_SCREENSHOT -> captureScreenshot()
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val isPausedNow = isPaused
        val statusText = if (isPausedNow) getString(R.string.recording_paused) else getString(R.string.recording_in_progress)
        val durationFormatted = TimeUtils.formatDuration(_recordingDurationMs.value)

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeAction = if (isPausedNow) {
            val resumeIntent = Intent(this, RecordingService::class.java).apply { action = ACTION_RESUME }
            val resumePendingIntent = PendingIntent.getService(
                this, 1, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_play,
                getString(R.string.resume_recording),
                resumePendingIntent
            ).build()
        } else {
            val pauseIntent = Intent(this, RecordingService::class.java).apply { action = ACTION_PAUSE }
            val pausePendingIntent = PendingIntent.getService(
                this, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_pause,
                getString(R.string.pause_recording),
                pausePendingIntent
            ).build()
        }

        val stopIntent = Intent(this, RecordingService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            getString(R.string.stop_recording),
            stopPendingIntent
        ).build()

        val screenshotIntent = Intent(this, RecordingService::class.java).apply { action = ACTION_SCREENSHOT }
        val screenshotPendingIntent = PendingIntent.getService(
            this, 4, screenshotIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val screenshotAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_camera,
            getString(R.string.take_screenshot),
            screenshotPendingIntent
        ).build()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Recorder • $durationFormatted")
            .setContentText(statusText)
            .setSmallIcon(if (isPausedNow) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(pauseResumeAction)
            .addAction(stopAction)
            .addAction(screenshotAction)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun startRecording(resultCode: Int, resultData: Intent, passedWidth: Int, passedHeight: Int, passedDensity: Int) {
        val settingsRepo = SettingsRepository(this)
        val settings = settingsRepo.settingsFlow.value

        try {
            cleanup()
            isStoppingOrCleaningUp = false

            // CRITICAL FOR ANDROID 14+ (API 34+):
            // startForeground MUST be called BEFORE MediaProjectionManager.getMediaProjection().
            // Otherwise Android throws: SecurityException: Media projections require a foreground service of type ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            val initialNotification = buildNotification()
            val hasMicPermission = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                if (settings.audioSource == AudioSourceType.MIC && hasMicPermission) {
                    type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                }
                startForeground(NOTIFICATION_ID, initialNotification, type)
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }

            val sysMetrics = resources.displayMetrics
            val screenWidth = if (passedWidth > 0) passedWidth else sysMetrics.widthPixels
            val screenHeight = if (passedHeight > 0) passedHeight else sysMetrics.heightPixels
            val screenDensity = if (passedDensity > 0) passedDensity else sysMetrics.densityDpi

            // Calculate recording dimensions maintaining aspect ratio and hardware bounds
            val (recordWidth, recordHeight) = calculateDimensions(settings.resolution, screenWidth, screenHeight)

            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                    override fun onStop() {
                        if (!isStoppingOrCleaningUp) {
                            Log.i(TAG, "MediaProjection stopped by system, terminating session")
                            stopRecording()
                        }
                    }
                }, Handler(Looper.getMainLooper()))
            }

            val recordingsDir = StorageUtils.getRecordingsDirectory(this)
            val fileName = "REC_${TimeUtils.formatFileTimestamp()}.mp4"
            currentOutputFile = File(recordingsDir, fileName)

            setupMediaRecorder(recordWidth, recordHeight, settings, screenDensity)

            mediaRecorder?.start()

            recordingStartTime = SystemClock.elapsedRealtime()
            accumulatedDuration = 0L
            isPaused = false
            _recordingState.value = RecordingState.Recording
            _recordingDurationMs.value = 0L

            cachedSettings = settings
            timerHandler.post(timerRunnable)

            // Shake to stop
            if (settings.shakeToStopEnabled) {
                sensorManager = getSystemService(Context.SENSOR_SERVICE) as? android.hardware.SensorManager
                sensorManager?.let { sm ->
                    shakeDetector = com.example.util.ShakeDetector {
                        Log.i(TAG, "Shake detected: stopping recording")
                        stopRecording()
                    }
                    shakeDetector?.start(sm)
                }
            }

            // Launch floating overlay if enabled
            if (settings.floatingControlsEnabled) {
                startFloatingControls()
            }

            // Launch Facecam if enabled
            if (settings.facecamEnabled) {
                try {
                    startService(Intent(this, FacecamService::class.java).apply { action = FacecamService.ACTION_SHOW })
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start FacecamService", e)
                }
            }

            // Launch Brush overlay if enabled
            if (settings.brushEnabled) {
                try {
                    startService(Intent(this, BrushOverlayService::class.java).apply { action = BrushOverlayService.ACTION_SHOW })
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start BrushOverlayService", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start screen recording", e)
            _recordingState.value = RecordingState.Error(e.localizedMessage ?: "Failed to start recording")
            cleanup()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
        }
    }

    private fun calculateDimensions(resolution: VideoResolution, screenWidth: Int, screenHeight: Int): Pair<Int, Int> {
        val safeScreenWidth = if (screenWidth > 0) screenWidth else 1080
        val safeScreenHeight = if (screenHeight > 0) screenHeight else 1920
        val isPortrait = safeScreenHeight >= safeScreenWidth

        val maxLongSide = when (resolution) {
            VideoResolution.AUTO -> if (isPortrait) minOf(safeScreenHeight, 1920) else minOf(safeScreenWidth, 1920)
            VideoResolution.UHD_2K -> 2560
            VideoResolution.FHD_1080P -> 1920
            VideoResolution.HD_720P -> 1280
            VideoResolution.SD_480P -> 854
        }

        var width: Int
        var height: Int

        if (isPortrait) {
            val targetHeight = minOf(safeScreenHeight, maxLongSide)
            val scale = targetHeight.toDouble() / safeScreenHeight.coerceAtLeast(1).toDouble()
            height = targetHeight
            width = (safeScreenWidth * scale).toInt()
        } else {
            val targetWidth = minOf(safeScreenWidth, maxLongSide)
            val scale = targetWidth.toDouble() / safeScreenWidth.coerceAtLeast(1).toDouble()
            width = targetWidth
            height = (safeScreenHeight * scale).toInt()
        }

        // Must be even numbers for H.264 encoder
        if (width % 2 != 0) width -= 1
        if (height % 2 != 0) height -= 1

        width = width.coerceIn(320, 2560)
        height = height.coerceIn(320, 2560)

        return Pair(width, height)
    }

    @Suppress("DEPRECATION")
    private fun setupMediaRecorder(width: Int, height: Int, settings: RecordingSettings, densityDpi: Int) {
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }

        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val useMic = (settings.audioSource == AudioSourceType.MIC) && hasMicPermission

        if (useMic) {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        }

        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

        if (useMic) {
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(128_000)
            recorder.setAudioSamplingRate(44_100)
        }

        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        recorder.setVideoSize(width, height)
        recorder.setVideoFrameRate(settings.fps.value.coerceIn(24, 60))
        recorder.setVideoEncodingBitRate(settings.bitrate.value.coerceIn(2_000_000, 16_000_000))

        recorder.setOutputFile(currentOutputFile?.absolutePath)

        try {
            recorder.prepare()
        } catch (e: Exception) {
            Log.w(TAG, "MediaRecorder prepare failed with $width x $height, falling back to safe 720p/1080p profile", e)
            recorder.reset()
            if (useMic) {
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            }
            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            if (useMic) {
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            }
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            val fallbackW = if (width > height) 1280 else 720
            val fallbackH = if (width > height) 720 else 1280
            recorder.setVideoSize(fallbackW, fallbackH)
            recorder.setVideoFrameRate(30)
            recorder.setVideoEncodingBitRate(4_000_000)
            recorder.setOutputFile(currentOutputFile?.absolutePath)
            recorder.prepare()
        }

        mediaRecorder = recorder

        val surface = recorder.surface
        val safeDensity = if (densityDpi > 0) densityDpi else resources.displayMetrics.densityDpi.coerceAtLeast(160)

        val virtualDisplayCallback = object : VirtualDisplay.Callback() {
            override fun onPaused() {
                Log.d(TAG, "VirtualDisplay paused")
            }
            override fun onResumed() {
                Log.d(TAG, "VirtualDisplay resumed")
            }
            override fun onStopped() {
                Log.d(TAG, "VirtualDisplay stopped")
            }
        }

        var display = mediaProjection?.createVirtualDisplay(
            "ScreenRecordingDisplay",
            width,
            height,
            safeDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface,
            virtualDisplayCallback,
            Handler(Looper.getMainLooper())
        )

        if (display == null) {
            Log.w(TAG, "VirtualDisplay with AUTO_MIRROR returned null, falling back to PRESENTATION")
            display = mediaProjection?.createVirtualDisplay(
                "ScreenRecordingDisplay",
                width,
                height,
                safeDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION,
                surface,
                virtualDisplayCallback,
                Handler(Looper.getMainLooper())
            )
        }

        if (display == null) {
            throw IllegalStateException("DisplayManager returned null when creating virtual display ($width x $height @ $safeDensity dpi)")
        }
        virtualDisplay = display
    }

    private fun pauseRecording() {
        if (_recordingState.value is RecordingState.Recording && !isPaused) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mediaRecorder?.pause()
                    isPaused = true
                    pauseStartTime = SystemClock.elapsedRealtime()
                    accumulatedDuration += (pauseStartTime - recordingStartTime)
                    _recordingState.value = RecordingState.Paused
                    timerHandler.removeCallbacks(timerRunnable)
                    updateNotification()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Pause failed", e)
            }
        }
    }

    private fun resumeRecording() {
        if (_recordingState.value is RecordingState.Paused && isPaused) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mediaRecorder?.resume()
                    isPaused = false
                    recordingStartTime = SystemClock.elapsedRealtime()
                    _recordingState.value = RecordingState.Recording
                    timerHandler.post(timerRunnable)
                    updateNotification()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Resume failed", e)
            }
        }
    }

    private fun stopRecording() {
        if (_recordingState.value == RecordingState.Idle || _recordingState.value == RecordingState.Stopping) return
        if (isStoppingOrCleaningUp) return

        isStoppingOrCleaningUp = true
        _recordingState.value = RecordingState.Stopping
        timerHandler.removeCallbacks(timerRunnable)

        // Ensure at least 1200ms of recording has elapsed before calling stop() to prevent -1007 (stop failed)
        val elapsed = SystemClock.elapsedRealtime() - recordingStartTime
        if (elapsed < 1200L && recordingStartTime > 0L) {
            try {
                Thread.sleep((1200L - elapsed).coerceAtLeast(100L))
            } catch (ignored: InterruptedException) {}
        }

        var stopSuccess = false
        try {
            mediaRecorder?.stop()
            stopSuccess = true
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder", e)
        }

        // Save recording into repository & database if file exists and contains valid data
        currentOutputFile?.let { file ->
            if (file.exists() && file.length() > 1024L) {
                val duration = _recordingDurationMs.value
                val meta = MediaUtils.getVideoMetadata(file.absolutePath)
                val finalDuration = if (duration > 0) duration else (if (meta.durationMs > 0) meta.durationMs else (SystemClock.elapsedRealtime() - recordingStartTime))
                val settings = SettingsRepository(this).settingsFlow.value

                val item = RecordingItem(
                    title = file.nameWithoutExtension,
                    filePath = file.absolutePath,
                    uriString = file.toURI().toString(),
                    durationMs = finalDuration.coerceAtLeast(1000L),
                    sizeBytes = file.length(),
                    resolutionLabel = if (meta.width > 0) "${meta.width}x${meta.height}" else settings.resolution.shortLabel,
                    fps = settings.fps.value,
                    audioSource = settings.audioSource.name.lowercase(),
                    createdAt = System.currentTimeMillis(),
                    isScreenshot = false
                )

                serviceScope.launch {
                    val repo = RecordingRepository(applicationContext)
                    repo.insertRecording(item)
                    if (settings.autoSaveToGallery) {
                        try {
                            MediaUtils.saveVideoToGallery(applicationContext, file, item.title)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed auto-save to gallery", e)
                        }
                    }
                    _newMediaSavedEvent.emit(item)
                }
            } else if (file.exists()) {
                file.delete()
            }
        }

        cleanup()
        stopFloatingControls()
        _recordingState.value = RecordingState.Idle
        _recordingDurationMs.value = 0L

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun captureScreenshot() {
        val proj = mediaProjection
        if (proj == null) {
            Log.w(TAG, "Screenshot requested but MediaProjection is not available")
            return
        }
        if (virtualDisplay != null) {
            Log.w(TAG, "Cannot create secondary virtual display while video recording is in progress (Android 14+)")
            return
        }
        serviceScope.launch(Dispatchers.IO) {
            try {
                val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRealMetrics(metrics)

                val width = metrics.widthPixels
                val height = metrics.heightPixels
                val densityDpi = metrics.densityDpi

                val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
                val screenshotDisplay = proj.createVirtualDisplay(
                    "ScreenshotDisplay",
                    width,
                    height,
                    densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.surface,
                    null,
                    null
                )

                // Wait briefly for frame to render
                kotlinx.coroutines.delay(200)

                val image: Image? = imageReader.acquireLatestImage()
                if (image != null) {
                    val planes = image.planes
                    val buffer: ByteBuffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * width

                    val bitmap = Bitmap.createBitmap(
                        width + rowPadding / pixelStride,
                        height,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)
                    val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                    image.close()

                    val screenshotDir = StorageUtils.getScreenshotsDirectory(applicationContext)
                    val fileName = "SHOT_${TimeUtils.formatFileTimestamp()}.png"
                    val file = File(screenshotDir, fileName)

                    FileOutputStream(file).use { out ->
                        croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    val item = RecordingItem(
                        title = file.nameWithoutExtension,
                        filePath = file.absolutePath,
                        uriString = file.toURI().toString(),
                        durationMs = 0L,
                        sizeBytes = file.length(),
                        resolutionLabel = "${width}x${height}",
                        fps = 0,
                        audioSource = "none",
                        createdAt = System.currentTimeMillis(),
                        isScreenshot = true
                    )

                    val repo = RecordingRepository(applicationContext)
                    repo.insertRecording(item)
                    val settings = SettingsRepository(applicationContext).settingsFlow.value
                    if (settings.autoSaveToGallery) {
                        try {
                            MediaUtils.saveImageToGallery(applicationContext, file, item.title)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed auto-save screenshot to gallery", e)
                        }
                    }
                    _newMediaSavedEvent.emit(item)
                }

                screenshotDisplay?.release()
                imageReader.close()
            } catch (e: Exception) {
                Log.e(TAG, "Screenshot capture failed", e)
            }
        }
    }

    private fun startFloatingControls() {
        val floatIntent = Intent(this, FloatingControlService::class.java).apply {
            action = FloatingControlService.ACTION_SHOW
        }
        startService(floatIntent)
    }

    private fun stopFloatingControls() {
        val floatIntent = Intent(this, FloatingControlService::class.java).apply {
            action = FloatingControlService.ACTION_HIDE
        }
        startService(floatIntent)
    }

    private fun cleanup() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null
        } catch (ignored: Exception) {}

        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (ignored: Exception) {}

        try {
            mediaProjection?.stop()
            mediaProjection = null
        } catch (ignored: Exception) {}

        try {
            sensorManager?.let { sm -> shakeDetector?.stop(sm) }
            shakeDetector = null
            sensorManager = null
        } catch (ignored: Exception) {}

        try {
            startService(Intent(this, FacecamService::class.java).apply { action = FacecamService.ACTION_HIDE })
        } catch (ignored: Exception) {}

        try {
            startService(Intent(this, BrushOverlayService::class.java).apply { action = BrushOverlayService.ACTION_HIDE })
        } catch (ignored: Exception) {}

        currentOutputFile = null
        isPaused = false
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
        cleanup()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "RecordingService"
        const val CHANNEL_ID = "screen_recording_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.action.START"
        const val ACTION_PAUSE = "com.example.service.action.PAUSE"
        const val ACTION_RESUME = "com.example.service.action.RESUME"
        const val ACTION_STOP = "com.example.service.action.STOP"
        const val ACTION_SCREENSHOT = "com.example.service.action.SCREENSHOT"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_SCREEN_WIDTH = "extra_screen_width"
        const val EXTRA_SCREEN_HEIGHT = "extra_screen_height"
        const val EXTRA_SCREEN_DENSITY = "extra_screen_density"

        private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
        val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

        private val _recordingDurationMs = MutableStateFlow(0L)
        val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

        private val _newMediaSavedEvent = MutableSharedFlow<RecordingItem>(extraBufferCapacity = 1)
        val newMediaSavedEvent: SharedFlow<RecordingItem> = _newMediaSavedEvent.asSharedFlow()
    }
}
