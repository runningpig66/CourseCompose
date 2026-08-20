package com.runningpig66.coursecompose.ch06

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log

/**
 * @author runningpig66
 * @date 2026/08/19 周三
 * @time 7:15
 *
 */
private const val TAG_SEC01C = "Sec01C"

private enum class BubbleTailSide {
    Start,
    End
}

/**
 * 一个可以真正复用的聊天气泡 Shape。
 *
 * cornerRadius / tailWidth / tailHeight：
 * 表示设计层面的固定视觉尺寸，因此使用 Dp。
 *
 * tailSide：使用 Start / End，而不是 Left / Right，
 * 因此可以跟随 LayoutDirection 自动镜像。
 */
private class AdaptiveBubbleShape(
    private val tailSide: BubbleTailSide,
    private val cornerRadius: Dp = 20.dp,
    private val tailWidth: Dp = 18.dp,
    private val tailHeight: Dp = 28.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val requestedRadiusPx = with(density) {
            cornerRadius.toPx()
        }.coerceAtLeast(0f)
        val requestedTailWidthPx = with(density) {
            tailWidth.toPx()
        }.coerceAtLeast(0f)

        val requestedTailHeightPx = with(density) {
            tailHeight.toPx()
        }.coerceAtLeast(0f)

        // Start / End 是逻辑方向。
        val tailOnLeft = when (tailSide) {
            BubbleTailSide.Start -> layoutDirection == LayoutDirection.Ltr
            BubbleTailSide.End -> layoutDirection == LayoutDirection.Rtl
        }
        /*
         * 防止极小组件中：tailWidth 比整个 Shape 都宽。这里把尾巴最多限制为总宽度的 28%。
         * 28% 不是 Compose 规则，而是这个 Shape 自己的设计策略。
         */
        val actualTailWidth = requestedTailWidthPx.coerceIn(
            minimumValue = 0f,
            maximumValue = size.width * 0.28f
        )

        /*
         * 尾巴会占用一部分横向空间。如果尾巴在左：
         *      body（的 Rect 矩形范围）
         *      ↓
         *    ┌─────────────┐
         *   <              │
         *    └─────────────┘
         * bodyLeft = tailWidth
         * 如果尾巴在右则反过来。
         */
        // 矩形主体的左侧起始坐标：
        val bodyRectLeft = if (tailOnLeft) { // 如果尾巴在左侧
            // 画布的最左边（0f 到 tailWidth 的区域）被尾巴占用了。所以矩形主体不能从 0f 开始画，必须往右挪，从尾巴结束的地方开始。
            actualTailWidth
        } else { // 如果尾巴在右侧
            // 画布的左边没有尾巴，干干净净。所以矩形主体直接紧贴着画布的最左侧边缘开始。
            0f
        }
        // 矩形主体的右侧结束坐标
        val bodyRectRight = if (tailOnLeft) {
            // 既然尾巴已经在左边了，那画布的右边就是空的。矩形主体可以一直延伸到画布的最右侧边缘。
            size.width
        } else {
            // 画布的最右边（靠近 size.width 的区域）要留给尾巴。所以矩形主体不能一直画到底，必须提前结束，把尾巴的宽度腾出来。
            size.width - actualTailWidth
        }
        // 矩形主体的实际宽度
        val bodyRectWidth = (bodyRectRight - bodyRectLeft).coerceAtLeast(0f)

        // 圆角半径不能大到超过：bodyWidth / 2, height / 2。否则小组件上的几何关系会失控。
        val actualRadius = requestedRadiusPx.coerceIn(
            minimumValue = 0f,
            maximumValue = minOf(
                bodyRectWidth / 2f,
                size.height / 2f
            )
        )

        // 圆角占用了上下两端以后，中间剩下的直线区域才适合放尾巴。
        val availableSideHeight = (size.height - actualRadius * 2f).coerceAtLeast(0f)

        val actualTailHeight = requestedTailHeightPx.coerceIn(
            minimumValue = 0f,
            maximumValue = availableSideHeight
        )

        // 把尾巴垂直居中。因为 actualTailHeight 已经受 availableSideHeight 限制，所以不会侵入上下圆角。
        val tailTop = (size.height - actualTailHeight) / 2f
        val tailBottom = tailTop + actualTailHeight
        val tailTipY = (tailTop + tailBottom) / 2f

        log(
            TAG_SEC01C,
            "size=$size, " +
                    "direction=$layoutDirection, " +
                    "tailSide=$tailSide, " +
                    "tailOnLeft=$tailOnLeft, " +
                    "radius=$actualRadius, " +
                    "tailWidth=$actualTailWidth, " +
                    "tailHeight=$actualTailHeight"
        )

        val path = if (tailOnLeft) {
            createLeftTailPath(
                size = size,
                bodyRectLeft = bodyRectLeft,
                bodyRectRight = bodyRectRight,
                radius = actualRadius,
                tailTop = tailTop,
                tailBottom = tailBottom,
                tailTipY = tailTipY
            )
        } else {
            createRightTailPath(
                size = size,
                bodyRectLeft = bodyRectLeft,
                bodyRectRight = bodyRectRight,
                radius = actualRadius,
                tailTop = tailTop,
                tailBottom = tailBottom,
                tailTipY = tailTipY
            )
        }
        return Outline.Generic(path)
    }

    private fun createLeftTailPath(
        size: Size,
        bodyRectLeft: Float,
        bodyRectRight: Float,
        radius: Float,
        tailTop: Float,
        tailBottom: Float,
        tailTipY: Float
    ): Path {
        return Path().apply {
            // 从左上圆角结束位置开始，顺时针绕完整个气泡。
            moveTo(bodyRectLeft + radius, 0f)
            // 顶边
            lineTo(bodyRectRight - radius, 0f)
            // 右上圆角
            quadraticTo(bodyRectRight, 0f, bodyRectRight, radius)
            // 右边
            lineTo(bodyRectRight, size.height - radius)
            // 右下圆角
            quadraticTo(bodyRectRight, size.height, bodyRectRight - radius, size.height)
            // 底边
            lineTo(bodyRectLeft + radius, size.height)
            // 左下圆角
            quadraticTo(bodyRectLeft, size.height, bodyRectLeft, size.height - radius)
            // 左侧向上走到尾巴下沿
            lineTo(bodyRectLeft, tailBottom)
            // 尾巴尖端
            lineTo(0f, tailTipY)
            // 回到气泡主体
            lineTo(bodyRectLeft, tailTop)
            // 继续向左上角
            lineTo(bodyRectLeft, radius)
            // 左上圆角
            quadraticTo(bodyRectLeft, 0f, bodyRectLeft + radius, 0f)
            close()
        }
    }

    private fun createRightTailPath(
        size: Size,
        bodyRectLeft: Float,
        bodyRectRight: Float,
        radius: Float,
        tailTop: Float,
        tailBottom: Float,
        tailTipY: Float
    ): Path {
        return Path().apply {
            // 从左上圆角结束位置开始，顺时针绕完整个气泡。
            moveTo(bodyRectLeft + radius, 0f)
            // 顶边
            lineTo(bodyRectRight - radius, 0f)
            // 右上圆角
            quadraticTo(bodyRectRight, 0f, bodyRectRight, radius)
            // 右边走到尾巴上沿
            lineTo(bodyRectRight, tailTop)
            // 尾巴尖端
            lineTo(size.width, tailTipY)
            // 回到气泡主体
            lineTo(bodyRectRight, tailBottom)
            // 继续向右下角
            lineTo(bodyRectRight, size.height - radius)
            // 右下圆角
            quadraticTo(bodyRectRight, size.height, bodyRectRight - radius, size.height)
            // 底边
            lineTo(bodyRectLeft + radius, size.height)
            // 左下圆角
            quadraticTo(bodyRectLeft, size.height, bodyRectLeft, size.height - radius)
            // 左边
            lineTo(bodyRectLeft, radius)
            // 左上圆角
            quadraticTo(bodyRectLeft, 0f, bodyRectLeft + radius, 0f)
            close()
        }
    }
}

