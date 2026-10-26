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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.AudioSourceType
import com.example.ui.MainViewModel
import com.example.ui.components.CountdownOverlay
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.RecordScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.theme.RecordRed
import com.example.ui.theme.ScreenRecorderTheme
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.startRecordingFlow(result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, "Screen recording permission cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Continue to screen capture intent even if optional permissions vary
        requestScreenCapture()
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
            requestScreenCapture()
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
                onBack = { viewModel.selectMediaForView(null) }
            )
        } else {
            Scaffold(
                containerColor = StudioDarkBg,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    NavigationBar(
                        containerColor = StudioSurface,
                        contentColor = TextPrimary,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("main_bottom_nav")
                    ) {
                        NavigationBarItem(
                            selected = currentTab == 0,
                            onClick = { currentTab = 0 },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.FiberManualRecord,
                                    contentDescription = "Record",
                                    tint = if (currentTab == 0) RecordRed else TextSecondary
                                )
                            },
                            label = {
                                Text(
                                    text = "Record",
                                    color = if (currentTab == 0) RecordRed else TextSecondary,
                                    fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = RecordRed.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.testTag("nav_record_tab")
                        )

                        NavigationBarItem(
                            selected = currentTab == 1,
                            onClick = { currentTab = 1 },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = "Gallery",
                                    tint = if (currentTab == 1) RecordRed else TextSecondary
                                )
                            },
                            label = {
                                Text(
                                    text = "Recordings",
                                    color = if (currentTab == 1) RecordRed else TextSecondary,
                                    fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = RecordRed.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.testTag("nav_gallery_tab")
                        )

                        NavigationBarItem(
                            selected = currentTab == 2,
                            onClick = { currentTab = 2 },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = if (currentTab == 2) RecordRed else TextSecondary
                                )
                            },
                            label = {
                                Text(
                                    text = "Settings",
                                    color = if (currentTab == 2) RecordRed else TextSecondary,
                                    fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = RecordRed.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.testTag("nav_settings_tab")
                        )
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    when (currentTab) {
                        0 -> RecordScreen(
                            viewModel = viewModel,
                            recordingState = recordingState,
                            durationMs = durationMs,
                            settings = settings,
                            storageInfo = storageInfo,
                            onStartRecordingClick = onRequestRecord,
                            onRequestOverlayPermission = onRequestOverlayPermission,
                            onNavigateToSettings = { currentTab = 2 }
                        )
                        1 -> GalleryScreen(
                            videoList = videos,
                            screenshotList = screenshots,
                            onPlayVideo = { viewModel.selectMediaForView(it) },
                            onRename = { item, newTitle -> viewModel.renameMedia(item, newTitle) },
                            onDelete = { item -> viewModel.deleteMedia(item) },
                            onToggleFavorite = { item -> viewModel.toggleFavorite(item) }
                        )
                        2 -> SettingsScreen(
                            viewModel = viewModel,
                            settings = settings,
                            onRequestOverlayPermission = onRequestOverlayPermission
                        )
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
