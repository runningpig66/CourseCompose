package com.runningpig66.coursecompose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeAnimateAsStateTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-06-03
 * @time 1:51
 *
 * 25. AnimationSpec-之-InfiniteRepeatableSpec.mp4
 *
 * 演示利用 infiniteRepeatable 与 Animatable 构建雷达扫描 (Radar Pulse) 动效。核心机制解析：
 * 1. infiniteRepeatable: 用于创建一个死循环的 AnimationSpec。配合 Animatable.animateTo() 使用时，
 * 会在所在的协程 (LaunchedEffect) 中建立一个永不挂起结束的死循环状态机。
 * 2. 状态映射与硬件加速 (graphicsLayer): 本组件不直接修改尺寸属性，而是维护一个 [0f, 1f] 的虚拟进度标量，
 * 在底层 GPU 绘制阶段 (graphicsLayer) 将其映射为 2D 矩阵的 Scale(缩放) 与 Alpha(透明度)，
 * 从而彻底避免 UI 重组 (Recomposition)，实现极低性能开销的无限动效。
 */
@Composable
fun RadarPulseDemo() {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            RadarPulseBoard()
        }
    }
}

@Composable
fun RadarPulseBoard() {
    // 维护一个一维浮点数状态机，代表波纹的生命周期进度 (0f -> 1f)
    val pulseAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        pulseAnim.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                // 使用匀速线性插值 (LinearEasing)，确保波纹向外扩散的速度恒定
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                // RepeatMode.Restart: 当进度到达 1f 时，瞬间截断并归零至 0f，模拟新波纹的产生
                repeatMode = RepeatMode.Restart,
                /*initialStartOffset = StartOffset(
                    offsetMillis = 300,
                    offsetType = StartOffsetType.FastForward
                )*/
            )
        )
    }

    val baseColor = Color(0xFF2196F3)

    Box(contentAlignment = Alignment.Center) {
        // 底层视图：动态向外扩散的波纹圆环
        Box(
            modifier = Modifier
                .size(45.dp) // 初始基准尺寸
                /* 性能优化铁律：使用 graphicsLayer 读取 pulseAnim.value。
                由于 Lambda 的闭包延迟执行特性，高频的状态变更将被拦截在 Draw (绘制) 阶段，
                不会向上触发所在 Composable 的 Composition (组合) 阶段，实现零重组 */
                .graphicsLayer {
                    // 标量映射：将 [0, 1] 的进度映射为 [1倍, 6倍] 的缩放比例
                    val scaleValue = 1f + (pulseAnim.value * 5)
                    this.scaleX = scaleValue
                    this.scaleY = scaleValue
                    // 标量映射：将 [0, 1] 的进度映射为完全不透明 [1f] 到完全透明 [0f]
                    this.alpha = 1f - pulseAnim.value
                }
                .background(color = baseColor, shape = CircleShape)
        )
        // 顶层视图：静止的实体中心锚点
        Box(
            modifier = Modifier
                .size(45.dp)
                .background(color = baseColor, shape = CircleShape)
        )
    }
}

@PhonePreviews
@Composable
fun RadarPulseDemoPreview() {
    CourseComposeAnimateAsStateTheme {
        RadarPulseDemo()
    }
}
