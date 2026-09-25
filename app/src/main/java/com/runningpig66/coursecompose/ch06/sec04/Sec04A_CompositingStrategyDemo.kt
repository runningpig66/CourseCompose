package com.runningpig66.coursecompose.ch06.sec04

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026/9/22
 * @time 11:16
 */
@Composable
fun Sec04A_CompositingStrategyDemo() {
    var layerAlpha by remember { mutableFloatStateOf(0.5f) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "CompositingStrategy",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(text = "当前图层 Alpha：$layerAlpha")

            Button(
                onClick = {
                    layerAlpha = if (layerAlpha == 1f) 0.5f else 1f
                }
            ) {
                Text(text = if (layerAlpha == 1f) "切换到 0.5" else "切换到 1.0")
            }

            StrategyExample(
                title = "Auto",
                description = "默认合成策略",
                layerAlpha = layerAlpha,
                strategy = CompositingStrategy.Auto
            )

            StrategyExample(
                title = "Offscreen",
                description = "强制使用离屏缓冲",
                layerAlpha = layerAlpha,
                strategy = CompositingStrategy.Offscreen
            )

            StrategyExample(
                title = "ModulateAlpha",
                description = "把 Alpha 调制到每条绘制指令",
                layerAlpha = layerAlpha,
                strategy = CompositingStrategy.ModulateAlpha
            )
        }
    }
}

@Composable
private fun StrategyExample(
    title: String,
    description: String,
    layerAlpha: Float,
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
                .height(130.dp)
                .background(Color(0xFFF1F1F1))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = layerAlpha
                        compositingStrategy = strategy
                    }
            ) {
                val horizontalPadding = 24.dp.toPx()
                val top = 20.dp.toPx()
                val rectHeight = size.height - 40.dp.toPx()
                val rectWidth = size.width * 0.6f

                drawRect(
                    color = Color.Red,
                    topLeft = Offset(
                        x = horizontalPadding,
                        y = top
                    ),
                    size = Size(
                        width = rectWidth,
                        height = rectHeight
                    )
                )

                drawRect(
                    color = Color.Blue,
                    topLeft = Offset(
                        x = size.width - horizontalPadding - rectWidth,
                        y = top
                    ),
                    size = Size(
                        width = rectWidth,
                        height = rectHeight
                    )
                )
            }
        }
    }
}

@PhonePreviews
@Composable
fun Sec04A_CompositingStrategyDemoPreview() {
    CourseComposeTheme {
        Sec04A_CompositingStrategyDemo()
    }
}
