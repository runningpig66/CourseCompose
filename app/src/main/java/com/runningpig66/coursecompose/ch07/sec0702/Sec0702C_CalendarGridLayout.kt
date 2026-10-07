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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log

/**
 * @author runningpig66
 * @date 2026/10/6
 * @time 18:11
 *
 * 7.2C 二维 Grid 自定义布局：
 * 将多个 Child 按 row / column 排列，计算固定列宽、每行最大高度、
 * Row 顶部偏移和 Parent 总高度，建立二维 Layout 算法模型。
 */
private const val TAG = "Sec0702C"

@Composable
fun Sec0702C_CalendarGridLayout() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "3-column custom grid",
                style = MaterialTheme.typography.titleMedium
            )

            CalendarGridLayout(
                columns = 3,
                modifier = Modifier
                    .width(300.dp)
                    .border(
                        width = 2.dp,
                        color = Color.Black
                    )
            ) {
                GridItem(
                    text = "A\n48dp",
                    height = 48.dp,
                    color = Color.Red
                )

                GridItem(
                    text = "B\n80dp",
                    height = 80.dp,
                    color = Color.Yellow,
                    textColor = Color.Black
                )

                GridItem(
                    text = "C\n56dp",
                    height = 56.dp,
                    color = Color.Blue
                )

                GridItem(
                    text = "D\n72dp",
                    height = 72.dp,
                    color = Color.Cyan,
                    textColor = Color.Black
                )

                GridItem(
                    text = "E\n40dp",
                    height = 40.dp,
                    color = Color.Magenta
                )

                GridItem(
                    text = "F\n64dp",
                    height = 64.dp,
                    color = Color.Green,
                    textColor = Color.Black
                )
            }
        }
    }
}

@Composable
private fun GridItem(
    text: String,
    height: Dp,
    color: Color,
    textColor: Color = Color.White
) {
    Box(
        modifier = Modifier
            .height(height)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CalendarGridLayout(
    columns: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    require(columns > 0)

    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        require(constraints.hasBoundedWidth) {
            "CalendarGridLayout requires a bounded width."
        }

        val layoutWidth = constraints.maxWidth
        val cellWidth = layoutWidth / columns

        log(
            TAG,
            "incoming=$constraints, " +
                    "layoutWidth=$layoutWidth, cellWidth=$cellWidth"
        )

        val placeables = measurables.mapIndexed { index, measurable ->
            val childConstraints = constraints.copy(
                minWidth = cellWidth,
                maxWidth = cellWidth,
                minHeight = 0 // ISSUE 第2行孩子的高度不用减吗？
            )

            val placeable = measurable.measure(childConstraints)

            val row = index / columns
            val column = index % columns

            log(
                TAG,
                "child[$index] row=$row column=$column " +
                        "size=${placeable.width} x ${placeable.height}"
            )

            placeable
        }

        // 总行数：对 child 数量做向上取整，例如 6 个 child、3 列 → 2 行
        val rowCount = (placeables.size + columns - 1) / columns
        // 保存每一行的实际高度；每行高度由该行最高的 child 决定
        val rowHeights = IntArray(rowCount)

        placeables.forEachIndexed { index, placeable ->
            val row = index / columns
            rowHeights[row] = maxOf(rowHeights[row], placeable.height)
        }

        val desiredHeight = rowHeights.sum()
        val layoutHeight = constraints.constrainHeight(desiredHeight)

        // 保存每一行顶部的 y 坐标，例如 [0, row0Height, row0Height + row1Height, ...]
        val rowTopOffsets = IntArray(rowCount)
        // 当前累计使用的高度，用来计算下一行的顶部坐标
        var accumulatedHeight = 0

        rowHeights.forEachIndexed { row, rowHeight ->
            rowTopOffsets[row] = accumulatedHeight
            accumulatedHeight += rowHeight
        }

        log(
            TAG,
            "rowHeights=${rowHeights.contentToString()}, " +
                    "desiredHeight=$desiredHeight, " +
                    "layoutHeight=$layoutHeight"
        )

        layout(
            width = layoutWidth,
            height = layoutHeight
        ) {
            placeables.forEachIndexed { index, placeable ->
                val row = index / columns
                val column = index % columns

                val x = column * cellWidth
                val y = rowTopOffsets[row]

                log(
                    TAG,
                    "place child[$index]: " +
                            "row=$row column=$column x=$x y=$y"
                )

                placeable.placeRelative(x = x, y = y)
            }
        }
    }
}

@PhonePreviews
@Composable
private fun Sec0702C_CalendarGridLayoutPreview() {
    CourseComposeTheme {
        Sec0702C_CalendarGridLayout()
    }
}
