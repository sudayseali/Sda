package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RecordingItem
import com.example.ui.theme.RecordRed
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.TimeUtils

@Composable
fun GifMakerDialog(
    item: RecordingItem,
    onDismiss: () -> Unit,
    onMakeGif: (startMs: Long, durationMs: Long) -> Unit
) {
    val totalDurationMs = item.durationMs.coerceAtLeast(1000L)
    var startPosMs by remember { mutableFloatStateOf(0f) }
    var gifDurationSec by remember { mutableFloatStateOf(3f) }
    var isProcessing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        containerColor = StudioSurfaceVariant,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Gif,
                    contentDescription = null,
                    tint = RecordRed,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Create Animated GIF",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Convert a snippet of \"${item.title}\" into a lightweight animated GIF for WhatsApp, Telegram & Discord.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Start Offset Slider
                Text(
                    text = "Start Time: ${TimeUtils.formatDuration(startPosMs.toLong())}",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = startPosMs,
                    onValueChange = { startPosMs = it },
                    valueRange = 0f..(totalDurationMs - 1000L).coerceAtLeast(0L).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = RecordRed,
                        activeTrackColor = RecordRed
                    ),
                    enabled = !isProcessing,
                    modifier = Modifier.testTag("slider_gif_start")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Duration Slider (1s - 6s)
                val maxAllowedDuration = ((totalDurationMs - startPosMs.toLong()) / 1000f).coerceIn(1f, 6f)
                val currentDuration = gifDurationSec.coerceAtMost(maxAllowedDuration)

                Text(
                    text = "GIF Duration: ${String.format("%.1f", currentDuration)} seconds",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = currentDuration,
                    onValueChange = { gifDurationSec = it },
                    valueRange = 1f..maxAllowedDuration.coerceAtLeast(1.1f),
                    colors = SliderDefaults.colors(
                        thumbColor = RecordRed,
                        activeTrackColor = RecordRed
                    ),
                    enabled = !isProcessing,
                    modifier = Modifier.testTag("slider_gif_duration")
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
                            color = RecordRed,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Extracting frames and encoding GIF...",
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
                    onMakeGif(startPosMs.toLong(), (gifDurationSec * 1000L).toLong())
                },
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = RecordRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_confirm_make_gif")
            ) {
                Text("Generate GIF", fontWeight = FontWeight.Bold)
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
