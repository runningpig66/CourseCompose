package com.runningpig66.coursecompose.ch06

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
 * @date 2026/08/18 周二
 * @time 2:26
 *
 * 使用三个自定义 Shape，分别返回：
 * 1. Outline.Rectangle
 * 2. Outline.Rounded
 * 3. Outline.Generic
 * 理解：Shape 最终可以用三种不同层级的几何结构描述边界。
 */
private const val TAG_SEC01B = "Sec01B"

private object RectangleOutlineShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val rect = Rect(left = 0f, top = 0f, right = size.width, bottom = size.height)
        val outline = Outline.Rectangle(rect)
        log(TAG_SEC01B, "Rectangle -> size=$size, bounds=${outline.bounds}")
        return outline
    }
}

private class RoundedOutlineShape(
    private val radius: Dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val requestedRadiusPx = with(density) {
            radius.toPx()
        }
        val radiusPx = requestedRadiusPx.coerceAtMost(
            size.minDimension / 2f
        )
        val cornerRadius = CornerRadius(
            x = radiusPx,
            y = radiusPx
        )
        val roundRect = RoundRect(
            left = 0f,
            top = 0f,
            right = size.width,
            bottom = size.height,
            topLeftCornerRadius = cornerRadius,
            topRightCornerRadius = cornerRadius,
            bottomRightCornerRadius = cornerRadius,
            bottomLeftCornerRadius = cornerRadius
        )
        val outline = Outline.Rounded(roundRect)
        log(TAG_SEC01B, "Rounded -> size=$size, radiusPx=$radiusPx, bounds=${outline.bounds}")
        return outline
    }
}

private object TriangleOutlineShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(size.width / 2f, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        val outline = Outline.Generic(path)
        log(TAG_SEC01B, "Generic -> size=$size, bounds=${outline.bounds}")
        return outline
    }
}

@Composable
fun Sec01B_OutlineAndPathLab(
    modifier: Modifier = Modifier
) {
    val roundedShape = remember {
        RoundedOutlineShape(radius = 28.dp)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "6.1B 三种 Outline",
                style = MaterialTheme.typography.headlineSmall
            )

            OutlineExample(
                title = "1. Outline.Rectangle",
                description = "Rect 就足够描述整个边界",
                shape = RectangleOutlineShape,
                color = MaterialTheme.colorScheme.primaryContainer
            )

            OutlineExample(
                title = "2. Outline.Rounded",
                description = "RoundRect 描述矩形 + 四个圆角",
                shape = roundedShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            )

            OutlineExample(
                title = "3. Outline.Generic",
                description = "简单矩形结构不够了，交给 Path",
                shape = TriangleOutlineShape,
                color = MaterialTheme.colorScheme.tertiaryContainer
            )
        }
    }
}

@Composable
private fun OutlineExample(
    title: String,
    description: String,
    shape: Shape,
    color: Color
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(
                    color = color,
                    shape = shape
                )
        )
    }
}

/* Output:
Rectangle -> size=Size(954.0, 263.0), bounds=Rect.fromLTRB(0.0, 0.0, 954.0, 263.0)
Rounded -> size=Size(954.0, 263.0), radiusPx=73.5, bounds=Rect.fromLTRB(0.0, 0.0, 954.0, 263.0)
Generic -> size=Size(954.0, 263.0), bounds=Rect.fromLTRB(0.0, 0.0, 954.0, 263.0)
 */

@PhonePreviews
@Composable
private fun Sec01BPreview() {
    CourseComposeTheme {
        Sec01B_OutlineAndPathLab()
    }
}
