package com.runningpig66.coursecompose.practice

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-06-09
 * @time 21:22
 *
 * 30. Transition：多属性的状态切换
 *
 * 本文件记录了 Transition 核心 API 的演进与探索过程：
 * 1. updateTransition: 基于外部状态 (big) 自动驱动，缺点是无法实现组件初次加载时的“入场动画”。
 * 2. MutableTransitionState: 探究底层状态机，通过手动制造 initialState 和 targetState 的差值来触发入场动画。
 * 3. 架构痛点 (铺垫 C30B): 本例中存在两个独立的 Transition 对象在管理同一块 UI，
 * 这在复杂工程中属于反模式，引出了后续使用 Enum 进行单状态机重构的需求。
 */
@Composable
fun C30A() {
    var big by remember { mutableStateOf(false) }
    // 常规状态机：依赖外部状态 big。初次组合时 targetState == currentState == false，故无开场动画。
    val bigTransition: Transition<Boolean> = updateTransition(targetState = big, label = "sizeAndCornerTransition")
    //val size1: Dp by animateDpAsState(if (big) 96.dp else 48.dp)
    val size2: Dp by bigTransition.animateDp(
        transitionSpec = {
            /*if (false isTransitioningTo true) spring() else tween()*/
            /*when {
                (false isTransitioningTo true) -> spring()
                else -> tween()
            }*/
            tween(durationMillis = 1000, easing = LinearEasing)
        },
        label = "size2Dp"
    ) { if (it) 96.dp else 48.dp }

    //val corner1 by animateDpAsState(if (big) 18.dp else 0.dp)
    val corner2 by bigTransition.animateDp(
        transitionSpec = { tween(durationMillis = 1000, easing = LinearEasing) },
        label = "corner2Dp"
    ) { if (it) 18.dp else 0.dp }

    // 自带初值的自定义状态机：为了实现开场位移动画，人为剥离出一个具备初值的 MutableTransitionState
    val mutableTransitionState = remember {
        MutableTransitionState(initialState = big)/*.apply { targetState = true }*/
    }
    // 动态绑定外部状态，实现点击后也能产生位移折返
    mutableTransitionState.targetState = !big

    val transition1 = updateTransition(mutableTransitionState) // Deprecated
    // 废弃旧 API，使用更符合语义的 rememberTransition 缓存自定义状态机
    val transition2 = rememberTransition(transitionState = mutableTransitionState, label = "offsetTransition")

    val offset: Dp by transition2.animateDp(
        transitionSpec = { tween(durationMillis = 1000, easing = LinearEasing) },
        label = "offsetDp"
    ) { if (it) 200.dp else 0.dp }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .padding(paddingValues = innerPadding)
                //.offset { IntOffset(x = offset.value.roundToPx(), y = offset.value.roundToPx()) }
                // 性能优化：通过 Lambda 延迟获取 offset 的真实值，使其只在布局 (Layout) 阶段生效。
                // 此时 offset 已被 by 关键字解包为 Dp，需用 IntOffset 转换包装。
                .offset { IntOffset(x = offset.roundToPx(), y = offset.roundToPx()) }
                // 缺陷留存：此处直接读取 size2 和 corner2，在没有 graphicsLayer 保护的情况下，会导致内部 Box 在动画期间经历严重的高频重组。
                .size(size2)
                .clip(RoundedCornerShape(corner2))
                .background(Color.Green.copy(alpha = 0.5f))
                .clickable { big = !big }
        )
    }
}

@PhonePreviews
@Composable
fun C30APreview() {
    CourseComposeTheme {
        C30A()
    }
}
