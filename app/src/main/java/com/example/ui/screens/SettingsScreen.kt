package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.RecordRed
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.StorageUtils

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    settings: RecordingSettings,
    onRequestOverlayPermission: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showWatermarkDialog by remember { mutableStateOf(false) }
    var watermarkInput by remember { mutableStateOf(settings.watermarkText) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setFacecam(true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Video Quality Section
        SettingsSectionHeader(title = "Video Quality", icon = Icons.Default.Videocam)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Resolution Selector
                Text("Resolution", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoResolution.values().forEach { res ->
                        RadioOptionRow(
                            title = res.label,
                            subtitle = when (res) {
                                VideoResolution.UHD_2K -> "2560x1440 • Ultra clarity"
                                VideoResolution.FHD_1080P -> "1920x1080 • Crystal clear (Recommended)"
                                VideoResolution.HD_720P -> "1280x720 • Fast encoding"
                                VideoResolution.SD_480P -> "854x480 • Minimal file size"
                                VideoResolution.AUTO -> "Device screen native dimensions"
                            },
                            isSelected = settings.resolution == res,
                            onClick = { viewModel.setResolution(res) },
                            testTag = "res_option_${res.name.lowercase()}"
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = StudioCardBorder)

                // Frame Rate
                Text("Frame Rate (FPS)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoFps.values().forEach { fps ->
                        PillSelectable(
                            title = "${fps.value} FPS",
                            isSelected = settings.fps == fps,
                            onClick = { viewModel.setFps(fps) },
                            modifier = Modifier.weight(1f),
                            testTag = "fps_option_${fps.value}"
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = StudioCardBorder)

                // Bitrate
                Text("Encoding Bitrate", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoBitrate.values().forEach { br ->
                        RadioOptionRow(
                            title = br.label,
                            subtitle = null,
                            isSelected = settings.bitrate == br,
                            onClick = { viewModel.setBitrate(br) },
                            testTag = "bitrate_option_${br.name.lowercase()}"
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = StudioCardBorder)

                // Orientation Lock
                Text("Video Orientation Lock", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoOrientation.values().forEach { orient ->
                        PillSelectable(
                            title = orient.label,
                            isSelected = settings.orientation == orient,
                            onClick = { viewModel.setOrientation(orient) },
                            modifier = Modifier.weight(1f),
                            testTag = "orientation_${orient.name.lowercase()}"
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = StudioCardBorder)

                // Video Encoder Format
                Text("Hardware Video Encoder (Codec)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoEncoderFormat.values().forEach { enc ->
                        RadioOptionRow(
                            title = enc.label,
                            subtitle = enc.description,
                            isSelected = settings.encoderFormat == enc,
                            onClick = { viewModel.setEncoderFormat(enc) },
                            testTag = "encoder_${enc.name.lowercase()}"
                        )
                    }
                }
            }
        }

        // Audio & Countdown
        SettingsSectionHeader(title = "Audio & Preparation", icon = Icons.Default.Audiotrack)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Audio Source", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AudioSourceType.values().forEach { audio ->
                        RadioOptionRow(
                            title = audio.label,
                            subtitle = audio.description,
                            isSelected = settings.audioSource == audio,
                            onClick = { viewModel.setAudioSource(audio) },
                            testTag = "audio_option_${audio.name.lowercase()}"
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = StudioCardBorder)

                Text("Countdown Timer", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CountdownOption.values().forEach { opt ->
                        PillSelectable(
                            title = if (opt.seconds == 0) "Off" else "${opt.seconds}s",
                            isSelected = settings.countdown == opt,
                            onClick = { viewModel.setCountdown(opt) },
                            modifier = Modifier.weight(1f),
                            testTag = "countdown_option_${opt.name.lowercase()}"
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = StudioCardBorder)

                // Smart Pause on Incoming Call
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Smart Pause on Incoming Call", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Auto-pause recording when phone rings or audio focus changes", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.pauseOnCallEnabled,
                        onCheckedChange = { viewModel.setPauseOnCall(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RecordRed,
                            uncheckedTrackColor = StudioSurface
                        ),
                        modifier = Modifier.testTag("settings_pause_on_call_switch")
                    )
                }
            }
        }

        // Interactive Tools & Overlays
        SettingsSectionHeader(title = "Overlays & Pro Tools", icon = Icons.Default.Layers)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Floating Widget
                val hasOverlay = viewModel.hasOverlayPermission()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Floating Control Bubble", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Draggable overlay button over apps", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.floatingControlsEnabled && hasOverlay,
                        onCheckedChange = { checked ->
                            if (checked && !hasOverlay) {
                                onRequestOverlayPermission()
                            } else {
                                viewModel.setFloatingControls(checked)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RecordRed,
                            uncheckedTrackColor = StudioSurface
                        ),
                        modifier = Modifier.testTag("settings_floating_switch")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = StudioCardBorder)

                // Facecam Front Camera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Facecam Overlay", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Draggable floating camera with shape & size customization", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.facecamEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    if (!hasOverlay) {
                                        onRequestOverlayPermission()
                                    } else {
                                        viewModel.setFacecam(true)
                                    }
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            } else {
                                viewModel.setFacecam(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentGreen,
                            uncheckedTrackColor = StudioSurface
                        ),
                        modifier = Modifier.testTag("settings_facecam_switch")
                    )
                }

                if (settings.facecamEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Facecam Shape", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FacecamShape.values().forEach { shape ->
                            PillSelectable(
                                title = shape.label,
                                isSelected = settings.facecamShape == shape,
                                onClick = { viewModel.setFacecamShape(shape) },
                                modifier = Modifier.weight(1f),
                                testTag = "facecam_shape_${shape.name.lowercase()}"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Facecam Window Size", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FacecamSize.values().forEach { size ->
                            PillSelectable(
                                title = size.label,
                                isSelected = settings.facecamSize == size,
                                onClick = { viewModel.setFacecamSize(size) },
                                modifier = Modifier.weight(1f),
                                testTag = "facecam_size_${size.name.lowercase()}"
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = StudioCardBorder)

                // Screen Brush & Drawing Tool
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Screen Drawing / Brush Tool", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Annotate and highlight screen during recording", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.brushEnabled,
                        onCheckedChange = { checked ->
                            if (checked && !hasOverlay) {
                                onRequestOverlayPermission()
                            } else {
                                viewModel.setBrush(checked)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RecordRed,
                            uncheckedTrackColor = StudioSurface
                        ),
                        modifier = Modifier.testTag("settings_brush_switch")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = StudioCardBorder)

                // Shake to Stop
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Shake to Stop Recording", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Shake phone briskly to end recording instantly", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.shakeToStopEnabled,
                        onCheckedChange = { viewModel.setShakeToStop(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RecordRed,
                            uncheckedTrackColor = StudioSurface
                        ),
                        modifier = Modifier.testTag("settings_shake_switch")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = StudioCardBorder)

                // Stop on Screen Off
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Stop on Screen Off", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Automatically end recording when screen locks", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.stopOnScreenOff,
                        onCheckedChange = { viewModel.setStopOnScreenOff(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RecordRed,
                            uncheckedTrackColor = StudioSurface
                        )
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = StudioCardBorder)

                // Vibration feedback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vibration Feedback", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Haptic pulse on start and stop recording", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.vibrationFeedback,
                        onCheckedChange = { viewModel.setVibrationFeedback(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RecordRed,
                            uncheckedTrackColor = StudioSurface
                        )
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = StudioCardBorder)

                // Invisible Magic Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Invisible Magic Button", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Stealth top-right touch area (Single tap = Pause, Double tap = Stop)", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.magicButtonEnabled && hasOverlay,
                        onCheckedChange = { checked ->
                            if (checked && !hasOverlay) {
                                onRequestOverlayPermission()
                            } else {
                                viewModel.setMagicButton(checked)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentCyan,
                            uncheckedTrackColor = StudioSurface
                        ),
                        modifier = Modifier.testTag("settings_magic_button_switch")
                    )
                }
            }
        }

        // Auto-Stop Limits
        SettingsSectionHeader(title = "Auto-Stop Limits", icon = Icons.Default.Speed)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Max Recording Duration", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AutoStopTimeOption.values().forEach { opt ->
                        PillSelectable(
                            title = opt.label,
                            isSelected = settings.autoStopTime == opt,
                            onClick = { viewModel.setAutoStopTime(opt) },
                            modifier = Modifier.weight(1f),
                            testTag = "auto_time_${opt.name.lowercase()}"
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = StudioCardBorder)

                Text("Max File Size Limit", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AutoStopFileSizeOption.values().forEach { opt ->
                        PillSelectable(
                            title = opt.label,
                            isSelected = settings.autoStopFileSize == opt,
                            onClick = { viewModel.setAutoStopFileSize(opt) },
                            modifier = Modifier.weight(1f),
                            testTag = "auto_size_${opt.name.lowercase()}"
                        )
                    }
                }
            }
        }

        // Custom Watermark
        SettingsSectionHeader(title = "Branding & Watermark", icon = Icons.Default.BrandingWatermark)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Custom Branding Watermark", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(
                            text = if (settings.watermarkEnabled) "Watermark: \"${settings.watermarkText}\"" else "Overlay personal text branding",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = settings.watermarkEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showWatermarkDialog = true
                            } else {
                                viewModel.setWatermark(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RecordRed,
                            uncheckedTrackColor = StudioSurface
                        ),
                        modifier = Modifier.testTag("settings_watermark_switch")
                    )
                }

                if (settings.watermarkEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showWatermarkDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioSurface),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change Branding Text", color = AccentCyan, fontSize = 13.sp)
                    }
                }
            }
        }

        // Show Touches Guide
        SettingsSectionHeader(title = "Show Touches / Taps", icon = Icons.Default.TouchApp)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "System Visual Touch Feedback",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "To show visual circles when tapping the screen during video tutorials, enable \"Show taps / Show touches\" in Android Developer Options.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent(Settings.ACTION_SETTINGS)
                                context.startActivity(intent)
                            } catch (ignored: Exception) {}
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Android Developer Settings", color = AccentCyan, fontSize = 13.sp)
                }
            }
        }

        // Storage Directory Info & Gallery Sync
        SettingsSectionHeader(title = "Storage & Gallery", icon = Icons.Default.Folder)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-save to Gallery", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Save videos and screenshots directly to phone Gallery / Photos (Movies/ScreenRecorder)", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.autoSaveToGallery,
                        onCheckedChange = { viewModel.setAutoSaveToGallery(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RecordRed,
                            uncheckedThumbColor = TextTertiary,
                            uncheckedTrackColor = StudioSurface
                        ),
                        modifier = Modifier.testTag("switch_auto_save_gallery")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = StudioCardBorder)

                Text("Recordings Directory", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = StorageUtils.getRecordingsDirectory(context).absolutePath,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Screenshots Directory", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = StorageUtils.getScreenshotsDirectory(context).absolutePath,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // About & CI/CD info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StudioSurface),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Screen Recorder Pro v1.0",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Native Kotlin • Jetpack Compose • GitHub CI/CD Ready",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    if (showWatermarkDialog) {
        AlertDialog(
            onDismissRequest = { showWatermarkDialog = false },
            title = { Text("Watermark Branding") },
            text = {
                Column {
                    Text("Enter text label to display on screen recordings:")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = watermarkInput,
                        onValueChange = { watermarkInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RecordRed,
                            unfocusedBorderColor = StudioCardBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setWatermark(true, watermarkInput.trim())
                        showWatermarkDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RecordRed)
                ) {
                    Text("Save & Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWatermarkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = RecordRed, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RadioOptionRow(
    title: String,
    subtitle: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(
                1.dp,
                if (isSelected) RecordRed.copy(alpha = 0.6f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            ),
        color = if (isSelected) RecordRed.copy(alpha = 0.12f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isSelected) RecordRed else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = subtitle, color = TextSecondary, fontSize = 11.sp)
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = RecordRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PillSelectable(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(
                1.dp,
                if (isSelected) RecordRed else StudioCardBorder,
                RoundedCornerShape(12.dp)
            ),
        color = if (isSelected) RecordRed.copy(alpha = 0.2f) else StudioSurface
    ) {
        Text(
            text = title,
            color = if (isSelected) RecordRed else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
