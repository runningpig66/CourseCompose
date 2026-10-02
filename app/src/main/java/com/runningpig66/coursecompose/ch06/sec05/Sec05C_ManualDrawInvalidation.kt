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
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log

/**
 * @author runningpig66
 * @date 2026/10/1
 * @time 1:13
 *
 * 6.5 阶段 C：DrawModifierNode 的手动绘制失效实验。
 * 关闭自动 invalidation，根据真正影响绘制的参数变化主动 invalidateDraw()，
 * 区分 Element update 与 Draw 是否需要重新执行。
 */
private const val Sec05C = "Sec05C"

@Composable
fun Sec05C_ManualDrawInvalidation() {
    var borderColor by remember { mutableStateOf(Color(0xFF0700FF)) }
    var debugVersion by remember { mutableIntStateOf(0) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Draw Invalidation 实验",
                style = MaterialTheme.typography.titleLarge
            )

            Box(
                modifier = Modifier
                    .size(width = 220.dp, height = 120.dp)
                    .background(
                        color = Color.Red,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .controlledBorder(
                        color = borderColor,
                        width = 6.dp,
                        cornerRadius = 20.dp,
                        debugVersion = debugVersion
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
                    borderColor =
                        if (borderColor == Color(0xFF0700FF)) {
                            Color(0xFF00A86B)
                        } else {
                            Color(0xFF0700FF)
                        }
                }
            ) {
                Text("改变 Border 颜色")
            }

            Button(
                onClick = {
                    debugVersion++
                }
            ) {
                Text("只改 debugVersion")
            }
        }
    }
}

private fun Modifier.controlledBorder(
    color: Color,
    width: Dp,
    cornerRadius: Dp,
    debugVersion: Int
): Modifier {
    return this then ControlledBorderElement(
        color = color,
        width = width,
        cornerRadius = cornerRadius,
        debugVersion = debugVersion
    )
}

private data class ControlledBorderElement(
    val color: Color,
    val width: Dp,
    val cornerRadius: Dp,
    val debugVersion: Int
) : ModifierNodeElement<ControlledBorderNode>() {

    override fun create(): ControlledBorderNode {
        val node = ControlledBorderNode(
            color = color,
            width = width,
            cornerRadius = cornerRadius,
            debugVersion = debugVersion
        )

        log(Sec05C, "create() -> Node@${System.identityHashCode(node)}")
        return node
    }

    override fun update(node: ControlledBorderNode) {
        log(Sec05C, "Element.update() -> color=$color, debugVersion=$debugVersion")

        node.update(
            color = color,
            width = width,
            cornerRadius = cornerRadius,
            debugVersion = debugVersion
        )
    }
}

private class ControlledBorderNode(
    var color: Color,
    var width: Dp,
    var cornerRadius: Dp,
    var debugVersion: Int
) : Modifier.Node(), DrawModifierNode {

    override val shouldAutoInvalidate: Boolean
        get() = false

    fun update(
        color: Color,
        width: Dp,
        cornerRadius: Dp,
        debugVersion: Int
    ) {
        val drawChanged = this.color != color ||
                this.width != width ||
                this.cornerRadius != cornerRadius

        this.color = color
        this.width = width
        this.cornerRadius = cornerRadius
        this.debugVersion = debugVersion

        if (drawChanged) {
            log(Sec05C, "影响绘制的参数改变 -> invalidateDraw()")
            invalidateDraw()
        } else {
            log(Sec05C, "只有非绘制参数改变 -> 不 invalidateDraw()")
        }
    }

    override fun ContentDrawScope.draw() {
        log(
            Sec05C,
            "Node.draw() -> " +
                    "Node@${System.identityHashCode(this@ControlledBorderNode)}, " +
                    "debugVersion=$debugVersion"
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
private fun Sec05C_ManualDrawInvalidationPreview() {
    CourseComposeTheme {
        Sec05C_ManualDrawInvalidation()
    }
}
