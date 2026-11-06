package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AudioSourceType
import com.example.service.RecordingState
import com.example.ui.MainViewModel
import com.example.ui.components.CountdownOverlay
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.RecordScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.RecordRed
import com.example.ui.theme.ScreenRecorderTheme
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.TimeUtils

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val (width, height, density) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = windowManager.currentWindowMetrics.bounds
                Triple(bounds.width(), bounds.height(), resources.configuration.densityDpi)
            } else {
                val metrics = resources.displayMetrics
                Triple(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
            }
            viewModel.startRecordingImmediately(
                result.resultCode,
                result.data!!,
                width,
                height,
                density
            )
        } else {
            viewModel.onRecordingCancelled()
            Toast.makeText(this, "Screen recording permission cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Continue to screen capture intent even if optional permissions vary
        viewModel.startCountdownAndCapture {
            requestScreenCapture()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ScreenRecorderTheme {
                ScreenRecorderApp(
                    viewModel = viewModel,
                    onRequestRecord = { checkPermissionsAndStartCapture() },
                    onRequestOverlayPermission = { requestOverlayPermission() }
                )
            }
        }
    }

    private fun checkPermissionsAndStartCapture() {
        val neededPermissions = mutableListOf<String>()

        if (viewModel.settings.value.audioSource == AudioSourceType.MIC) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.RECORD_AUDIO)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (neededPermissions.isNotEmpty()) {
            permissionsLauncher.launch(neededPermissions.toTypedArray())
        } else {
            viewModel.startCountdownAndCapture {
                requestScreenCapture()
            }
        }
    }

    private fun requestScreenCapture() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
}

