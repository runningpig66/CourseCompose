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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.logD

/**
 * @author runningpig66
 * @date 2026/08/09 周日
 * @time 2:58
 *
 * 4.14 snapshotFlow() - 基础行为实验
 *
 * 本例用于第一次建立 snapshotFlow 的实际运行直觉，
 * 观察 Compose Snapshot State 转换为 cold Flow 后的基本行为。
 *
 * 重点验证：
 * 1. snapshotFlow() 只负责创建 cold Flow，没有 collect 时 block 不会执行。
 * 2. 开始 collect 后，block 会立即执行一次，并发射当前计算结果。
 * 3. collection 存活期间，block 中读取的 Snapshot State 发生变化时会重新计算。
 * 4. State 变化导致 block 重新执行，不代表一定 emit；
 *    如果新的 block 计算结果与上一次 emission 相等，则不会重复发射。
 * 5. collection 被取消后，snapshotFlow 不会继续在后台记录状态变化；
 *    再次 collect 时会重新读取此刻的最新状态，而不会补发期间的历史变化。
 *
 * 本例同时用于区分三个不同概念：
 * Compose 重组、snapshotFlow block 重新执行、Flow emission 并不是同一件事。
 *
 * notes: 4.14 snapshotFlow().md
 */
private const val TAG14A = "Sec14A"

@Composable
fun Sec14ASnapshotFlowBasics() {
    var count by remember { mutableIntStateOf(0) }
    var collecting by remember { mutableStateOf(false) }
    val countFlow = remember {
        snapshotFlow {
            logD("$TAG14A snapshotFlow block 执行：count = $count")
            count
            // count / 2 // State 改变会使 block 重新计算；但计算结果相同则不重复 emit
        }
    }
    LaunchedEffect(collecting) {
        if (!collecting) {
            logD("$TAG14A 当前没有 collect")
            return@LaunchedEffect
        }
        logD("$TAG14A 开始 collect")
        try {
            countFlow.collect { value ->
                logD("$TAG14A collector 收到：$value")
            }
        } finally {
            logD("$TAG14A collect 被取消")
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
            Text(text = "当前 count = $count", fontSize = 24.sp)
            Button(onClick = { count++ }) { Text("+1") }
            Button(onClick = { count = count }) { Text("写入相同值") }
            Button(onClick = { collecting = !collecting }) {
                Text(text = if (collecting) "停止 collect" else "开始 collect")
            }
        }
    }
}

@PhonePreviews
@Composable
fun Sec14APreview() {
    CourseComposeTheme {
        Sec14ASnapshotFlowBasics()
    }
}
