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
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log

/**
 * @author runningpig66
 * @date 2026/10/5
 * @time 22:30
 *
 * 7.2A Layout() 基础实验：
 * 使用 Layout 自定义多子项纵向布局，练习 Measurable → Placeable、
 * Parent 尺寸计算以及多个 Child 的 placement。
 */
private const val TAG = "Sec0702A"

@Composable
fun Sec0702A_BasicCustomLayout() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Layout() basic experiment",
                style = MaterialTheme.typography.titleMedium
            )

            BasicVerticalLayout(
                modifier = Modifier.border(
                    width = 2.dp,
                    color = Color.Green
                )
            ) {
                //Column {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(24.dp)
                        .background(Color.Red.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("A")
                }

                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(96.dp)
                        .background(Color.Yellow.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("B")
                }

                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .height(48.dp)
                        .background(Color.Blue.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("C")
                }
                //}
            }
        }
    }
}

@Composable
private fun BasicVerticalLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        log(
            TAG,
            "measure start: childCount=" +
                    "${measurables.size}, constraints=$constraints"
        )

        val placeables = measurables.mapIndexed { index, measurable ->
            val placeable = measurable.measure(constraints)

            log(
                TAG,
                "child[$index] measured: " +
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
            "parent size: $layoutWidth x $layoutHeight"
        )

        layout(
            width = layoutWidth,
            height = layoutHeight
        ) {
            var y = 0

            placeables.forEachIndexed { index, placeable ->
                log(
                    TAG,
                    "place child[$index]: x=0, y=$y"
                )

                placeable.placeRelative(0, y)

                y += placeable.height
            }
        }
    }
}

@PhonePreviews
@Composable
private fun Sec0702A_BasicCustomLayoutPreview() {
    CourseComposeTheme {
        Sec0702A_BasicCustomLayout()
    }
}
