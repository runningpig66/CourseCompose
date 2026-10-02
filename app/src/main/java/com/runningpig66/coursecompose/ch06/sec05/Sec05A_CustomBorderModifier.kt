package com.runningpig66.coursecompose.ch06.sec05

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log

/**
 * @author runningpig66
 * @date 2026/9/26
 * @time 3:57
 *
 * 6.5 阶段 A：第一次实现自定义绘制 Modifier。
 * 使用 ModifierNodeElement + DrawModifierNode 封装可复用的圆角边框，
 * 建立自定义 Modifier 基本结构与 ContentDrawScope.draw() 的第一手直觉。
 */
private const val Sec05A = "Sec05A"

@Composable
fun Sec05A_CustomBorderModifier() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Text(
                text = "自定义 DrawModifierNode",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .customBorder(
                        color = Color(0xFF6750A4),
                        width = 4.dp,
                        cornerRadius = 16.dp
                    )
                    .padding(24.dp)
            )

            Box(
                modifier = Modifier
                    .size(width = 220.dp, height = 120.dp)
                    .background(
                        color = Color(0xFFFF0000),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .customBorder(
                        color = Color(0xFF0700FF),
                        width = 6.dp,
                        cornerRadius = 20.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "同一个 Modifier\n可以复用",
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// 对外暴露的 Modifier API。调用者只需要认识这一层。
private fun Modifier.customBorder(
    color: Color,
    width: Dp,
    cornerRadius: Dp
): Modifier {
    return this then CustomBorderElement(
        color = color,
        width = width,
        cornerRadius = cornerRadius
    )
}

// 描述这条 Modifier 当前携带的参数，并负责创建 / 更新真正工作的 Node。
private data class CustomBorderElement(
    val color: Color,
    val width: Dp,
    val cornerRadius: Dp
) : ModifierNodeElement<CustomBorderNode>() {
    override fun create(): CustomBorderNode {
        log(Sec05A, "Element.create()")
        return CustomBorderNode(
            color = color,
            width = width,
            cornerRadius = cornerRadius
        )
    }

    override fun update(node: CustomBorderNode) {
        log(Sec05A, "Element.update()")
        node.color = color
        node.width = width
        node.cornerRadius = cornerRadius
    }
}

// 真正参与 Compose Modifier 节点系统并执行绘制工作的对象
private class CustomBorderNode(
    var color: Color,
    var width: Dp,
    var cornerRadius: Dp
) : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() {
        log(Sec05A, "Node.draw() size=$size")

        // 先绘制原本的组件内容
        drawContent()

        val strokeWidth = width.toPx()
        val halfStrokeWidth = strokeWidth / 2f
        val borderRadius = (cornerRadius.toPx() - halfStrokeWidth).coerceAtLeast(0f)

        drawRoundRect(
            color = color,
            topLeft = Offset(
                x = halfStrokeWidth,
                y = halfStrokeWidth
            ),
            size = Size(
                width = (size.width - strokeWidth).coerceAtLeast(0f),
                height = (size.height - strokeWidth).coerceAtLeast(0f)
            ),
            cornerRadius = CornerRadius(
                x = borderRadius,
                y = borderRadius
            ),
            style = Stroke(width = strokeWidth)
        )
    }
}

@PhonePreviews
@Composable
private fun Sec05A_CustomBorderModifierPreview() {
    CourseComposeTheme {
        Sec05A_CustomBorderModifier()
    }
}
