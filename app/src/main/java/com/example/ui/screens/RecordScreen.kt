package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioSourceType
import com.example.data.model.RecordingSettings
import com.example.service.RecordingState
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.RecordRed
import com.example.ui.theme.RecordRedDark
import com.example.ui.theme.RecordRedGlow
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.StorageUtils
import com.example.util.TimeUtils

@Composable
fun RecordScreen(
    viewModel: MainViewModel,
    recordingState: RecordingState,
    durationMs: Long,
    settings: RecordingSettings,
    storageInfo: StorageUtils.StorageInfo,
    onStartRecordingClick: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isRecording = recordingState is RecordingState.Recording
    val isPaused = recordingState is RecordingState.Paused
    val isActive = isRecording || isPaused

    // Infinite pulsing animation for record button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.15f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isRecording) 800 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Storage & Remaining Time Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("storage_info_card"),
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AccentCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Storage",
                        tint = AccentCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Storage Available",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${StorageUtils.formatBytes(storageInfo.freeBytes)} Free",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AccentGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "~${storageInfo.estimatedMinutesRemaining}m left",
                        color = AccentGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Quality Presets Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QualityChip(
                label = settings.resolution.shortLabel,
                subLabel = "Res",
                icon = Icons.Default.Videocam,
                onClick = onNavigateToSettings,
                modifier = Modifier.weight(1f)
            )
            QualityChip(
                label = "${settings.fps.value} FPS",
                subLabel = "Speed",
                icon = Icons.Default.Settings,
                onClick = onNavigateToSettings,
                modifier = Modifier.weight(1f)
            )
            QualityChip(
                label = when (settings.audioSource) {
                    AudioSourceType.MIC -> "Mic"
                    AudioSourceType.INTERNAL -> "Device"
                    AudioSourceType.MUTE -> "Muted"
                },
                subLabel = "Audio",
                icon = when (settings.audioSource) {
                    AudioSourceType.MIC -> Icons.Default.Mic
                    AudioSourceType.INTERNAL -> Icons.AutoMirrored.Filled.VolumeUp
                    AudioSourceType.MUTE -> Icons.Default.MicOff
                },
                onClick = onNavigateToSettings,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Central Studio Record Button Area
        Box(
            modifier = Modifier.size(230.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ambient outer glow ring
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                (if (isActive) RecordRed else AccentCyan).copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Outer decorative ring
            Box(
                modifier = Modifier
                    .size(175.dp)
                    .clip(CircleShape)
                    .border(
                        width = 3.dp,
                        color = if (isActive) RecordRed else StudioCardBorder,
                        shape = CircleShape
                    )
            )

            // Inner main trigger button
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                if (isActive) RecordRed else RecordRedDark,
                                if (isActive) RecordRedDark else Color(0xFF8E0B19)
                            )
                        )
                    )
                    .clickable {
                        if (isActive) {
                            viewModel.stopRecording()
                        } else {
                            onStartRecordingClick()
                        }
                    }
                    .testTag("main_record_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isActive) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Recording Status & Live Timer
        if (isActive) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = TimeUtils.formatDuration(durationMs),
                    color = TextPrimary,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (isPaused) "RECORDING PAUSED" else "RECORDING IN PROGRESS",
                    color = if (isPaused) TextSecondary else RecordRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Tap to Record",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Captures full screen at ${settings.resolution.shortLabel} • ${settings.fps.value} FPS",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Secondary Active Action Controls (Pause / Resume / Screenshot)
        if (isActive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        if (isPaused) viewModel.resumeRecording() else viewModel.pauseRecording()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    modifier = Modifier.testTag("action_pause_resume_button")
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        tint = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isPaused) "Resume" else "Pause")
                }

                Button(
                    onClick = { viewModel.takeScreenshot() },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("action_take_screenshot_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Screenshot",
                        tint = AccentCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Screenshot", color = TextPrimary)
                }
            }
        } else {
            // When Idle: Floating Overlay & Quick Screenshot
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StudioSurface),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Floating Tool Widget",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Quick controls on top of other apps",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
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
                                uncheckedTrackColor = StudioSurfaceVariant
                            ),
                            modifier = Modifier.testTag("floating_controls_switch")
                        )
                    }

                    if (!viewModel.hasOverlayPermission() && settings.floatingControlsEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Permission required to display floating widget over other apps.",
                            color = AccentCyan,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable { onRequestOverlayPermission() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityChip(
    label: String,
    subLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .border(1.dp, StudioCardBorder, RoundedCornerShape(14.dp)),
        color = StudioSurfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subLabel,
                color = TextTertiary,
                fontSize = 10.sp
            )
        }
    }
}