@Composable
fun Sec01C_AdaptiveBubbleShape(
    modifier: Modifier = Modifier
) {
    var layoutDirection by remember {
        mutableStateOf(LayoutDirection.Ltr)
    }
    var tailSide by remember {
        mutableStateOf(BubbleTailSide.Start)
    }

    // Shape 不需要把 LayoutDirection 存进去。当前方向会在 createOutline() 调用时由 Compose 传入。
    val bubbleShape = remember(tailSide) {
        AdaptiveBubbleShape(
            tailSide = tailSide,
            cornerRadius = 20.dp,
            tailWidth = 18.dp,
            tailHeight = 30.dp
        )
    }
    Scaffold { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "6.1C Adaptive Bubble Shape",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "当前方向：$layoutDirection 尾巴：$tailSide",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, alignment = Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = {
                        layoutDirection =
                            if (layoutDirection == LayoutDirection.Ltr) {
                                LayoutDirection.Rtl
                            } else {
                                LayoutDirection.Ltr
                            }
                    }
                ) {
                    Text(text = "切换 LTR / RTL")
                }

                Button(
                    onClick = {
                        tailSide =
                            if (tailSide == BubbleTailSide.Start) {
                                BubbleTailSide.End
                            } else {
                                BubbleTailSide.Start
                            }
                    }
                ) {
                    Text(text = "切换 Start / End")
                }
            }

            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        text = "① 正常尺寸",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        shape = bubbleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    start =
                                        if (tailSide == BubbleTailSide.Start) {
                                            34.dp
                                        } else {
                                            18.dp
                                        },
                                    end =
                                        if (tailSide == BubbleTailSide.End) {
                                            34.dp
                                        } else {
                                            18.dp
                                        },
                                    top = 16.dp,
                                    bottom = 16.dp
                                ),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(text = "同一个 Shape 会根据 Size、Density 和 LayoutDirection 生成轮廓。")
                        }
                    }

                    Text(
                        text = "② 更窄的尺寸",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Box(
                        modifier = Modifier
                            .width(220.dp)
                            .height(82.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = bubbleShape
                            )
                    )

                    Text(
                        text = "③ 故意压缩到很小",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(44.dp)
                            .background(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = bubbleShape
                            )
                    )
                }
            }
        }
    }
}

@PhonePreviews
@Composable
private fun Sec01CPreview() {
    CourseComposeTheme {
        Sec01C_AdaptiveBubbleShape()
    }
}
