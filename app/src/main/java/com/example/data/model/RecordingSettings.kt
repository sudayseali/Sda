package com.example.data.model

enum class VideoResolution(val label: String, val shortLabel: String, val width: Int, val height: Int) {
    AUTO("Auto (Native)", "Native", 0, 0),
    UHD_2K("2K (1440p)", "2K", 1440, 2560),
    FHD_1080P("1080p Full HD", "1080p", 1080, 1920),
    HD_720P("720p HD", "720p", 720, 1280),
    SD_480P("480p SD", "480p", 480, 854)
}

enum class VideoFps(val label: String, val value: Int) {
    FPS_60("60 FPS (Ultra Smooth)", 60),
    FPS_30("30 FPS (Standard)", 30)
}

enum class VideoBitrate(val label: String, val value: Int) {
    BITRATE_16M("16 Mbps (Studio Quality)", 16_000_000),
    BITRATE_12M("12 Mbps (High Quality)", 12_000_000),
    BITRATE_8M("8 Mbps (Recommended)", 8_000_000),
    BITRATE_4M("4 Mbps (Compact)", 4_000_000)
}

enum class AudioSourceType(val label: String, val description: String) {
    MIC("Microphone", "Record your voice and ambient sound"),
    INTERNAL("Internal Audio", "Record game or device audio (Android 10+)"),
    MUTE("Mute", "Record video only without audio")
}

enum class CountdownOption(val label: String, val seconds: Int) {
    NONE("No countdown", 0),
    SEC_3("3 seconds", 3),
    SEC_5("5 seconds", 5),
    SEC_10("10 seconds", 10)
}

enum class AutoStopTimeOption(val label: String, val minutes: Int) {
    UNLIMITED("No limit", 0),
    MIN_5("5 minutes", 5),
    MIN_15("15 minutes", 15),
    MIN_30("30 minutes", 30),
    MIN_60("1 hour", 60)
}

enum class AutoStopFileSizeOption(val label: String, val bytes: Long) {
    UNLIMITED("No limit", 0L),
    MB_200("200 MB", 200L * 1024 * 1024),
    MB_500("500 MB", 500L * 1024 * 1024),
    GB_1("1 GB", 1024L * 1024 * 1024),
    GB_2("2 GB", 2048L * 1024 * 1024)
}

data class RecordingSettings(
    val resolution: VideoResolution = VideoResolution.FHD_1080P,
    val fps: VideoFps = VideoFps.FPS_60,
    val bitrate: VideoBitrate = VideoBitrate.BITRATE_8M,
    val audioSource: AudioSourceType = AudioSourceType.MIC,
    val countdown: CountdownOption = CountdownOption.SEC_3,
    val floatingControlsEnabled: Boolean = true,
    val stopOnScreenOff: Boolean = true,
    val vibrationFeedback: Boolean = true,
    val highQualityAudio: Boolean = true,
    val shakeToStopEnabled: Boolean = false,
    val facecamEnabled: Boolean = false,
    val brushEnabled: Boolean = false,
    val showTouchesEnabled: Boolean = false,
    val autoStopTime: AutoStopTimeOption = AutoStopTimeOption.UNLIMITED,
    val autoStopFileSize: AutoStopFileSizeOption = AutoStopFileSizeOption.UNLIMITED,
    val watermarkEnabled: Boolean = false,
    val watermarkText: String = "Screen Recorder",
    val autoSaveToGallery: Boolean = true
)

