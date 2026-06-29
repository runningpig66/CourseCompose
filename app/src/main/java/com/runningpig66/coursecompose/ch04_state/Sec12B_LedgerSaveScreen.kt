package com.runningpig66.coursecompose.ch04_state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author runningpig66
 * @date 2026-06-30
 * @time 4:06
 *
 * notes: Sec12_rememberCoroutineScope.md
 */
private const val C412B = "Sec12B_LedgerSaveScreen"

data class LedgerRecord(
    val id: String,
    val category: String,
    val amount: Double,
    val timestamp: Long
)

// 模拟的后端/数据库持久化操作（挂起函数）
suspend fun saveRecordToDatabase(category: String, amount: Double): LedgerRecord {
    // 模拟耗时
    delay(2000.milliseconds)

    // 模拟生成唯一 ID 和时间戳并返回持久化后的数据
    return LedgerRecord(
        id = "TXN_${(10000..99999).random()}",
        category = category,
        amount = amount,
        timestamp = System.currentTimeMillis()
    )
}

@Composable
fun Sec12B_LedgerSaveScreen() {
    // 获取与当前屏幕绑定的协程作用域
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var inputAmount by remember { mutableStateOf("") }
    // Is there mutableStateListOf necessary
    var recordsList by remember {
        mutableStateOf(
            // Simple Data
            listOf(
                LedgerRecord(
                    id = "TXN_${(10000..99999).random()}",
                    category = "Fruit",
                    amount = 11.11,
                    timestamp = System.currentTimeMillis()
                ),
                LedgerRecord(
                    id = "TXN_${(10000..99999).random()}",
                    category = "Fruit",
                    amount = 22.22,
                    timestamp = System.currentTimeMillis()
                )
            )
        )
    }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = inputAmount,
                onValueChange = { inputAmount = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("请输入金额") },
                singleLine = true,
                enabled = !isLoading
            )
            Button(
                onClick = {
                    val amount = inputAmount.toDoubleOrNull() ?: 0.0
                    if (amount <= 0) return@Button
                    // 在 Click 回调中启动协程处理耗时任务
                    scope.launch {
                        try {
                            isLoading = true
                            // 执行挂起函数，当前协程挂起，但不阻塞主线程
                            val newRecord = saveRecordToDatabase(category = "餐饮", amount = amount)
                            // 拿到结果后，更新 UI 数据源
                            log("$C412B: 保存成功！记录id：${newRecord.id}")
                            recordsList = listOf(newRecord) + recordsList
                            inputAmount = "" // Empty TextField

                            // 为了不让挂起函数 showSnackbar 阻塞后续 finally 块中重置状态的代码，为 Snackbar 单独启动一个子协程
                            launch {
                                snackbarHostState.showSnackbar("保存成功！记录id：${newRecord.id}")
                            }
                        } catch (e: Exception) {
                            log("$C412B: Outer Exception: ${e.message}")
                            // 放行协程的正常取消信号
                            if (e is CancellationException) {
                                throw e
                            }
                            // 只有真正的业务异常（如断网、数据库异常），才由我们自己处理和弹窗
                            launch {
                                log("$C412B: Inner Exception: ${e.message}")
                                // 真实项目中这里处理网络异常等
                                snackbarHostState.showSnackbar("保存失败：${e.message}")
                            }
                        } finally {
                            // 无论成功或失败，解除 Loading 状态
                            isLoading = false
                        }
                    }
                }, modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && inputAmount.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(16.dp))
                    Text("正在同步...")
                } else {
                    Text("保存账单")
                }
            }

            Text(text = "今日账单", style = MaterialTheme.typography.titleMedium)

            LazyColumn(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recordsList) { record ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("[${record.category}] ${record.id}")
                            Text("${record.amount}")
                        }
                    }
                }
            }
        }
    }
}

@PhonePreviews
@Composable
fun Sec12B_LedgerSaveScreenPreview() {
    CourseComposeTheme {
        Sec12B_LedgerSaveScreen()
    }
}
