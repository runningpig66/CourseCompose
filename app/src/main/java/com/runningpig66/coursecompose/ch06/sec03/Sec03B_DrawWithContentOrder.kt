package com.runningpig66.coursecompose.ch06.sec03

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026/9/16
 * @time 3:29
 */
@Composable
fun Sec03B_DrawWithContentOrder() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "drawWithContent 绘制顺序",
                style = MaterialTheme.typography.headlineSmall
            )
            DrawOrderCard(
                title = "A：先自定义绘制，再 drawContent()",
                mode = DrawOrderMode.Behind
            )

            DrawOrderCard(
                title = "B：先 drawContent()，再自定义绘制",
                mode = DrawOrderMode.Front
            )

            DrawOrderCard(
                title = "C：完全不调用 drawContent()",
                mode = DrawOrderMode.HideContent
            )
        }
    }
}

private enum class DrawOrderMode {
    Behind,
    Front,
    HideContent
}

@Composable
private fun DrawOrderCard(
    title: String,
    mode: DrawOrderMode
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .drawWithContent {
                // 凑巧写了一个边框，到后面才发现示例代码的 border，不过也发现了有趣的现象
                drawRoundRect(
                    color = Color.White,
                    alpha = 0.5f,
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
                when (mode) {
                    DrawOrderMode.Behind -> {
                        drawCircle(
                            color = Color(0xFF5C6BC0),
                            radius = 52.dp.toPx(),
                            center = center
                        )
                        drawContent()
                    }

                    DrawOrderMode.Front -> {
                        drawContent()
                        drawCircle(
                            color = Color(0x995C6BC0),
                            radius = 52.dp.toPx(),
                            center = center
                        )
                        /*drawRect(
                            color = Color.Black.copy(alpha = 0.5f)
                        )*/
                    }

                    DrawOrderMode.HideContent -> {
                        drawCircle(
                            color = Color(0xFF5C6BC0),
                            radius = 52.dp.toPx(),
                            center = center
                        )
                        // 故意不调用 drawContent()
                    }
                }
            }
            .border(
                width = 1.dp,
                color = Color.Red
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@PhonePreviews
@Composable
fun Sec03B_DrawWithContentOrderPreview() {
    CourseComposeTheme {
        Sec03B_DrawWithContentOrder()
    }
}
