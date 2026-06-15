package com.runningpig66.coursecompose.practice

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeAnimateAsStateTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import kotlinx.coroutines.launch

/**
 * @author runningpig66
 * @date 2026-06-09
 * @time 1:24
 *
 * Compose 渲染三阶段与状态延迟读取 (性能优化实战)
 *
 * 一、 核心概念：渲染流水线三阶段
 * 1. 组合 (Composition)：执行 @Composable 函数，生成或更新 UI 树。性能开销最大。
 * 2. 布局 (Layout)：包含测量 (Measure) 和放置 (Placement) 两个子阶段。性能开销中等。
 * 3. 绘制 (Draw)：将像素渲染到屏幕上。性能开销最小 (多由 GPU 硬件加速)。
 *
 * 二、 性能优化原则：状态读取位置决定重组层级
 * Compose 引擎的重组范围，严格取决于 State (如 animX.value) 是在哪个阶段被读取的。
 * - 常规读取 (触发重组)：Modifier.offset(x = animX.value)
 * 状态在 Composable 函数体内部被直接读取，会触发完整的 Composition -> Layout -> Draw 流水线。
 * 在动画等高频场景下，会导致严重的重组风暴，消耗大量 CPU 资源。
 * - 延迟读取 (优化重组)：Modifier.offset { IntOffset(animX.value, 0) }
 * 利用 Lambda 的闭包特性，将状态的读取推迟到 Layout 阶段内部执行。引擎会直接跳过组合阶段 (Composition)，实现零重组。
 * 此外，offset { } 在底层对 RenderNode 进行了优化，平移操作直接交由 GPU 处理，进而连绘制阶段 (Draw) 也能一并跳过。
 */
const val TAG = "ComposableStepTest"

@Composable
fun C29B() {
    val animX = remember { Animatable(0.dp, Dp.VectorConverter) }
    val decay = remember { exponentialDecay<Dp>(frictionMultiplier = 1f) }
    val scope = rememberCoroutineScope()
    var isOptimized by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Button(
                onClick = { isOptimized = !isOptimized },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(text = "当前模式: ${if (isOptimized) "优化模式 (延迟读取)" else "常规模式 (触发重组)"}")
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        scope.launch {
                            animX.snapTo(0.dp)
                            animX.animateDecay(
                                initialVelocity = 10000.dp,
                                animationSpec = decay
                            )
                        }
                    }
            ) {
                // 监测点 1：组合阶段 (Composition)
                Log.d(TAG, "Step 1: 组合阶段 (Composition) 执行")

                val trackWidth = maxWidth - 100.dp

                val dynamicModifier: Modifier = if (isOptimized) {
                    /* 优化模式：Lambda 延迟读取
                     * 1. 状态 (animX.value) 的读取被包裹在 Lambda 表达式内。
                     * 2. 日志验证：动画运行期间，Step 1 不会打印。
                     * 3. 性能表现：仅触发 Layout 阶段及其子阶段，极大降低 CPU 消耗。*/
                    Modifier.offset {
                        // 监测点 2：布局放置阶段 (Placement)
                        Log.d(TAG, "  -> Step 2: 布局放置阶段 (Placement) 执行 [Lambda内]")
                        val animValuePx = animX.value.roundToPx()
                        val trackWidthPx = trackWidth.roundToPx()
                        val usedValuePx = animValuePx % (trackWidthPx * 2)
                        val currentXPx = if (usedValuePx < trackWidthPx) {
                            usedValuePx
                        } else {
                            (trackWidthPx * 2) - usedValuePx
                        }
                        IntOffset(x = currentXPx, y = 0)
                    }
                } else {
                    /* 常规模式：组合阶段直接读取
                     * 1. 状态 (animX.value) 直接暴露在 BoxWithConstraints 的作用域中。
                     * 2. 日志验证：状态高频变化引发重组风暴，依次打印 Step 1 -> Step 2.5 -> Step 3。
                     * (注：由于尺寸未改变，自定义的 layout 测量代码块可能被系统的测量缓存优化而跳过，但无法避免严重的重组开销)。*/
                    val usedValue = (animX.value.value % (trackWidth.value * 2)).dp
                    val currentX = if (usedValue < trackWidth) {
                        usedValue
                    } else {
                        (trackWidth * 2) - usedValue
                    }
                    Modifier.offset(x = currentX, y = 0.dp)
                }

                Box(
                    modifier = dynamicModifier
                        .size(100.dp)
                        .background(Color.Blue.copy(alpha = 0.5f))
                        // 监测点：整体 Layout 测量过程
                        .layout { measurable, constraints ->
                            Log.d(TAG, "Step 2: 布局阶段 (Layout) 执行")
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                placeable.placeRelative(0, 0)
                            }
                        }
                        // 监测点：内部 Placement (放置) 动作
                        .onPlaced {
                            Log.d(TAG, "  -> Step 2.5: 节点位置被放置 (onPlaced)")
                        }
                        // 监测点：绘制阶段 (Draw)
                        .drawBehind {
                            Log.d(TAG, "Step 3: 绘制阶段 (Draw) 执行")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Launch", color = Color.White)
                }
            }
        }
    }
}

@PhonePreviews
@Composable
fun C29BPreview() {
    CourseComposeAnimateAsStateTheme {
        C29B()
    }
}