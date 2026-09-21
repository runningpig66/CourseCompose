package com.runningpig66.coursecompose.ch06.sec03

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log

/**
 * @author runningpig66
 * @date 2026/9/18
 * @time 5:19
 */
private const val Sec03C = "Sec03C"

@Composable
fun Sec03C_ModifierDrawingLayers() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Text(
                text = "Modifier 绘制链",
                style = MaterialTheme.typography.headlineSmall
            )
            NestedDrawModifierDemo()
            PaddingAndDrawAreaDemo()
        }
    }
}

@Composable
fun NestedDrawModifierDemo() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "实验一：两个 drawWithContent 嵌套",
            style = MaterialTheme.typography.titleMedium
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                // 外层 Draw Modifier
                .drawWithContent {
                    log(Sec03C, "1. Outer BEFORE")

                    drawRect(color = Color(0x225C6BC0))

                    drawContent()

                    log(Sec03C, "4. Outer AFTER")

                    drawRect(
                        color = Color(0xFF5C6BC0),
                        style = Stroke(width = 6.dp.toPx())
                    )
                }
                // 内层 Draw Modifier
                .drawWithContent {
                    log(Sec03C, "2. Inner BEFORE")

                    drawCircle(
                        color = Color(0x6681C784),
                        radius = 55.dp.toPx(),
                        center = center
                    )

                    drawContent()

                    log(Sec03C, "3. Inner AFTER")

                    // 这样可以更好地观察step 3被step 4覆盖的效果，同时保留step 3的痕迹
                    val inset = 0.dp.toPx()

                    // a wide red border rect
                    drawRect(
                        color = Color(0xFFEF5350),
                        topLeft = Offset(x = inset, y = inset),
                        size = Size(
                            width = size.width - inset * 2,
                            height = size.height - inset * 2
                        ),
                        // 这样可以更好地观察step 3被step 4覆盖的效果，同时保留step 3的痕迹
                        style = Stroke(width = 16.dp.toPx())
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "真正的 Box 内容",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun PaddingAndDrawAreaDemo() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "实验二：drawBehind 与 padding 的顺序",
            style = MaterialTheme.typography.titleMedium
        )

        Text(text = "A：drawBehind → padding")

        Box(
            modifier = Modifier
                .size(width = 240.dp, height = 110.dp)
                .drawBehind {
                    log(Sec03C, "A size = ${size.width} x ${size.height}")

                    drawRect(color = Color(0x335C6BC0))

                    drawRect(
                        color = Color(0xFF5C6BC0),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "drawBehind 在外层")
        }

        Text(text = "B：padding → drawBehind")

        Box(
            modifier = Modifier
                .size(width = 240.dp, height = 110.dp)
                .padding(20.dp)
                .drawBehind {
                    log(Sec03C, "B size = ${size.width} x ${size.height}")

                    drawRect(color = Color(0x335C6BC0))

                    drawRect(
                        color = Color(0xFF5C6BC0),
                        style = Stroke(width = 3.dp.toPx())
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text("drawBehind 在内层")
        }
    }
}

@PhonePreviews
@Composable
fun Sec03C_ModifierDrawingLayersPreview() {
    CourseComposeTheme {
        Sec03C_ModifierDrawingLayers()
    }
}
