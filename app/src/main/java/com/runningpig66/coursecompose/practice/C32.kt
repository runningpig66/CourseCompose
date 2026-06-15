package com.runningpig66.coursecompose.practice

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import com.runningpig66.coursecompose.ui.theme.CourseComposeAnimateAsStateTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-06-12
 * @time 1:45
 *
 * 32. Transition-延伸：Crossfade() 交叉淡入淡出
 *
 * 本文件记录了 Crossfade 的底层特性与尺寸跳变陷阱。
 * 它的核心职责非常单一：仅通过改变 Alpha 透明度来实现新旧组件的交替，底层绝对不会进行任何关于尺寸差异的平滑过渡计算。
 * 在动画执行期间，它会粗暴地选取新旧组件中较大的尺寸作为占位，动画结束瞬间再猛烈回缩。
 * 因此，它只适合用于新旧组件物理尺寸完全一致的场景，例如固定区域大小的骨架屏与真实内容的交替，或者被父级定死尺寸的单页面视图切换。
 */
@Composable
fun C32() {
    var shown by remember { mutableStateOf(true) }
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Crossfade(
                targetState = shown,
                //modifier = Modifier.size(96.dp),
                animationSpec = tween(2000)
            ) { state ->
                // 这里的外部嵌套 Box 是为了强行修复 Crossfade 的底层缺陷。
                // Crossfade 底层基于 Box 测量，但未开放 contentAlignment 参数，且默认写死了 TopStart (左上角) 对齐。
                // 这导致在 96dp 占位空间内淡入 24dp 的小方块时，小方块会死死贴在左上角，直到动画结束才瞬间因为外层 Column 的居中规则跳跃到中间。
                // 为了屏蔽这种恶劣的跳变与错位，我们在内层手动套了一个尺寸固定为 96dp 且内容居中的 Box。
                // 这样无论内部状态如何切换，外层容器看到的尺寸永远恒定，内部的淡入淡出也就能乖乖保持在中心位置。
                Box(
                    modifier = Modifier.size(96.dp),
                    contentAlignment = Alignment.Center
                ) {
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
            }
            OutlinedButton(onClick = { shown = !shown }) {
                Text(text = "Switch")
            }
            // Transition<T>.Crossfade()
        }
    }
}

@PhonePreviews
@Composable
fun C32Preview() {
    CourseComposeAnimateAsStateTheme {
        C32()
    }
}
