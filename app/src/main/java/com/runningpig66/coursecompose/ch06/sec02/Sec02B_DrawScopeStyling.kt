package com.runningpig66.coursecompose.ch06.sec02

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026/08/23 周日
 * @time 4:51
 */
@Composable
fun Sec02B_DrawScopeStyling() {
    var useStroke by remember { mutableStateOf(false) }
    var ovalAlpha by remember { mutableFloatStateOf(0.65f) }
    var reverseOvalOrder by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(text = "RoundedRect style: ${if (useStroke) "Stroke" else "Fill"}")

            Button(onClick = {
                useStroke = !useStroke
            }) {
                Text(text = "Switch Fill / Stroke")
            }

            Text(text = "Oval alpha: ${"%.2f".format(ovalAlpha)}")

            Slider(
                value = ovalAlpha,
                onValueChange = { ovalAlpha = it },
                valueRange = 0f..1f
            )

            Button(onClick = {
                reverseOvalOrder = !reverseOvalOrder
            }) {
                Text(text = "Reverse oval drawing order")
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
            ) {
                val gap = 20.dp.toPx()
                val strokeWidth = 6.dp.toPx()

                drawRect(color = Color(0xFFF4F4F4))

                // 1. Brush + Fill / Stroke
                val roundRectTop = gap
                val roundRectHeight = 90.dp.toPx()
                val roundRectSize = Size(
                    width = size.width - gap * 2f,
                    height = roundRectHeight
                )

                val gradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF42A5F5),
                        Color(0xFF7E57C2)
                    ),
                    start = Offset(
                        x = gap,
                        y = roundRectTop
                    ),
                    end = Offset(
                        x = size.width - gap,
                        y = roundRectTop + roundRectHeight
                    )
                )

                val roundRectStyle: DrawStyle =
                    if (useStroke) {
                        Stroke(width = strokeWidth)
                    } else {
                        Fill
                    }

                drawRoundRect(
                    brush = gradient,
                    topLeft = Offset(
                        x = gap,
                        y = roundRectTop
                    ),
                    size = roundRectSize,
                    cornerRadius = CornerRadius(
                        x = 20.dp.toPx(),
                        y = 20.dp.toPx()
                    ),
                    alpha = ovalAlpha,
                    style = roundRectStyle
                )

                // 2. Alpha + drawing order
                val ovalTop = 145.dp.toPx()
                val ovalSize = Size(
                    width = size.width * 0.52f,
                    height = 95.dp.toPx()
                )

                val firstOvalTopLeft = Offset(
                    x = gap,
                    y = ovalTop
                )

                val secondOvalTopLeft = Offset(
                    x = size.width * 0.38f,
                    y = ovalTop + 25.dp.toPx()
                )

                if (!reverseOvalOrder) {
                    drawOval(
                        color = Color(0xFFEF5350),
                        topLeft = firstOvalTopLeft,
                        size = ovalSize,
                        alpha = ovalAlpha
                    )

                    drawOval(
                        color = Color(0xFF42A5F5),
                        topLeft = secondOvalTopLeft,
                        size = ovalSize,
                        alpha = ovalAlpha
                    )
                } else {
                    drawOval(
                        color = Color(0xFF42A5F5),
                        topLeft = secondOvalTopLeft,
                        size = ovalSize,
                        alpha = ovalAlpha
                    )

                    drawOval(
                        color = Color(0xFFEF5350),
                        topLeft = firstOvalTopLeft,
                        size = ovalSize,
                        alpha = ovalAlpha
                    )
                }

                // 3. drawArc
                val arcTop = 295.dp.toPx()
                val arcDiameter = minOf(
                    size.width * 0.32f,
                    110.dp.toPx()
                )

                val arcSize = Size(
                    width = arcDiameter,
                    height = arcDiameter
                )

                drawArc(
                    color = Color(0xFF00897B),
                    startAngle = 210f,
                    sweepAngle = 240f,
                    useCenter = false,
                    topLeft = Offset(
                        x = gap,
                        y = arcTop
                    ),
                    size = arcSize,
                    style = Stroke(
                        width = 8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                drawArc(
                    color = Color(0xFFFFA726),
                    startAngle = -90f,
                    sweepAngle = 110f,
                    useCenter = true,
                    topLeft = Offset(
                        x = size.width - gap - arcDiameter,
                        y = arcTop
                    ),
                    size = arcSize
                )

                // 4. StrokeCap
                val lineStartX = gap
                val lineEndX = size.width - gap
                val firstLineY = 435.dp.toPx()
                val lineSpacing = 28.dp.toPx()
                val lineWidth = 10.dp.toPx()

                drawLine(
                    color = Color(0xFF5C6BC0),
                    start = Offset(
                        lineStartX,
                        firstLineY
                    ),
                    end = Offset(
                        lineEndX,
                        firstLineY
                    ),
                    strokeWidth = lineWidth,
                    cap = StrokeCap.Butt
                )

                drawLine(
                    color = Color(0xFF5C6BC0),
                    start = Offset(
                        lineStartX,
                        firstLineY + lineSpacing
                    ),
                    end = Offset(
                        lineEndX,
                        firstLineY + lineSpacing
                    ),
                    strokeWidth = lineWidth,
                    cap = StrokeCap.Round
                )

                drawLine(
                    color = Color(0xFF5C6BC0),
                    start = Offset(
                        lineStartX,
                        firstLineY + lineSpacing * 2f
                    ),
                    end = Offset(
                        lineEndX,
                        firstLineY + lineSpacing * 2f
                    ),
                    strokeWidth = lineWidth,
                    cap = StrokeCap.Square
                )
            }
        }
    }
}

@PhonePreviews
@Composable
fun Sec02B_DrawScopeStylingPreview() {
    CourseComposeTheme {
        Sec02B_DrawScopeStyling()
    }
}