@Composable
fun ScreenRecorderApp(
    viewModel: MainViewModel,
    onRequestRecord: () -> Unit,
    onRequestOverlayPermission: () -> Unit
) {
    var currentTab by remember { mutableIntStateOf(0) }
    val recordingState by viewModel.recordingState.collectAsState()
    val durationMs by viewModel.recordingDurationMs.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val storageInfo by viewModel.storageInfo.collectAsState()
    val videos by viewModel.videoRecordings.collectAsState()
    val screenshots by viewModel.screenshots.collectAsState()
    val trashRecordings by viewModel.trashRecordings.collectAsState()
    val selectedMedia by viewModel.selectedMedia.collectAsState()
    val countdown by viewModel.countdownSeconds.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioDarkBg)
    ) {
        if (selectedMedia != null && !selectedMedia!!.isScreenshot) {
            VideoPlayerScreen(
                item = selectedMedia!!,
                onBack = { viewModel.selectMediaForView(null) },
                onSaveToGallery = { viewModel.saveMediaToGallery(selectedMedia!!) },
                onTrim = { startMs, endMs -> viewModel.trimVideo(selectedMedia!!, startMs, endMs) {} },
                onExtractAudio = { viewModel.extractAudio(selectedMedia!!) {} },
                onMakeGif = { startMs, durMs -> viewModel.createGif(selectedMedia!!, startMs, durMs) {} },
                onCompressVideo = { _ -> viewModel.compressVideo(selectedMedia!!) {} }
            )
        } else {
            Scaffold(
                containerColor = StudioDarkBg,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    NavigationBar(
                        containerColor = Color(0xFF0F111A),
                        contentColor = TextPrimary,
                        modifier = Modifier
                            .border(1.dp, Color(0x1AFFFFFF))
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("main_bottom_nav")
                    ) {
                        // 1. Home Tab
                        NavigationBarItem(
                            selected = currentTab == 0,
                            onClick = { currentTab = 0 },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Home",
                                    tint = if (currentTab == 0) AccentCyan else TextSecondary
                                )
                            },
                            label = {
                                Text(
                                    text = "Home",
                                    color = if (currentTab == 0) AccentCyan else TextSecondary,
                                    fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = AccentCyan.copy(alpha = 0.15f)),
                            modifier = Modifier.testTag("nav_home_tab")
                        )

                        // 2. Recordings Tab
                        NavigationBarItem(
                            selected = currentTab == 1,
                            onClick = { currentTab = 1 },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (videos.isNotEmpty()) {
                                            Badge(containerColor = RecordRed, contentColor = Color.White) {
                                                Text("${videos.size}")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = "Recordings",
                                        tint = if (currentTab == 1) AccentCyan else TextSecondary
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = "Recordings",
                                    color = if (currentTab == 1) AccentCyan else TextSecondary,
                                    fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = AccentCyan.copy(alpha = 0.15f)),
                            modifier = Modifier.testTag("nav_gallery_tab")
                        )

                        // 3. Center Elevated Action Button (Record Trigger)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val isLiveRecording = recordingState is RecordingState.Recording || recordingState is RecordingState.Paused
                                    if (isLiveRecording) {
                                        viewModel.stopRecording()
                                    } else {
                                        onRequestRecord()
                                    }
                                }
                                .testTag("nav_center_trigger"),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFF06B6D4))
                                        )
                                    )
                                    .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                val isLiveRecording = recordingState is RecordingState.Recording || recordingState is RecordingState.Paused
                                Icon(
                                    imageVector = if (isLiveRecording) Icons.Default.FiberManualRecord else Icons.Default.Videocam,
                                    contentDescription = "Trigger Record",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        // 4. Tools Tab
                        NavigationBarItem(
                            selected = currentTab == 2,
                            onClick = { currentTab = 2 },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Tools",
                                    tint = if (currentTab == 2) AccentCyan else TextSecondary
                                )
                            },
                            label = {
                                Text(
                                    text = "Tools",
                                    color = if (currentTab == 2) AccentCyan else TextSecondary,
                                    fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = AccentCyan.copy(alpha = 0.15f)),
                            modifier = Modifier.testTag("nav_tools_tab")
                        )

                        // 5. Profile / Settings Tab
                        NavigationBarItem(
                            selected = currentTab == 3,
                            onClick = { currentTab = 3 },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = if (currentTab == 3) AccentCyan else TextSecondary
                                )
                            },
                            label = {
                                Text(
                                    text = "Profile",
                                    color = if (currentTab == 3) AccentCyan else TextSecondary,
                                    fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = AccentCyan.copy(alpha = 0.15f)),
                            modifier = Modifier.testTag("nav_profile_tab")
                        )
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    val isLiveRecording = recordingState is RecordingState.Recording || recordingState is RecordingState.Paused
                    if (currentTab != 0 && isLiveRecording) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { currentTab = 0 },
                            shape = RoundedCornerShape(16.dp),
                            color = RecordRed.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, RecordRed.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(RecordRed)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "RECORDING LIVE • ${TimeUtils.formatDuration(durationMs)}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "TAP TO VIEW ›",
                                    color = RecordRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (currentTab) {
                            0 -> RecordScreen(
                                viewModel = viewModel,
                                recordingState = recordingState,
                                durationMs = durationMs,
                                settings = settings,
                                storageInfo = storageInfo,
                                onStartRecordingClick = onRequestRecord,
                                onRequestOverlayPermission = onRequestOverlayPermission,
                                onNavigateToSettings = { currentTab = 3 }
                            )
                            1 -> GalleryScreen(
                                videoList = videos,
                                screenshotList = screenshots,
                                trashList = trashRecordings,
                                onPlayVideo = { viewModel.selectMediaForView(it) },
                                onSaveToGallery = { viewModel.saveMediaToGallery(it) },
                                onRename = { item, newTitle -> viewModel.renameMedia(item, newTitle) },
                                onDelete = { item -> viewModel.deleteMedia(item) },
                                onToggleFavorite = { item -> viewModel.toggleFavorite(item) },
                                onRestoreFromTrash = { viewModel.restoreFromTrash(it) },
                                onDeletePermanently = { viewModel.deletePermanently(it) },
                                onClearTrash = { viewModel.clearTrash() },
                                onTrimVideo = { item, startMs, endMs -> viewModel.trimVideo(item, startMs, endMs) {} },
                                onExtractAudio = { item -> viewModel.extractAudio(item) {} },
                                onMakeGif = { item, startMs, durMs -> viewModel.createGif(item, startMs, durMs) {} },
                                onCompressVideo = { item -> viewModel.compressVideo(item) {} }
                            )
                            2 -> GalleryScreen(
                                videoList = videos,
                                screenshotList = screenshots,
                                trashList = trashRecordings,
                                onPlayVideo = { viewModel.selectMediaForView(it) },
                                onSaveToGallery = { viewModel.saveMediaToGallery(it) },
                                onRename = { item, newTitle -> viewModel.renameMedia(item, newTitle) },
                                onDelete = { item -> viewModel.deleteMedia(item) },
                                onToggleFavorite = { item -> viewModel.toggleFavorite(item) },
                                onRestoreFromTrash = { viewModel.restoreFromTrash(it) },
                                onDeletePermanently = { viewModel.deletePermanently(it) },
                                onClearTrash = { viewModel.clearTrash() },
                                onTrimVideo = { item, startMs, endMs -> viewModel.trimVideo(item, startMs, endMs) {} },
                                onExtractAudio = { item -> viewModel.extractAudio(item) {} },
                                onMakeGif = { item, startMs, durMs -> viewModel.createGif(item, startMs, durMs) {} },
                                onCompressVideo = { item -> viewModel.compressVideo(item) {} }
                            )
                            3 -> SettingsScreen(
                                viewModel = viewModel,
                                settings = settings,
                                onRequestOverlayPermission = onRequestOverlayPermission
                            )
                        }
                    }
                }
            }
    }

    // Live countdown overlay when active
    countdown?.let { sec ->
        CountdownOverlay(secondsLeft = sec)
    }
}
}


