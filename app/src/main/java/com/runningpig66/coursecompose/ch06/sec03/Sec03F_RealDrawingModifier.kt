package com.runningpig66.coursecompose.ch06.sec03

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log

/**
 * @author runningpig66
 * @date 2026/9/21
 * @time 17:24
 */
private const val Sec03F = "Sec03F"

@Composable
fun Sec03F_RealDrawingModifier() {
    var selected by remember { mutableStateOf(true) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "日程卡片",
                style = MaterialTheme.typography.headlineSmall
            )

            Button(
                onClick = { selected = !selected }
            ) {
                Text(text = if (selected) "取消选中" else "选中日程")
            }

            CalendarEventCard(
                title = "Compose Drawing",
                time = "14:00 - 16:00",
                selected = selected
            )
        }
    }
}

@Composable
private fun CalendarEventCard(
    title: String,
    time: String,
    selected: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            // 第一层：简单背景绘制，没有值得缓存的对象。
            .drawBehind {
                drawRoundRect(
                    color = Color(0xFF20232D),
                    cornerRadius = CornerRadius(20.dp.toPx())
                )
                drawRect(
                    color = Color(0xFF7986CB),
                    size = Size(width = 6.dp.toPx(), height = size.height)
                )
            }
            // 第二层：曲线 Path 与当前 size 强相关，但尺寸不变时可以跨多次 Draw 复用。
            .drawWithCache {
                log(Sec03F, "Build decoration path, size=$size")

                val decorationPath = Path().apply {
                    moveTo(x = size.width * 0.55f, y = size.height)
                    quadraticTo(
                        x1 = size.width * 0.78f, y1 = size.height * 0.40f,
                        x2 = size.width, y2 = size.height * 0.58f
                    )
                    lineTo(x = size.width, y = size.height)
                    close()
                }

                onDrawBehind {
                    drawPath(
                        path = decorationPath,
                        color = Color(0xFF7986CB).copy(alpha = 0.18f)
                    )
                }
            }
            .padding(
                horizontal = 12.dp,
                vertical = 10.dp
            )
            // 第三层：selected 只决定当前这一帧是否需要在内容上覆盖高亮。
            .drawWithContent {
                drawContent()

                // 注意 selected 直接在 Draw lambda 中读取。
                if (selected) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.10f),
                        cornerRadius = CornerRadius(20.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color(0xFF9FA8DA),
                        cornerRadius = CornerRadius(20.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = time,
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Modifier 绘制体系练习",
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@PhonePreviews
@Composable
fun Sec03F_RealDrawingModifierPreview() {
    CourseComposeTheme {
        Sec03F_RealDrawingModifier()
    }
}
