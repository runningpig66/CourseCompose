package com.runningpig66.coursecompose.practice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeAnimateAsStateTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author runningpig66
 * @date 2026-06-07
 * @time 23:34
 *
 * 29. 打断施法：动画的边界限制、结束和取消
 *
 * 1. 动画的互斥与异常打断 (MutatorMutex)：当多个协程试图同时驱动同一个 [Animatable] 时，新发起的动画指令会抢占底层互斥锁。
 * 旧动画会被强制中断，并在原挂起点抛出 [CancellationException]（属于非正常结束）。
 * 2. 主动取消动画 (Manual Stop)：调用挂起函数 stop()，其本质同样是基于互斥锁机制向当前运行的动画协程发送取消信号。
 * 3. 边界限制与状态机接力 (Bounds & AnimationResult)：通过 updateBounds() 设定物理上下界。当动画触碰边界时会触发正常结束，
 * 并返回包含 BoundReached 状态的 AnimationResult。结合 while 循环，
 * 提取上一次撞墙瞬间的末速度并取反 (-endState.velocity) 作为新动画的初速度，从而在宏观上模拟出物理反弹效果。
 * 4. 架构进阶预警 (离散碰撞缺陷)：上述 while 循环接力法属于基础的“离散碰撞检测”，
 * 在微观的帧同步上会产生“时间吞噬(黏墙效应)”与“摩擦力断层(动能失真)”。
 * 关于 100% 精度的无损物理反弹方案（空间折叠/画纸切割法）以及 Compose 渲染三阶段的性能优化，请参阅实践类 [C29B]。
 *
 * notes: Compose 动画中的离散碰撞与时间速度丢失陷阱.md
 */
@Composable
fun C29A() {
    Scaffold { innerPadding ->
        // 测试 stop()
        val anim1 = remember { Animatable(0.dp, Dp.VectorConverter) }
        // 测试 updateBounds()
        val animY = remember { Animatable(0.dp, Dp.VectorConverter) }
        val animX = remember { Animatable(0.dp, Dp.VectorConverter) }
        val decay = remember {
            exponentialDecay<Dp>(frictionMultiplier = 0.5f)
        }
        // 获取当前 Composable 生命周期绑定的协程作用域
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            launch {
                delay(1000.milliseconds)
                try {
                    anim1.animateDecay(
                        initialVelocity = 2000.dp,
                        animationSpec = decay
                    )
                } catch (_: CancellationException) {
                    /* 当多个协程同时驱动同一个 Animatable 时，底层使用 MutatorMutex 保证互斥。
                    后发起的动画（或 stop()）会向当前正在运行的动画协程抛出一个 CancellationException，
                    强制它让出控制权——这正是此处能 catch 到该异常的原因。
                    捕获后一般不需要重新抛出，因为动画引擎内部已完成状态清理，协程安静结束即可，不会导致父作用域崩溃。
                    注意：在主线程调度下，此时新动画协程正挂起等待锁。如果 catch 块或其后续代码执行耗时操作，会阻塞新动画的启动，
                    表现为“旧动画停止 → 界面卡顿 → 新动画延迟开始”。*/
                    println("Catch CancellationException.")
                }
            }

            launch {
                delay(1500.milliseconds)
                anim1.animateDecay(
                    initialVelocity = (-1000).dp,
                    animationSpec = decay
                )
            }

            /*launch {
                delay(2000.milliseconds)
                anim.stop()
            }*/

            /* 注意：不能直接在 LaunchedEffect 块内调用 anim.stop()。stop() 只能中断当前正在执行的动画协程。
            当 LaunchedEffect 进入组合时（T=0 毫秒），内部的几个 launch 都还处在 delay() 挂起阶段，
            尚未真正开始 animateDecay，此时没有任何动画在运行，调用 stop() 会静默返回，达不到打断效果。
            正确做法是在动画已经启动的时间点调用 stop()，例如在第三个 launch 里 delay(2000) 后执行。*/
            //-anim.stop()
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LaunchedEffect(maxWidth, maxHeight) {
                /* Side Effect (副作用) 隔离原则：测量与状态同步
                纯函数准则：永远不要在 Composable 的正常组合流（Composition）中直接执行状态修改（如裸调 updateBounds）。
                由于 Compose 的重组机制可能是高频、并发甚至被中途取消的，裸写副作用会导致严重的“状态泄漏”与“不可预测的 Bug”。
                安全的桥梁：当状态修改严格依赖底层 UI 的测量结果（如 BoxWithConstraints 测出的 maxHeight）时，
                必须将测量值作为 Key 传入 LaunchedEffect（或放在 onClick 等事件回调中）。
                这样做不仅将不纯的修改动作安全隔离到了协程中，还能保证：只要屏幕或容器尺寸发生变化（Key 改变，例如旋屏），
                动画的物理边界就会自动触发重新校准，完美闭环。*/
                animY.updateBounds(upperBound = maxHeight - 100.dp)
                animX.updateBounds(upperBound = maxWidth - 208.dp, lowerBound = 0.dp) // minus paddingLeft

                delay(1000.milliseconds)

                launch {
                    animY.animateDecay(
                        initialVelocity = 1800.dp,
                        animationSpec = decay
                    )
                }
                /* 离散碰撞检测的物理失真陷阱：
                此处的 while 循环通过捕获 resultX 的 BoundReached 状态来实现“撞墙反弹”。
                虽然宏观视觉上可行，但在微观的帧同步（Frame Timing）机制上存在两处致命的物理精度丢失：
                1. 帧时间吞噬（视觉上的“黏墙效应”）：假设单帧为 16.6ms。若引擎在第 4ms 算出坐标越界，
                updateBounds 会立即强制截断坐标并结束当前动画。这导致单帧剩余的 12.6ms 被白白抛弃，方块在这 12.6ms 内死死贴在边界上，
                直到下一帧 while 循环才唤醒反向动画。在高速或低帧率下，肉眼会察觉到撞击瞬间的微小停顿。
                2. 动能继承失真（摩擦力计算断层）：resultX.endState.velocity 记录的是“撞墙那一瞬间”的绝对瞬时速度。
                因为剩余的 12.6ms 帧时间被引擎吞噬，这期间本该继续产生的“摩擦力减速损耗”被彻底跳过。
                当我们使用 -resultX.endState.velocity 直接作为新动画的初始动能时，
                相当于给系统放了一个“摩擦力假期”。这会导致每次反弹的初速度，都略快于真实的物理衰减模型，能量无法完美守恒。
                终极解法：对于极高精度的弹性模拟，应废弃 updateBounds 截断，改用“无界直线连续运动 + UI 渲染层数学模运算（空间折叠）”的架构。*/
                launch {
                    var resultX = animX.animateDecay(
                        initialVelocity = 1800.dp,
                        animationSpec = decay
                    )
                    while (resultX.endReason == AnimationEndReason.BoundReached) {
                        resultX = animX.animateDecay(
                            initialVelocity = -resultX.endState.velocity,
                            animationSpec = decay
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, anim1.value.roundToPx()) }
                        .size(100.dp)
                        .background(Color.Green.copy(alpha = 0.5f))
                        .clickable {
                            scope.launch { anim1.stop() }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "stop测试",
                    )
                }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(animX.value.roundToPx(), animY.value.roundToPx()) }
                        .size(100.dp)
                        .background(Color.Red.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Bounds测试",
                    )
                }
            }
        }
    }
}

@PhonePreviews
@Composable
fun C29APreview() {
    CourseComposeAnimateAsStateTheme {
        C29A()
    }
}
