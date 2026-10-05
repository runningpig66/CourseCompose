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
 * @date 2026/10/5
 * @time 1:53
 *
 * 7.1C：观察 Modifier 顺序对布局层级、测量结果与绘制区域的影响。
 * 对比 layout、padding、background 的不同组合，理解 Modifier 链的内外层关系。
 */
private const val TAG = "Sec0701C"

@Composable
fun Sec0701C_ModifierChainOrder() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Modifier 链与布局层级",
                style = MaterialTheme.typography.titleMedium
            )

            ChainProbe(
                title = "A：layout → background → padding",
                modifier = Modifier
                    .extraSpaceLayout("A")
                    .background(Color(0xFFBBDEFB))
                    .border(2.dp, Color(0xFF1976D2))
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    )
            )

            ChainProbe(
                title = "B：background → layout → padding",
                modifier = Modifier
                    .background(Color(0xFFBBDEFB))
                    .border(2.dp, Color(0xFF1976D2))
                    .extraSpaceLayout("B")
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    )
            )

            ChainProbe(
                title = "C：layout → padding → background",
                modifier = Modifier
                    .extraSpaceLayout("C")
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ) // TODO 这个 padding 甚至在 preview 上不显示范围
                    .background(Color(0xFFBBDEFB))
                    .border(2.dp, Color(0xFF1976D2))
            )
        }
    }
}

@Composable
private fun ChainProbe(
    title: String,
    modifier: Modifier
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
                text = "MEASURED CHILD",
                color = Color.Black,
                modifier = modifier
            )
        }
    }
}

private fun Modifier.extraSpaceLayout(
    label: String
): Modifier {
    return layout { measurable, constraints ->
        log(TAG, "[$label] incoming = $constraints")

        val placeable = measurable.measure(constraints)

        log(
            TAG,
            "[$label] wrapped content = " +
                    "${placeable.width} x ${placeable.height}"
        )

        val layoutWidth = constraints.constrainWidth(
            placeable.width + 80.dp.roundToPx()
        )
        val layoutHeight = constraints.constrainHeight(
            placeable.height + 56.dp.roundToPx()
        )

        log(
            TAG,
            "[$label] layout = " +
                    "$layoutWidth x $layoutHeight"
        )

        layout(
            width = layoutWidth,
            height = layoutHeight
        ) {
            val x = (layoutWidth - placeable.width) / 2
            val y = (layoutHeight - placeable.height) / 2

            placeable.placeRelative(x, y)
        }
    }
}

@PhonePreviews
@Composable
private fun Sec0701C_ModifierChainOrderPreview() {
    CourseComposeTheme {
        Sec0701C_ModifierChainOrder()
    }
}
