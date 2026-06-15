package com.runningpig66.coursecompose.practice

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-06-12
 * @time 3:12
 *
 * 33. Transition 延伸：AnimatedContent() 基础 API
 *
 * 1. 作用定位：AnimatedContent 用于在不同的 Composable 之间进行状态切换，并提供完整的转场动画支持。
 * 2. 核心参数：transitionSpec。该参数接收一个 Lambda，其运行在 AnimatedContentTransitionScope 作用域中，
 * 开发者可以直接通过 initialState 和 targetState 获取切换前后的状态，从而针对不同流向定制动画。
 * 3. 三维独立轨道：它底层包含三个相互独立但协同工作的动画配置：
 * - 入场动画 (EnterTransition)：新组件如何出现。
 * - 出场动画 (ExitTransition)：旧组件如何消失。
 * - 尺寸过渡 (SizeTransform)：外部容器尺寸如何平滑过渡。
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun C33A() {
    var shown by remember { mutableStateOf(true) }
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = shown,
                transitionSpec = {
                    // 无论入场离场，始终让红色压着绿色，通过 Segment 中的状态判断实现
                    if (targetState) {
                        // ContentTransform(fadeIn(), fadeOut())
                        // Deprecated: Infix fun EnterTransition.with(ExitTransition) has been renamed to togetherWith
                        //-fadeIn() with fadeOut()
                        // ContentTransform 的组装：使用 togetherWith 中缀函数，将 EnterTransition 和 ExitTransition 结合。
                        fadeIn(tween(3000)) togetherWith
                                fadeOut(tween(3000, 3000))
                    } else {
                        (fadeIn(tween(3000)) togetherWith
                                fadeOut(tween(3000, 3000))).apply {
                            // 渲染层级控制：让下面入场的 Box 层级 -1
                            // targetContentZIndex 控制视觉上的遮盖关系。
                            // 默认情况下，新入场的组件 ZIndex 为 0f，会覆盖在旧组件之上。
                            // 这里设置为 -1f，表示强制让即将入场的新组件在旧组件底层进行渲染。
                            targetContentZIndex = -1f
                        } using SizeTransform(clip = false)
                        // 尺寸轨道控制：using 也是一个中缀函数，用于将 SizeTransform 挂载到 ContentTransform 上。
                        // clip = false 表示在容器尺寸渐变期间，若内部内容超出了当前容器的物理边界，系统不执行强制裁切。
                    }
                }
            ) { state ->
                // 根据当前的 targetState 提供对应的组件树。框架会在 Exit 动画执行完毕后，自动销毁并回收不可见的旧组件。
                if (state) {
                    TransitionSquare()
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.Green.copy(alpha = 0.5f))
                    )
                }
            }
            OutlinedButton(onClick = { shown = !shown }) {
                Text(text = "Switch")
            }
            // Transition<S>.AnimatedContent()
        }
    }
}

@PhonePreviews
@Composable
fun C33APreview() {
    CourseComposeTheme {
        C33A()
    }
}
