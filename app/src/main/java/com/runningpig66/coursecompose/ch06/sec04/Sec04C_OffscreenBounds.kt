package com.runningpig66.coursecompose.ch06.sec04

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026/9/24
 * @time 3:32
 */
@Composable
fun Sec04C_OffscreenBounds() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Text(
                text = "Offscreen Buffer Bounds",
                style = MaterialTheme.typography.headlineSmall
            )

            BoundsExample(
                title = "Auto",
                description = "没有触发离屏光栅化",
                strategy = CompositingStrategy.Auto
            )

            BoundsExample(
                title = "Offscreen",
                description = "强制创建 Offscreen Buffer",
                strategy = CompositingStrategy.Offscreen
            )
        }
    }
}

@Composable
private fun BoundsExample(
    title: String,
    description: String,
    strategy: CompositingStrategy
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall
        )

        Box(
            modifier = Modifier
                .size(220.dp)
                .background(Color(0xFFC1C1C1))
        ) {
            Canvas(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        // alpha = 0.5f // test code
                        compositingStrategy = strategy
                    }
                    // 单独画出 Canvas 的真实布局边界，方便观察。
                    .border(2.dp, Color.Black)
            ) {
                drawRect(
                    color = Color(0xFFE040FB),
                    size = Size(
                        width = 200.dp.toPx(),
                        height = 200.dp.toPx()
                    )
                )
            }
        }
    }
}

@PhonePreviews
@Composable
fun Sec04C_OffscreenBoundsPreview() {
    CourseComposeTheme {
        Sec04C_OffscreenBounds()
    }
}
