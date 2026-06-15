package com.runningpig66.coursecompose.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-06-11
 * @time 2:59
 *
 * 31. Transition 延伸：AnimatedVisibility() 基础 API 练习
 *
 * 1. AnimatedVisibility 并非单纯的视觉动画封装，而是一个“带有动画缓冲区的组件挂载/卸载管理器”。
 * 当 visible 为 false 且出场动画播放完毕后，它会将内部组件彻底从 Compose UI 树上移除，释放内存与布局开销。
 * 2. 四维变化：它的入场 (enter) 和出场 (exit) 参数，本质上是对内部 TransitionData 四个维度的配置：
 * Fade (透明度)、Slide (位移)、ChangeSize (裁切/尺寸)、Scale (缩放)。
 */
@Composable
fun C31A() {
    var shown by remember { mutableStateOf(true) }
    Scaffold { innerPadding ->
        // AnimatedVisibility 作为 Column/Row 的直接子元素时，会触发特殊的扩展函数版本。
        // 例如在 Column 中，它的默认动画会自动包含 expandVertically/shrinkVertically (纵向展开/收缩)。
        Column(
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = shown,

                // 维度 1：Fade (淡入淡出) - 改变 Alpha 透明度
                /*enter = fadeIn(animationSpec = tween(2000), initialAlpha = 0.3f),*/

                // 维度 2：Slide (滑动) - 改变位移 (Offset)
                // 内部 Lambda 会传入组件的真实尺寸 (fullSize/fullWidth)，方便基于自身尺寸计算偏移量。
                /*enter = slideIn(
                    animationSpec = tween(2000),
                    initialOffset = { fullSize -> IntOffset(-fullSize.width / 2, -fullSize.height / 2) }
                )*/
                /*enter = slideInHorizontally(animationSpec = tween(durationMillis = 2000)) { fullWidth -> -fullWidth },*/
                /*enter = slideInVertically(animationSpec = tween(durationMillis = 2000)) { fullHeight -> -fullHeight },*/

                // 维度 3：Expand/Shrink (展开/收缩) - 改变裁切边界 (Clip)
                // 极其容易误解的 API：Expand 并非放大，它的底层实现是“动态修改裁切区域 (Clip)”。
                // expandFrom：决定裁切保留的锚点（例如 TopStart 会给人一种从左上角“钻出来”的视觉错觉）。
                // clip = false：关闭物理裁切后，你会发现组件其实一开始就在那里，只是跟着位移在动，印证了它的裁切本质。
                /*enter = expandIn(
                    animationSpec = tween(5000),
                    expandFrom = Alignment.TopStart,
                    clip = false,
                    initialSize = { fullSize -> IntSize(fullSize.width/2, fullSize.height / 2) }
                )*/
                /*enter = expandHorizontally(tween(5000)),*/
                /*enter = expandVertically(tween(5000)),*/

                // 维度 4：Scale (放缩) - 硬件加速级别的光学放缩 (GraphicsLayer) 这里的缩放发生底层的绘制阶段。
                // transformOrigin：极其关键的属性，决定缩放的轴心（锚点）。(1f, 0f) 代表以右上角为轴心进行放缩。
                /*enter = scaleIn(
                    animationSpec = tween(5000),
                    initialScale = 0f,
                    transformOrigin = TransformOrigin(1f, 0f)
                ),*/

                // 使用 + 运算符组合多种动画。底层原理：运算符重载。它会将左右两个对象的配置合并。现代 Compose 版本的冲突解决规则：
                // 右侧优先级高于左侧。例如下方代码，右侧的 initialAlpha = 0.1f 会覆盖左侧的 1f，最终以 0.1f 生效。
                //enter = fadeIn(initialAlpha = 1f) + fadeIn(animationSpec = tween(5000), initialAlpha = 0.1f),

                enter = expandVertically(tween(1000)),
                // 命名规范提示：出场动画绝大多数是将 In 替换为 Out (如 fadeOut, scaleOut)。
                // 唯一的例外是 Expand (展开) 的反义词，为了符合语义，官方将其命名为 Shrink (收缩)。
                exit = shrinkVertically(tween(1000)),
            ) {
                // 注意：AnimatedVisibility 内部默认只支持一个直接的子 Composable 参与测量布局。
                // 如果需要控制多个元素，必须用 Box/Column/Row 将它们包裹起来。
                TransitionSquare()
                // 关于 Transition<T>.AnimatedVisibility() 的实战运用，请参考 C31B.kt 中的枚举单例状态机融合案例。
            }
            OutlinedButton(onClick = { shown = !shown }) {
                Text(text = "Switch")
            }
        }
    }
}

@Composable
fun TransitionSquare() {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Red.copy(alpha = 0.5f))
    )
}

@PhonePreviews
@Composable
fun C31APreview() {
    CourseComposeTheme {
        C31A()
    }
}
