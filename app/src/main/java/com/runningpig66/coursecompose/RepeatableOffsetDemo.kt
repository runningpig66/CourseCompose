package com.runningpig66.coursecompose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
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
 * @time 3:13
 *
 * 演示 Jetpack Compose 中 RepeatableSpec 的 initialStartOffset (时间轴相位偏移) 机制。
 * initialStartOffset 用于在并发动画或无限循环动画 (InfiniteRepeatable) 中，
 * 通过干预底层状态机在 T=0 时刻的内部时钟 (Internal Clock)，制造物理推演上的时间差（即相位偏移）。
 * 该机制是实现多轨协同动画（如级联加载、波浪指示器）的核心 API。
 *
 * StartOffset 参数与执行策略详解：StartOffset 决定了动画在被触发瞬间 (T=0) 的时间轴干预方式。
 * 1. offsetMillis: 相位偏移的时间标量（毫秒），必须大于等于 0。
 * 2. offsetType (偏移策略枚举):
 * - StartOffsetType.FastForward (快进模式 / 默认行为): 在动画启动瞬间，将内部时钟直接推进至 offsetMillis。
 * UI 不会从 initialValue 开始渲染，而是直接通过插值器计算出 T=offsetMillis 时的空间坐标并从此帧开始执行。
 * 首次迭代的实际可视播放时长将被缩减，时长等于 (单次 duration - offsetMillis)。后续迭代恢复正常。
 * - StartOffsetType.Delay (延迟模式): 在动画启动瞬间，将状态机挂起 (Suspend) 持续 offsetMillis 毫秒。
 * 在延迟期间，UI 严格维持 initialValue 不变。倒计时结束后，内部时钟才从 0 开始正常推演。
 * 首次迭代的完整物理表现未被破坏，但整个动画周期的总耗时将被延长，总耗时等于 (原有总时长 + offsetMillis)。
 */
@Composable
fun RepeatableOffsetDemo() {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            RepeatableOffsetBoard()
        }
    }
}

@Composable
fun RepeatableOffsetBoard(modifier: Modifier = Modifier) {
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

        LaunchedEffect(isPlaying) {
            if (isPlaying) {
                val baseSpec = tween<Dp>(durationMillis = duration)

                // 1. 零偏移对照组
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

                // 2. FastForward (快进 300ms)
                launch {
                    anim2.animateTo(
                        targetValue = maxOffset,
                        animationSpec = repeatable(
                            iterations = 3,
                            animation = baseSpec,
                            repeatMode = RepeatMode.Restart,
                            initialStartOffset = StartOffset(
                                offsetMillis = 300,
                                offsetType = StartOffsetType.FastForward
                            )
                        )
                    )
                }

                // 3. Delay (延迟 300ms)
                launch {
                    anim3.animateTo(
                        targetValue = maxOffset,
                        animationSpec = repeatable(
                            iterations = 3,
                            animation = baseSpec,
                            repeatMode = RepeatMode.Restart,
                            initialStartOffset = StartOffset(
                                offsetMillis = 300,
                                offsetType = StartOffsetType.Delay
                            )
                        )
                    )
                }

                // 4. Reverse 模式 + FastForward (快进 500ms，即单次周期的 50%)
                // 验证在折返模式下，时间轴的偏移如何影响空间折返点。
                launch {
                    anim4.animateTo(
                        targetValue = maxOffset,
                        animationSpec = repeatable(
                            iterations = 3,
                            animation = baseSpec,
                            repeatMode = RepeatMode.Reverse,
                            initialStartOffset = StartOffset(
                                offsetMillis = 500,
                                offsetType = StartOffsetType.FastForward
                            )
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
                Text(text = if (isPlaying) "Reset" else "Start")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OffsetTrack("1. 零偏移 (0ms)\n标准 T=0 启动", anim1.value, Color(0xFF2196F3))
            OffsetTrack("2. FastForward (300ms)\n越过 30% 轨迹首帧启动", anim2.value, Color(0xFF4CAF50))
            OffsetTrack("3. Delay (300ms)\n状态机挂起 300ms 后启动", anim3.value, Color(0xFFFF9800))
            OffsetTrack("4. Reverse + FastForward (500ms)\n越过 50% 轨迹，发生错峰折返", anim4.value, Color(0xFFE91E63))
        }
    }
}

@Composable
fun OffsetTrack(
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
fun RepeatableOffsetDemoPreview() {
    CourseComposeTheme {
        RepeatableOffsetDemo()
    }
}
