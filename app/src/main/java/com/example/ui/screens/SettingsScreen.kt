package com.example.ui.screens

import android.os.Build
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.example.data.model.AudioSourceType
import com.example.data.model.CountdownOption
import com.example.data.model.RecordingSettings
import com.example.data.model.VideoBitrate
import com.example.data.model.VideoFps
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
                    VideoBitrate.values().forEach { bitrate ->
                        RadioOptionRow(
                            title = bitrate.label,
                            subtitle = null,
                            isSelected = settings.bitrate == bitrate,
                            onClick = { viewModel.setBitrate(bitrate) },
                            testTag = "bitrate_option_${bitrate.name.lowercase()}"
                        )
                    }
                }
            }
        }

        // Audio Settings Section
        SettingsSectionHeader(title = "Audio Recording", icon = Icons.Default.Audiotrack)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AudioSourceType.values().forEach { source ->
                    RadioOptionRow(
                        title = source.label,
                        subtitle = source.description,
                        isSelected = settings.audioSource == source,
                        onClick = { viewModel.setAudioSource(source) },
                        testTag = "audio_option_${source.name.lowercase()}"
                    )
                }
            }
        }

        // Controls & General
        SettingsSectionHeader(title = "Controls & Feedback", icon = Icons.Default.Layers)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Countdown
                Text("Countdown Timer", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CountdownOption.values().forEach { option ->
                        PillSelectable(
                            title = if (option.seconds == 0) "Off" else "${option.seconds}s",
                            isSelected = settings.countdown == option,
                            onClick = { viewModel.setCountdown(option) },
                            modifier = Modifier.weight(1f),
                            testTag = "countdown_option_${option.seconds}"
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = StudioCardBorder)

                // Floating Widget
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Floating Overlay Widget", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Draggable overlay bubble over other apps", color = TextSecondary, fontSize = 12.sp)
                    }
                    val hasOverlay = viewModel.hasOverlayPermission()
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
            }
        }

        // Storage Directory Info
        SettingsSectionHeader(title = "Storage Info", icon = Icons.Default.Folder)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
