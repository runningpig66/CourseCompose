package com.runningpig66.coursecompose.ch06

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026/08/28 周五
 * @time 3:25
 */
@Composable
fun Sec02C_DrawTransform() {
    var rotationDegrees by remember { mutableFloatStateOf(30f) }
    var scaleFactor by remember { mutableFloatStateOf(1.15f) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "1. translate()：移动坐标原点")

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                drawRect(color = Color(0xFFF3F4F6))

                val cardWidth = 120.dp.toPx()
                val cardHeight = 58.dp.toPx()
                val cardSize = Size(cardWidth, cardHeight)
                val margin = 16.dp.toPx()
                val top = (size.height - cardHeight) / 2f

                // 第一张：直接使用 Canvas 局部坐标定位
                drawTransformCard(
                    topLeft = Offset(
                        x = margin,
                        y = top
                    ),
                    cardSize = cardSize,
                    alpha = 0.45f
                )

                // 第二张：先移动坐标系，然后仍然从 (0, 0) 开始画
                translate(
                    left = size.width - margin - cardWidth,
                    top = top
                ) {
                    drawTransformCard(
                        topLeft = Offset.Zero,
                        cardSize = cardSize
                    )
                }
            }

            Text(text = "2. rotate()：${rotationDegrees.toInt()}°")

            Slider(
                value = rotationDegrees,
                onValueChange = { rotationDegrees = it },
                valueRange = -180f..180f
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .height(150.dp)
            ) {
                drawRect(color = Color(0xFFF3F4F6))

                val cardSize = Size(
                    width = 150.dp.toPx(),
                    height = 64.dp.toPx()
                )

                val cardTopLeft = Offset(
                    x = center.x - cardSize.width / 2f,
                    y = center.y - cardSize.height / 2f
                )

                // 原始未旋转区域，用于对比
                drawRoundRect(
                    color = Color(0xFF9E9E9E),
                    topLeft = cardTopLeft,
                    size = cardSize,
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )

                translate(
                    left = cardTopLeft.x,
                    top = cardTopLeft.y
                ) {
                    val localPivot = Offset(
                        x = cardSize.width / 2f,
                        y = cardSize.height / 2f
                    )

                    rotate(
                        degrees = rotationDegrees,
                        pivot = localPivot
                    ) {
                        drawTransformCard(
                            topLeft = Offset.Zero,
                            cardSize = cardSize
                        )
                    }

                    drawCircle(
                        color = Color.Red,
                        radius = 4.dp.toPx(),
                        center = localPivot
                    )
                }
            }

            Text(text = "3. scale()：${"%.2f".format(scaleFactor)}")

            Slider(
                value = scaleFactor,
                onValueChange = { scaleFactor = it },
                valueRange = 0.5f..1.5f
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                drawRect(color = Color(0xFFF3F4F6))

                val cardSize = Size(
                    width = 150.dp.toPx(),
                    height = 64.dp.toPx()
                )

                val cardTopLeft = Offset(
                    x = center.x - cardSize.width / 2f,
                    y = center.y - cardSize.height / 2f
                )

                drawRoundRect(
                    color = Color(0xFF9E9E9E),
                    topLeft = cardTopLeft,
                    size = cardSize,
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )

                translate(
                    left = cardTopLeft.x,
                    top = cardTopLeft.y
                ) {
                    val localPivot = Offset(
                        x = cardSize.width / 2f,
                        y = cardSize.height / 2f
                    )

                    scale(
                        scaleX = scaleFactor,
                        scaleY = scaleFactor,
                        pivot = localPivot
                    ) {
                        drawTransformCard(
                            topLeft = Offset.Zero,
                            cardSize = cardSize
                        )
                    }

                    drawCircle(
                        color = Color.Red,
                        radius = 4.dp.toPx(),
                        center = localPivot
                    )
                }
            }

            Text("4. inset()：建立更小的局部绘制区域")

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                drawRect(color = Color(0xFFF3F4F6))

                drawRect(
                    color = Color(0xFF757575),
                    style = Stroke(width = 2.dp.toPx())
                )

                inset(
                    left = 24.dp.toPx(),
                    top = 18.dp.toPx(),
                    right = 24.dp.toPx(),
                    bottom = 18.dp.toPx()
                ) {
                    drawRect(color = Color(0xFFDDEEFF))

                    drawRect(
                        color = Color(0xFF1976D2),
                        style = Stroke(width = 6.dp.toPx())
                    )

                    drawLine(
                        color = Color(0xFF78909C),
                        start = Offset.Zero,
                        end = Offset(
                            x = size.width,
                            y = size.height
                        ),
                        strokeWidth = 2.dp.toPx()
                    )

                    drawCircle(
                        color = Color(0xFF43A047),
                        radius = size.minDimension * 0.22f,
                        center = center
                    )
                }
            }

            Text("5. withTransform() + clipRect()")

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                drawRect(color = Color(0xFFF3F4F6))

                val horizontalInset = 24.dp.toPx()
                val verticalInset = 18.dp.toPx()

                val clipLeft: Float = horizontalInset
                val clipTop: Float = verticalInset
                val clipRight = size.width - horizontalInset
                val clipBottom = size.height - verticalInset

                val transformPivot = center

                clipRect(
                    left = clipLeft,
                    top = clipTop,
                    right = clipRight,
                    bottom = clipBottom
                ) {
                    drawRect(
                        color = Color(0xFFA8F5E9),
                        topLeft = Offset(
                            x = clipLeft,
                            y = clipTop
                        ),
                        size = Size(
                            width = clipRight - clipLeft,
                            height = clipBottom - clipTop
                        )
                    )

                    val cardSize = Size(
                        width = size.width * 0.58f,
                        height = 80.dp.toPx()
                    )

                    val cardTopLeft = Offset(
                        x = center.x - cardSize.width / 2f,
                        y = center.y - cardSize.height / 2f
                    )

                    withTransform(transformBlock = {
                        rotate(
                            degrees = rotationDegrees,
                            pivot = transformPivot
                        )

                        scale(
                            scaleX = scaleFactor,
                            scaleY = scaleFactor,
                            pivot = transformPivot
                        )
                    }) {
                        drawTransformCard(
                            topLeft = cardTopLeft,
                            cardSize = cardSize
                        )
                    }
                }

                drawRect(
                    color = Color(0xFF2E7D32),
                    topLeft = Offset(
                        x = clipLeft,
                        y = clipTop
                    ),
                    size = Size(
                        width = clipRight - clipLeft,
                        height = clipBottom - clipTop
                    ),
                    style = Stroke(width = 4.dp.toPx())
                )
            }
        }
    }
}

private fun DrawScope.drawTransformCard(
    topLeft: Offset,
    cardSize: Size,
    alpha: Float = 1f
) {
    val cornerRadius = 14.dp.toPx()
    val centerY = topLeft.y + cardSize.height / 2f

    drawRoundRect(
        color = Color(0xFF42A5F5),
        topLeft = topLeft,
        size = cardSize,
        cornerRadius = CornerRadius(
            x = cornerRadius,
            y = cornerRadius
        ),
        alpha = alpha
    )

    drawLine(
        color = Color.White,
        start = Offset(
            x = topLeft.x + 16.dp.toPx(),
            y = centerY
        ),
        end = Offset(
            x = topLeft.x + cardSize.width - 16.dp.toPx(),
            y = centerY
        ),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
        alpha = alpha
    )

    drawCircle(
        color = Color(0xFFFFCA28),
        radius = 8.dp.toPx(),
        center = Offset(
            x = topLeft.x + 28.dp.toPx(),
            y = centerY
        ),
        alpha = alpha
    )
}

@PhonePreviews
@Composable
fun Sec02C_DrawTransformPreview() {
    CourseComposeTheme {
        Sec02C_DrawTransform()
    }
}
