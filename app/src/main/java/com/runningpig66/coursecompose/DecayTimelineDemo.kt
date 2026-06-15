package com.runningpig66.coursecompose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author runningpig66
 * @date 2026-06-06
 * @time 20:21
 *
 * 27. 消散型动画-AnimateDecay()
 *
 * 演示指数衰减 (exponentialDecay) 与 原生样条衰减 (splineBasedDecay) 的工程应用与手感差异。
 *
 * 1. exponentialDecay (纯数学指数引擎)：纯数学模型，基于非线性摩擦力标量进行动能扣减。
 * 高度自由的自定义 UI 动效（如轮盘阻尼、悬浮窗越界滑行）。调整摩擦系数 (如 0.4f) 可获得极度平滑的长尾阻尼感。
 *
 * 2. splineBasedDecay (原生样条引擎)：Android 系统级标准滑动模型，底层拟合了 Scroller 的物理动力学曲线。
 * 强依赖设备屏幕像素密度 (Density) 进行真实物理距离换算，确保多端设备的物理滑动体验绝对一致。
 * 用于构建自定义长列表、时间轴等，强制对标原生 RecyclerView 交互规范的滚动容器。
 *
 * 3. rememberSplineBasedDecay (环境感知语法糖)：
 * 隐式从 Compose 上下文中提取 LocalDensity，并提供实例缓存。业务开发中用于替代裸调 splineBasedDecay 的样板代码。
 */
@Composable
fun DecayTimelineDemo() {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            DecayTimelineBoard()
        }
    }
}

@Composable
fun DecayTimelineBoard() {
    var triggerCount by remember { mutableIntStateOf(0) }
    // 状态机：记录两个时间轴的 Y 轴偏移量
    val timeline1OffsetY = remember { Animatable(0.dp, Dp.VectorConverter) }
    val timeline2OffsetY = remember { Animatable(0.dp, Dp.VectorConverter) }
    // 提取当前视图层级的物理像素密度，为样条引擎提供计算基准
    val density = LocalDensity.current

    LaunchedEffect(triggerCount) {
        if (triggerCount > 0) {
            // 重置起点
            timeline1OffsetY.snapTo(0.dp)
            timeline2OffsetY.snapTo(0.dp)

            // 预留观察缓冲时间
            delay(1000.milliseconds)

            // 模拟手指向上疯狂滑动松开时，产生的一个向上的负向初速度 (-1000dp/s)
            val flingVelocity = (-1000).dp

            // 赛道 1：纯数学指数衰减 (橙色 - 指数平滑)
            launch {
                /* animateDecay 的 3 个参数：1. initialVelocity (初速度)：你给的初始动能。正数往下/右滑，负数往上/左滑。
                2. animationSpec (衰减规范)：你选的物理引擎（指数还是样条）。
                3. block (帧回调闭包)：这是一个高阶函数。动画运行的每一帧都会回调这个 block。在 99% 的情况下你不需要传它。
                但在那 1% 的复杂场景里（比如你想在速度降到 100dp/s 时，触发一个额外的音效，或者提前强制结束动画），你就可以在这个 block 里写监听逻辑。 */
                timeline1OffsetY.animateDecay(
                    initialVelocity = flingVelocity,
                    // 故意调大一点摩擦力，防止它在屏幕上滑得太远看不见停顿
                    /* exponentialDecay 的 2 个参数：1. frictionMultiplier (摩擦力倍率)：默认 1.0f。你刚才调到 3.0f，摩擦力变大，所以它早早停下了。
                    2. absVelocityThreshold (绝对速度阈值)：默认 0.1f。这是底层的“安全刹车片”。因为指数衰减公式在数学上永远不会到 0（0.1, 0.01, 0.001...），
                    为了防止协程陷入无限死循环烧毁 CPU，系统规定：只要速度的绝对值降到 0.1 以下，就直接认定为 0，强制结束动画。这个参数业务层永远不需要动。*/
                    animationSpec = exponentialDecay(frictionMultiplier = 0.4f)
                )
            }

            // 赛道 2：Android 原生样条曲线衰减 (绿色 - 系统标准阻尼)
            launch {
                timeline2OffsetY.animateDecay(
                    initialVelocity = flingVelocity,
                    // 传入 Density，启动原生列表级物理手感计算
                    animationSpec = splineBasedDecay(density = density)
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { triggerCount++ }) {
            Text(text = "触发脱手滑动 (初始速度 -1000dp/s)")
        }
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 赛道 1 列
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Exponential (指数)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                VerticalTimelineTrack(
                    offsetY = timeline1OffsetY.value,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            // 赛道 2 列
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SplineBased (样条)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                VerticalTimelineTrack(
                    offsetY = timeline2OffsetY.value,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/* 构建具备底层视口隔离机制的自定义滚动轨道。核心架构：外部 Box (静态视口层) + 内部 Column (动态物理滚动层) */
@Composable
fun VerticalTimelineTrack(offsetY: Dp, color: Color) {
    // 静态视口层 (Viewport)
    Box( // Tip: 把 modifier 配置下移到子 Column, 删除这个 Box 似乎也可以；但 G 不建议。
        modifier = Modifier
            .width(80.dp)
            /* IMP: 渲染指令裁剪：clipToBounds(). (注释后会导致子 view 滑动时超出父组件给予的范围)
            强制截断超越当前 Box 物理尺寸的子元素绘制指令。无论内部列表滑动至何处，均不会越界污染外部的 UI 空间。*/
            .clipToBounds()
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        // 动态物理滚动层 (Content)
        Column(
            modifier = Modifier
                // 实时映射状态机 Y 轴偏移矢量
                .offset { IntOffset(0, offsetY.roundToPx()) }
                /* IMP: 测量约束击穿：wrapContentHeight(unbounded = true). (注释后会导致后半部分 16 到 30 的 Item 无法绘制出来)
                突破 Compose 布局树向下传递的 Constraints.maxHeight 限制。
                允许 Column 根据实际子元素数量进行无限延伸的测算 (Measure)，避免被父容器提前截断。*/
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
        )
        {
            for (i in 0..80) { // 生成超长虚拟胶片，提供充足的滑行跑道
                Text(
                    text = "Item $i",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(if (i % 2 == 0) color else color.copy(alpha = 0.5f)),
                    color = Color.White
                )
            }
        }
    }
}

@PhonePreviews
@Composable
fun DecayTimelineDemoPreview() {
    CourseComposeTheme {
        DecayTimelineDemo()
    }
}
