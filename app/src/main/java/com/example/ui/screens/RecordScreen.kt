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
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioSourceType
import com.example.data.model.RecordingSettings
import com.example.data.model.VideoEncoderFormat
import com.example.data.model.VideoFps
import com.example.data.model.VideoOrientation
import com.example.service.RecordingState
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlowAmbientCyan
import com.example.ui.theme.GlowAmbientRed
import com.example.ui.theme.NeonPurple
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

    // Multi-stage breathing & pulse animation for record button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.18f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isRecording) 700 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = if (isRecording) 0.85f else 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isRecording) 700 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F111A),
                        Color(0xFF090A10),
                        Color(0xFF06070B)
                    )
                )
            )
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Studio Status Bar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Live Status Capsule
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = if (isActive) RecordRed.copy(alpha = 0.16f) else StudioSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isActive) RecordRed.copy(alpha = 0.6f) else GlassBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (isActive) RecordRed else AccentGreen)
                            .scale(if (isActive) pulseScale else 1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPaused) "PAUSED" else if (isRecording) "ON AIR • REC" else "STUDIO READY",
                        color = if (isActive) RecordRed else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Quick Snapshot Trigger
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .clickable { viewModel.takeScreenshot() }
                    .testTag("action_take_screenshot_button"),
                shape = RoundedCornerShape(30.dp),
                color = StudioSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Screenshot",
                        tint = AccentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Snapshot",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Storage & Remaining Capacity Glass Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("storage_info_card"),
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(AccentCyan.copy(alpha = 0.25f), AccentGreen.copy(alpha = 0.15f))
                                )
                            ),
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
                            text = "Storage Space",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${StorageUtils.formatBytes(storageInfo.freeBytes)} Available",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AccentGreen.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "~${storageInfo.estimatedMinutesRemaining}m left",
                                color = AccentGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Storage Progress Meter
                val usedRatio = if (storageInfo.totalBytes > 0) {
                    ((storageInfo.totalBytes - storageInfo.freeBytes).toFloat() / storageInfo.totalBytes.toFloat()).coerceIn(0.05f, 0.98f)
                } else 0.45f

                LinearProgressIndicator(
                    progress = { usedRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (usedRatio > 0.9f) RecordRed else AccentCyan,
                    trackColor = StudioSurface,
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total: ${StorageUtils.formatBytes(storageInfo.totalBytes)}",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (settings.encoderFormat == VideoEncoderFormat.HEVC) NeonPurple.copy(alpha = 0.15f) else StudioSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (settings.encoderFormat == VideoEncoderFormat.HEVC) NeonPurple.copy(alpha = 0.4f) else StudioCardBorder
                        )
                    ) {
                        Text(
                            text = if (settings.encoderFormat == VideoEncoderFormat.HEVC) "⚡ HEVC Active (-50% storage)" else "H.264 Codec",
                            color = if (settings.encoderFormat == VideoEncoderFormat.HEVC) NeonPurple else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Quick Quality Presets Row (Glass Pills)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            QualityChip(
                label = if (settings.encoderFormat == VideoEncoderFormat.HEVC) "HEVC" else "H.264",
                subLabel = if (settings.encoderFormat == VideoEncoderFormat.HEVC) "50% Small" else "Codec",
                icon = Icons.Default.HighQuality,
                onClick = onNavigateToSettings,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Central Studio Record Button Area
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ambient outer glow ring
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                (if (isActive) RecordRed else AccentCyan).copy(alpha = auraAlpha * 0.4f),
                                (if (isActive) GlowAmbientRed else GlowAmbientCyan),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Outer metallic decorative ring
            Box(
                modifier = Modifier
                    .size(185.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                StudioSurfaceVariant,
                                StudioSurface
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                if (isActive) RecordRed else AccentCyan.copy(alpha = 0.8f),
                                if (isActive) RecordRedDark else Color(0xFF2C2F40)
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Inner main trigger button
            Box(
                modifier = Modifier
                    .size(145.dp)
                    .clip(CircleShape)
                    .shadow(elevation = 16.dp, shape = CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                if (isActive) RecordRed else Color(0xFFFF334B),
                                if (isActive) RecordRedDark else Color(0xFFB51025)
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
                        modifier = Modifier.size(56.dp)
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(RecordRed)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "REC",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Recording Status & Live Timer
        if (isActive) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = TimeUtils.formatDuration(durationMs),
                    color = TextPrimary,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isPaused) StudioSurfaceVariant else RecordRed.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isPaused) "PAUSED" else "LIVE CAPTURE",
                        color = if (isPaused) TextSecondary else RecordRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Tap Core to Record",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Crisp ${settings.resolution.shortLabel} • ${settings.fps.value} FPS • ${if (settings.orientation == VideoOrientation.LANDSCAPE) "Landscape Game" else "Auto"}",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Secondary Active Action Controls (Pause / Resume / Stop)
        if (isActive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        if (isPaused) viewModel.resumeRecording() else viewModel.pauseRecording()
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = StudioSurfaceVariant,
                        contentColor = TextPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("action_pause_resume_button")
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        tint = if (isPaused) AccentGreen else AccentCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPaused) "Resume" else "Pause",
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { viewModel.stopRecording() },
                    colors = ButtonDefaults.buttonColors(containerColor = RecordRed),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop & Save",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Stop & Save",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Pro Features Quick-Launch Dashboard Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "PRO STUDIO TOOLS",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )

                // Row 1: Gaming Mode & Facecam
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProFeatureCard(
                        title = "Gaming Mode",
                        subtitle = if (settings.orientation == VideoOrientation.LANDSCAPE) "Forced Landscape" else "Auto Orientation",
                        icon = Icons.Default.SportsEsports,
                        isActive = settings.orientation == VideoOrientation.LANDSCAPE,
                        activeColor = NeonPurple,
                        onClick = {
                            viewModel.setOrientation(
                                if (settings.orientation == VideoOrientation.LANDSCAPE) VideoOrientation.AUTO else VideoOrientation.LANDSCAPE
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )

                    val hasOverlay = viewModel.hasOverlayPermission()
                    ProFeatureCard(
                        title = "Facecam",
                        subtitle = if (settings.facecamEnabled) "${settings.facecamShape.label} • ${settings.facecamSize.label}" else "Camera Off",
                        icon = Icons.Default.CameraFront,
                        isActive = settings.facecamEnabled,
                        activeColor = AccentGreen,
                        onClick = {
                            if (!settings.facecamEnabled && !hasOverlay) {
                                onRequestOverlayPermission()
                            } else {
                                viewModel.setFacecam(!settings.facecamEnabled)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Invisible Magic Button & Brush Tool
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val hasOverlay = viewModel.hasOverlayPermission()
                    ProFeatureCard(
                        title = "Magic Touch",
                        subtitle = if (settings.magicButtonEnabled) "Stealth Pause/Stop" else "Disabled",
                        icon = Icons.Default.TouchApp,
                        isActive = settings.magicButtonEnabled,
                        activeColor = AccentCyan,
                        onClick = {
                            if (!settings.magicButtonEnabled && !hasOverlay) {
                                onRequestOverlayPermission()
                            } else {
                                viewModel.setMagicButton(!settings.magicButtonEnabled)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    ProFeatureCard(
                        title = "Live Brush",
                        subtitle = if (settings.brushEnabled) "Screen Drawing On" else "Disabled",
                        icon = Icons.Default.Brush,
                        isActive = settings.brushEnabled,
                        activeColor = RecordRed,
                        onClick = {
                            if (!settings.brushEnabled && !hasOverlay) {
                                onRequestOverlayPermission()
                            } else {
                                viewModel.setBrush(!settings.brushEnabled)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Floating Widget Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Floating Tool Bubble",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Overlay widget controls on top of other apps",
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
                                uncheckedTrackColor = StudioSurface
                            ),
                            modifier = Modifier.testTag("floating_controls_switch")
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
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
        color = StudioSurfaceVariant.copy(alpha = 0.8f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = subLabel,
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ProFeatureCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .border(
                1.dp,
                if (isActive) activeColor.copy(alpha = 0.6f) else GlassBorder,
                RoundedCornerShape(18.dp)
            ),
        color = if (isActive) activeColor.copy(alpha = 0.12f) else StudioSurfaceVariant.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isActive) activeColor.copy(alpha = 0.25f) else StudioSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isActive) activeColor else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isActive) activeColor else Color.Transparent)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = if (isActive) activeColor else TextSecondary,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}
