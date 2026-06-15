package com.runningpig66.coursecompose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay

/**
 * @author runningpig66
 * @date 2026-05-28
 * @time 4:02
 *
 * 演示 Jetpack Compose 中 KeyframesSpec (关键帧动画规范) 的工程实战应用。
 *
 * KeyframesSpec 专为多阶段、非线性且路径复杂的动画场景设计（如多段折返移动）。
 * 区别于 TweenSpec 的两点一线平滑过渡，KeyframesSpec 允许开发者在给定的时间轴内，
 * 显式定义特定时间戳 (Timestamp) 所对应的绝对状态值 (State)，并精细化控制相邻两个关键帧节点之间的速度插值器 (Easing)。
 *
 * 本示例构建了一个“密码错误视觉反馈”组件，通过单一 Boolean 状态驱动：
 * 1. 尺寸/位移系统：通过底层 Animatable 与 KeyframesSpec 协同，在 X 轴产生非线性物理震荡。
 * 2. 颜色系统：利用高层封装 animateColorAsState 驱动背景色完成异常预警的平滑过渡。
 * 两组动画协程并发执行，状态生命周期与 Composable 节点严格绑定。
 */
@Composable
fun KeyframesShakeDemo() {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            PasswordVerifyButton()
        }
    }
}

@Composable
fun PasswordVerifyButton(modifier: Modifier = Modifier) {
    // 组件是否处于异常反馈周期
    var isError by remember { mutableStateOf(false) }
    // 用于驱动 X 轴维度的偏移量插值计算 (初始坐标系归零)
    val offsetX = remember { Animatable(0.dp, Dp.VectorConverter) }
    // 依据 isError 状态计算颜色值的平滑渐变
    val bgColor by animateColorAsState(
        targetValue = if (isError) Color(0xFFE53935) else Color(0xFF333333),
        label = "ErrorBackgroundColor"
    )

    // 监听 isError 状态，触发并管理底层动画协程
    LaunchedEffect(isError) {
        if (isError) {
            // 依据传入的 Spec 执行具体的插值计算
            offsetX.animateTo(
                targetValue = 0.dp, // 约束终态：动画生命周期结束时必须回归的坐标点
                animationSpec = keyframes {
                    durationMillis = 300 // 关键帧周期的总耗时 (毫秒)
                    // 关键帧节点配置 [State] at [Timestamp] using [Easing]
                    (-15).dp at 50 // T=50ms 处，X轴负向偏移 15dp
                    15.dp at 150 // T=150ms 处，X轴正向偏移 15dp
                    (-15).dp at 250 // T=250ms 处，X轴负向偏移 15dp
                    // T>250ms 阶段，底层引擎将基于剩余时间(50ms)自动插值回归至 targetValue(0.dp)
                }
            )
            delay(500)
            // 挂起函数执行完毕即表示动画计算结束，重置驱动状态以满足状态机的可复用性
            isError = false
        }
    }

    Text(
        text = if (isError) "密码错误！" else "点击验证密码",
        modifier = modifier
            // TODO State backed values should use the lambda overload of Modifier.offset
            // 绑定离散值：根据 offsetX 输出的高频离散状态，实时驱动该节点的 Layout 阶段偏移
            .offset { IntOffset(offsetX.value.roundToPx(), 0) }
            .size(width = 200.dp, height = 50.dp)
            // 绑定渐变值，由 animateColorAsState 维护的颜色状态
            .background(color = bgColor, shape = RoundedCornerShape(8.dp))
            .clickable { if (!isError) isError = true }
            .wrapContentSize(),
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )
}

@PhonePreviews
@Composable
fun KeyframesShakePreview() {
    CourseComposeAnimateAsStateTheme {
        KeyframesShakeDemo()
    }
}
