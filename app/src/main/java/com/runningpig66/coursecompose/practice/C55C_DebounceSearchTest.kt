package com.runningpig66.coursecompose.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author runningpig66
 * @date 2026-06-24
 * @time 3:53
 *
 * 演示 LaunchedEffect 的动态 Key 打断与重建机制。
 * 利用“Key 发生变化立刻取消旧协程”的底层特性，优雅实现输入防抖 (Debounce Search)。
 */
private const val TAG55 = "DebounceSearch"

@Composable
fun C55C_DebounceSearchTest() {
    var searchQuery by remember { mutableStateOf("") }
    var searchResult by remember { mutableStateOf("等待输入...") }

    Scaffold { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("搜索关键字") }
            )
            Text("状态：$searchResult")
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResult = "等待输入..."
            return@LaunchedEffect
        }

        searchResult = "正在输入... (协程挂起等待中)"
        // 挂起函数：设定 500 毫秒的防抖窗口
        // 如果在 500ms 内 searchQuery 再次改变，本协程将在此处被 CancellationException 强行中断
        delay(500.milliseconds)
        // 只有当协程成功熬过 500ms 的挂起期未被取消，才会执行后续的网络请求逻辑
        searchResult = "发起网络请求搜索: $searchQuery"

        // 模拟网络耗时
        delay(1000.milliseconds)
        searchResult = "搜索完成：$searchQuery 的假数据结果"
    }
}

// 提前演示 snapshotFlow: 把 Compose 的状态（State）转换为 Kotlin 的数据流（Flow）
@OptIn(FlowPreview::class)
@Composable
fun FlowDebounceSearchScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var searchResult by remember { mutableStateOf("等待输入...") }
    // UI 部分保持不变
    OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it })
    Text("状态：$searchResult")

    LaunchedEffect(Unit) {
        snapshotFlow { searchQuery }
            .debounce(500.milliseconds)
            .filter { it.isNotBlank() }
            .collectLatest { query ->
                searchResult = "发起网络请求搜索: $query"
                delay(1000.milliseconds)
                searchResult = "搜索完成：$query 的假数据结果"
            }
    }
}

@PhonePreviews
@Composable
fun C55C_DebounceSearchTestPreview() {
    CourseComposeTheme {
        C55C_DebounceSearchTest()
    }
}
