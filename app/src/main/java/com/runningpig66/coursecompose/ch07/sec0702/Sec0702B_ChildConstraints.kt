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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log

/**
 * @author runningpig66
 * @date 2026/10/6
 * @time 2:23
 *
 * 7.2B Child Constraints 实验：
 * 对比直接下传 incoming Constraints 与 Parent 主动设计 Child Constraints，
 * 理解 min/max、剩余空间以及 Layout bounds 与 clipToBounds() 的区别。
 */
private const val TAG = "Sec0702B"

@Composable
fun Sec0702B_ChildConstraints() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "A. incoming Constraints 原样传给每个 child",
                style = MaterialTheme.typography.titleMedium
            )

            NaiveVerticalLayout(
                modifier = Modifier
                    .width(280.dp)
                    .height(140.dp)
                    .border(2.dp, Color.Magenta)
                    .clipToBounds() // Addition 1
            ) {
                ExperimentChildren()
            }

            Text(
                text = "B. Parent 为 child 重新设计 Constraints",
                style = MaterialTheme.typography.titleMedium
            )

            RemainingHeightVerticalLayout(
                modifier = Modifier
                    .width(280.dp)
                    .height(220.dp)
                    .border(2.dp, Color.Green)
                    .clipToBounds()
            ) {
                ExperimentChildren()
            }
        }
    }
}

@Composable
private fun ExperimentChildren() {
    Box(
        modifier = Modifier
            .width(120.dp)
            .height(24.dp)
            .background(Color.Red),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "A",
            color = Color.White
        )
    }

    Box(
        modifier = Modifier
            .width(200.dp)
            .height(96.dp)
            .background(Color.Yellow),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "B",
            color = Color.Black
        )
    }

    Box(
        modifier = Modifier
            .width(150.dp)
            .height(48.dp)
            .background(Color.Blue),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "C",
            color = Color.White
        )
    }
}

@Composable
private fun NaiveVerticalLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        log(TAG, "[Naive] incoming = $constraints")

        val placeables = measurables.mapIndexed { index, measurable ->
            log(
                TAG,
                "[Naive] child[$index] constraints = $constraints"
            )

            val placeable = measurable.measure(constraints)

            log(
                TAG,
                "[Naive] child[$index] result = " +
                        "${placeable.width} x ${placeable.height}"
            )

            placeable
        }

        val desiredWidth = placeables.maxOfOrNull { it.width } ?: 0
        val desiredHeight = placeables.sumOf { it.height }

        val layoutWidth = constraints.constrainWidth(desiredWidth)
        val layoutHeight = constraints.constrainHeight(desiredHeight)

        log(
            TAG,
            "[Naive] desired = $desiredWidth x $desiredHeight, " +
                    "final = $layoutWidth x $layoutHeight"
        )

        layout(
            width = layoutWidth,
            height = layoutHeight
        ) {
            var y = 0

            placeables.forEach { placeable ->
                placeable.placeRelative(0, y)

                y += placeable.height
            }
        }
    }
}

@Composable
private fun RemainingHeightVerticalLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        log(
            TAG,
            "[Budget] incoming = $constraints"
        )

        var remainingHeight = constraints.maxHeight

        val placeables = measurables.mapIndexed { index, measurable ->
            val childMaxHeight =
                if (constraints.hasBoundedHeight) {
                    remainingHeight
                } else {
                    Constraints.Infinity
                }
            // Addition 1 定义上面真的有意义吗？无限高度本身就是无限高度，有限本身就是有限

            val childConstraints = constraints.copy(
                minWidth = 0,
                minHeight = 0,
                maxHeight = childMaxHeight
            )

            val placeable = measurable.measure(childConstraints)

            log(
                TAG,
                "[Budget] child[$index] result = " +
                        "${placeable.width} x ${placeable.height}"
            )

            if (constraints.hasBoundedHeight) {
                remainingHeight = (remainingHeight - placeable.height).coerceAtLeast(0)
            }

            placeable
        }

        val desiredWidth = placeables.maxOfOrNull { it.width } ?: 0
        val desiredHeight = placeables.sumOf { it.height }

        val layoutWidth = constraints.constrainWidth(desiredWidth)
        val layoutHeight = constraints.constrainHeight(desiredHeight)

        log(
            TAG,
            "[Budget] desired = $desiredWidth x $desiredHeight, " +
                    "final = $layoutWidth x $layoutHeight"
        )

        layout(
            width = layoutWidth,
            height = layoutHeight
        ) {
            var y = 0

            placeables.forEach { placeable ->
                placeable.placeRelative(0, y)

                y += placeable.height
            }
        }
    }
}

@PhonePreviews
@Composable
private fun Sec0702B_ChildConstraintsPreview() {
    CourseComposeTheme {
        Sec0702B_ChildConstraints()
    }
}
