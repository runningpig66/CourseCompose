package com.runningpig66.coursecompose.ch07.sec0702

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log

/**
 * @author runningpig66
 * @date 2026/10/7
 * @time 1:27
 *
 * 7.2D Single-pass Measurement 实验：
 * 使用前一个 Child 的测量结果计算后一个 Child 的 Constraints，
 * 并验证同一 Measurable 在一次 measurement pass 中不能重复 measure()。
 */
private const val TAG = "Sec0702D"

// 改成 true 后重新运行，用于最后的“重复 measure”实验。
// 该实验会故意触发运行时异常，观察后改回 false。
private const val RUN_DOUBLE_MEASURE_TEST = false

@Composable
fun Sec0702D_SinglePassMeasurement() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Dependent measurement",
                style = MaterialTheme.typography.titleMedium
            )

            DependentMeasurementLayout(
                modifier = Modifier
                    .width(300.dp)
                    .height(180.dp)
                    // Key 4: 当前父布局允许 300×180dp，因此 width / height 将传入 Layout 本体的约束收紧为确定尺寸。
                    // 日志中表现为 minWidth == maxWidth、minHeight == maxHeight。
                    .border(width = 2.dp, color = Color.Black)
            ) {
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .background(Color.Red),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "A · Header · 48dp",
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .height(200.dp)
                        .background(Color.Blue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "B · wants 200dp",
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun DependentMeasurementLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        // Key 3: modifier 本身不凭空产生 Constraints。
        // 父节点传来的 Constraints 会沿 Modifier 布局链向内传播，width / height 等布局 Modifier 可以在途中修改它。
        content = content
    ) { measurables, constraints ->
        // Key 2: constraints 是 Layout 的 MeasurePolicy.measure() 收到的测量约束，
        // 来源于父布局，并可能已经被外层 layout Modifier（如 width / height）调整过。

        require(measurables.size == 2) {
            "DependentMeasurementLayout requires exactly 2 children."
        }

        log(TAG, "incoming=$constraints")

        /*
         * A 是顶部 child。
         *
         * Parent 决定：
         * - A 使用完整可用宽度；
         * - 高度由 A 自己决定，但不能超过 Parent 的 maxHeight。
         */
        // Key 1: copy() 未指定的字段保持 incoming Constraints 原值。
        // 当前 maxHeight 仍是 Layout 的 180.dp 对应 px；这里只把高度从 [180, 180] 放宽为 [0, 180]。
        val headerConstraints = constraints.copy(
            minWidth = constraints.maxWidth,
            maxWidth = constraints.maxWidth,
            // Key 5: incoming 高度原本是精确的 [180dp, 180dp]；
            // 将 minHeight 改成 0 后，A 得到 [0, 180dp]，于是可以采用自己的 48.dp 高度。
            minHeight = 0
        )

        val headerPlaceable = measurables[0].measure(headerConstraints)

        log(
            TAG,
            "A result=${headerPlaceable.width} x ${headerPlaceable.height}"
        )

        /*
         * 故意违反 single-pass measurement。
         *
         * 改 RUN_DOUBLE_MEASURE_TEST = true 后，
         * 同一个 measurables[0] 会在本次 measure pass 中再次被 measure。
         */
        if (RUN_DOUBLE_MEASURE_TEST) {
            measurables[0].measure(headerConstraints)
            // Error: java.lang.IllegalStateException:
            // measure() may not be called multiple times on the same Measurable.
            // If you want to get the content size of the Measurable before calculating the final constraints,
            // please use methods like minIntrinsicWidth()/maxIntrinsicWidth() and minIntrinsicHeight()/maxIntrinsicHeight()
        }

        /*
         * B 的可用高度依赖 A 的实际测量结果。
         *
         * Parent 总高度 180dp；
         * 如果 A 实际占 48dp，
         * B 最多还能使用 132dp。
         */
        val remainingHeight =
            if (constraints.hasBoundedHeight) {
                (constraints.maxHeight - headerPlaceable.height).coerceAtLeast(0)
            } else {
                Constraints.Infinity
            }

        val bodyConstraints = Constraints(
            minWidth = constraints.maxWidth,
            maxWidth = constraints.maxWidth,
            minHeight = 0,
            maxHeight = remainingHeight
        )

        log(
            TAG,
            "B constraints=$bodyConstraints"
        )

        val bodyPlaceable = measurables[1].measure(bodyConstraints)

        log(
            TAG,
            "B result=${bodyPlaceable.width} x ${bodyPlaceable.height}"
        )

        /*
         * 这个 Layout 本身已经被外面的 width(300.dp)、height(180.dp)
         * 约束为固定尺寸。
         */
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight

        layout(
            width = layoutWidth,
            height = layoutHeight
        ) {
            headerPlaceable.placeRelative(0, 0)
            bodyPlaceable.placeRelative(0, headerPlaceable.height)
        }
    }
}

/* Output false:
incoming=Constraints(minWidth = 788, maxWidth = 788, minHeight = 473, maxHeight = 473)
A result=788 x 126
B constraints=Constraints(minWidth = 788, maxWidth = 788, minHeight = 0, maxHeight = 347)
B result=788 x 347
 */

@PhonePreviews
@Composable
private fun Sec0702D_SinglePassMeasurementPreview() {
    CourseComposeTheme {
        Sec0702D_SinglePassMeasurement()
    }
}
