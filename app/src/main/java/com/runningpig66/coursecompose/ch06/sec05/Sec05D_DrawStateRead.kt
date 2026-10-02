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
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
 * @date 2026/10/2
 * @time 2:10
 *
 * TODO SideEffect、Element、Node 创建与绘制之间的实际顺序；
 *  SideEffect 一定早于 Node create/update不能作为普遍规则。
 *
 * 6.5 阶段 D：在 Drawing 阶段直接读取 Snapshot State。
 * 验证 State.value 变化可以直接触发 Draw invalidation，
 * 从而跳过 Recomposition 和 Element.update()，直接重新执行 draw()。
 */
private const val Sec05D = "Sec05D"

@Composable
fun Sec05D_DrawStateRead() {
    val borderColorState = remember { mutableStateOf(Color(0xFF0700FF)) }

    SideEffect {
        log(Sec05D, "Composition 完成")
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Draw 阶段直接读取 State",
                style = MaterialTheme.typography.titleLarge
            )

            Box(
                modifier = Modifier
                    .size(width = 220.dp, height = 120.dp)
                    .background(
                        color = Color.Red,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .stateBorder(
                        colorState = borderColorState,
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

            Button(
                onClick = {
                    borderColorState.value =
                        if (borderColorState.value == Color(0xFF0700FF)) {
                            Color(0xFF00A86B)
                        } else {
                            Color(0xFF0700FF)
                        }
                }
            ) {
                Text("改变 State")
            }
        }
    }
}

private fun Modifier.stateBorder(
    colorState: State<Color>,
    width: Dp,
    cornerRadius: Dp
): Modifier {
    return this then StateBorderElement(
        colorState = colorState,
        width = width,
        cornerRadius = cornerRadius
    )
}

private data class StateBorderElement(
    // 修改 colorState.value 不会改变这个 Element 持有的 State 对象。
    // value 在 draw() 中读取，因此它变化时直接触发 Draw 失效，不依赖 Element.update()。
    val colorState: State<Color>,
    val width: Dp,
    val cornerRadius: Dp
) : ModifierNodeElement<StateBorderNode>() {

    override fun create(): StateBorderNode {
        val node = StateBorderNode(
            colorState = colorState,
            width = width,
            cornerRadius = cornerRadius
        )

        log(
            Sec05D,
            "create() -> Node@${System.identityHashCode(node)}"
        )

        return node
    }

    override fun update(node: StateBorderNode) {
        log(Sec05D, "Element.update()")

        node.colorState = colorState
        node.width = width
        node.cornerRadius = cornerRadius
    }
}

private class StateBorderNode(
    var colorState: State<Color>,
    var width: Dp,
    var cornerRadius: Dp
) : Modifier.Node(), DrawModifierNode {

    override fun ContentDrawScope.draw() {
        // State.value 改变后仍然会先触发 Draw invalidation，再重新执行 draw()。
        // 这里只是 invalidation 的来源变成了 draw() 对 Snapshot State 的读取与观察。
        val color = colorState.value

        log(
            Sec05D,
            "Node.draw() -> " +
                    "Node@${System.identityHashCode(this@StateBorderNode)}, " +
                    "color=$color"
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
private fun Sec05D_DrawStateReadPreview() {
    CourseComposeTheme {
        Sec05D_DrawStateRead()
    }
}
