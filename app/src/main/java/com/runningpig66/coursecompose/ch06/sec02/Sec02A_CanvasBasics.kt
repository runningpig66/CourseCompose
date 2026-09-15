package com.runningpig66.coursecompose.ch06.sec02

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026/08/23 周日
 * @time 3:14
 */
@Composable
fun Sec02A_CanvasBasics() {
    var canvasWidth by remember { mutableStateOf(280.dp) }
    var measuredSize by remember { mutableStateOf(IntSize.Zero) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Canvas target width: $canvasWidth\n" +
                        "Measured size: ${measuredSize.width} × ${measuredSize.height} px"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    canvasWidth = 120.dp
                }) {
                    Text(text = "120 dp")
                }

                Button(onClick = {
                    canvasWidth = 340.dp
                }) {
                    Text(text = "340 dp")
                }
            }

            Canvas(
                modifier = Modifier
                    .width(canvasWidth)
                    .height(240.dp)
                    .onSizeChanged { measuredSize = it }
            ) {
                // 整个 Canvas 的绘制区域
                drawRect(color = Color(0xFFF1F3F5))
                // 左上角四分之一区域
                drawRect(
                    color = Color(0xFF90CAF9),
                    topLeft = Offset.Zero,
                    size = Size(
                        width = size.width / 2f,
                        height = size.height / 2f
                    )
                )
                // 水平中心线
                drawLine(
                    color = Color(0xFF616161),
                    start = Offset(
                        x = 0f,
                        y = center.y
                    ),
                    end = Offset(
                        x = size.width,
                        y = center.y
                    ),
                    strokeWidth = 2.dp.toPx()
                )
                // 垂直中心线
                drawLine(
                    color = Color(0xFF616161),
                    start = Offset(
                        x = center.x,
                        y = 0f
                    ),
                    end = Offset(
                        x = center.x,
                        y = size.height
                    ),
                    strokeWidth = 2.dp.toPx()
                )
                // 左上到右下的对角线
                drawLine(
                    color = Color(0xFFE53935),
                    start = Offset.Zero,
                    end = Offset(
                        x = size.width,
                        y = size.height
                    ),
                    strokeWidth = 3.dp.toPx()
                )
                // 半径按照 Canvas 较短边的 12% 计算
                val circleRadius = minOf(
                    size.width,
                    size.height
                ) * 0.12f
                drawCircle(
                    color = Color(0xFF43A047),
                    radius = circleRadius,
                    // radius = 60f,
                    center = center,
                    style = Stroke(
                        width = 2.dp.toPx()
                    )
                )
            }
        }
    }
}

@PhonePreviews
@Composable
fun Sec02A_CanvasBasicsPreview() {
    CourseComposeTheme {
        Sec02A_CanvasBasics()
    }
}
