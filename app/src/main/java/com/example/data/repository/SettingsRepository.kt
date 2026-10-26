package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AudioSourceType
import com.example.data.model.CountdownOption
import com.example.data.model.RecordingSettings
import com.example.data.model.VideoBitrate
import com.example.data.model.VideoFps
import com.example.data.model.VideoResolution
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("screen_recorder_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<RecordingSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): RecordingSettings {
        val resName = prefs.getString("resolution", VideoResolution.FHD_1080P.name) ?: VideoResolution.FHD_1080P.name
        val fpsName = prefs.getString("fps", VideoFps.FPS_60.name) ?: VideoFps.FPS_60.name
        val bitrateName = prefs.getString("bitrate", VideoBitrate.BITRATE_8M.name) ?: VideoBitrate.BITRATE_8M.name
        val audioName = prefs.getString("audio_source", AudioSourceType.MIC.name) ?: AudioSourceType.MIC.name
        val countdownName = prefs.getString("countdown", CountdownOption.SEC_3.name) ?: CountdownOption.SEC_3.name
        val floating = prefs.getBoolean("floating_controls", true)
        val stopScreenOff = prefs.getBoolean("stop_screen_off", true)
        val vibration = prefs.getBoolean("vibration_feedback", true)

        val resolution = try { VideoResolution.valueOf(resName) } catch (e: Exception) { VideoResolution.FHD_1080P }
        val fps = try { VideoFps.valueOf(fpsName) } catch (e: Exception) { VideoFps.FPS_60 }
        val bitrate = try { VideoBitrate.valueOf(bitrateName) } catch (e: Exception) { VideoBitrate.BITRATE_8M }
        val audioSource = try { AudioSourceType.valueOf(audioName) } catch (e: Exception) { AudioSourceType.MIC }
        val countdown = try { CountdownOption.valueOf(countdownName) } catch (e: Exception) { CountdownOption.SEC_3 }

        return RecordingSettings(
            resolution = resolution,
            fps = fps,
            bitrate = bitrate,
            audioSource = audioSource,
            countdown = countdown,
            floatingControlsEnabled = floating,
            stopOnScreenOff = stopScreenOff,
            vibrationFeedback = vibration
        )
    }

    fun updateResolution(resolution: VideoResolution) {
        prefs.edit().putString("resolution", resolution.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(resolution = resolution)
    }

    fun updateFps(fps: VideoFps) {
        prefs.edit().putString("fps", fps.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(fps = fps)
    }

    fun updateBitrate(bitrate: VideoBitrate) {
        prefs.edit().putString("bitrate", bitrate.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(bitrate = bitrate)
    }

    fun updateAudioSource(audioSource: AudioSourceType) {
        prefs.edit().putString("audio_source", audioSource.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(audioSource = audioSource)
    }

    fun updateCountdown(countdown: CountdownOption) {
        prefs.edit().putString("countdown", countdown.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(countdown = countdown)
    }

    fun updateFloatingControls(enabled: Boolean) {
        prefs.edit().putBoolean("floating_controls", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(floatingControlsEnabled = enabled)
    }

    fun updateStopOnScreenOff(enabled: Boolean) {
        prefs.edit().putBoolean("stop_screen_off", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(stopOnScreenOff = enabled)
    }

    fun updateVibrationFeedback(enabled: Boolean) {
        prefs.edit().putBoolean("vibration_feedback", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(vibrationFeedback = enabled)
    }
}
