package com.runningpig66.coursecompose.practice

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import kotlin.math.max

/**
 * @author runningpig66
 * @date 2026-06-17
 * @time 3:20
 * 
 * 37. LayoutModifier 和 Modifier.layout() 机制剖析
 *
 * Compose 渲染流程的基础认知：
 * 1. 组合（Composition）：执行 @Composable 函数，在内存中生成一棵包含节点信息的树（LayoutNode）。
 * 2. 布局（Layout）：遍历 LayoutNode 树，完成两个核心任务：测量尺寸（Measure）和放置位置（Place）。
 * 3. 绘制（Drawing）：根据上一步算好的物理尺寸和坐标，调用 Canvas 将像素画到屏幕上。
 *
 * Modifier.layout 的作用域：
 * 它专门用于插手第二阶段（布局阶段）。它采用的是“装饰器”模式，只能修改当前组件外部的包裹边界（如增加间距、偏移尺寸），
 * 绝对无法干涉组件内部的排版规则。若要干涉内部多个子节点的排列规则，需使用 Layout() 函数。
 *
 * 布局阶段的三大核心对象：
 * - Constraints（约束条件）：父节点向下传递的尺寸范围区间（minWidth~maxWidth, minHeight~maxHeight）。它是一个允许的区间，而非死板的固定值。
 * - Measurable（待测量的组件）：尚未进行测量的子节点代理对象。它正在等待接收约束条件以便计算自身大小。
 * - Placeable（已测量的组件）：Measurable 经过 measure() 方法测量后返回的成品对象，内部已包含了子节点计算出的确切宽高。
 *
 * 单次测量原则（Single Pass）：Compose 从物理架构上杜绝了传统 View 树中可能出现的多次测量导致的性能灾难。
 * 父容器（如 Row）在测量子组件时，会分发约束 -> 拿到第一个子组件的实际尺寸 -> 立即从总可用空间中扣减该尺寸 ->
 * 将剩余的可用空间（新约束）传递给下一个子组件。每一个组件有且仅被 measure() 一次。
 *
 * Modifier.layout 的能力边界（黑盒屏障）：该修饰符的局限性严格体现在步骤2的测量阶段。在其提供的作用域中，
 * 系统仅向外暴露一个单一的 measurable 参数。这意味着，无论被修饰的是一个基础的 Text，还是一个嵌套了百余个子控件的复杂 Column，
 * 在 Modifier.layout 视觉下均被视为一个不可穿透的单一整体（黑盒）。调用 measure() 只能触发该整体内部的自我测量，
 * 并返回一个代表其总体占用空间的 placeable 对象。开发者只能基于测量结果向父容器上报一个外壳尺寸 (layout(w, h))，
 * 并在该外壳形成的局部坐标系内，决定内部实际组件 (placeable) 的相对放置偏移 (placeRelative(x, y))。
 * 该修饰符无权决定其外壳在父容器中的最终物理位置，也绝对无法获取、干预或重新排列其内部包含的任何具体子控件。
 * 若需打破黑盒，对内部多个子节点进行精细化排版，必须放弃修饰符，改用底层的 Layout() 组合函数。
 *
 * 实际应用场景：基于其“仅修饰外部轮廓、无法干涉内部”的物理特性，Modifier.layout 通常用于轻量级的单体包装需求。一般用于：
 * 1. 制造非标准的内边距或外边距（如根据运行时的约束动态计算 padding）。
 * 2. 拦截并篡改父组件传递的约束条件以强制改变组件外观（如强制一个按比例缩放的组件呈现为绝对正方形）。
 * 3. 实现组件在自身所分配区域内的特殊几何偏移或对齐效果。
 * 它提供了一种无需重写整个父级容器排版规则，即可从外部微调单一组件占位特征的轻量级方案。
 */
