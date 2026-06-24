package com.runningpig66.coursecompose.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author runningpig66
 * @date 2026-06-24
 * @time 6:21
 *
 * 实战：演示标准的页面网络请求初始化与 Loading/Success/Error 状态机流转。
 * 结合“触发器模式 (Trigger State)”，巧妙破解了静态 Key 无法被手动重试唤醒的限制。
 */
// 模拟页面状态
sealed interface LedgerUiState {
    object Loading : LedgerUiState
    data class Success(val bills: List<String>) : LedgerUiState
    data class Error(val message: String) : LedgerUiState
}

@Composable
fun C55E_NetworkInitTest() {
    // 初始化状态为 Loading。UI 第一次挂载时，呈现加载状态。
    var uiState by remember { mutableStateOf<LedgerUiState>(LedgerUiState.Loading) }
    // 定义一个重试触发器（本质是个计数器）
    var retryTrigger by remember { mutableIntStateOf(0) }

    // 利用 LaunchedEffect(Unit) 在组件首次挂载时触发网络请求
    // 2. 将 Key 从 Unit 换成 retryTrigger
    LaunchedEffect(retryTrigger) {
        // 每次重新执行时，先将 UI 切回 Loading 骨架屏
        uiState = LedgerUiState.Loading
        uiState = fetchLedgerData()
    }

    Scaffold { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val currentState = uiState) {
                is LedgerUiState.Loading -> {
                    LoadingView()
                }

                is LedgerUiState.Success -> {
                    SuccessView(currentState.bills)
                }

                is LedgerUiState.Error -> {
                    ErrorView(currentState.message) { retryTrigger++ }
                }
            }
        }
    }
}

@Preview
@Composable
fun LoadingView() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Text("正在同步账单数据...")
    }
}

@Preview
@Composable
fun SuccessView(bills: List<String> = emptyList()) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "同步成功，本月账单如下：", color = Color.Green)
        bills.forEach { bill ->
            Text(text = "• $bill", modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

@Preview
@Composable
fun ErrorView(message: String = "", onRetryClick: () -> Unit = {}) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = Color.Red)
        Button(onClick = onRetryClick) { Text("点击重试") }
    }
}

// 模拟后端接口
suspend fun fetchLedgerData(isNetworkSuccess: Boolean = false): LedgerUiState {
    delay(1500.milliseconds) // 模拟网络请求耗时
    return if (isNetworkSuccess) {
        LedgerUiState.Success(listOf("打车: -¥35.0", "外卖: -¥28.5", "工资: +¥8500.0"))
    } else {
        LedgerUiState.Error("网络连接超时，请检查后重试")
    }
}

@PhonePreviews
@Composable
fun C55E_NetworkInitTestPreview() {
    CourseComposeTheme {
        C55E_NetworkInitTest()
    }
}
