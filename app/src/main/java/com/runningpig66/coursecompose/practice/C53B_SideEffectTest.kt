package com.runningpig66.coursecompose.practice

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-06-19
 * @time 4:34
 *
 * 53. 副作用（附带效应）和 SideEffect()
 *
 * 示例目标：演示 SideEffect 的滞后执行特性与视觉错位现象。核心现象复盘：
 * 1. 初次渲染：屏幕 Text 显示 "外部变量: 0"，随后控制台打印 "SideEffect 执行... 1"。
 * 2. 点击按钮：屏幕 Text 变为 "外部变量: 1"，随后控制台打印 "SideEffect 执行... 2"。
 * 为什么会出现“屏幕落后于日志”的视觉错位？
 * Compose 的执行分为“打草稿（生成虚拟 UI 树）”和“交卷（渲染到屏幕）”两步。
 * 当引擎执行到 Text("外部变量...$externalCount2") 时，它读取的是那一瞬间内存里的旧值。
 * SideEffect 的铁律是：必须等到当前的 UI 草稿 100% 成功画到屏幕上之后，才会在主线程执行。
 * 因此，externalCount2++ 永远发生在当前帧已经上屏之后。它修改的值，只能在下一次重组时才能反映到屏幕上。
 * 为什么 SideEffect 必须写在 Scaffold 内部？
 * 因为 Column 是 inline（内联）函数，没有独立作用域。只有将 SideEffect 写在真正发生重组的最小闭包（如 Scaffold 的尾随 Lambda）内部，
 * 点击按钮触发局部重组时，SideEffect 才会被顺带触发。如果写在外层，外层不重组，它就不执行。
 *
 * notes: C53_副作用和 SideEffect.md
 */
var externalCount2 = 0

@Composable
fun C53B_SideEffectTest() {
    var clickCount by remember { mutableIntStateOf(0) }

    /*SideEffect {
        externalCount2++
        Log.d(TAG, "SideEffect 执行，当前 externalCountB 的值是：$externalCount2")
    }*/

    Scaffold { innerPadding ->
        // 使用官方 API 隔离副作用。这段代码被推迟到了UI 成功渲染到屏幕后才会被执行
        SideEffect {
            externalCount2++
            Log.d(TAG, "SideEffect 执行，当前 externalCountB 的值是：$externalCount2")
        }

        Column(Modifier.padding(innerPadding)) {
            Text("按钮实际点击次数：$clickCount")
            Text("外部变量 (安全同步后)：$externalCount2")
            Button(onClick = { clickCount++ }) {
                Text("点击增加次数")
            }
        }
    }
}

@PhonePreviews
@Composable
fun C53B_SideEffectTestPreview() {
    CourseComposeTheme {
        C53B_SideEffectTest()
    }
}
