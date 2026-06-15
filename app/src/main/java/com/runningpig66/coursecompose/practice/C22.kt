package com.runningpig66.coursecompose.practice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeAnimateAsStateTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-05-31
 * @time 2:36
 */
@Composable
fun MainView() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

        var big by remember { mutableStateOf(false) }
        val size by animateDpAsState(if (big) 96.dp else 48.dp)
        val size1 = remember(big) { if (big) 96.dp else 48.dp }
        val offset = remember(big) { if (big) 0.dp else 300.dp }

        val anim = remember {
            Animatable(size1, Dp.VectorConverter)
        }
        val offsetAnim = remember {
            Animatable(offset, Dp.VectorConverter)
        }

        LaunchedEffect(big) {
            //anim.snapTo(if (big) 192.dp else 0.dp)

            /* Out (离开)：指代动画的“起步阶段”（离开起始坐标的瞬间）。
            In (进入)：指代动画的“刹车阶段”（进入目标坐标的瞬间）。
            FastOutSlowInEasing 起步阶段快速加速（Fast Out），接近终点时拥有较长缓冲的平滑减速（Slow In）。最慢-最快-最慢。
            LinearOutSlowInEasing 初始瞬间即达到最大速度（Linear Out，无起步加速阶段），随后平滑减速直到停止（Slow In）。入场：最快-最慢。
            FastOutLinearInEasing 起步平滑加速（Fast Out），但到达终点时保持极高速度，不做缓冲直接切断（Linear In）。退场：最慢-最快。
            LinearEasing 匀速运动，$f(t) = t$。加速度始终为 0。匀速。*/
            //anim.animateTo(size1, TweenSpec(durationMillis = 2000, easing = FastOutSlowInEasing))
            //offsetAnim.animateTo(offset, TweenSpec(durationMillis = 2000, easing = LinearOutSlowInEasing))
            //offsetAnim.animateTo(offset, TweenSpec(easing = Easing { it * 0.1.toFloat() }))
            //offsetAnim.animateTo(offset, tween(durationMillis = 2000, easing = LinearOutSlowInEasing))

            //offsetAnim.animateTo(offset, SnapSpec(2000))
            //offsetAnim.animateTo(offset, snap(500))

            /*anim.animateTo(size1, KeyframesSpec(KeyframesSpecConfig<Dp>().apply {
            }))*/

            /*anim.animateTo(size1, keyframes {
                48.dp at 0 using FastOutLinearInEasing
                144.dp at 500
                durationMillis = 1000
                delayMillis = 10
            })*/

            /*anim.animateTo(
                48.dp,
                *//*spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = Spring.StiffnessVeryLow,
                    visibilityThreshold = 1.dp
                )*//*
                spring(0.1f, Spring.StiffnessHigh), // SpringSpec
                2000.dp
            )*/

            /*anim.animateTo(
                size1,
                repeatable(
                    iterations = 2,
                    animation = tween(),
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(
                        offsetMillis = 300,
                        offsetType = StartOffsetType.FastForward
                    )
                )
            )*/

            anim.animateTo(
                targetValue = size1,
                animationSpec = infiniteRepeatable(
                    animation = tween(),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(
                        offsetMillis = 300,
                        offsetType = StartOffsetType.FastForward
                    )
                )
            )
        }

        Box(
            modifier = Modifier
                //.offset(offsetAnim.value, offsetAnim.value)
                .padding(innerPadding)
                .size(anim.value)
                .background(Color.Green)
                .clickable {
                    big = !big
                }
        )
    }
}

@PhonePreviews
@Composable
fun HomeIndexScreenPreview() {
    CourseComposeAnimateAsStateTheme {
        MainView()
    }
}
