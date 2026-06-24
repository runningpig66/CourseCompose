package com.runningpig66.coursecompose.practice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-06-25
 * @time 2:37
 *
 * 实战：演示利用 LaunchedEffect 配合 Animatable 实现丝滑的单次进场动画。
 * 结合 graphicsLayer 将动画状态的读取推迟到绘制阶段，跳过了高频动画引发的重组性能陷阱。
 */
@Composable
fun C55F_EntranceAnimationTest() {
    var isCardVisible by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            Button(
                onClick = { isCardVisible = !isCardVisible },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Text(if (isCardVisible) "隐藏卡片" else "显示卡片")
            }
            if (isCardVisible) {
                SmoothEntranceCard(Modifier.align(Alignment.Center))
            }
        }
    }
}

@Preview
@Composable
fun SmoothEntranceCard(modifier: Modifier = Modifier) {
    // 定义底层动画引擎状态：初始值为 0f（代表完全透明且位于底部）
    val animationProgress = remember { Animatable(0f) }

    // 在组件成功挂载到 UI 树的瞬间，触发动画
    LaunchedEffect(Unit) {
        // animateTo 是一个挂起函数 (suspend), 它会与屏幕刷新率完美同步，在每一帧计算新值并挂起主线程
        animationProgress.animateTo(
            targetValue = 1f, // 目标值：1f（代表完全不透明且位于原位）
            animationSpec = tween(durationMillis = 800)
        )
    }
    // 将状态机的值映射到物理 UI 属性上
    Box(
        modifier
            .size(280.dp, 160.dp)
            .offset {
                // 根据动画进度计算 Y 轴偏移量 (1f -> 0偏移, 0f -> 向下偏移 200px)
                val yOffset = (1f - animationProgress.value) * 200f
                IntOffset(x = 0, y = yOffset.toInt())
            }
            .graphicsLayer {
                // 根据动画进度计算透明度 (1f -> 完全显示, 0f -> 完全透明)
                this.alpha = animationProgress.value
            }
            //.alpha(animationProgress.value)
            .background(Color.White, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "进场动画", color = Color.Black)
    }
}

@PhonePreviews
@Composable
fun C55F_EntranceAnimationTestPreview() {
    CourseComposeTheme {
        C55F_EntranceAnimationTest()
    }
}
