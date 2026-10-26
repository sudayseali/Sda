package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.with
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RecordRed

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CountdownOverlay(secondsLeft: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = secondsLeft,
            transitionSpec = {
                scaleIn(animationSpec = tween(400)) togetherWith scaleOut(animationSpec = tween(400))
            },
            label = "countdown"
        ) { targetSec ->
            Text(
                text = targetSec.toString(),
                color = RecordRed,
                fontSize = 120.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
