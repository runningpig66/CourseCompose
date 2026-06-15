package com.runningpig66.coursecompose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import java.util.Locale

/**
 * @author runningpig66
 * @date 2026-05-31
 * @time 4:22
 *
 * 演示 Jetpack Compose 中 SpringSpec (弹簧动画规范) 的工程实战与参数解析。
 *
 * SpringSpec 是基于物理阻尼谐振子（Damped Harmonic Oscillator）模型的动画规范。
 * 它通过计算给定时间的弹性恢复力与阻尼力，动态输出连续的离散状态值。
 * 本组件旨在提供对 SpringSpec 核心参数的可视化调节与验证。
 */
@Composable
fun SpringPlaygroundDemo() {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            SpringPlaygroundBoard()
        }
    }
}

@Composable
fun SpringPlaygroundBoard(modifier: Modifier = Modifier) {
    // 物理参数状态管理
    var dampingRatio by remember { mutableFloatStateOf(Spring.DampingRatioHighBouncy) } // 阻尼比 (默认 0.2f)
    var stiffness by remember { mutableFloatStateOf(Spring.StiffnessMedium) } // 刚度 (默认 1500f)
    var initialVelocity by remember { mutableFloatStateOf(2000f) } // 初始速度

    // 触发器：通过改变整型变量触发 LaunchedEffect
    var triggerCount by remember { mutableIntStateOf(0) }

    // 动画引擎：驱动 X 轴偏移
    val offsetX = remember { Animatable(0.dp, Dp.VectorConverter) }

    LaunchedEffect(triggerCount) {
        if (triggerCount > 0) {
            // 每次触发前瞬间重置到原点，支持高频连续点击
            offsetX.snapTo(0.dp)

            // 执行物理弹簧计算
            offsetX.animateTo(
                targetValue = 0.dp, // 目标位置始终是原点，全靠 initialVelocity 产生震荡
                /* fun <T> spring(
                dampingRatio: Float = Spring.DampingRatioNoBouncy,
                stiffness: Float = Spring.StiffnessMedium,
                visibilityThreshold: T? = null,): SpringSpec<T>

                spring() 方法参数详解：@param dampingRatio 阻尼比。控制振荡衰减的速率。
                = 1.0 (NoBouncy): 临界阻尼 (Critical Damping)。系统以最快速度收敛至目标值，无过冲 (Overshoot)。
                < 1.0 (如 0.2f): 欠阻尼 (Under-damped)。系统会围绕目标值产生过冲和振荡。值越趋近于 0，振荡衰减所需时间越长。
                > 1.0: 过阻尼 (Over-damped)。系统无过冲，但收敛速度慢于临界阻尼。

                @param stiffness 刚度。弹簧系数 (Spring Constant)。控制趋向目标值的拉力大小。
                数值越大，形变产生的恢复力越大，系统响应越快，振荡周期越短。

                @param visibilityThreshold 可见性阈值。判定动画结束的容差界限。
                当当前值与目标值的差值，以及当前速度均小于此阈值时，底层动画引擎将提前终止计算并结束挂起函数，用于节省 GPU 渲染开销。*/
                animationSpec = spring(
                    dampingRatio = dampingRatio,
                    stiffness = stiffness,
                    visibilityThreshold = 1.dp
                ),
                /* initialVelocity 初始速度：动画启动时 (t=0) 赋予状态类型的初始一阶导数。
                作用：在 targetValue 与初始值相同（即无需绝对位移）的场景下，通过注入非零的初始速度，强制破坏平衡状态，
                使其产生位移并交由 spring() 的阻尼机制进行收敛，从而实现原地振荡（如输入框报错摇晃）的效果。*/
                initialVelocity = initialVelocity.dp
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.value.roundToPx(), 0) }
                    .size(60.dp)
                    .background(Color(0xFFE91E63), CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { triggerCount++ },
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("触发动画")
        }

        Spacer(modifier = Modifier.height(24.dp))

        PhysicsSliderControl(
            title = "DampingRatio (阻尼比): ${String.format(Locale.US, "%.2f", dampingRatio)}",
            value = dampingRatio,
            valueRange = 0.1f..1.5f, // 0.1(极度弹) 到 1.5(过阻尼，像在泥浆中)
            onValueChange = { dampingRatio = it }
        )

        PhysicsSliderControl(
            title = "Stiffness (刚度): ${stiffness.toInt()}",
            value = stiffness,
            valueRange = 10f..10000f, // 从极度松软到极度坚硬
            onValueChange = { stiffness = it }
        )

        PhysicsSliderControl(
            title = "InitialVelocity (初始初速度): ${initialVelocity.toInt()} dp/s",
            value = initialVelocity,
            valueRange = 0f..5000f, // 注入的爆发力
            onValueChange = { initialVelocity = it }
        )
    }
}

@Composable
fun PhysicsSliderControl(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

@PhonePreviews
@Composable
fun SpringPlaygroundPreview() {
    SpringPlaygroundDemo()
}
