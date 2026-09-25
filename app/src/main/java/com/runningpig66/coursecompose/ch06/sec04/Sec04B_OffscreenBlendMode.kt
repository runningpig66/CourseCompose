package com.runningpig66.coursecompose.ch06.sec04

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026/9/23
 * @time 4:20
 *
 * Offscreen 可以让 BlendMode 只作用于当前 Composable 的内容，而不影响已经绘制在其后的背景内容。
 */
@Composable
fun Sec04B_OffscreenBlendMode() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "BlendMode.Clear",
                style = MaterialTheme.typography.headlineSmall
            )

            StatusAvatarExample(
                title = "Auto",
                description = "alpha = 1f，不主动创建 Offscreen Buffer",
                strategy = CompositingStrategy.Auto
            )

            StatusAvatarExample(
                title = "Offscreen",
                description = "先把当前 Layer 绘制到 Offscreen Buffer",
                strategy = CompositingStrategy.Offscreen
            )
        }
    }
}

@Composable
private fun StatusAvatarExample(
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
                .fillMaxWidth()
                .height(190.dp)
                .background(Color(0xFFFFF1B8))
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.Center)
                    .graphicsLayer {
                        // alpha = 0.99f // test code
                        compositingStrategy = strategy
                    }
                    .drawWithContent {
                        val avatarRadius = size.minDimension * 0.42f
                        val statusCenter = Offset(
                            x = center.x + avatarRadius * 0.72f,
                            y = center.y + avatarRadius * 0.72f
                        )
                        val cutoutRadius = 20.dp.toPx()
                        val statusRadius = 14.dp.toPx()

                        // 1. 头像主体
                        drawCircle(
                            color = Color(0xFF536DFE),
                            radius = avatarRadius
                        )

                        // 2. 绘制 Box 中真正的内容：中间的字母 A
                        drawContent()

                        // 3. 先挖出一个较大的圆形区域
                        drawCircle(
                            // BlendMode.Clear 会清除当前 destination 中被 source 圆覆盖的像素。
                            // 这里的 color 不决定最终显示颜色，但 drawCircle() 的 API 仍要求传入 color。
                            color = Color.White,
                            radius = cutoutRadius,
                            center = statusCenter,
                            blendMode = BlendMode.Clear
                        )

                        // 4. 再画一个更小的在线状态点
                        drawCircle(
                            color = Color(0xFF00C853),
                            radius = statusRadius,
                            center = statusCenter
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }
    }
}

@PhonePreviews
@Composable
fun Sec04B_OffscreenBlendModePreview() {
    CourseComposeTheme {
        Sec04B_OffscreenBlendMode()
    }
}
