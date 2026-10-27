package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AudioSourceType
import com.example.data.model.AutoStopFileSizeOption
import com.example.data.model.AutoStopTimeOption
import com.example.data.model.CountdownOption
import com.example.data.model.FacecamShape
import com.example.data.model.FacecamSize
import com.example.data.model.RecordingSettings
import com.example.data.model.VideoBitrate
import com.example.data.model.VideoEncoderFormat
import com.example.data.model.VideoFps
import com.example.data.model.VideoOrientation
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
        val orientationName = prefs.getString("orientation", VideoOrientation.AUTO.name) ?: VideoOrientation.AUTO.name
        val encoderName = prefs.getString("encoder_format", VideoEncoderFormat.H264.name) ?: VideoEncoderFormat.H264.name
        val fpsName = prefs.getString("fps", VideoFps.FPS_60.name) ?: VideoFps.FPS_60.name
        val bitrateName = prefs.getString("bitrate", VideoBitrate.BITRATE_8M.name) ?: VideoBitrate.BITRATE_8M.name
        val audioName = prefs.getString("audio_source", AudioSourceType.MIC.name) ?: AudioSourceType.MIC.name
        val countdownName = prefs.getString("countdown", CountdownOption.SEC_3.name) ?: CountdownOption.SEC_3.name
        val floating = prefs.getBoolean("floating_controls", true)
        val magicButton = prefs.getBoolean("magic_button", false)
        val stopScreenOff = prefs.getBoolean("stop_screen_off", true)
        val pauseOnCall = prefs.getBoolean("pause_on_call", true)
        val vibration = prefs.getBoolean("vibration_feedback", true)
        val shakeToStop = prefs.getBoolean("shake_to_stop", false)
        val facecam = prefs.getBoolean("facecam_enabled", false)
        val facecamShapeName = prefs.getString("facecam_shape", FacecamShape.CIRCLE.name) ?: FacecamShape.CIRCLE.name
        val facecamSizeName = prefs.getString("facecam_size", FacecamSize.MEDIUM.name) ?: FacecamSize.MEDIUM.name
        val brush = prefs.getBoolean("brush_enabled", false)
        val showTouches = prefs.getBoolean("show_touches", false)
        val autoStopTimeStr = prefs.getString("auto_stop_time", AutoStopTimeOption.UNLIMITED.name) ?: AutoStopTimeOption.UNLIMITED.name
        val autoStopFileSizeStr = prefs.getString("auto_stop_file_size", AutoStopFileSizeOption.UNLIMITED.name) ?: AutoStopFileSizeOption.UNLIMITED.name
        val watermark = prefs.getBoolean("watermark_enabled", false)
        val watermarkText = prefs.getString("watermark_text", "Screen Recorder") ?: "Screen Recorder"
        val autoSaveToGallery = prefs.getBoolean("auto_save_to_gallery", true)

        val resolution = try { VideoResolution.valueOf(resName) } catch (e: Exception) { VideoResolution.FHD_1080P }
        val orientation = try { VideoOrientation.valueOf(orientationName) } catch (e: Exception) { VideoOrientation.AUTO }
        val encoderFormat = try { VideoEncoderFormat.valueOf(encoderName) } catch (e: Exception) { VideoEncoderFormat.H264 }
        val fps = try { VideoFps.valueOf(fpsName) } catch (e: Exception) { VideoFps.FPS_60 }
        val bitrate = try { VideoBitrate.valueOf(bitrateName) } catch (e: Exception) { VideoBitrate.BITRATE_8M }
        val audioSource = try { AudioSourceType.valueOf(audioName) } catch (e: Exception) { AudioSourceType.MIC }
        val countdown = try { CountdownOption.valueOf(countdownName) } catch (e: Exception) { CountdownOption.SEC_3 }
        val autoStopTime = try { AutoStopTimeOption.valueOf(autoStopTimeStr) } catch (e: Exception) { AutoStopTimeOption.UNLIMITED }
        val autoStopFileSize = try { AutoStopFileSizeOption.valueOf(autoStopFileSizeStr) } catch (e: Exception) { AutoStopFileSizeOption.UNLIMITED }
        val facecamShape = try { FacecamShape.valueOf(facecamShapeName) } catch (e: Exception) { FacecamShape.CIRCLE }
        val facecamSize = try { FacecamSize.valueOf(facecamSizeName) } catch (e: Exception) { FacecamSize.MEDIUM }

        return RecordingSettings(
            resolution = resolution,
            orientation = orientation,
            encoderFormat = encoderFormat,
            fps = fps,
            bitrate = bitrate,
            audioSource = audioSource,
            countdown = countdown,
            floatingControlsEnabled = floating,
            magicButtonEnabled = magicButton,
            stopOnScreenOff = stopScreenOff,
            pauseOnCallEnabled = pauseOnCall,
            vibrationFeedback = vibration,
            shakeToStopEnabled = shakeToStop,
            facecamEnabled = facecam,
            facecamShape = facecamShape,
            facecamSize = facecamSize,
            brushEnabled = brush,
            showTouchesEnabled = showTouches,
            autoStopTime = autoStopTime,
            autoStopFileSize = autoStopFileSize,
            watermarkEnabled = watermark,
            watermarkText = watermarkText,
            autoSaveToGallery = autoSaveToGallery
        )
    }

    fun updateResolution(resolution: VideoResolution) {
        prefs.edit().putString("resolution", resolution.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(resolution = resolution)
    }

    fun updateOrientation(orientation: VideoOrientation) {
        prefs.edit().putString("orientation", orientation.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(orientation = orientation)
    }

    fun updateEncoderFormat(format: VideoEncoderFormat) {
        prefs.edit().putString("encoder_format", format.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(encoderFormat = format)
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

    fun updateMagicButton(enabled: Boolean) {
        prefs.edit().putBoolean("magic_button", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(magicButtonEnabled = enabled)
    }

    fun updateStopOnScreenOff(enabled: Boolean) {
        prefs.edit().putBoolean("stop_screen_off", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(stopOnScreenOff = enabled)
    }

    fun updatePauseOnCall(enabled: Boolean) {
        prefs.edit().putBoolean("pause_on_call", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(pauseOnCallEnabled = enabled)
    }

    fun updateVibrationFeedback(enabled: Boolean) {
        prefs.edit().putBoolean("vibration_feedback", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(vibrationFeedback = enabled)
    }

    fun updateShakeToStop(enabled: Boolean) {
        prefs.edit().putBoolean("shake_to_stop", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(shakeToStopEnabled = enabled)
    }

    fun updateFacecam(enabled: Boolean) {
        prefs.edit().putBoolean("facecam_enabled", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(facecamEnabled = enabled)
    }

    fun updateFacecamShape(shape: FacecamShape) {
        prefs.edit().putString("facecam_shape", shape.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(facecamShape = shape)
    }

    fun updateFacecamSize(size: FacecamSize) {
        prefs.edit().putString("facecam_size", size.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(facecamSize = size)
    }

    fun updateBrush(enabled: Boolean) {
        prefs.edit().putBoolean("brush_enabled", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(brushEnabled = enabled)
    }

    fun updateShowTouches(enabled: Boolean) {
        prefs.edit().putBoolean("show_touches", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(showTouchesEnabled = enabled)
    }

    fun updateAutoStopTime(option: AutoStopTimeOption) {
        prefs.edit().putString("auto_stop_time", option.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(autoStopTime = option)
    }

    fun updateAutoStopFileSize(option: AutoStopFileSizeOption) {
        prefs.edit().putString("auto_stop_file_size", option.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(autoStopFileSize = option)
    }

    fun updateWatermark(enabled: Boolean, text: String? = null) {
        val editor = prefs.edit().putBoolean("watermark_enabled", enabled)
        if (text != null) {
            editor.putString("watermark_text", text)
        }
        editor.apply()
        _settingsFlow.value = _settingsFlow.value.copy(
            watermarkEnabled = enabled,
            watermarkText = text ?: _settingsFlow.value.watermarkText
        )
    }

    fun updateAutoSaveToGallery(enabled: Boolean) {
        prefs.edit().putBoolean("auto_save_to_gallery", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(autoSaveToGallery = enabled)
    }
}
