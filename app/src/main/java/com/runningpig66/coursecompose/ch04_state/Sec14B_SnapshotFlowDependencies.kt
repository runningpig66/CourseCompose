package com.runningpig66.coursecompose.ch04_state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log

/**
 * @author runningpig66
 * @date 2026/08/09 周日
 * @time 5:53
 *
 * 4.14 snapshotFlow() - Snapshot State 依赖追踪实验
 *
 * 本例用于验证 snapshotFlow 最核心的运行机制：它观察的不是“代码中出现了哪些 State”，
 * 而是 block 每次实际执行过程中真正读取到了哪些 Snapshot State。
 *
 * 重点验证：
 * 1. State read 即使发生在多层普通函数调用内部，仍然可以被 snapshotFlow 追踪。
 * 2. 使用 by 属性委托不会改变本质，最终发生的仍然是 Snapshot State read。
 * 3. 一个 block 可以同时依赖多个 Snapshot State。
 * 4. 依赖集合是动态的：条件分支改变后，本轮实际读取的 State 也会改变，
 *    snapshotFlow 会重新建立新的依赖集合。
 * 5. 没有在当前执行路径中读取到的 State，即使对象已经作为参数传入，
 *    其变化也不会触发当前 snapshotFlow 重新计算。
 * 6. snapshotFlow block 应当只负责读取 State 和计算结果；
 *    不应在其中修改 Snapshot State 或执行真正的业务副作用。
 *
 * 本例的核心心智模型：
 * 判断 snapshotFlow 的依赖时，不看 State 写在哪一层，
 * 而看 State read 是否发生在本次 snapshotFlow block 的实际执行调用链中。
 *
 * notes: 4.14 snapshotFlow().md
 */
private const val TAG14B = "Sec14B" // log("$TAG14B ")

@Composable
fun Sec14BSnapshotFlowDependencies() {
    val usePrimary = remember { mutableStateOf(true) }
    val primary = remember { mutableIntStateOf(10) }
    val secondary = remember { mutableIntStateOf(100) }

    LaunchedEffect(Unit) {
        snapshotFlow {
            log("$TAG14B snapshotFlow block 开始")
            readSelectedValue(usePrimary, primary, secondary)
            // Error: Cannot modify a state object in a read-only snapshot
            //-primary.intValue++
        }.collect { value ->
            log("$TAG14B collector 收到：$value")
        }
    }

    Scaffold { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "当前数据源：${if (usePrimary.value) "Primary" else "Secondary"}", fontSize = 24.sp)
            Text(text = "Primary = ${primary.intValue}", fontSize = 24.sp)
            Text(text = "Secondary = ${secondary.intValue}", fontSize = 24.sp)

            Button(onClick = { primary.intValue++ }) { Text("Primary +1") }
            Button(onClick = { secondary.intValue++ }) { Text("Secondary +1") }
            Button(onClick = { usePrimary.value = !usePrimary.value }) { Text("切换数据源") }
        }
    }
}

private fun readSelectedValue(
    usePrimary: State<Boolean>,
    primary: State<Int>,
    secondary: State<Int>
): Int {
    val shouldUsePrimary = usePrimary.value

    log("$TAG14B readSelectedValue(): usePrimary = $shouldUsePrimary")

    return if (shouldUsePrimary) {
        readPrimary(primary)
    } else {
        readSecondary(secondary)
    }
}

private fun readPrimary(state: State<Int>): Int {
    val value = state.value
    log("$TAG14B readPrimary(): $value")
    return value
}

private fun readSecondary(state: State<Int>): Int {
    val value = state.value
    log("$TAG14B readSecondary(): $value")
    return value
}

@PhonePreviews
@Composable
fun Sec14BPreview() {
    CourseComposeTheme {
        Sec14BSnapshotFlowDependencies()
    }
}
