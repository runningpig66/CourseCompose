package com.runningpig66.coursecompose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import java.util.Locale

/**
 * @author runningpig66
 * @date 2026-06-06
 * @time 19:10
 *
 * 27. 消散型动画-AnimateDecay()
 *
 * 演示 Compose 动画系统中的纯数学动能衰减引擎：exponentialDecay。
 *
 * 在 Compose 动画体系中，DecayAnimationSpec 是一类不依赖 targetValue (终点位置) 的特殊物理状态机。
 * exponentialDecay (指数衰减) 是该体系中最基础的数学模型，它完全脱离 Android 系统的物理像素密度约束，仅通过指数级衰减公式递减数值，直至动能耗尽。
 * 适用于无需遵循系统级滑动规范的自定义组件动效（例如：轮盘抽奖的非线性减速、悬浮球的边界滑行、
 * 或特殊数值的递减动画）。通过降低摩擦系数，可以构建极度平滑的“长尾”阻尼视觉效果。
 */
@Composable
fun ExponentialDecayDemo() {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            ExponentialDecayBoard()
        }
    }
}

@Composable
fun ExponentialDecayBoard(modifier: Modifier = Modifier) {
    // 摩擦力倍率标量：控制指数公式中的衰减曲率，值越小动能损耗越慢。
    var friction by remember { mutableFloatStateOf(1.0f) }
    // 触发器
    var triggerCount by remember { mutableIntStateOf(0) }
    // 状态机：记录 X 轴偏移量
    val offsetX = remember { Animatable(0.dp, Dp.VectorConverter) }

    LaunchedEffect(triggerCount) {
        if (triggerCount > 0) {
            // 每次测试前，瞬间重置到屏幕左侧起点
            offsetX.snapTo(0.dp)

            /* 启动动能衰减计算
             * @param initialVelocity 注入状态机的初始速率矢量 (dp/s)。
             * @param animationSpec 定义动能耗散规则的规范接口。
             */
            offsetX.animateDecay(
                initialVelocity = 4000.dp, // 4000 dp/s 初始速度
                animationSpec = exponentialDecay(
                    // 摩擦力倍率：核心变量。建议区间 [0.3f, 1.0f]，用于调试非线性阻尼视觉。
                    frictionMultiplier = friction,
                    /* 绝对速度阈值 (底层保护机制)：由于指数计算具备渐进特征（无限趋近于 0），当实时计算的绝对速率低于 0.1f 时，
                    引擎将主动终止协程，避免无穷尽的微小重绘计算，业务层通常无需干预。*/
                    absVelocityThreshold = 0.1f
                )
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Box(
                modifier = Modifier
                    // 在 Draw 阶段应用实时坐标推演结果
                    .offset { IntOffset(offsetX.value.roundToPx(), 0) }
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer, CircleShape))
        }

        Button(onClick = { triggerCount++ }) {
            Text("赋予 4000dp/s 初始速度")
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Friction (摩擦系数):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = String.format(Locale.US, "%.2f", friction),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Slider(
                value = friction,
                onValueChange = { friction = it },
                valueRange = 0.1f..10f // 滑动范围：0.1 冰面 ~ 10 泥潭
            )
        }
    }
}

@PhonePreviews
@Composable
fun ExponentialDecayDemoPreview() {
    CourseComposeTheme {
        ExponentialDecayDemo()
    }
}