@Composable
fun C37() {
    Scaffold { innerPadding ->
        // Row 充当父容器，它负责向内部的子组件分发约束条件，并根据子组件的上报尺寸安排它们的位置。
        Row(
            Modifier
                //.clipToBounds()
                .padding(innerPadding)
                .background(Color.Yellow.copy(alpha = 0.5f))
        ) {
            // 一个 paddingSize = 10.dp 的正方形 Text
            Text(
                "running",
                Modifier
                    // 裁切修饰符必须放在 layout 之前。修饰符从左到右执行，clipToBounds 会以紧随其后的 layout 所上报的外壳尺寸作为裁切边界。
                    // 如果不加此修饰符，由于 Compose 默认不裁剪超出边界的内容，内部偏移的 Text 将会穿模覆盖到右侧的红块上。
                    // 不能将 clipToBounds 加在父容器 Row 上，因为 Row 只能裁切超出 Row 边界的内容，无法阻止 Row 内部的兄弟组件互相覆盖。
                    .clipToBounds()
                    .layout { measurable, constraints ->
                        // 步骤 1：拦截并修改约束 (Intercept Constraints)。外部传入的 constraints 是 Row 给出的可用空间区间。
                        // 我们通过减去两倍的 padding 像素，人为缩小了这个可用区间，并将缩小后的假约束传递给 Text。
                        val paddingPx = 10.dp.roundToPx()
                        val modifiedConstraints = constraints.copy(
                            maxWidth = constraints.maxWidth - paddingPx * 2,
                            maxHeight = constraints.maxHeight - paddingPx * 2
                        )
                        // 工程实践提示：此处直接相减存在风险，如果 constraints.minWidth 减去 padding 后变成负数，会直接抛出
                        // IllegalArgumentException 导致崩溃。实际开发中通常使用 constraints.offset() 方法，它底层会自动处理负数截断。

                        // 步骤 2：测量内部组件 (Measure)。将缩小后的 modifiedConstraints 传递给 Text 进行实际测量。
                        // Text 根据传入的范围和自身的文本内容（"running"）计算出真实需要的尺寸。因为 "running" 的实际宽度远小于修改后的约束上限，
                        // 所以文本可以完整渲染，不会被裁切或换行。测量完成后返回的 placeable 包含了 Text 真实的物理宽高。
                        // measurable.measure() => 等价于传统 View 中触发 child.measure()。
                        val placeable = measurable.measure(modifiedConstraints)

                        // 第三步：计算加上 Padding 后的总大小，并向外层汇报（Layout）
                        // 2. 等 Text 算好尺寸后，这个壳再把尺寸偷偷加一点（比如加上 padding），然后上报给最外层。

                        // 步骤 3：上报自身整体尺寸 (Report Size)。计算当前 Modifier 作为一个整体（包含 padding）所需要占用的总尺寸。
                        // 此处使用了 max() 是为了强行实现正方形的特殊需求，常规的 Padding 逻辑应直接使用加法：
                        // val width = placeable.width + paddingPx * 2; val height = placeable.height + paddingPx * 2
                        val size = max(placeable.width + paddingPx * 2, placeable.height + paddingPx * 2)

                        // 调用 MeasureScope 提供的 layout() 函数，向外层容器（Row）汇报：
                        // “我这个组件最终占用的尺寸是 size * size”，同时开启 PlacementScope 作用域，允许你把内部组件放到指定位置。
                        // layout(width, height) { ... } => 等价于传统 View 中的 setMeasuredDimension(width, height)（定尺寸）。
                        layout(width = size, height = size) {
                            // 步骤 4：放置内部组件 (Place)。机制说明：在 layout {} 大括号内部，存在一个独立的局部坐标系。
                            // 它的 (0, 0) 原点永远是当前组件刚刚上报的那个“外壳（size * size 区域）”的左上角。
                            // placeRelative(x, y) 并不是移动外壳在 Row 中的位置，而是在外壳内部，移动那个纯粹的 Text 实体（内芯）。
                            // 将 Text 在局部坐标系中向右、向下偏移 paddingPx，即可在视觉上形成外壳边缘留白的 Padding 效果。
                            // 如果填入 (100, 0)，Text 会被画在外壳区域的外面，如果此时没有 clipToBounds，就会产生越界穿模。
                            // placeable.placeRelative(x, y) => 等价于传统 View 中的 child.layout(x, y, ...)（定位置）。
                            // 这里的 placeable 是步骤 2 measurable.measure() 返回的那个纯粹的 Text 实体（只有 150 像素宽的那个“running”）。
                            placeable.placeRelative(paddingPx, paddingPx) // RTL
                            // placeable.placeRelative(100, 0)
                        }
                    }
            )

            // 测试用兄弟组件。由于 Compose 的单次测量和约束扣减机制：Row 在处理完上面的 Text 之后，会将原始总宽度减去 Text 上报的 size，
            // 然后拿着剩余的空间约束去询问当前的红块。并且 Row 会精准地将红块的 X 坐标放置在黄块结束的位置。
            Box(
                Modifier
                    .size(100.dp)
                    .background(Color.Red.copy(alpha = 0.25f))
            )
        }
    }
}

/**
 * 传统 View 测量与布局对比参考：传统 Android View (如 ViewGroup) 的布局机制是“内部接管”。
 * onMeasure 需要遍历所有的子 View 并调用它们的 measure()，容易导致多次重复测量的性能问题。
 * onLayout 同样需要遍历子 View 并给它们分配坐标。
 *
 * Compose 的 Modifier.layout 是“外部拦截”（装饰器机制）。它无法深入组件内部修改排版规则，
 * 只能在单一数据流（获取约束 -> 测量唯一内部元素 -> 上报包裹尺寸 -> 放置唯一内部元素）中完成对单体组件尺寸和位置的修饰。
 */
class SquareImage(context: Context, attrs: AttributeSet?) : ImageView(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = max(measuredWidth, measuredHeight)
        setMeasuredDimension(size, size)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
    }
}

@PhonePreviews
@Composable
fun C37Preview() {
    CourseComposeTheme {
        C37()
    }
}
