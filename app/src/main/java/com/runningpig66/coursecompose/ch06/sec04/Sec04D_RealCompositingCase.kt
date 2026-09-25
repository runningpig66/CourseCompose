package com.runningpig66.coursecompose.ch06.sec04

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026/9/24
 * @time 13:59
 */
@Composable
fun Sec04D_RealCompositingCase() {
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
                text = "Compositing Strategy Decisions",
                style = MaterialTheme.typography.headlineSmall
            )

            ExampleSection(
                title = "1. Auto：普通组件整体透明",
                description = "复杂内容，保持默认整层合成"
            ) {
                DisabledCategoryCard()
            }

            ExampleSection(
                title = "2. Offscreen：需要像素隔离",
                description = "Clear / Mask 等操作限定在当前 Layer"
            ) {
                CutoutStatusBadge()
            }

            ExampleSection(
                title = "3. ModulateAlpha：内容明确不重叠",
                description = "逐条 alpha 与整层 alpha 视觉等价"
            ) {
                SignalIndicator(alpha = 0.4f)
            }
        }
    }
}

@Composable
private fun ExampleSection(
    title: String,
    description: String,
    content: @Composable () -> Unit
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

        content()
    }
}

@Composable
private fun DisabledCategoryCard() {
    Box(
        modifier = Modifier
            .size(
                width = 220.dp,
                height = 90.dp
            )
            .background(
                color = Color(0xFF536DFE),
                shape = RoundedCornerShape(18.dp)
            )
            // addition 2: graphicsLayer 在 Modifier 的调用链中的书写顺序有影响吗？
            // Tip: background 在 graphicsLayer 外面，所以 graphicsLayer 的 alpha = 0.5f 不会把
            // 前面的 background 一起包进这个 Layer；它主要影响后面的内容。
            .graphicsLayer {
                alpha = 0.5f
                compositingStrategy = CompositingStrategy.Auto
            }
        // .alpha(0.5f)
        ,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "餐饮",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "¥ 128",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    // 复制上个 Box，仅对比展示 graphicsLayer 不同位置是否影响到蓝色背景
    Box(
        modifier = Modifier
            .size(
                width = 220.dp,
                height = 90.dp
            )
            // addition 2: graphicsLayer 的位置有意义：它只把后续绘制内容包含进当前 Layer。
            // 如果希望背景、内容一起应用 alpha，应把 graphicsLayer 放在 background 前面。
            .graphicsLayer {
                alpha = 0.5f
                compositingStrategy = CompositingStrategy.Auto
            }
            // .alpha(0.5f)
            .background(
                color = Color(0xFF536DFE),
                shape = RoundedCornerShape(18.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "餐饮",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "¥ 128",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun CutoutStatusBadge() {
    Box(
        modifier = Modifier
            .size(120.dp)
            .background(Color(0xFFF5FFBE))
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawCircle(
                    color = Color(0xFF536DFE),
                    radius = size.minDimension * 0.42f
                )

                drawContent()

                val badgeCenter = Offset(
                    x = size.width * 0.76f,
                    y = size.height * 0.76f
                )

                drawCircle(
                    color = Color.White,
                    radius = 19.dp.toPx(),
                    center = badgeCenter,
                    blendMode = BlendMode.Clear
                )

                drawCircle(
                    color = Color(0xFF00C853),
                    radius = 13.dp.toPx(),
                    center = badgeCenter
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

@Composable
private fun SignalIndicator(
    alpha: Float
) {
    Canvas(
        modifier = Modifier
            .size(
                width = 120.dp,
                height = 48.dp
            )
            .graphicsLayer {
                this.alpha = alpha
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
    ) {
        val barWidth = 18.dp.toPx()
        val gap = 12.dp.toPx()

        repeat(4) { index ->
            val heightRatio = (index + 1) / 4f
            val barHeight = size.height * heightRatio

            drawRect(
                color = Color(0xFF536DFE),
                topLeft = Offset(
                    x = index * (barWidth + gap),
                    y = size.height - barHeight
                ),
                size = Size(
                    width = barWidth,
                    height = barHeight
                )
            )
        }
    }
}

@PhonePreviews
@Composable
fun Sec04D_RealCompositingCasePreview() {
    CourseComposeTheme {
        Sec04D_RealCompositingCase()
    }
}
