package com.runningpig66.coursecompose.ch06.sec03

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log

/**
 * @author runningpig66
 * @date 2026/9/21
 * @time 5:54
 *
 * 三个按钮分别验证三个不同问题：
 * 1. 只改变 Draw 阶段读取的状态；
 * 2. 改变 Cache Build 阶段读取的状态；
 * 3. 改变绘制区域尺寸。
 */
private const val Sec03D = "Sec03D"

private class DrawCacheDebugCounter {
    var buildCount = 0
    var drawCount = 0
}

@Composable
fun Sec03D_DrawWithCacheLifecycle() {

    // 在 Cache Build 中读取
    val gradientVariant = remember { mutableIntStateOf(0) }

    // 只在真正 Draw 时读取
    val circlePosition = remember { mutableFloatStateOf(0.25f) }

    // 用于改变组件尺寸
    var expanded by remember { mutableStateOf(false) }

    // 普通 Kotlin 对象，只用来记录日志次数，故意不用 Compose State，避免它自己参与失效机制
    val counter = remember { DrawCacheDebugCounter() }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "drawWithCache 生命周期",
                style = MaterialTheme.typography.headlineSmall
            )

            Button(
                onClick = {
                    circlePosition.floatValue =
                        if (circlePosition.floatValue < 0.5f) 0.75f else 0.25f
                }
            ) {
                Text("1. 移动圆：只触发 Draw")
            }

            Button(
                onClick = {
                    gradientVariant.intValue =
                        if (gradientVariant.intValue == 0) 1 else 0
                }
            ) {
                Text("2. 切换渐变：使 Cache 失效")
            }

            Button(
                onClick = { expanded = !expanded }
            ) {
                Text("3. 改变尺寸")
            }

            Box(
                modifier = Modifier
                    .width(if (expanded) 320.dp else 240.dp)
                    .height(180.dp)
                    .drawWithCache {
                        counter.buildCount++

                        // 关键：gradientVariant 在 Cache Build 中读取。
                        val variant = gradientVariant.intValue
                        // variant=0 蓝色系渐变，=1 红色系渐变
                        val gradientBrush =
                            if (variant == 0) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF2540D2),
                                        Color(0xFF1EB2A4)
                                    ),
                                    start = Offset.Zero,
                                    end = Offset(
                                        x = size.width,
                                        y = size.height
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFED1818),
                                        Color(0xFFA76F1B)
                                    ),
                                    start = Offset.Zero,
                                    end = Offset(
                                        x = size.width,
                                        y = size.height
                                    )
                                )
                            }

                        // Path 同样只在 Cache Build 时创建，而且它依赖当前 size。
                        val decorationPath = Path().apply {
                            moveTo(x = 0f, y = size.height * 0.72f)
                            quadraticTo(
                                x1 = size.width * 0.5f, y1 = size.height * 0.42f,
                                x2 = size.width, y2 = size.height * 0.72f
                            )
                            lineTo(x = size.width, y = size.height)
                            lineTo(x = 0f, y = size.height)
                            close()
                        }

                        log(
                            Sec03D,
                            "CACHE BUILD #${counter.buildCount}, " +
                                    "variant=$variant, size=$size"
                        )

                        onDrawWithContent {
                            counter.drawCount++

                            // 注意：circlePosition 到这里才读取。
                            val position = circlePosition.floatValue

                            log(
                                Sec03D,
                                "DRAW #${counter.drawCount}, " +
                                        "circlePosition=$position"
                            )

                            drawRect(brush = gradientBrush)

                            drawPath(
                                path = decorationPath,
                                color = Color.White.copy(alpha = 0.22f)
                            )

                            drawContent()

                            drawCircle(
                                color = Color.White,
                                radius = 14.dp.toPx(),
                                center = Offset(
                                    x = size.width * position,
                                    y = size.height * 0.28f
                                )
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cached Drawing",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@PhonePreviews
@Composable
fun Sec03D_DrawWithCacheLifecyclePreview() {
    CourseComposeTheme {
        Sec03D_DrawWithCacheLifecycle()
    }
}
