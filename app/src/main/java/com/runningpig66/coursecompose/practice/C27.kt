package com.runningpig66.coursecompose.practice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author runningpig66
 * @date 2026-06-06
 * @time 17:00
 *
 * 27. 消散型动画-AnimateDecay()
 *
 * DecayAnimationSpec 衰减动画
 * AnimateDecay()
 * exponentialDecay()
 * splineBasedDecay()
 * rememberSplineBasedDecay()
 */
@Composable
fun C27() {
    Scaffold { innerPadding ->
        var selected by remember { mutableStateOf(false) }
        val anim = remember { Animatable(0.dp, Dp.VectorConverter) }
        var padding2 by remember { mutableStateOf(anim.value) } // only Dp assigned

        //splineBasedDecay<Dp>(density = LocalDensity.current)
        val decay1 = rememberSplineBasedDecay<Dp>()
        val decay2 = exponentialDecay<Dp>()

        LaunchedEffect(selected) {
            anim.snapTo(0.dp)
            delay(100.milliseconds)

            anim.animateDecay(
                initialVelocity = 2000.dp,
                animationSpec = decay2,
            ) {
                // Tip 反模式：如果你想让两个方块保持相同的移动轨迹，它们应当共享同一个状态源（直接都读取 anim.value）。
                // 引入一个多余的 MutableState 进行毫无意义的中转同步，徒增了内存分配与状态追踪的开销。
                padding2 = this.value
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .padding(top = anim.value)
                    .size(100.dp)
                    .background(Color.Green.copy(alpha = 0.5f))
                    .clickable(true) {
                        selected = !selected
                    }
            )
            Box(
                modifier = Modifier
                    // Tip 反模式：此处直接使用 Modifier.padding 驱动动画，会引发高频的 Layout 阶段重绘，
                    // 我们在之前的 graphicsLayer 讨论中已经明确过这是性能隐患，此处不再赘述。
                    .padding(top = padding2)
                    .size(100.dp)
                    .background(Color.Red.copy(alpha = 0.5f))
            )
        }
    }
}

@PhonePreviews
@Composable
fun C27Preview() {
    CourseComposeAnimateAsStateTheme {
        C27()
    }
}
