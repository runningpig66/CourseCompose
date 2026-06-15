package com.runningpig66.coursecompose

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningpig66.coursecompose.ui.theme.CourseComposeAnimateAsStateTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-05-28
 * @time 4:32
 *
 * 演示 Jetpack Compose 中 TweenSpec 与 Easing 曲线的工程映射关系。
 *
 * TweenSpec (补间动画规范) 用于定义两点之间的连续物理过渡。
 * 其核心参数为 Easing (缓动函数)，负责控制插值器在时间轴上的加速度分布。
 * 本组件构建了四条并行赛道，旨在直观验证官方预设曲线的物理身位差与适用场景。
 */
@Composable
fun TweenEasingRaceDemo() {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            EasingRaceBoard()
        }
    }
}

@Composable
fun EasingRaceBoard(modifier: Modifier = Modifier) {
    // 业务状态：标识竞速任务是否触发
    var isFinished by remember { mutableStateOf(false) }

    // 使用 BoxWithConstraints 动态获取当前容器的可用边界尺寸
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 定义运动元素的固定尺寸
        val trackItemSize = 40.dp
        // 基于容器最大宽度动态计算偏移极值 (总宽度 - 元素自身宽度)
        val maxOffset = maxWidth - trackItemSize
        val targetOffset = if (isFinished) maxOffset else 0.dp
        val duration = 1500

        // 状态分配：获取 State<Dp> 对象而非直接委托取值，以便传递给底层进行性能优化
        // 1. 标准曲线 (最慢-最快-最慢) - 常用于屏幕内元素位移
        val offset1State = animateDpAsState(
            targetValue = targetOffset,
            animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing),
            label = "FastOutSlowIn"
        )

        // 2. 减速入场曲线 (最快-最慢) - 常用于元素从屏幕外进入
        val offset2State = animateDpAsState(
            targetValue = targetOffset,
            animationSpec = tween(durationMillis = duration, easing = LinearOutSlowInEasing),
            label = "LinearOutSlowIn"
        )

        // 3. 加速退场曲线 (最慢-最快) - 常用于元素离开屏幕边缘
        val offset3State = animateDpAsState(
            targetValue = targetOffset,
            animationSpec = tween(durationMillis = duration, easing = FastOutLinearInEasing),
            label = "FastOutLinearIn"
        )

        // 4. 线性匀速曲线 - 常用于透明度或无限循环 Loading
        val offset4State = animateDpAsState(
            targetValue = targetOffset,
            animationSpec = tween(durationMillis = duration, easing = LinearEasing),
            label = "Linear"
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = { isFinished = !isFinished }) {
                Text(if (isFinished) "重置赛道" else "触发并发动画")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 渲染四条赛道
            EasingTrack("FastOutSlowIn (标准)", offset1State, Color(0xFF2196F3))
            EasingTrack("LinearOutSlowIn (入场)", offset2State, Color(0xFF4CAF50))
            EasingTrack("FastOutLinearIn (退场)", offset3State, Color(0xFFFF9800))
            EasingTrack("Linear (匀速)", offset4State, Color(0xFF9E9E9E))
        }
    }
}

@Composable
fun EasingTrack(
    title: String,
    offsetState: State<Dp>,
    color: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color(0xFFEEEEEE)) // 赛道背景
        ) {
            Box(
                modifier = Modifier
                    // 使用 Lambda 重载读取 offsetState.value，
                    // 确保高频数值的更新仅触发 Layout 阶段，完全跳过 Recomposition (重组) 阶段。
                    .offset { IntOffset(offsetState.value.roundToPx(), 0) }
                    .size(40.dp)
                    .background(color, CircleShape)
            )
        }
    }
}

@PhonePreviews
@Composable
fun TweenEasingRacePreview() {
    CourseComposeAnimateAsStateTheme {
        TweenEasingRaceDemo()
    }
}
