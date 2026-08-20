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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log

/**
 * @author runningpig66
 * @date 2026/08/17 周一
 * @time 23:39
 *
 * 一个简单的自定义 Shape：把矩形的右上角切掉一块。
 */
private const val TAG_SEC01A = "Sec01A"

private class CutTopRightShape(
    private val cutSize: Dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cutSizePx = with(density) {
            cutSize.toPx()
        }.coerceIn(
            minimumValue = 0f,
            maximumValue = minOf(size.width, size.height)
        )
        log(TAG_SEC01A, "createOutline() size=$size, cutSizePx=$cutSizePx, layoutDirection=$layoutDirection")

        val path = Path().apply {
            // 左上角
            moveTo(0f, 0f)
            // 沿顶部向右走，但在右上角之前停下
            lineTo(size.width - cutSizePx, 0f)
            // 斜着切到右边
            lineTo(size.width, cutSizePx)
            // 右下角
            lineTo(size.width, size.height)
            // 左下角
            lineTo(0f, size.height)
            // 回到起点，形成闭合区域
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun Sec01A_ShapeOutlineIntro(
    modifier: Modifier = Modifier
) {
    // 这是同一个 Shape 对象。后面它会被不同尺寸的组件使用。
    val cutShape = remember {
        CutTopRightShape(cutSize = 32.dp)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "6.1A Shape / Outline",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "① 先看我们熟悉的内置 Shape",
                style = MaterialTheme.typography.titleMedium
            )


            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(text = "RoundedCornerShape(28.dp)")
                }
            }

            Text(
                text = "② 再看我们自己实现的 Shape",
                style = MaterialTheme.typography.titleMedium
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                shape = cutShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(text = "CutTopRightShape\n右上角由我们自己定义")
                }
            }

            Text(
                text = "③ 同一个 Shape，交给两个不同尺寸的组件",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                        .background(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = cutShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "weight(1f)")
                }

                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(100.dp)
                        .background(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = cutShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "120.dp")
                }
            }

            Text(
                text = "打开 Logcat / Run 控制台，观察 createOutline() 收到的 Size。",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/* Output:
createOutline() size=Size(954.0, 289.0), cutSizePx=84.0, layoutDirection=Ltr
createOutline() size=Size(954.0, 289.0), cutSizePx=84.0, layoutDirection=Ltr
createOutline() size=Size(597.0, 263.0), cutSizePx=84.0, layoutDirection=Ltr
createOutline() size=Size(315.0, 263.0), cutSizePx=84.0, layoutDirection=Ltr
 */

@PhonePreviews
@Composable
fun Sec01APreview() {
    CourseComposeTheme {
        Sec01A_ShapeOutlineIntro()
    }
}
