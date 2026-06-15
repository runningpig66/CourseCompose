package com.runningpig66.coursecompose.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.CombinedModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-06-15
 * @time 10:03
 *
 * 35. then()、CombinedModifier 和 Modifier.Element 底层结构解析
 *
 * 1. 左倾二叉树模型：Modifier 的链式调用在底层并非使用 List 或数组结构，而是通过 CombinedModifier 不断向左嵌套，构建出一棵左倾二叉树。
 * 2. 节点类型分类：
 * - 伴生对象 (Modifier)：充当占位符或链条的起始根节点，不具备实际 UI 修饰作用。
 * - 分支节点 (CombinedModifier)：用于连接两个 Modifier。内部包含 outer (左子树) 和 inner (右子节点)，本身不承载 UI 属性。
 * - 叶子节点 (Modifier.Element)：真正存储 UI 配置（如 BackgroundElement、SizeElement 等）的节点。
 * 3. 为什么使用二叉树而不是 List？（不可变性与性能考量）
 * Modifier 是严格不可变的对象。如果使用 List，每次通过链式调用追加新修饰符时，都需要进行 O(N) 复杂度的数组深度拷贝。
 * 使用 CombinedModifier 树形结构，当执行 A.then(B) 时，只需创建一个新的 CombinedModifier 节点，
 * 分别指向原有的 A 节点和新增的 B 节点，时间复杂度为 O(1)。这解释了 C34.kt 中局部追加背景色不会影响外部传入的原 Modifier 实例：
 * 因为原节点在内存中的引用和内部结构并未改变，仅仅是基于它衍生出了一个拥有新根节点的数据结构（结构共享）。
 * 4. 树的遍历机制 (foldIn 与 foldOut)：
 * - foldIn：正序遍历（从外到内 / 从左到右），先声明的修饰符先被遍历，常用于事件分发或状态累加。
 * - foldOut：逆序遍历（从内到外 / 从右到左），后声明的修饰符先被遍历。这是后续 Layout (布局测量) 的核心，
 * 因为测量体系需要先确定最内层的尺寸，再逐层向外计算 Padding 等包裹约束。
 */
@Composable
fun C35() {
    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // modifierA 链式调用的底层实际形态即为 modifierB。
            // 每次调用修饰符（如 background、padding），本质上就是隐式调用 then() 方法，生成一个新的 CombinedModifier 分支节点。
            val modifierA = Modifier
                .background(Color.Blue)
                .then(Modifier.padding(8.dp))
                .then(Modifier.size(80.dp))

            // modifierB 直观地展示了 modifierA 在底层的左倾二叉树结构：
            // 最外层是 CombinedModifier，它的 left 侧是包含了 Background 和 Padding 的子树，right 侧是 Size 叶子节点。
            val modifierB = CombinedModifier(
                CombinedModifier(Modifier.background(Color.Blue), Modifier.padding(8.dp)),
                Modifier.size(80.dp)
            )

            // 遍历顺序说明：
            // 对于 modifier1.then(modifier2).then(modifier3).then(modifier4) 组成的树结构：
            // foldIn  (正序): 遍历顺序为 1 -> 2 -> 3 -> 4
            // foldOut (逆序): 遍历顺序为 4 -> 3 -> 2 -> 1
        }
    }
}

@PhonePreviews
@Composable
fun C35Preview() {
    CourseComposeTheme {
        C35()
    }
}
