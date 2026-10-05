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
 * @time 15:15
 *
 * 7.1A：第一次使用 Modifier.layout()，观察 Constraints、measure、layout 与 placement 的完整流程。
 * 通过额外扩大布局区域并居中放置内容，建立 Compose 自定义布局的基础心智模型。
 */
private const val TAG = "Sec0701A"

@Composable
fun Sec0701A_BasicLayoutModifier() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Modifier.layout() 第一次实验",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "橙色：layout Modifier 对外占据的区域\n" +
                        "蓝色：真正被测量的内部内容"
            )

            Box(
                modifier = Modifier
                    .background(Color(0xFFFFE0B2))
                    .border(2.dp, Color(0xFFF57C00))
            ) {
                Text(
                    text = "MEASURED CHILD",
                    color = Color.Black,
                    modifier = Modifier
                        .layout { measurables, constraints ->
                            log(TAG, "① incoming constraints = $constraints")

                            // 这里的 measurable 不是“这个 Text / 当前 Modifier 自己”这么简单。
                            // 更准确地说，它代表的是当前 layout Modifier 内侧、接下来需要被它测量的那一层内容。
                            val placeable = measurables.measure(constraints)

                            log(TAG, "② measured child = ${placeable.width} x ${placeable.height}")

                            val extraWidth = 80.dp.roundToPx()
                            val extraHeight = 56.dp.roundToPx()

                            // 我虽然想要这么大，但最后仍然遵守父级传进来的 Constraints
                            val layoutWidth = constraints.constrainWidth(placeable.width + extraWidth)
                            val layoutHeight = constraints.constrainHeight(placeable.height + extraHeight)

                            log(TAG, "③ report layout size = $layoutWidth x $layoutHeight")

                            // 我这一层最终决定自己的尺寸是 width × height。
                            layout(width = layoutWidth, height = layoutHeight) {
                                val x = (layoutWidth - placeable.width) / 2
                                val y = (layoutHeight - placeable.height) / 2

                                log(TAG, "④ place child at x=$x, y=$y")

                                // 把刚才已经测好的 Placeable 放到当前 Layout 区域中的 (x, y)。
                                placeable.placeRelative(x, y)
                            }
                        }
                        .background(Color(0xFFBBDEFB))
                        .border(2.dp, Color(0xFF1976D2))
                        .padding(
                            horizontal = 16.dp,
                            vertical = 12.dp
                        )
                )
            }
        }
    }
}

/* Output:
① incoming constraints = Constraints(minWidth = 0, maxWidth = 954, minHeight = 0, maxHeight = 1784)
② measured child = 450 x 127
③ report layout size = 660 x 274
④ place child at x=105, y=73
 */

@PhonePreviews
@Composable
private fun Sec0701A_BasicLayoutModifierPreview() {
    CourseComposeTheme {
        Sec0701A_BasicLayoutModifier()
    }
}
