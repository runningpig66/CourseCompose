package com.runningpig66.coursecompose.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-06-15
 * @time 9:01
 *
 * 34. modifier: Modifier = Modifier 的含义与前置基础
 *
 * 1. 默认占位符：函数签名中的 Modifier = Modifier，等号右侧代表 Modifier 接口的伴生对象 (Companion Object)。
 * 它是一个没有任何修饰效果的空节点，通常作为修饰链条的通用起点。
 * 2. 不可变性 (Immutability)：Modifier 对象是绝对不可变的。对其调用任何修饰函数（如 .background、.size）时，都不会修改原对象，
 * 而是返回一个包含了新旧规则的全新 Modifier 实例。这一设计彻底阻断了不同子组件间共享 Modifier 时产生的状态污染。
 * 3. 发射 (Emit) 与拉平机制：自定义的 @Composable 函数并非物理容器。其内部声明的多个组件，
 * 在底层会被直接发射给调用方外层的实际布局容器（如 Column/Row）。在测量树中，这些组件会被拉平为同级的兄弟节点进行排版。
 */
@Composable
fun C34() {
    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Custom(Modifier.size(80.dp))
        }
    }
}

@Composable
fun Custom(modifier: Modifier = Modifier) {
    // 基于传入的 modifier 生成一个追加了背景色的新实例 blueModifier，原 modifier 本身不发生改变
    val blueModifier = modifier.background(Color.Blue)
    // 发射节点 1：应用了外部尺寸限制与内部配置的蓝色背景
    Box(blueModifier) {
        Text("111", fontSize = 24.sp)
    }
    // 发射节点 2：复用原有的 modifier，仅包含外部尺寸限制，无背景色。
    // 由于底层的拉平机制，这两个 Box 将跳过 Custom 函数层级，直接作为外部 Column 的子节点参与垂直排版。
    Box(modifier) {
        Text("222", fontSize = 24.sp)
    }
}

@PhonePreviews
@Composable
fun C34Preview() {
    CourseComposeTheme {
        C34()
    }
}
