package com.runningpig66.coursecompose.ch06.sec02

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026/08/30
 * @time 0:13
 */
@Composable
fun Sec02D_PathDrawing() {
    var useGradient by remember { mutableStateOf(true) }
    var showStroke by remember { mutableStateOf(true) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        useGradient = !useGradient
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (useGradient) {
                            "Use Color"
                        } else {
                            "Use Gradient"
                        }
                    )
                }

                Button(
                    onClick = {
                        showStroke = !showStroke
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (showStroke) {
                            "Hide Stroke"
                        } else {
                            "Show Stroke"
                        }
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
            ) {
                drawRect(color = Color(0xFF4E4E4E))

                val horizontalPadding = 24.dp.toPx()
                val itemWidth = size.width - horizontalPadding * 2f
                val itemHeight = 105.dp.toPx()

                // 1. 直接构造并绘制 Path
                val firstTop = 24.dp.toPx()
                val bookmarkPath = createBookmarkPath(
                    width = itemWidth,
                    height = itemHeight
                )

                translate(
                    left = horizontalPadding,
                    top = firstTop
                ) {
                    if (useGradient) {
                        drawPath(
                            path = bookmarkPath,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF42A5F5),
                                    Color(0xFF7E57C2)
                                ),
                                start = Offset.Zero,
                                end = Offset(
                                    x = itemWidth,
                                    y = itemHeight
                                )
                            )
                        )
                    } else {
                        drawPath(
                            path = bookmarkPath,
                            color = Color(0xFF42A5F5)
                        )
                    }

                    if (showStroke) {
                        drawPath(
                            path = bookmarkPath,
                            color = Color(0xFF1A237E),
                            style = Stroke(
                                width = 3.dp.toPx(),
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                // 2. 同一个 Path，只画 Stroke
                val secondTop = 165.dp.toPx()

                translate(
                    left = horizontalPadding,
                    top = secondTop
                ) {
                    drawPath(
                        path = bookmarkPath,
                        color = Color(0xFF00897B),
                        style = Stroke(
                            width = 8.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // 3. Shape -> Outline -> drawOutline
                val thirdTop = 305.dp.toPx()
                val outlineSize = Size(
                    width = itemWidth,
                    height = itemHeight
                )

                translate(
                    left = horizontalPadding,
                    top = thirdTop
                ) {
                    // TODO new usage
                    val outline = BookmarkShape.createOutline(
                        size = outlineSize,
                        layoutDirection = layoutDirection,
                        density = this
                    )

                    drawOutline(
                        outline = outline,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFFB74D),
                                Color(0xFFEF5350)
                            )
                        )
                    )

                    drawOutline(
                        outline = outline,
                        color = Color(0xFF6D4C41),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

private fun createBookmarkPath(
    width: Float,
    height: Float
): Path {
    val notchDepth = width * 0.12f
    val centerY = height / 2f
    return Path().apply {
        moveTo(x = 0f, y = 0f)
        lineTo(x = width, y = 0f)
        lineTo(x = width - notchDepth, y = centerY)
        lineTo(x = width, y = height)
        lineTo(x = 0f, y = height)
        close()
    }
}

private val BookmarkShape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            path = createBookmarkPath(
                width = size.width,
                height = size.height
            )
        )
    }
}

@PhonePreviews
@Composable
fun Sec02D_PathDrawingPreview() {
    CourseComposeTheme {
        Sec02D_PathDrawing()
    }
}
