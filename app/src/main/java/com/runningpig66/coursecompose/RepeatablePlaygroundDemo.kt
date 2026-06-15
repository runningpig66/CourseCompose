package com.runningpig66.coursecompose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import kotlinx.coroutines.launch

/**
 * @author runningpig66
 * @date 2026-06-02
 * @time 1:19
 *
 * 永远不要用 Reverse + 偶数次：除非你的起点和终点重合（比如原地呼吸灯），否则一旦涉及位移，这种组合必定会导致动画结束时的“灵异瞬移”。
 *
 * repeatable() 构建器参数详述：用于创建一个 RepeatableSpec 实例，控制底层动画引擎如何重复执行特定的基础动画。
 * 1. iterations 迭代次数 (Int)。
 * 必须大于等于 1。定义动画生命周期内包含的完整单次执行周期数。
 * 注意：在 Reverse 模式下，一次正向运动和一次反向运动被分别计算为两次独立的 iteration。
 * 2. animation 基础动画规范 (DurationBasedAnimationSpec)。
 * 必须是基于时长的动画规范（如 tween 或 keyframes，不能是 spring）。
 * 它定义了单次迭代过程中的物理推演逻辑（如耗时与缓动函数）。
 * 3. repeatMode 重复模式 (RepeatMode)。
 * 定义相邻两次迭代之间的空间坐标与状态衔接规则。
 * - RepeatMode.Restart: 截断式重置。当前迭代结束时，底层引擎会在 0 毫秒内将状态值强制重置为 initialValue，
 * 下一次迭代始终保持正向推演。物理表现为到达终点后的瞬间闪回。
 * - RepeatMode.Reverse: 镜像式折返。当前迭代结束时，底层引擎会将目标值与初始值对调，
 * 下一次迭代执行完全反向的推演。物理表现为到达终点后的原路返回。
 */
@Composable
fun RepeatablePlaygroundDemo() {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            RepeatablePlaygroundBoard()
        }
    }
}

@Composable
fun RepeatablePlaygroundBoard(modifier: Modifier = Modifier) {
    var isPlaying by remember { mutableStateOf(false) }

    val anim1 = remember { Animatable(0.dp, Dp.VectorConverter) }
    val anim2 = remember { Animatable(0.dp, Dp.VectorConverter) }
    val anim3 = remember { Animatable(0.dp, Dp.VectorConverter) }
    val anim4 = remember { Animatable(0.dp, Dp.VectorConverter) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val trackImageSize = 40.dp
        val maxOffset = this.maxWidth - trackImageSize
        val duration = 1000

        /* 为什么当前场景不能使用 animateDpAsState？
        animateDpAsState 是高度封装的纯声明式 API（状态驱动）。它的底层逻辑是：对任何目标状态的变更，强制且无差别地应用过渡动画。
        在本例的 Reset 阶段中，当 targetValue 变为 0.dp 时，它依然会触发从 maxOffset 到 0.dp 的回退动画，无法做到瞬间切断并归位。
        Animatable 提供了底层的命令式协程控制权。它不仅支持通过 animateTo() 驱动复杂的组合动画，
        更允许通过 snapTo() 在 0 毫秒内瞬间重置物理状态，强制打断上一帧正在执行的动画树。选型建议：
        1. 单纯的 A ⇌ B 对称状态平滑过渡 -> 使用 animateDpAsState。
        2. 涉及动画中途强制打断、瞬间物理重置、或复杂的串行挂起任务流 -> 必须降级使用 Animatable。*/
        /*val anim0 = animateDpAsState(
            targetValue = if (isPlaying) maxOffset else 0.dp,
            animationSpec = repeatable(
                iterations = 3,
                animation = tween(durationMillis = duration),
                repeatMode = RepeatMode.Restart
            )
        )*/

        LaunchedEffect(isPlaying) {
            if (isPlaying) {
                val baseSpec = tween<Dp>(durationMillis = duration)

                // 1. Restart + 奇数次 (3次)
                launch {
                    anim1.animateTo(
                        targetValue = maxOffset,
                        animationSpec = repeatable(
                            iterations = 3,
                            animation = baseSpec,
                            repeatMode = RepeatMode.Restart
                        )
                    )
                }

                // 2. Reverse + 奇数次 (3次)
                launch {
                    anim2.animateTo(
                        targetValue = maxOffset,
                        animationSpec = repeatable(
                            iterations = 3,
                            animation = baseSpec,
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                }

                // 3. Restart + 偶数次 (4次)
                launch {
                    anim3.animateTo(
                        targetValue = maxOffset,
                        animationSpec = repeatable(
                            iterations = 4,
                            animation = baseSpec,
                            repeatMode = RepeatMode.Restart
                        )
                    )
                }

                // 4. Reverse + 偶数次 (4次)
                launch {
                    anim4.animateTo(
                        targetValue = maxOffset,
                        animationSpec = repeatable(
                            iterations = 4,
                            animation = baseSpec,
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                }
            } else {
                anim1.snapTo(0.dp)
                anim2.snapTo(0.dp)
                anim3.snapTo(0.dp)
                anim4.snapTo(0.dp)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = { isPlaying = !isPlaying }) {
                Text(text = if (isPlaying) "reset" else "start")
            }
            Spacer(modifier = Modifier.height(8.dp))
            RepeatableTrack("1. Restart + 奇数(3次)\n结束于终点", anim1.value, Color(0xFF2196F3))
            RepeatableTrack("2. Reverse + 奇数(3次)\n结束于终点 (去-回-去)", anim2.value, Color(0xFF4CAF50))
            RepeatableTrack("3. Restart + 偶数(4次)\n结束于终点 (一直闪回)", anim3.value, Color(0xFFFF9800))
            RepeatableTrack("4. Reverse + 偶数(4次)\n结束于起点 (去-回-去-回)", anim4.value, Color(0xFFE91E63))
        }
    }
}

@Composable
fun RepeatableTrack(
    title: String,
    offsetValue: Dp,
    color: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color(0xFFEEEEEE)) // 赛道背景
        ) {
            Box(
                modifier = Modifier
                    .offset(x = offsetValue, y = 0.dp)
                    .size(40.dp)
                    .background(color = color, shape = CircleShape)
            )
        }
    }
}

@PhonePreviews
@Composable
fun RepeatablePlaygroundDemoPreview() {
    CourseComposeTheme {
        RepeatablePlaygroundDemo()
    }
}
