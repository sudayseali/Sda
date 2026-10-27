package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.RecordingItem
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.DetailsDialog
import com.example.ui.components.RenameDialog
import com.example.ui.components.VideoTrimmerDialog
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.RecordRed
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.MediaUtils
import com.example.util.StorageUtils
import com.example.util.TimeUtils
import java.io.File

@Composable
fun GalleryScreen(
    videoList: List<RecordingItem>,
    screenshotList: List<RecordingItem>,
    trashList: List<RecordingItem> = emptyList(),
    onPlayVideo: (RecordingItem) -> Unit,
    onSaveToGallery: (RecordingItem) -> Unit = {},
    onRename: (RecordingItem, String) -> Unit,
    onDelete: (RecordingItem) -> Unit,
    onToggleFavorite: (RecordingItem) -> Unit,
    onRestoreFromTrash: (RecordingItem) -> Unit = {},
    onDeletePermanently: (RecordingItem) -> Unit = {},
    onClearTrash: () -> Unit = {},
    onTrimVideo: (RecordingItem, Long, Long) -> Unit = { _, _, _ -> },
    onExtractAudio: (RecordingItem) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var itemToRename by remember { mutableStateOf<RecordingItem?>(null) }
    var itemToDelete by remember { mutableStateOf<RecordingItem?>(null) }
    var itemForDetails by remember { mutableStateOf<RecordingItem?>(null) }
    var itemToTrim by remember { mutableStateOf<RecordingItem?>(null) }
    var showClearTrashConfirm by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = StudioSurface,
            contentColor = RecordRed
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "Videos (${videoList.size})",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 0) RecordRed else TextSecondary,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.testTag("tab_videos")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "Screenshots (${screenshotList.size})",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 1) RecordRed else TextSecondary,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.testTag("tab_screenshots")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Text(
                        text = "Trash (${trashList.size})",
                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 2) RecordRed else TextSecondary,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.testTag("tab_trash")
            )
        }

        when (selectedTab) {
            0 -> {
                if (videoList.isEmpty()) {
                    EmptyGalleryState(isScreenshots = false)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("gallery_video_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(videoList, key = { it.id }) { item ->
                            MediaCard(
                                item = item,
                                onClick = { onPlayVideo(item) },
                                onPlayClick = { onPlayVideo(item) },
                                onSaveToGalleryClick = { onSaveToGallery(item) },
                                onRenameClick = { itemToRename = item },
                                onTrimClick = { itemToTrim = item },
                                onDeleteClick = { itemToDelete = item },
                                onDetailsClick = { itemForDetails = item },
                                onShareClick = { MediaUtils.shareMedia(context, item.filePath, false) },
                                onFavoriteClick = { onToggleFavorite(item) }
                            )
                        }
                    }
                }
            }
            1 -> {
                if (screenshotList.isEmpty()) {
                    EmptyGalleryState(isScreenshots = true)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("gallery_screenshot_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(screenshotList, key = { it.id }) { item ->
                            MediaCard(
                                item = item,
                                onClick = { MediaUtils.shareMedia(context, item.filePath, true) },
                                onPlayClick = {},
                                onSaveToGalleryClick = { onSaveToGallery(item) },
                                onRenameClick = { itemToRename = item },
                                onTrimClick = null,
                                onDeleteClick = { itemToDelete = item },
                                onDetailsClick = { itemForDetails = item },
                                onShareClick = { MediaUtils.shareMedia(context, item.filePath, true) },
                                onFavoriteClick = { onToggleFavorite(item) }
                            )
                        }
                    }
                }
            }
            2 -> {
                // Trash Tab
                if (trashList.isEmpty()) {
                    EmptyTrashState()
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Trash action bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Auto-purged after 30 days",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )
                            Button(
                                onClick = { showClearTrashConfirm = true },
                                colors = ButtonDefaults.buttonColors(containerColor = RecordRed.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("clear_trash_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = RecordRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Empty Trash", color = RecordRed, fontSize = 12.sp)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("gallery_trash_list"),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(trashList, key = { it.id }) { item ->
                                TrashItemCard(
                                    item = item,
                                    onRestore = { onRestoreFromTrash(item) },
                                    onDeletePermanently = { onDeletePermanently(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    itemToRename?.let { item ->
        RenameDialog(
            item = item,
            onDismiss = { itemToRename = null },
            onConfirm = { newTitle ->
                onRename(item, newTitle)
                itemToRename = null
            }
        )
    }

    itemToTrim?.let { item ->
        VideoTrimmerDialog(
            item = item,
            onDismiss = { itemToTrim = null },
            onTrim = { startMs, endMs ->
                onTrimVideo(item, startMs, endMs)
                itemToTrim = null
            },
            onExtractAudio = {
                onExtractAudio(item)
                itemToTrim = null
            }
        )
    }

    itemToDelete?.let { item ->
        DeleteConfirmDialog(
            item = item,
            onDismiss = { itemToDelete = null },
            onConfirm = {
                onDelete(item)
                itemToDelete = null
            }
        )
    }

    itemForDetails?.let { item ->
        DetailsDialog(
            item = item,
            onDismiss = { itemForDetails = null }
        )
    }

    if (showClearTrashConfirm) {
        AlertDialog(
            onDismissRequest = { showClearTrashConfirm = false },
            title = { Text("Empty Trash?") },
            text = { Text("All recordings in the trash will be deleted permanently and cannot be recovered.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearTrash()
                        showClearTrashConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RecordRed)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearTrashConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaCard(
    item: RecordingItem,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onSaveToGalleryClick: () -> Unit,
    onRenameClick: () -> Unit,
    onTrimClick: (() -> Unit)?,
    onDeleteClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onShareClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("media_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
        border = BorderStroke(1.dp, StudioCardBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Media Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (item.isScreenshot) 170.dp else 190.dp)
                    .background(Color(0xFF0B0C10))
            ) {
                if (item.isScreenshot) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(item.filePath))
                            .crossfade(true)
                            .build(),
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF1E212E), Color(0xFF0F1017))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(RecordRed.copy(alpha = 0.9f))
                                .clickable(onClick = onPlayClick)
                                .testTag("play_button_${item.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Duration Badge
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = TimeUtils.formatDuration(item.durationMs),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Quality Badge
                    Surface(
                        color = AccentCyan.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = item.resolutionLabel,
                            color = AccentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Info row & quick menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${StorageUtils.formatBytes(item.sizeBytes)} • ${TimeUtils.formatDate(item.createdAt)}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                // Favorite heart
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (item.isFavorite) RecordRed else TextTertiary
                    )
                }

                // Quick save to gallery
                IconButton(
                    onClick = onSaveToGalleryClick,
                    modifier = Modifier.testTag("save_gallery_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.SaveAlt,
                        contentDescription = "Save to Gallery",
                        tint = RecordRed
                    )
                }

                // Quick share
                IconButton(onClick = onShareClick) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = TextSecondary
                    )
                }

                // Menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("menu_button_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = TextSecondary
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(StudioSurfaceVariant)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Save to Gallery (Ku keydi)", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.SaveAlt, contentDescription = null, tint = RecordRed) },
                            onClick = {
                                menuExpanded = false
                                onSaveToGalleryClick()
                            }
                        )
                        if (onTrimClick != null) {
                            DropdownMenuItem(
                                text = { Text("Trim & Extract Audio", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null, tint = AccentCyan) },
                                onClick = {
                                    menuExpanded = false
                                    onTrimClick()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Rename", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary) },
                            onClick = {
                                menuExpanded = false
                                onRenameClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Details", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary) },
                            onClick = {
                                menuExpanded = false
                                onDetailsClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move to Trash", color = RecordRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = RecordRed) },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrashItemCard(
    item: RecordingItem,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant.copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, StudioCardBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${if (item.isScreenshot) "Screenshot" else item.resolutionLabel} • ${StorageUtils.formatBytes(item.sizeBytes)}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            // Restore action
            IconButton(
                onClick = onRestore,
                modifier = Modifier.testTag("restore_trash_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = "Restore",
                    tint = AccentCyan
                )
            }

            // Delete permanently action
            IconButton(
                onClick = onDeletePermanently,
                modifier = Modifier.testTag("delete_permanent_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = "Delete Permanently",
                    tint = RecordRed
                )
            }
        }
    }
}

@Composable
private fun EmptyGalleryState(isScreenshots: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(RecordRed.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isScreenshots) Icons.Default.VideoLibrary else Icons.Default.Videocam,
                    contentDescription = null,
                    tint = RecordRed,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isScreenshots) "No Screenshots Yet" else "No Recordings Yet",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isScreenshots) {
                    "Captured screenshots will appear here"
                } else {
                    "Your recorded screen videos will appear here"
                },
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun EmptyTrashState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Trash is Empty",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Deleted videos and screenshots will be safely kept here for 30 days before permanent deletion.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
