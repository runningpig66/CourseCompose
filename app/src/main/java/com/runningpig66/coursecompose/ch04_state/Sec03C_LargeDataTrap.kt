package com.runningpig66.coursecompose.ch04_state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author runningpig66
 * @date 2026/07/17
 * @time 0:56
 *
 * 物理硬限制陷阱：演示 1MB Binder 事务限制引发的 TransactionTooLargeException 崩溃，及“分离凭证与负载”的标准规避方案。
 *
 * notes: Sec03_rememberSaveable().md
 */
@Composable
fun Sec03C_LargeDataTrap() {
    var isTrapMode by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(
                    onClick = { isTrapMode = true },
                    enabled = !isTrapMode
                ) {
                    Text(text = "切换至 陷阱模式 模式")
                }
                Button(
                    onClick = { isTrapMode = false },
                    enabled = isTrapMode
                ) {
                    Text(text = "切换至 安全模式 模式")
                }
            }

            HorizontalDivider()

            if (isTrapMode) {
                BadLargeDataScreen()
            } else {
                GoodLargeDataScreen()
            }
        }
    }
}

// 反面教材：直接将海量数据交给 rememberSaveable
@Composable
private fun BadLargeDataScreen() {
    // 危险：此处的 String 可能达到几 MB 的体积
    var massiveText by rememberSaveable { mutableStateOf("NULL") }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "【陷阱模式】\n点击下方按钮将生成约 2MB 大小的字符串并存入 rememberSaveable。\n随后按下 Home 键将应用退至后台，应用将直接崩溃 (TransactionTooLargeException)。",
            color = MaterialTheme.colorScheme.error
        )
        Button(
            onClick = {
                // 模拟生成 2MB 的巨型字符串 (100万个中文字符，约占 2MB 内存)
                massiveText = "测".repeat(1000000)
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("生成海量数据 (危险操作)")
        }
        // 仅截取前100个字符展示，防止 UI 渲染卡顿
        Text(text = "当前数据预览: ${massiveText.take(100)}...")
    }
}

// 正确解法：仅保存恢复凭证（如 ID、请求参数）
@Composable
private fun GoodLargeDataScreen() {
    // 安全：只将查询参数或 ID 交给 rememberSaveable，体积通常只有几十字节
    var currentQueryId by rememberSaveable { mutableStateOf("") }
    // 真正的海量数据交给普通的 remember（或 ViewModel），它在内存中，不参与跨进程打包
    var memoryData by remember { mutableStateOf("NULL") }

    // 监听凭证变化，模拟根据凭证去本地数据库/网络拉取海量数据
    LaunchedEffect(currentQueryId) {
        if (currentQueryId.isNotEmpty()) {
            memoryData = "正在根据凭证 [$currentQueryId] 拉取数据..."
            delay(1000.milliseconds) // 模拟 IO 耗时
            memoryData = "安".repeat(1000000) // 模拟耗时查询返回的长文本
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "【安全模式】\n无论加载多大的数据，退到后台都不会崩溃。因为跨进程保存的只有极小的 currentQueryId，海量数据只存在于当前进程堆内存中。",
            color = MaterialTheme.colorScheme.primary
        )
        Button(
            onClick = {
                currentQueryId = "QUERY_${System.currentTimeMillis()}"
            }
        ) {
            Text("生成海量数据 (安全操作)")
        }
        Text(text = "当前凭证: $currentQueryId")
        Text(text = "当前数据预览: ${memoryData.take(100)}...")
    }
}

@PhonePreviews
@Composable
fun Sec03C_LargeDataTrapPreview() {
    CourseComposeTheme {
        Sec03C_LargeDataTrap()
    }
}
