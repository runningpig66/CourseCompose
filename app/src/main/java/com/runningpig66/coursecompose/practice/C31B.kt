package com.runningpig66.coursecompose.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-06-11
 * @time 23:07
 *
 * 31. Transition 延伸：AnimatedVisibility() 实践练习
 *
 * 本文件展示了如何将 AnimatedVisibility 融入到单例枚举状态机中。同时通过留存的测试代码，
 * 记录了布局阶段修改尺寸与绘制阶段图形缩放的本质区别，以及 AnimatedVisibility 作为组件生命周期管理器的核心价值。
 */
enum class BoxState2 {
    Init, Normal, Big
}

@Composable
fun C31B() {
    val transitionState = remember {
        MutableTransitionState(initialState = BoxState2.Init).apply {
            this.targetState = BoxState2.Normal
        }
    }
    val globalTransition = rememberTransition(transitionState = transitionState, label = "全局状态机")

    val sizeDp: Dp by globalTransition.animateDp(
        transitionSpec = { tween(2000, easing = LinearEasing) }, label = "尺寸"
    ) { state ->
        when (state) {
            BoxState2.Init, BoxState2.Normal -> 100.dp
            BoxState2.Big -> 200.dp
        }
    }

    val cornerDp by globalTransition.animateDp(
        transitionSpec = { tween(2000, easing = LinearEasing) }, label = "圆角"
    ) { state ->
        when (state) {
            BoxState2.Init, BoxState2.Normal -> 0.dp
            BoxState2.Big -> 18.dp
        }
    }

    val offsetDp by globalTransition.animateDp(
        transitionSpec = { tween(2000, easing = LinearEasing) }, label = "位移"
    ) { state ->
        when (state) {
            BoxState2.Init -> 0.dp
            BoxState2.Normal -> 200.dp
            BoxState2.Big -> 0.dp
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(x = offsetDp.roundToPx(), y = offsetDp.roundToPx()) }

                    // 测试效果1：真实的物理重塑
                    // 这里退回使用了 Modifier.size() 而没有继续使用 graphicsLayer 硬件加速。
                    // 当使用 size 时，改变的是组件真实的物理边界，内部的排版引擎会重新测量，因此 Text 依然能保持 24sp 的清晰度不变。
                    // 相比之下，使用 graphicsLayer 仅仅是在底层的绘制阶段进行光学放大，
                    // 如果用它去强行拉伸包含了矢量文字的排版树，会导致字间距等比例变形，甚至出现像素模糊的劣质感。
                    // 因此，在需要保留内部元素独立排版规则的场景，我们必须接受组合阶段的重组开销，以换取正确的物理布局。
                    .size(sizeDp)
                    .clip(RoundedCornerShape(cornerDp))

                    // 测试效果2：暴力的光学滤镜
                    /*.size(100.dp)
                    .graphicsLayer {
                        val scale = sizeDp.toPx() / 100.dp.toPx()
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                        shape = RoundedCornerShape(cornerDp)
                        clip = true
                    }*/

                    .background(Color.Red.copy(alpha = 0.5f))
                    .clickable {
                        transitionState.targetState =
                            if (transitionState.targetState == BoxState2.Normal) {
                                BoxState2.Big
                            } else {
                                BoxState2.Normal
                            }
                    },
                contentAlignment = Alignment.Center
            ) {
                // AnimatedVisibility 在这里的价值不仅限于提供淡入淡出和缩放的视觉效果，它的底层本质是一个带有动画缓冲区的 if 语句组件管理器。
                // 如果仅仅使用 animateFloat 控制透明度到 0，组件即便隐形也依然会驻留在 UI 树上，持续消耗布局测量和内存资源。
                // AnimatedVisibility 会拦截销毁指令，等配置的出场动画彻底播放完毕后，才会将内部组件从树上彻底卸载释放。
                // 此外，通过调用 globalTransition 的扩展函数，我们将该组件的显隐生命周期强行收编进了外层的 Transition，
                // 确保了文字的出入场与方块的尺寸、位移在底层的时钟与协程调度上达到绝对的同步。

                // AnimatedVisibility 它不再自己控制显隐，而是根据 globalTransition 的当前枚举状态来判断自己该不该出现
                globalTransition.AnimatedVisibility(
                    visible = { targetEnum ->
                        targetEnum == BoxState2.Big
                    },
                    enter = fadeIn(tween(2000)) + scaleIn(tween(2000)),
                    exit = fadeOut(tween(2000)) + scaleOut(tween(2000)),
                ) {
                    Text(
                        text = "BIG",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 固定的测试文本
                /*Text(
                    text = "BIG",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )*/
            }
        }
    }
}

@PhonePreviews
@Composable
fun C31BPreview() {
    CourseComposeTheme {
        C31B()
    }
}
