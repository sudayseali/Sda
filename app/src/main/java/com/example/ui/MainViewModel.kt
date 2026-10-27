package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AudioSourceType
import com.example.data.model.CountdownOption
import com.example.data.model.FacecamShape
import com.example.data.model.FacecamSize
import com.example.data.model.RecordingItem
import com.example.data.model.RecordingSettings
import com.example.data.model.VideoBitrate
import com.example.data.model.VideoEncoderFormat
import com.example.data.model.VideoFps
import com.example.data.model.VideoOrientation
import com.example.data.model.VideoResolution
import com.example.data.repository.RecordingRepository
import com.example.data.repository.SettingsRepository
import com.example.service.FloatingControlService
import com.example.service.RecordingService
import com.example.service.RecordingState
import com.example.util.MediaUtils
import com.example.util.StorageUtils
import com.example.util.VideoEditorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val recordingRepo = RecordingRepository(application)
    private val settingsRepo = SettingsRepository(application)

    val recordingState: StateFlow<RecordingState> = RecordingService.recordingState
    val recordingDurationMs: StateFlow<Long> = RecordingService.recordingDurationMs

    val settings: StateFlow<RecordingSettings> = settingsRepo.settingsFlow

    val videoRecordings: StateFlow<List<RecordingItem>> = recordingRepo.videoRecordings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val screenshots: StateFlow<List<RecordingItem>> = recordingRepo.screenshots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashRecordings: StateFlow<List<RecordingItem>> = recordingRepo.trashRecordings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _storageInfo = MutableStateFlow(
        StorageUtils.getStorageInfo(application, settings.value.bitrate.value, 0L)
    )
    val storageInfo: StateFlow<StorageUtils.StorageInfo> = _storageInfo.asStateFlow()

    private val _selectedMedia = MutableStateFlow<RecordingItem?>(null)
    val selectedMedia: StateFlow<RecordingItem?> = _selectedMedia.asStateFlow()

    private val _countdownSeconds = MutableStateFlow<Int?>(null)
    val countdownSeconds: StateFlow<Int?> = _countdownSeconds.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        refreshStorageInfo()
        viewModelScope.launch {
            recordingRepo.syncWithFileSystem()
            refreshStorageInfo()
        }

        viewModelScope.launch {
            RecordingService.newMediaSavedEvent.collect {
                refreshStorageInfo()
                _userMessage.value = if (it.isScreenshot) "Screenshot captured!" else "Recording saved!"
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun refreshStorageInfo() {
        val totalBytes = videoRecordings.value.sumOf { it.sizeBytes } + screenshots.value.sumOf { it.sizeBytes }
        _storageInfo.value = StorageUtils.getStorageInfo(
            getApplication(),
            settings.value.bitrate.value,
            totalBytes
        )
    }

    private var isStartingCapture = false

    fun startCountdownAndCapture(onCountdownFinished: () -> Unit) {
        if (isStartingCapture || recordingState.value !is RecordingState.Idle) {
            return
        }
        isStartingCapture = true
        val countdown = settings.value.countdown.seconds
        if (countdown > 0) {
            viewModelScope.launch {
                for (sec in countdown downTo 1) {
                    _countdownSeconds.value = sec
                    delay(1000)
                }
                _countdownSeconds.value = null
                onCountdownFinished()
            }
        } else {
            _countdownSeconds.value = null
            onCountdownFinished()
        }
    }

    fun onRecordingCancelled() {
        isStartingCapture = false
        _countdownSeconds.value = null
    }

    fun startRecordingImmediately(resultCode: Int, resultData: Intent, screenWidth: Int, screenHeight: Int, densityDpi: Int) {
        isStartingCapture = false
        val context = getApplication<Application>()
        val serviceIntent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
            putExtra(RecordingService.EXTRA_RESULT_CODE, resultCode)
            putExtra(RecordingService.EXTRA_RESULT_DATA, resultData)
            putExtra(RecordingService.EXTRA_SCREEN_WIDTH, screenWidth)
            putExtra(RecordingService.EXTRA_SCREEN_HEIGHT, screenHeight)
            putExtra(RecordingService.EXTRA_SCREEN_DENSITY, densityDpi)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    fun pauseRecording() {
        val context = getApplication<Application>()
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_PAUSE
        }
        context.startService(intent)
    }

    fun resumeRecording() {
        val context = getApplication<Application>()
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_RESUME
        }
        context.startService(intent)
    }

    fun stopRecording() {
        val context = getApplication<Application>()
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun takeScreenshot() {
        val context = getApplication<Application>()
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_SCREENSHOT
        }
        context.startService(intent)
    }

    fun selectMediaForView(item: RecordingItem?) {
        _selectedMedia.value = item
    }

    fun renameMedia(item: RecordingItem, newTitle: String) {
        viewModelScope.launch {
            val success = recordingRepo.renameRecording(item.id, newTitle)
            if (success) {
                _userMessage.value = "Renamed successfully"
                refreshStorageInfo()
            } else {
                _userMessage.value = "Rename failed"
            }
        }
    }

    fun deleteMedia(item: RecordingItem) {
        viewModelScope.launch {
            val success = recordingRepo.deleteRecording(item)
            if (success) {
                if (_selectedMedia.value?.id == item.id) {
                    _selectedMedia.value = null
                }
                _userMessage.value = "Moved to Trash"
                refreshStorageInfo()
            } else {
                _userMessage.value = "Failed to move to Trash"
            }
        }
    }

    fun restoreFromTrash(item: RecordingItem) {
        viewModelScope.launch {
            recordingRepo.restoreFromTrash(item.id)
            _userMessage.value = "Restored: ${item.title}"
            refreshStorageInfo()
        }
    }

    fun deletePermanently(item: RecordingItem) {
        viewModelScope.launch {
            val success = recordingRepo.deletePermanently(item)
            if (success) {
                _userMessage.value = "Permanently deleted"
                refreshStorageInfo()
            } else {
                _userMessage.value = "Permanent deletion failed"
            }
        }
    }

    fun clearTrash() {
        viewModelScope.launch {
            val success = recordingRepo.clearTrash()
            if (success) {
                _userMessage.value = "Trash cleared"
                refreshStorageInfo()
            }
        }
    }

    fun trimVideo(item: RecordingItem, startMs: Long, endMs: Long, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val recordingsDir = StorageUtils.getRecordingsDirectory(getApplication())
                val destFile = java.io.File(recordingsDir, "TRIM_${System.currentTimeMillis()}_${item.title}.mp4")
                val success = com.example.util.VideoEditorUtils.trimVideo(
                    inputPath = item.filePath,
                    outputPath = destFile.absolutePath,
                    startMs = startMs,
                    endMs = endMs
                )
                if (success && destFile.exists() && destFile.length() > 0) {
                    val meta = com.example.util.MediaUtils.getVideoMetadata(destFile.absolutePath)
                    val trimmedItem = RecordingItem(
                        title = destFile.nameWithoutExtension,
                        filePath = destFile.absolutePath,
                        uriString = destFile.toURI().toString(),
                        durationMs = if (meta.durationMs > 0) meta.durationMs else (endMs - startMs),
                        sizeBytes = destFile.length(),
                        resolutionLabel = if (meta.width > 0) "${meta.width}x${meta.height}" else item.resolutionLabel,
                        fps = item.fps,
                        audioSource = item.audioSource,
                        createdAt = System.currentTimeMillis(),
                        isScreenshot = false
                    )
                    recordingRepo.insertRecording(trimmedItem)
                    refreshStorageInfo()
                    _userMessage.value = "Trimmed video saved successfully!"
                    onDone(true)
                } else {
                    _userMessage.value = "Failed to trim video"
                    onDone(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.value = "Error trimming video: ${e.message}"
                onDone(false)
            }
        }
    }

    fun extractAudio(item: RecordingItem, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val recordingsDir = StorageUtils.getRecordingsDirectory(getApplication())
                val destFile = java.io.File(recordingsDir, "AUDIO_${System.currentTimeMillis()}_${item.title}.m4a")
                val success = com.example.util.VideoEditorUtils.extractAudio(
                    inputPath = item.filePath,
                    outputPath = destFile.absolutePath
                )
                if (success && destFile.exists() && destFile.length() > 0) {
                    _userMessage.value = "Audio extracted: ${destFile.name}"
                    onDone(true)
                } else {
                    _userMessage.value = "Audio extraction failed (no audio track or unsupported codec)"
                    onDone(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.value = "Error extracting audio: ${e.message}"
                onDone(false)
            }
        }
    }

    fun toggleFavorite(item: RecordingItem) {
        viewModelScope.launch {
            recordingRepo.toggleFavorite(item.id, !item.isFavorite)
        }
    }

    fun saveMediaToGallery(item: RecordingItem, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = java.io.File(item.filePath)
                if (!file.exists()) {
                    _userMessage.value = "Faylka lama helin (File not found)"
                    onDone(false)
                    return@launch
                }
                val uri = if (item.isScreenshot) {
                    MediaUtils.saveImageToGallery(getApplication(), file, item.title)
                } else {
                    MediaUtils.saveVideoToGallery(getApplication(), file, item.title)
                }
                if (uri != null) {
                    _userMessage.value = if (item.isScreenshot) {
                        "Sawirka waxaa lagu keydiyay Gallery-ga!"
                    } else {
                        "Fiidiyowga waxaa lagu keydiyay Gallery-ga!"
                    }
                    onDone(true)
                } else {
                    _userMessage.value = "Ku keydinta Gallery-ga ma guuleysan (Failed to save to Gallery)"
                    onDone(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.value = "Khalad: ${e.message}"
                onDone(false)
            }
        }
    }

    fun setAutoSaveToGallery(enabled: Boolean) {
        settingsRepo.updateAutoSaveToGallery(enabled)
    }


    // Settings actions
    fun setResolution(res: VideoResolution) {
        settingsRepo.updateResolution(res)
        refreshStorageInfo()
    }

    fun setFps(fps: VideoFps) {
        settingsRepo.updateFps(fps)
    }

    fun setBitrate(bitrate: VideoBitrate) {
        settingsRepo.updateBitrate(bitrate)
        refreshStorageInfo()
    }

    fun setAudioSource(source: AudioSourceType) {
        settingsRepo.updateAudioSource(source)
    }

    fun setCountdown(option: CountdownOption) {
        settingsRepo.updateCountdown(option)
    }

    fun setFloatingControls(enabled: Boolean) {
        settingsRepo.updateFloatingControls(enabled)
    }

    fun setStopOnScreenOff(enabled: Boolean) {
        settingsRepo.updateStopOnScreenOff(enabled)
    }

    fun setVibrationFeedback(enabled: Boolean) {
        settingsRepo.updateVibrationFeedback(enabled)
    }

    fun setShakeToStop(enabled: Boolean) {
        settingsRepo.updateShakeToStop(enabled)
    }

    fun setFacecam(enabled: Boolean) {
        settingsRepo.updateFacecam(enabled)
    }

    fun setBrush(enabled: Boolean) {
        settingsRepo.updateBrush(enabled)
    }

    fun setShowTouches(enabled: Boolean) {
        settingsRepo.updateShowTouches(enabled)
    }

    fun setAutoStopTime(option: com.example.data.model.AutoStopTimeOption) {
        settingsRepo.updateAutoStopTime(option)
    }

    fun setAutoStopFileSize(option: com.example.data.model.AutoStopFileSizeOption) {
        settingsRepo.updateAutoStopFileSize(option)
    }

    fun setWatermark(enabled: Boolean, text: String? = null) {
        settingsRepo.updateWatermark(enabled, text)
    }

    fun setOrientation(orientation: VideoOrientation) {
        settingsRepo.updateOrientation(orientation)
    }

    fun setEncoderFormat(format: VideoEncoderFormat) {
        settingsRepo.updateEncoderFormat(format)
    }

    fun setMagicButton(enabled: Boolean) {
        settingsRepo.updateMagicButton(enabled)
    }

    fun setPauseOnCall(enabled: Boolean) {
        settingsRepo.updatePauseOnCall(enabled)
    }

    fun setFacecamShape(shape: FacecamShape) {
        settingsRepo.updateFacecamShape(shape)
    }

    fun setFacecamSize(size: FacecamSize) {
        settingsRepo.updateFacecamSize(size)
    }

    fun createGif(item: RecordingItem, startMs: Long, durationMs: Long, onDone: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val picturesDir = StorageUtils.getScreenshotsDirectory(getApplication())
                val destFile = java.io.File(picturesDir, "GIF_${System.currentTimeMillis()}_${item.title}.gif")
                val success = VideoEditorUtils.createGifFromVideo(
                    inputPath = item.filePath,
                    outputPath = destFile.absolutePath,
                    startMs = startMs,
                    durationMs = durationMs
                )
                if (success && destFile.exists() && destFile.length() > 0) {
                    val gifItem = RecordingItem(
                        title = destFile.nameWithoutExtension,
                        filePath = destFile.absolutePath,
                        uriString = destFile.toURI().toString(),
                        durationMs = durationMs,
                        sizeBytes = destFile.length(),
                        resolutionLabel = "GIF Animation",
                        fps = 10,
                        audioSource = "none",
                        createdAt = System.currentTimeMillis(),
                        isScreenshot = true
                    )
                    recordingRepo.insertRecording(gifItem)
                    if (settings.value.autoSaveToGallery) {
                        MediaUtils.saveImageToGallery(getApplication(), destFile, gifItem.title)
                    }
                    refreshStorageInfo()
                    _userMessage.value = "GIF si guul leh ayaa loo sameeyay! (GIF Created)"
                    onDone(true)
                } else {
                    _userMessage.value = "Sameynta GIF ma guuleysan (Failed to create GIF)"
                    onDone(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.value = "Khalad GIF: ${e.message}"
                onDone(false)
            }
        }
    }

    fun compressVideo(item: RecordingItem, onDone: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val recordingsDir = StorageUtils.getRecordingsDirectory(getApplication())
                val destFile = java.io.File(recordingsDir, "COMP_${System.currentTimeMillis()}_${item.title}.mp4")
                val success = VideoEditorUtils.compressVideo(
                    inputPath = item.filePath,
                    outputPath = destFile.absolutePath
                )
                if (success && destFile.exists() && destFile.length() > 0) {
                    val meta = MediaUtils.getVideoMetadata(destFile.absolutePath)
                    val compressedItem = RecordingItem(
                        title = destFile.nameWithoutExtension,
                        filePath = destFile.absolutePath,
                        uriString = destFile.toURI().toString(),
                        durationMs = if (meta.durationMs > 0) meta.durationMs else item.durationMs,
                        sizeBytes = destFile.length(),
                        resolutionLabel = if (meta.width > 0) "${meta.width}x${meta.height}" else item.resolutionLabel,
                        fps = item.fps,
                        audioSource = item.audioSource,
                        createdAt = System.currentTimeMillis(),
                        isScreenshot = false
                    )
                    recordingRepo.insertRecording(compressedItem)
                    if (settings.value.autoSaveToGallery) {
                        MediaUtils.saveVideoToGallery(getApplication(), destFile, compressedItem.title)
                    }
                    refreshStorageInfo()
                    _userMessage.value = "Fiidiyowga waa la cadaadiyay! (Compressed successfully)"
                    onDone(true)
                } else {
                    _userMessage.value = "Cadaadinta fiidiyowga ma guuleysan"
                    onDone(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.value = "Khalad: ${e.message}"
                onDone(false)
            }
        }
    }

    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(getApplication())
        } else {
            true
        }
    }
}
