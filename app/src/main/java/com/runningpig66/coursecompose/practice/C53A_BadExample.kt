package com.runningpig66.coursecompose.practice

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-06-18
 * @time 6:27
 *
 * 53. 副作用（附带效应）和 SideEffect() - BadExample
 *
 * 示例目标：演示在 Compose 函数体中直接执行副作用（修改外部状态）的危险性。
 * 预期逻辑（开发者天真的想法）：我们希望每次 UI 刷新时，外部变量 externalCount 都能与内部状态 clickCount 保持同步递增。
 *
 * 实际错误表现与原因：
 * 1. 状态脱节：由于 Compose 的“就近重组”优化机制，点击按钮时，只有局部参与了重组，
 * 导致 clickCount 增加了，但外部的 externalCount 根本没有被执行，数据彻底脱节。
 * 2. 生命周期错乱：externalCount 作为静态/全局变量，其生命周期依附于进程，而不是当前 UI 树。
 * 退出页面再重新进入时，内部状态 clickCount 归零，而 externalCount 却保留了历史脏数据继续累加。
 * 3. 潜在的执行风暴：如果外部环境高频触发重组，externalCount 会在后台发生不可控的疯狂递增。
 * 核心守则：绝对不要在 @Composable 函数的纯渲染逻辑中，裸露地编写修改外界状态、网络请求、写库等业务逻辑。UI 函数只能用来描述 UI。
 *
 * notes: C53_副作用和 SideEffect.md
 */
// 1. 外部普通的全局变量（非 Compose 状态）
var externalCount = 0

@Composable
fun C53A_BadExample() {
    // 2. Compose 内部规范的受控状态，改变它会触发当前函数的重组
    var clickCount by remember { mutableIntStateOf(0) }

    // 危险：在 Composable 函数体中直接执行副作用，直接修改了外部变量，并执行了 I/O 操作 (打印 Log)
    externalCount++
    Log.d(TAG, "副作用执行，当前 externalCount 的值是: $externalCount")

    Scaffold { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            Text("按钮的实际点击次数（受控状态）：$clickCount")
            Text("副作用影响次数（外部变量）：$externalCount")
            Button(onClick = { clickCount++ }) {
                Text("点击增加次数")
            }
        }
    }
}

// 1. 这是一个纯函数。只要 a 和 b 不变，返回值永远不变。且执行过程不影响外部任何事物。
fun add(a: Int, b: Int): Int {
    return a + b
}

// 定义一个外部变量
var globalCounter = 0

// 2. 这是一个非纯函数（包含副作用）。
// 问题 1：它修改了外部变量 globalCounter。
// 问题 2：它与控制台发生了 I/O 交互（println）。
fun addAndIncrement(a: Int, b: Int): Int {
    globalCounter++
    println("Current Counter: $globalCounter")
    return a + b
}

@PhonePreviews
@Composable
fun C53Preview() {
    CourseComposeTheme {
        C53A_BadExample()
    }
}
