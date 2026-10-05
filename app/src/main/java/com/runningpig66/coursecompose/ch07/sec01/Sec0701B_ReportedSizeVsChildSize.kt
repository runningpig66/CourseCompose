package com.runningpig66.coursecompose.ch07.sec01

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log

/**
 * @author runningpig66
 * @date 2026/10/3
 * @time 20:23
 *
 * 7.1B：区分 child 的测量尺寸、当前 Layout Modifier 的对外尺寸与最终摆放位置。
 * 通过修改 Constraints 和 layout size，验证 Measure 与 Placement 各自负责什么。
 */
private const val TAG = "Sec0701B"

@Composable
fun Sec0701B_ReportedSizeVsChildSize() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Constraints / Measure / Layout",
                style = MaterialTheme.typography.titleMedium
            )

            LayoutProbe(
                title = "A：原始 Constraints",
                limitChildWidth = false,
                enlargeLayout = false
            )

            LayoutProbe(
                title = "B：只限制 child 的测量宽度",
                limitChildWidth = true,
                enlargeLayout = false
            )

            LayoutProbe(
                title = "C：child 正常测量，但当前层变大",
                limitChildWidth = false,
                enlargeLayout = true
            )

            // TODO TEST
            LayoutProbe(
                title = "D：同时限制 child 的测量宽度 & 但当前层变大",
                limitChildWidth = true,
                enlargeLayout = true
            )
        }
    }
}

@Composable
private fun LayoutProbe(
    title: String,
    limitChildWidth: Boolean,
    enlargeLayout: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title)

        Box(
            modifier = Modifier
                .background(Color(0xFFFFE0B2))
                .border(2.dp, Color(0xFFF57C00))
        ) {
            Text(
                text = "Compose layout measurement can change " +
                        "the space available to this child.",
                color = Color.Black,
                modifier = Modifier
                    .layout { measurable, constraints ->
                        // Constraints = 父级允许的尺寸范围
                        log(TAG, "$title incoming = $constraints")

                        val childConstraints =
                            if (limitChildWidth) {
                                constraints.copy(
                                    minWidth = 0,
                                    maxWidth = minOf(
                                        constraints.maxWidth,
                                        160.dp.roundToPx() // density 2.625
                                    )
                                )
                            } else {
                                constraints
                            }

                        // Placeable.width / height = 内侧内容在指定 Constraints 下测出的尺寸
                        val placeable = measurable.measure(childConstraints)

                        log(TAG, "$title child = ${placeable.width} x ${placeable.height}")

                        val extraWidth =
                            if (enlargeLayout) {
                                80.dp.roundToPx()
                            } else {
                                0
                            }

                        val extraHeight =
                            if (enlargeLayout) {
                                48.dp.roundToPx()
                            } else {
                                0
                            }

                        val layoutWidth = constraints.constrainWidth(placeable.width + extraWidth)
                        val layoutHeight = constraints.constrainHeight(placeable.height + extraHeight)

                        log(TAG, "$title layout = $layoutWidth x $layoutHeight")

                        // layout(width, height) 用来结束当前测量并返回测量结果；= 当前这一层最终向外报告的尺寸
                        // 它提供的 placement block 会在后面的 Placement 阶段执行。
                        layout(width = layoutWidth, height = layoutHeight) {
                            val x = (layoutWidth - placeable.width) / 2
                            val y = (layoutHeight - placeable.height) / 2

                            // 已经测好的 child，在当前这块区域中的哪个位置。
                            placeable.placeRelative(x, y)
                        }
                    }
                    .background(Color(0xFFBBDEFB))
                    .border(2.dp, Color(0xFF1976D2))
            )
        }
    }
}

@PhonePreviews
@Composable
private fun Sec0701B_ReportedSizeVsChildSizePreview() {
    CourseComposeTheme {
        Sec0701B_ReportedSizeVsChildSize()
    }
}
