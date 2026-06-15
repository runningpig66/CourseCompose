package com.runningpig66.coursecompose

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-06-02
 * @time 4:08
 */
@Composable
fun WaveLoadingDemo() {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            WaveLoadingIndicator()
        }
    }
}

@Composable
fun WaveLoadingIndicator(
    modifier: Modifier = Modifier,
    circleColor: Color = Color(0xFF2196F3),
    circleSize: Float = 16f,
    travelDistance: Float = 20f
) {
    val infiniteTransition = rememberInfiniteTransition()
    val duration = 300

    val circle1OffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -travelDistance,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = duration),
            repeatMode = RepeatMode.Reverse
        ),
        label = "circle1OffsetY"
    )

    val circle2OffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -travelDistance,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = duration),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(
                offsetMillis = 100,
                offsetType = StartOffsetType.FastForward
            )
        ),
        label = "circle2OffsetY"
    )

    val circle3OffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -travelDistance,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = duration),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(
                offsetMillis = 200,
                offsetType = StartOffsetType.FastForward
            )
        ),
        label = "circle3OffsetY"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val yOffsets = listOf(circle1OffsetY, circle2OffsetY, circle3OffsetY)
        yOffsets.forEach { yOffsetY ->
            Box(
                modifier = Modifier
                    .size(circleSize.dp)
                    .offset(x = 0.dp, y = yOffsetY.dp)
                    .background(color = circleColor, shape = CircleShape)
            )
        }
    }
}

@PhonePreviews
@Composable
fun WaveLoadingDemoPreview() {
    CourseComposeTheme {
        WaveLoadingDemo()
    }
}
