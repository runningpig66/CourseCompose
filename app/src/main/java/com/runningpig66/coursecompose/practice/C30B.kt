package com.runningpig66.coursecompose.practice

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-06-10
 * @time 4:57
 *
 * 30. Transition：多属性的状态切换
 *
 * 基于 Enum 和单 Transition 的标准状态机实现
 * 1. 状态枚举化：使用 Enum 替代 Boolean 变量，明确方块在不同业务阶段的具体状态，便于后续扩充新状态。
 * 2. 单一数据源：移除多余的外部 mutableStateOf 变量，直接由 MutableTransitionState 统一管理全局状态，避免数据同步异常。
 * 3. 差值触发开场：在 remember 初始化阶段，故意使得 initialState 和 targetState 不一致，借此触发系统执行入场动画。
 * 4. 状态切换逻辑：在点击事件中，强制根据 targetState（目标状态）而非 currentState（当前状态）进行判断。
 * 此举确保了在动画执行中途发生连续点击时，系统能立即响应并打断重置动画。
 */
// 定义方块的业务生命周期状态
enum class BoxState {
    Init, // 初始隐藏态：位于 (0,0)，等待入场
    Normal, // 常规静止态：位于 (200,200)，准备接受点击
    Big // 放大交互态：回到 (0,0)，尺寸变大，出现圆角
}

@Composable
fun C30B() {
    // 实例化全局状态源，并通过 apply 制造初始差值以触发开场动画
    val transitionState = remember {
        MutableTransitionState(initialState = BoxState.Init).apply {
            targetState = BoxState.Normal
        }
    }

    val globalTransition = rememberTransition(transitionState = transitionState, label = "全局状态机")

    // 定义子动画：尺寸变化
    val sizeDp by globalTransition.animateDp(
        transitionSpec = { tween(durationMillis = 1000, easing = LinearEasing) }, label = "尺寸变化"
    ) { boxState ->
        when (boxState) {
            BoxState.Init -> 48.dp
            BoxState.Normal -> 48.dp
            BoxState.Big -> 96.dp
        }
    }

    // 定义子动画：圆角变化
    val cornerDp by globalTransition.animateDp(
        transitionSpec = { tween(durationMillis = 1000, easing = LinearEasing) }, label = "圆角变化"
    ) { boxState ->
        when (boxState) {
            BoxState.Init -> 0.dp
            BoxState.Normal -> 0.dp
            BoxState.Big -> 18.dp
        }
    }

    // 定义子动画：位移变化
    val offsetDp by globalTransition.animateDp(
        transitionSpec = { tween(durationMillis = 1000, easing = LinearEasing) }, label = "位置变化"
    ) { boxState ->
        when (boxState) {
            BoxState.Init -> 0.dp
            BoxState.Normal -> 200.dp
            BoxState.Big -> 0.dp
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                // 使用 Lambda 延迟读取位移状态，将运算压入 Layout 阶段，规避重组
                .offset { IntOffset(x = offsetDp.roundToPx(), y = offsetDp.roundToPx()) }
                //.size(sizeDp)
                //.clip(RoundedCornerShape(cornerDp))
                .size(48.dp)
                // 将尺寸变化和圆角变化，全部压入 Draw 阶段！
                .graphicsLayer {
                    // 计算缩放比例：用当前动画的尺寸除以基础尺寸 48dp
                    // sizeDp 从 48dp 变到 96dp，scale 就会从 1.0f 平滑变到 2.0f
                    val scale = sizeDp.toPx() / 48.dp.toPx()
                    scaleX = scale
                    scaleY = scale
                    // 将缩放锚点设置在左上角 (0f, 0f). 这样放大时，方块只会向右、向下膨胀，不会跑到屏幕外面。
                    transformOrigin = TransformOrigin(0f, 0f)
                    // 直接在 GPU 层面进行圆角裁剪，彻底抛弃 Modifier.clip()
                    shape = RoundedCornerShape(cornerDp)
                    clip = true
                }
                .background(Color.Red.copy(alpha = 0.5f))
                .clickable {
                    // 点击交互：基于目标状态进行切换，支持动画中途的无缝打断
                    transitionState.targetState =
                        if (transitionState.targetState == BoxState.Normal) {
                            BoxState.Big
                        } else {
                            BoxState.Normal
                        }
                }
        )
    }
}

@PhonePreviews
@Composable
fun C30BPreview() {
    CourseComposeTheme {
        C30B()
    }
}
