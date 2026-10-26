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
import com.example.data.model.RecordingItem
import com.example.data.model.RecordingSettings
import com.example.data.model.VideoBitrate
import com.example.data.model.VideoFps
import com.example.data.model.VideoResolution
import com.example.data.repository.RecordingRepository
import com.example.data.repository.SettingsRepository
import com.example.service.FloatingControlService
import com.example.service.RecordingService
import com.example.service.RecordingState
import com.example.util.StorageUtils
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

    fun startCountdownAndCapture(onCountdownFinished: () -> Unit) {
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
            onCountdownFinished()
        }
    }

    fun startRecordingImmediately(resultCode: Int, resultData: Intent, screenWidth: Int, screenHeight: Int, densityDpi: Int) {
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
                _userMessage.value = "Deleted successfully"
                refreshStorageInfo()
            } else {
                _userMessage.value = "Delete failed"
            }
        }
    }

    fun toggleFavorite(item: RecordingItem) {
        viewModelScope.launch {
            recordingRepo.toggleFavorite(item.id, !item.isFavorite)
        }
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

    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(getApplication())
        } else {
            true
        }
    }
}
