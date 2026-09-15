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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log

/**
 * @author runningpig66
 * @date 2026/9/16
 * @time 2:10
 *
 * 1. 从 Canvas 走向 Modifier.drawBehind()
 */
private const val Sec03A = "Sec03A"

@Composable
fun Sec03A_DrawBehindIntro() {
    var expanded by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Modifier.drawBehind()",
                style = MaterialTheme.typography.headlineSmall
            )

            Button(onClick = { expanded = !expanded }) {
                Text(text = "切换 Box 宽度")
            }

            Box(
                modifier = Modifier
                    .width(if (expanded) 440.dp else 220.dp)
                    .height(140.dp)
                    .drawBehind {
                        log(Sec03A, "drawBehind: width=${size.width}, height=${size.height}")

                        // 整个 Box 的自定义背景
                        drawRoundRect(
                            color = Color(0xFF404046),
                            cornerRadius = CornerRadius(24.dp.toPx())
                        )

                        // 右上角装饰圆
                        drawCircle(
                            color = Color(0xFF5C6BC0),
                            radius = 18.dp.toPx(),
                            center = Offset(
                                x = size.width - 67.dp.toPx(),
                                y = 34.dp.toPx()
                            )
                        )

                        // 底部装饰线
                        drawLine(
                            color = Color(0xFF5C6BC0),
                            start = Offset(
                                x = 20.dp.toPx(),
                                y = size.height - 18.dp.toPx()
                            ),
                            end = Offset(
                                x = size.width - 20.dp.toPx(),
                                y = size.height - 18.dp.toPx()
                            ),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "我是 Box 的正常内容",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "背景和装饰来自 drawBehind()"
                    )
                }
            }
        }
    }
}

@PhonePreviews
@Composable
fun Sec03A_DrawBehindIntroPreview() {
    CourseComposeTheme {
        Sec03A_DrawBehindIntro()
    }
}
