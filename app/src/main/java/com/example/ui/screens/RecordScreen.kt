package com.example.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AudioSourceType
import com.example.data.model.RecordingSettings
import com.example.data.model.VideoBitrate
import com.example.data.model.VideoEncoderFormat
import com.example.data.model.VideoFps
import com.example.data.model.VideoOrientation
import com.example.data.model.VideoResolution
import com.example.service.RecordingState
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlowAmbientCyan
import com.example.ui.theme.GlowAmbientRed
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.RecordRed
import com.example.ui.theme.RecordRedDark
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.StorageUtils
import com.example.util.TimeUtils
import java.io.File

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

    var selectedTab by remember { mutableIntStateOf(0) }
    var showVipModal by remember { mutableStateOf(false) }


    // Breathing pulse for recording indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.2f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isRecording) 600 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D0F18),
                        Color(0xFF090A12),
                        Color(0xFF06070B)
                    )
                )
            )
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        // -------------------------------------------------------------
        // 1. TOP HEADER BAR (SDA PRO + Diamond & Settings Icons)
        // -------------------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Gradient Play Logo Icon
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFF06B6D4))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SDA",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // PRO Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NeonPurple,
                            border = BorderStroke(1.dp, Color(0x66FFFFFF))
                        ) {
                            Text(
                                text = "PRO",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Screen Recorder",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Right Action Buttons (Diamond & Gear)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // VIP Diamond Button
                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { showVipModal = true },
                    shape = CircleShape,
                    color = Color(0x2500E5FF),
                    border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = "VIP PRO",
                            tint = AccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Settings Gear Button
                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onNavigateToSettings),
                    shape = CircleShape,
                    color = StudioSurfaceVariant,
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 2. TELEMETRY & FACECAM CARD DECK (Top Banner)
        // -------------------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Telemetry & Live Status Ticker Card
            Card(
                modifier = Modifier
                    .weight(1.3f)
                    .testTag("recording_telemetry_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xBB161A28)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Recording Ticker Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isActive) RecordRed else AccentGreen)
                                .scale(if (isActive) pulseScale else 1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isActive) TimeUtils.formatDuration(durationMs) else "00:00:00",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = if (isPaused) "Paused..." else if (isRecording) "Recording..." else "Studio Ready",
                        color = if (isActive) RecordRed else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 18.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Telemetry Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("FPS", color = TextTertiary, fontSize = 9.sp)
                            Text("${settings.fps.value}", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("RES", color = TextTertiary, fontSize = 9.sp)
                            Text(settings.resolution.shortLabel, color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("BR", color = TextTertiary, fontSize = 9.sp)
                            Text(settings.bitrate.label.split(" ")[0], color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sub Telemetry Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x33000000), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SportsEsports, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(settings.recordingMode, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Thermostat, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("38°C", color = TextSecondary, fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("${StorageUtils.formatBytes(storageInfo.freeBytes).split(" ")[0]}GB", color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Right Floating Facecam Card
            Card(
                modifier = Modifier
                    .weight(0.85f)
                    .height(135.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xBB161A28)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, if (settings.facecamEnabled) AccentCyan.copy(alpha = 0.6f) else GlassBorder)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Facecam Avatar Preview / Live Camera Feed Mock
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF25293A), Color(0xFF131520))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraFront,
                            contentDescription = "Facecam Preview",
                            tint = if (settings.facecamEnabled) AccentCyan else TextTertiary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Facecam Bottom Overlay Control Pill
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        color = Color(0xEE0F111B),
                        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CameraFront,
                                    contentDescription = null,
                                    tint = if (settings.facecamEnabled) AccentCyan else TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Facecam",
                                    color = TextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            val hasOverlay = viewModel.hasOverlayPermission()
                            Switch(
                                checked = settings.facecamEnabled,
                                onCheckedChange = { checked ->
                                    if (checked && !hasOverlay) {
                                        onRequestOverlayPermission()
                                    } else {
                                        viewModel.setFacecam(checked)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AccentCyan,
                                    uncheckedTrackColor = StudioSurface
                                ),
                                modifier = Modifier
                                    .scale(0.65f)
                                    .testTag("facecam_quick_switch")
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // -------------------------------------------------------------
        // 3. CENTER HERO CANVAS AREA (Screen Wallpaper + Dual Overlays)
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(26.dp))
                .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(26.dp))
                .background(Color(0xFF0F1018))
        ) {
            // Screen Background Image Wallpaper
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("/public/assets/aistudio/file_0000000073d0821187877c858c33ac26_a94e0019.png")
                    .crossfade(true)
                    .build(),
                contentDescription = "Screen Capture Preview Wallpaper",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dark ambient gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x30000000),
                                Color(0x10000000),
                                Color(0x70000000)
                            )
                        )
                    )
            )


        }

        Spacer(modifier = Modifier.height(18.dp))

        // -------------------------------------------------------------
        // 4. BOTTOM SHEET SETTINGS & AUDIO MIXER (Tabbed Studio Panel)
        // -------------------------------------------------------------
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xEE121420)),
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {

                // Top Category Navigation Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf("Recording Settings", "Video", "Audio", "More")
                    tabs.forEachIndexed { index, tabTitle ->
                        val isSelected = selectedTab == index
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { selectedTab = index }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (index) {
                                        0 -> Icons.Default.Videocam
                                        1 -> Icons.Default.HighQuality
                                        2 -> Icons.Default.VolumeUp
                                        else -> Icons.Default.Settings
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) AccentCyan else TextTertiary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tabTitle,
                                    color = if (isSelected) TextPrimary else TextTertiary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            // Active Tab Indicator Line
                            Box(
                                modifier = Modifier
                                    .width(if (isSelected) 36.dp else 0.dp)
                                    .height(2.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(if (isSelected) AccentCyan else Color.Transparent)
                            )
                        }
                    }
                }

                // QUALITY PRESET CARDS ROW (3 Horizontal Cards)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Resolution Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onNavigateToSettings),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1B1E2D),
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Resolution", color = TextTertiary, fontSize = 10.sp)
                                Text(settings.resolution.shortLabel, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("${settings.resolution.width}×${settings.resolution.height}", color = TextSecondary, fontSize = 9.sp)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                        }
                    }

                    // FPS Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onNavigateToSettings),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1B1E2D),
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("FPS", color = TextTertiary, fontSize = 10.sp)
                                Text("${settings.fps.value} FPS", color = AccentGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Smooth", color = TextSecondary, fontSize = 9.sp)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Bitrate Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onNavigateToSettings),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1B1E2D),
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Bitrate", color = TextTertiary, fontSize = 10.sp)
                                Text(settings.bitrate.label.split(" ")[0], color = AccentCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("High Quality", color = TextSecondary, fontSize = 9.sp)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // DUAL AUDIO MIXER CARD DECK
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161926)),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AccentCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Audio Mixer", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Mix internal audio and microphone", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Internal Audio Slider & Switch Row
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Internal Audio", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(" (Game/Music)", color = TextTertiary, fontSize = 10.sp)
                                }

                                Switch(
                                    checked = settings.internalAudioEnabled,
                                    onCheckedChange = { viewModel.setInternalAudioEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = AccentCyan,
                                        uncheckedTrackColor = StudioSurface
                                    ),
                                    modifier = Modifier.scale(0.7f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Slider(
                                    value = settings.internalAudioVolume,
                                    onValueChange = { viewModel.setInternalAudioVolume(it) },
                                    enabled = settings.internalAudioEnabled,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = AccentCyan,
                                        activeTrackColor = AccentCyan,
                                        inactiveTrackColor = StudioSurface
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${(settings.internalAudioVolume * 100).toInt()}%",
                                    color = if (settings.internalAudioEnabled) AccentCyan else TextTertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Microphone Audio Slider & Switch Row
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Mic, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Microphone", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(" (Your Voice)", color = TextTertiary, fontSize = 10.sp)
                                }

                                Switch(
                                    checked = settings.micAudioEnabled,
                                    onCheckedChange = { viewModel.setMicAudioEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = NeonPurple,
                                        uncheckedTrackColor = StudioSurface
                                    ),
                                    modifier = Modifier.scale(0.7f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Slider(
                                    value = settings.micAudioVolume,
                                    onValueChange = { viewModel.setMicAudioVolume(it) },
                                    enabled = settings.micAudioEnabled,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = NeonPurple,
                                        activeTrackColor = NeonPurple,
                                        inactiveTrackColor = StudioSurface
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${(settings.micAudioVolume * 100).toInt()}%",
                                    color = if (settings.micAudioEnabled) NeonPurple else TextTertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SECONDARY CONFIGURATION CARDS ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Facecam Settings Box
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1B1E2D),
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CameraFront, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Facecam", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("1080P • 30FPS", color = TextSecondary, fontSize = 9.sp)
                                }
                            }

                            val hasOverlay = viewModel.hasOverlayPermission()
                            Switch(
                                checked = settings.facecamEnabled,
                                onCheckedChange = { checked ->
                                    if (checked && !hasOverlay) onRequestOverlayPermission() else viewModel.setFacecam(checked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AccentCyan,
                                    uncheckedTrackColor = StudioSurface
                                ),
                                modifier = Modifier.scale(0.65f)
                            )
                        }
                    }

                    // Orientation Box
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onNavigateToSettings),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1B1E2D),
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ScreenRotation, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Orientation", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(settings.orientation.shortLabel, color = TextSecondary, fontSize = 9.sp)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ADVANCED SETTINGS ROW
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToSettings),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1B1E2D),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Advanced Settings", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Touch indicators, audio focus, auto stop, shake to stop, etc.", color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    // VIP PRO MODAL DIALOG
    if (showVipModal) {
        AlertDialog(
            onDismissRequest = { showVipModal = false },
            containerColor = Color(0xFF141724),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Diamond, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SDA PRO Unlocked", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• 2K 1440p Ultra Clarity Screen Recording", color = TextSecondary, fontSize = 13.sp)
                    Text("• HEVC / H.265 Hardware Acceleration (-50% Size)", color = TextSecondary, fontSize = 13.sp)
                    Text("• Dual Audio Mixer (Game + Mic Voice simultaneously)", color = TextSecondary, fontSize = 13.sp)
                    Text("• Custom Facecam Shapes (Circle, Square, Rounded)", color = TextSecondary, fontSize = 13.sp)
                    Text("• Studio Suite: Animated GIF Creator & Compressor", color = TextSecondary, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showVipModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text("Enjoy PRO Features", color = Color.White)
                }
            }
        )
    }
}
