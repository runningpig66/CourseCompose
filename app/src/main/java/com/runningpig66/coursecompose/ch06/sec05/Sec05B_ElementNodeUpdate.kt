package com.runningpig66.coursecompose.ch06.sec05

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log

/**
 * @author runningpig66
 * @date 2026/9/27
 * @time 6:02
 *
 * 6.5 阶段 B：观察 ModifierNodeElement 与 Modifier.Node 的更新关系。
 * 通过重组、Element 参数变化和日志验证 Node 的 create、reuse、update，
 * 理解 Element 是声明式描述，Node 是可长期复用的运行时对象。
 */
private const val Sec05B = "Sec05B"

@Composable
fun Sec05B_ElementNodeUpdate() {
    var recomposeCount by remember { mutableIntStateOf(0) }
    var useBlueBorder by remember { mutableStateOf(true) }
    val borderColor = if (useBlueBorder) Color(0xFF0700FF) else Color(0xFF00A86B)

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Element / Node 更新实验",
                style = MaterialTheme.typography.titleLarge
            )

            ElementNodeTarget(
                recomposeCount = recomposeCount,
                borderColor = borderColor
            )

            Button(
                onClick = { recomposeCount++ }
            ) {
                Text("只触发重组")
            }

            Button(
                onClick = { useBlueBorder = !useBlueBorder }
            ) {
                Text("改变 Border 参数")
            }
        }
    }
}

@Composable
private fun ElementNodeTarget(
    recomposeCount: Int,
    borderColor: Color
) {
    SideEffect {
        log(
            Sec05B,
            "Composition 完成：count=$recomposeCount, color=$borderColor"
        )
    }

    Box(
        modifier = Modifier
            .size(width = 220.dp, height = 120.dp)
            .background(
                color = Color(0xFFFF0000),
                shape = RoundedCornerShape(20.dp)
            )
            .elementNodeBorder(
                color = borderColor,
                width = 6.dp,
                cornerRadius = 20.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "观察 Logcat",
            color = Color.White
        )
    }
}

private fun Modifier.elementNodeBorder(
    color: Color,
    width: Dp,
    cornerRadius: Dp
): Modifier {
    val element = ElementNodeBorderElement(
        color = color,
        width = width,
        cornerRadius = cornerRadius
    )

    log(
        Sec05B,
        "创建新的 Element@" +
                System.identityHashCode(element) +
                ", color=$color"
    )
    return this then element
}

// ADDITION 1: 新旧 Element equals 相等时直接 Reuse，Node 不执行 update。
//  如果 Node 的创建只是用了 Element 主构造器参数的一部分呢？
//  恰巧 Element 变化的是 Node 未使用的参数，Node 还能成功 Reuse 吗？
// 用 data class 自动得到正确的结构相等实现。新旧 Modifier 描述到底有没有真正发生变化。
private data class ElementNodeBorderElement(
    val color: Color,
    val width: Dp,
    val cornerRadius: Dp
) : ModifierNodeElement<ElementNodeBorderNode>() {
    override fun create(): ElementNodeBorderNode {
        val node = ElementNodeBorderNode(
            color = color,
            width = width,
            cornerRadius = cornerRadius
        )

        log(
            Sec05B,
            "Element.create() -> Node@" +
                    System.identityHashCode(node) +
                    ", color=$color"
        )
        return node
    }

    // 把这一轮 Element 中最新的声明式配置，同步进已经存在的运行时 Node。
    override fun update(node: ElementNodeBorderNode) {
        log(
            Sec05B,
            "Element.update() -> Node@" +
                    System.identityHashCode(node) +
                    ", color: ${node.color} -> $color"
        )

        node.color = color
        node.width = width
        node.cornerRadius = cornerRadius
    }
}

private class ElementNodeBorderNode(
    var color: Color,
    var width: Dp,
    var cornerRadius: Dp
) : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() {
        log(
            Sec05B,
            "Node.draw() -> Node@" +
                    System.identityHashCode(this@ElementNodeBorderNode) +
                    ", color=$color"
        )

        drawContent()

        val strokeWidth = width.toPx()
        val halfStrokeWidth = strokeWidth / 2f
        val radius = (cornerRadius.toPx() - halfStrokeWidth).coerceAtLeast(0f)

        drawRoundRect(
            color = color,
            topLeft = Offset(x = halfStrokeWidth, y = halfStrokeWidth),
            size = Size(
                width = (size.width - strokeWidth).coerceAtLeast(0f),
                height = (size.height - strokeWidth).coerceAtLeast(0f)
            ),
            cornerRadius = CornerRadius(x = radius, y = radius),
            style = Stroke(width = strokeWidth)
        )
    }
}

@PhonePreviews
@Composable
private fun Sec05B_ElementNodeUpdatePreview() {
    CourseComposeTheme {
        Sec05B_ElementNodeUpdate()
    }
}
