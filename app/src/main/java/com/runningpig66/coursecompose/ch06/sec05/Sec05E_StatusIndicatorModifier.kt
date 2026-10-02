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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026/10/2
 * @time 4:39
 *
 * 6.5 阶段 E：将 DrawModifierNode 封装成可复用的视觉 Modifier。
 * 以状态圆点为例练习参数设计、Dp 到 px、LayoutDirection
 * 以及 DrawModifierNode 在实际 UI 中的基本封装方式。
 */
enum class StatusIndicatorCorner {
    TopStart,
    TopEnd,
    BottomStart,
    BottomEnd
}

@Composable
fun Sec05E_StatusIndicatorModifier() {
    var active by remember { mutableStateOf(true) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "可复用 Status Indicator",
                style = MaterialTheme.typography.titleLarge
            )

            Box(
                modifier = Modifier
                    .size(width = 220.dp, height = 120.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .statusIndicator(
                        visible = active,
                        color = Color(0xFF00A86B),
                        radius = 7.dp,
                        edgePadding = 8.dp,
                        corner = StatusIndicatorCorner.TopEnd
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("同步状态")
            }

            Button(
                onClick = {
                    active = !active
                }
            ) {
                Text(text = if (active) "隐藏状态圆点" else "显示状态圆点")
            }
        }
    }
}

fun Modifier.statusIndicator(
    visible: Boolean,
    color: Color,
    radius: Dp = 6.dp,
    edgePadding: Dp = 6.dp,
    corner: StatusIndicatorCorner = StatusIndicatorCorner.TopEnd
): Modifier {
    return this then StatusIndicatorElement(
        visible = visible,
        color = color,
        radius = radius,
        edgePadding = edgePadding,
        corner = corner
    )
}

private data class StatusIndicatorElement(
    val visible: Boolean,
    val color: Color,
    val radius: Dp,
    val edgePadding: Dp,
    val corner: StatusIndicatorCorner
) : ModifierNodeElement<StatusIndicatorNode>() {
    override fun create(): StatusIndicatorNode {
        return StatusIndicatorNode(
            visible = visible,
            color = color,
            radius = radius,
            edgePadding = edgePadding,
            corner = corner
        )
    }

    override fun update(node: StatusIndicatorNode) {
        node.visible = visible
        node.color = color
        node.radius = radius
        node.edgePadding = edgePadding
        node.corner = corner
    }
}

private class StatusIndicatorNode(
    var visible: Boolean,
    var color: Color,
    var radius: Dp,
    var edgePadding: Dp,
    var corner: StatusIndicatorCorner
) : Modifier.Node(), DrawModifierNode {

    override fun ContentDrawScope.draw() {
        drawContent()

        if (!visible) return

        val radiusPx = radius.toPx()
        val edgePaddingPx = edgePadding.toPx()

        val startX = edgePaddingPx + radiusPx
        val endX = size.width - edgePaddingPx - radiusPx

        val topY = edgePaddingPx + radiusPx
        val bottomY = size.height - edgePaddingPx - radiusPx

        val x = when (corner) {
            StatusIndicatorCorner.TopStart,
            StatusIndicatorCorner.BottomStart -> {
                if (layoutDirection == LayoutDirection.Ltr) startX else endX
            }

            StatusIndicatorCorner.TopEnd,
            StatusIndicatorCorner.BottomEnd -> {
                if (layoutDirection == LayoutDirection.Ltr) endX else startX
            }
        }

        val y = when (corner) {
            StatusIndicatorCorner.TopStart,
            StatusIndicatorCorner.TopEnd -> topY

            StatusIndicatorCorner.BottomStart,
            StatusIndicatorCorner.BottomEnd -> bottomY
        }

        drawCircle(
            color = color,
            radius = radiusPx,
            center = Offset(x, y)
        )
    }
}

@PhonePreviews
@Composable
private fun Sec05E_StatusIndicatorModifierPreview() {
    CourseComposeTheme {
        Sec05E_StatusIndicatorModifier()
    }
}
