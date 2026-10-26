package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RecordingItem
import com.example.ui.theme.RecordRed
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.StorageUtils
import com.example.util.TimeUtils

@Composable
fun DetailsDialog(
    item: RecordingItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("File Details", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                DetailRow("Title", item.title)
                DetailRow("Format", if (item.isScreenshot) "PNG Image" else "MP4 Video")
                if (!item.isScreenshot) {
                    DetailRow("Duration", TimeUtils.formatDuration(item.durationMs))
                    DetailRow("Resolution", item.resolutionLabel)
                    DetailRow("Frame Rate", "${item.fps} FPS")
                    DetailRow("Audio", item.audioSource.uppercase())
                }
                DetailRow("Size", StorageUtils.formatBytes(item.sizeBytes))
                DetailRow("Date", TimeUtils.formatDate(item.createdAt))
                DetailRow("File Path", item.filePath)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = RecordRed)
            ) {
                Text("Close")
            }
        },
        containerColor = StudioSurfaceVariant,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label: ",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 13.sp,
            modifier = Modifier.weight(0.65f)
        )
    }
}
