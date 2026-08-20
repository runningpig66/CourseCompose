package com.runningpig66.coursecompose.ch06

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026/08/18 周二
 * @time 4:31
 *
 * 使用不同 Path 构造方式生成 Outline.Generic，观察 Path 本身的几何行为。
 */
private enum class PathDemoType {
    Polygon,
    Quadratic,
    Cubic,
    Arc,
    MultiContour,
    TicketCutout
}

private class PathDemoShape(
    private val type: PathDemoType
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = when (type) {
            PathDemoType.Polygon -> createPolygonPath(size)
            PathDemoType.Quadratic -> createQuadraticPath(size)
            PathDemoType.Cubic -> createCubicPath(size)
            PathDemoType.Arc -> createArcPath(size)
            PathDemoType.MultiContour -> createMultiContourPath(size)
            PathDemoType.TicketCutout -> createTicketPath(size)
        }
        return Outline.Generic(path)
    }
}

/**
 * 直线构成的五边形。
 * 用于观察：moveTo -> lineTo -> close
 */
private fun createPolygonPath(size: Size): Path {
    val w = size.width
    val h = size.height

    return Path().apply {
        moveTo(x = w * 0.5f, y = 0f)
        lineTo(x = w, y = h * 0.38f)
        lineTo(x = w * 0.8f, y = h)
        lineTo(x = w * 0.2f, y = h)
        lineTo(x = 0f, y = h * 0.38f)
        // close()
    }
}

/**
 * 顶边是一条二次贝塞尔曲线。
 * 当前点 P0：(0, h * 0.72)
 * 控制点 P1：(w / 2, 0)
 * 终点 P2：(w, h * 0.72)
 */
private fun createQuadraticPath(size: Size): Path {
    val w = size.width
    val h = size.height

    return Path().apply {
        moveTo(x = 0f, y = h)
        lineTo(x = 0f, y = h * 0.72f)
        quadraticTo(x1 = w * 0.5f, y1 = 0f, x2 = w, y2 = h * 0.72f)
        lineTo(x = w, y = h)
        close()
    }
}

/**
 * 顶边是一条三次贝塞尔曲线。
 * 当前点 P0：(0, h * 0.55)
 * 控制点 P1：(w * 0.25, 0)
 * 控制点 P2：(w * 0.75, h)
 * 终点 P3：(w, h * 0.45)
 */
private fun createCubicPath(size: Size): Path {
    val w = size.width
    val h = size.height

    return Path().apply {
        moveTo(x = 0f, y = h)
        lineTo(x = 0f, y = h * 0.55f)
        cubicTo(x1 = w * 0.25f, y1 = 0f, x2 = w * 0.75f, y2 = h, x3 = w, y3 = h * 0.45f)
        lineTo(x = w, y = h)
        close()
    }
}

/**
 * 顶部使用半个椭圆弧。
 */
private fun createArcPath(size: Size): Path {
    val w = size.width
    val h = size.height

    val ovalBounds = Rect(left = 0f, top = 0f, right = w, bottom = h)

    return Path().apply {
        moveTo(x = 0f, y = h)
        lineTo(x = 0f, y = h * 0.5f)
        arcTo(rect = ovalBounds, startAngleDegrees = 180f, sweepAngleDegrees = 180f, forceMoveTo = false)
        lineTo(x = w, y = h)
        close()
    }
}

/**
 * 一个 Path 中放入两个彼此分离的 contour。
 */
private fun createMultiContourPath(size: Size): Path {
    val w = size.width
    val h = size.height

    return Path().apply {
        addRect(
            Rect(
                left = 0f,
                top = h * 0.15f,
                right = w * 0.40f,
                bottom = h * 0.85f
            )
        )

        addOval(
            Rect(
                left = w * 0.60f,
                top = h * 0.15f,
                right = w,
                bottom = h * 0.85f
            )
        )
    }
}

/**
 * 一个矩形主体减去左右两个圆形区域，得到票券式缺口。
 */
private fun createTicketPath(size: Size): Path {
    val w = size.width
    val h = size.height

    val outerPath = Path().apply {
        addRect(
            Rect(
                left = 0f,
                top = 0f,
                right = w,
                bottom = h
            )
        )
    }

    val radius = h * 0.18f

    val cutoutPath = Path().apply {
        addOval(
            Rect(
                left = -radius,
                top = h / 2f - radius,
                right = radius,
                bottom = h / 2f + radius
            )
        )

        addOval(
            Rect(
                left = w - radius,
                top = h / 2f - radius,
                right = w + radius,
                bottom = h / 2f + radius
            )
        )
    }

    return Path.combine(
        operation = PathOperation.Difference,
        path1 = outerPath,
        path2 = cutoutPath
    )
}

@Composable
fun Sec01B_OutlineAndPathLab2(
    modifier: Modifier = Modifier
) {
    val polygonShape = remember {
        PathDemoShape(PathDemoType.Polygon)
    }

    val quadraticShape = remember {
        PathDemoShape(PathDemoType.Quadratic)
    }

    val cubicShape = remember {
        PathDemoShape(PathDemoType.Cubic)
    }

    val arcShape = remember {
        PathDemoShape(PathDemoType.Arc)
    }

    val multiContourShape = remember {
        PathDemoShape(PathDemoType.MultiContour)
    }

    val ticketShape = remember {
        PathDemoShape(PathDemoType.TicketCutout)
    }

    Scaffold { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Text(
                text = "6.1B Path Lab",
                style = MaterialTheme.typography.headlineSmall
            )

            PathExample(
                title = "1. moveTo + lineTo + close",
                description = "五条直线组成一个闭合五边形",
                shape = polygonShape,
                color = MaterialTheme.colorScheme.primaryContainer
            )
            PathExample(
                title = "2. quadraticTo",
                description = "一个控制点控制的二次贝塞尔曲线",
                shape = quadraticShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            )

            PathExample(
                title = "3. cubicTo",
                description = "两个控制点控制的三次贝塞尔曲线",
                shape = cubicShape,
                color = MaterialTheme.colorScheme.tertiaryContainer
            )

            PathExample(
                title = "4. arcTo",
                description = "沿指定椭圆的一部分运动",
                shape = arcShape,
                color = MaterialTheme.colorScheme.primaryContainer
            )

            PathExample(
                title = "5. multiple contours",
                description = "一个 Path 中可以存在多个彼此独立的轮廓",
                shape = multiContourShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            )

            PathExample(
                title = "6. PathOperation.Difference",
                description = "矩形减去两个圆形区域，得到票券缺口",
                shape = ticketShape,
                color = MaterialTheme.colorScheme.tertiaryContainer
            )
        }
    }
}

@Composable
private fun PathExample(
    title: String,
    description: String,
    shape: Shape,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                .height(110.dp)
                .background(
                    color = color,
                    shape = shape
                )
        )
    }
}

@PhonePreviews
@Composable
private fun Sec01BPathLabPreview() {
    CourseComposeTheme {
        Sec01B_OutlineAndPathLab2()
    }
}
