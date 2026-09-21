package com.runningpig66.coursecompose.ch06.sec03

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log

/**
 * @author runningpig66
 * @date 2026/9/21
 * @time 14:18
 */
private const val Sec03E = "Sec03E"

private class CacheBuildCounter {
    var count = 0
}

@Composable
fun Sec03E_DrawCacheInvalidation() {
    var revealed by remember { mutableStateOf(false) }

    val progressState = animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = 1800),
        label = "ChartReveal"
    )

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "drawWithCache 缓存边界",
                style = MaterialTheme.typography.headlineSmall
            )

            Button(
                onClick = { revealed = !revealed }
            ) {
                Text(text = "播放 / 反向播放动画")
            }

            Text(
                text = "BAD：progress 读取在 Cache Build",
                style = MaterialTheme.typography.titleMedium
            )

            BadCacheChart(progressState = progressState)

            Text(
                text = "GOOD：progress 只读取在 Draw",
                style = MaterialTheme.typography.titleMedium
            )

            GoodCacheChart(progressState = progressState)
        }
    }
}

@Composable
private fun BadCacheChart(progressState: State<Float>) {
    val counter = remember { CacheBuildCounter() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .drawWithCache {
                counter.count++

                // 错误重点：progress 是高频动画状态，却在 Cache Build 中读取。
                val progress = progressState.value
                // val progress by progressState

                // 这两个 Path 本来只依赖 size，progress 改变根本不要求它们重建。
                val gridPath = buildGridPath(size)
                val chartPath = buildChartPath(size)

                // 只有这一项真正依赖 progress。
                val revealRight = size.width * progress

                log(
                    Sec03E,
                    "BAD CACHE BUILD #${counter.count}, " +
                            "progress=$progress"
                )

                onDrawBehind {
                    drawRect(color = Color(0xFF151922))
                    drawPath(
                        path = gridPath,
                        color = Color.White.copy(alpha = 0.16f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    clipRect(right = revealRight) {
                        drawPath(
                            path = chartPath,
                            color = Color(0xFF7986CB),
                            style = Stroke(
                                width = 4.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                    drawRect(
                        color = Color.White.copy(alpha = 0.45f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
    )
}

@Composable
private fun GoodCacheChart(progressState: State<Float>) {
    val counter = remember { CacheBuildCounter() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .drawWithCache {
                counter.count++

                //  Cache Build 中只保留真正稳定、值得跨 Draw 复用的资源。
                val gridPath = buildGridPath(size)
                val chartPath = buildChartPath(size)

                log(
                    Sec03E,
                    "GOOD CACHE BUILD #${counter.count}"
                )

                onDrawBehind {
                    // 高频动画状态推迟到真正 Draw 时才读取。
                    val progress = progressState.value
                    val revealRight = size.width * progress

                    drawRect(color = Color(0xFF151922))

                    drawPath(
                        path = gridPath,
                        color = Color.White.copy(alpha = 0.16f),
                        style = Stroke(width = 1.dp.toPx())
                    )

                    clipRect(
                        right = revealRight
                    ) {
                        drawPath(
                            path = chartPath,
                            color = Color(0xFF7986CB),
                            style = Stroke(
                                width = 4.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    drawRect(
                        color = Color.White.copy(alpha = 0.45f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
    )
}

private fun buildGridPath(size: Size): Path {
    return Path().apply {
        for (column in 1..4) {
            val x = size.width * column / 5f
            moveTo(x = x, y = 0f)
            lineTo(x = x, y = size.height)
        }
        for (row in 1..3) {
            val y = size.height * row / 4f
            moveTo(x = 0f, y = y)
            lineTo(x = size.width, y = y)
        }
    }
}

private fun buildChartPath(size: Size): Path {
    return Path().apply {
        moveTo(x = 0f, y = size.height * 0.72f)
        lineTo(x = size.width * 0.15f, y = size.height * 0.55f)
        lineTo(x = size.width * 0.30f, y = size.height * 0.68f)
        lineTo(x = size.width * 0.46f, y = size.height * 0.30f)
        lineTo(x = size.width * 0.62f, y = size.height * 0.44f)
        lineTo(x = size.width * 0.78f, y = size.height * 0.20f)
        lineTo(x = size.width, y = size.height * 0.34f)
    }
}

@PhonePreviews
@Composable
fun Sec03E_DrawCacheInvalidationPreview() {
    CourseComposeTheme {
        Sec03E_DrawCacheInvalidation()
    }
}
