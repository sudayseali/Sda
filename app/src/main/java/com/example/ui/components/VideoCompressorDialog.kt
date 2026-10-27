package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RecordingItem
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.RecordRed
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.StorageUtils

@Composable
fun VideoCompressorDialog(
    item: RecordingItem,
    onDismiss: () -> Unit,
    onCompress: (targetScale: Float) -> Unit
) {
    var selectedPreset by remember { mutableIntStateOf(0) }
    var isProcessing by remember { mutableStateOf(false) }

    val originalSizeStr = StorageUtils.formatBytes(item.sizeBytes)
    val estimatedSize70 = StorageUtils.formatBytes((item.sizeBytes * 0.35f).toLong())
    val estimatedSize85 = StorageUtils.formatBytes((item.sizeBytes * 0.18f).toLong())

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        containerColor = StudioSurfaceVariant,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Compress,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Compress Video",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Reduce file size for fast uploading and sharing over WhatsApp, Telegram, or Email.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Original Size: $originalSizeStr",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Preset 1: WhatsApp / Social (Balanced)
                CompressOptionRow(
                    title = "WhatsApp & Social (Recommended)",
                    subtitle = "720p HD • Est. size ~$estimatedSize70 (~65% reduction)",
                    isSelected = selectedPreset == 0,
                    onClick = { if (!isProcessing) selectedPreset = 0 },
                    testTag = "preset_whatsapp"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Preset 2: Maximum Compression
                CompressOptionRow(
                    title = "Maximum Compression (Ultra Small)",
                    subtitle = "480p SD • Est. size ~$estimatedSize85 (~82% reduction)",
                    isSelected = selectedPreset == 1,
                    onClick = { if (!isProcessing) selectedPreset = 1 },
                    testTag = "preset_max_compress"
                )

                if (isProcessing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StudioSurface, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = AccentCyan,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Compressing video and optimizing bitrate...",
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isProcessing = true
                    val scale = if (selectedPreset == 0) 0.65f else 0.45f
                    onCompress(scale)
                },
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_confirm_compress")
            ) {
                Text("Start Compressing", fontWeight = FontWeight.Bold, color = StudioSurface)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isProcessing,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun CompressOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val borderColor = if (isSelected) AccentCyan else StudioCardBorder
    val bgColor = if (isSelected) AccentCyan.copy(alpha = 0.12f) else StudioSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isSelected) AccentCyan else TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
