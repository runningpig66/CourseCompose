package com.runningpig66.coursecompose.ch04_state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

/**
 * @author runningpig66
 * @date 2026/08/10 周一
 * @time 0:13
 *
 * 4.14 snapshotFlow() - LazyListState 真实使用场景
 *
 * 本例使用 LazyListState.firstVisibleItemIndex 演示 snapshotFlow 在真实
 * Compose UI 状态中的典型用途：把高频变化的 Snapshot State 接入 Kotlin Flow 操作链，
 * 再逐步转换成真正具有业务意义的信号。
 *
 * 示例需求：当列表从“最近账单区域”进入“较旧账单区域”时触发一次业务动作。
 *
 * 重点验证：
 * 1. snapshotFlow 将 firstVisibleItemIndex 的变化转换成 Flow<Int>。
 * 2. map 将原始 index 转换成“是否进入旧账单区域”的 Boolean 业务状态。
 * 3. distinctUntilChanged() 去除 map 后产生的连续重复 Boolean，使下游只关心真正的区域边界变化。
 * 4. filter 进一步只保留“进入旧区域”这一方向的变化。
 * 5. Flow operator 的顺序会改变最终业务语义，不能机械交换。
 * 6. snapshotFlow 自己已经会对 block 的返回结果去重；
 *    但经过 map 等下游转换后可能再次产生重复值，因此此时 distinctUntilChanged() 仍然可能有实际意义。
 *
 * 本例体现 snapshotFlow 的核心开发价值：
 * 当 Compose UI 层的 Snapshot State 不只是用于绘制 UI，而需要经过
 * Flow 的 map、filter、distinctUntilChanged、take 等连续数据处理后执行协程副作用时，
 * 可以使用 snapshotFlow 作为 State → Flow 的桥梁。
 *
 * notes: 4.14 snapshotFlow().md
 */
private const val TAG14C = "Sec14C" // log("$TAG14C ")
private const val OLD_BILL_THRESHOLD = 10

@Composable
fun Sec14CLazyListSnapshotFlow() {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex
        }
            .onEach { index -> log("$TAG14C ① snapshotFlow emit: index = $index") }
            .map { index -> index >= OLD_BILL_THRESHOLD }
            .onEach { inOldBillArea -> log("$TAG14C ② map 后: inOldBillArea = $inOldBillArea") }
            .distinctUntilChanged()
            .onEach { inOldBillArea -> log("$TAG14C ③ distinctUntilChanged 后: $inOldBillArea") }
            .filter { inOldBillArea -> inOldBillArea }
            .take(1) // 本次 collection 生命周期中只执行一次
            .collect { log("$TAG14C ④ 业务动作：进入较旧账单区域，可以准备加载更旧账单") }
    }

    Scaffold { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "当第一条可见账单的 index >= $OLD_BILL_THRESHOLD 时触发动作", fontSize = 24.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(onClick = {
                    coroutineScope.launch {
                        listState.scrollToItem(0)
                    }
                }) { Text(text = "回到顶部", fontSize = 24.sp) }
                Button(onClick = {
                    coroutineScope.launch {
                        listState.scrollToItem(12)
                    }
                }) { Text(text = "跳到旧账单区", fontSize = 24.sp) }
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = List(40) { it }) {
                    Card(Modifier.fillMaxWidth()) {
                        Text(
                            text = "账单 ${it + 1}    index = $it",
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@PhonePreviews
@Composable
fun Sec14CPreview() {
    CourseComposeTheme {
        Sec14CLazyListSnapshotFlow()
    }
}
