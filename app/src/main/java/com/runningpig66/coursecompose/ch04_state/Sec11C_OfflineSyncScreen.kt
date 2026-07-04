package com.runningpig66.coursecompose.ch04_state

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.RetainedEffect
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author runningpig66
 * @date 2026-07-03
 * @time 3:45
 *
 * 实战案例：多任务离线同步引擎。验证在 Activity 销毁与重建的夹缝中，后台协程计算的绝对连续性，以及底层内存指针重定向的无缝衔接现象。
 *
 * notes: Sec11_RetainedEffect.md
 */
private const val C411C = "Sec11C_OfflineSyncScreen"

@Composable
fun Sec11C_OfflineSyncScreen() {
    // 定义同步队列的常量
    val totalFiles = 5
    // 记录当前正在同步第几个文件 (1 到 5)
    var currentFileIndex by rememberSaveable { mutableIntStateOf(0) }
    // 记录当前单个文件的同步百分比 (0f 到 1f)
    val progressState = rememberSaveable { mutableFloatStateOf(0f) }
    var currentFileProgress by progressState
    // 记录整体引擎是否正在运行
    var isSyncing by rememberSaveable { mutableStateOf(false) }
    // 记录是否全部完成
    var isCompleted by rememberSaveable { mutableStateOf(false) }
    // 引入免疫配置变更的作用域
    val retainedScope = rememberRetainedCoroutineScope2()

    Scaffold { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "离线数据同步",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "总任务进度:")
                        Text(text = "$currentFileIndex / $totalFiles")
                    }
                    Text(
                        text =
                            if (isCompleted) {
                                "全部 $totalFiles 个文件同步完成！"
                            } else {
                                if (isSyncing) "正在同步：$currentFileIndex.jpg" else "等待同步..."
                            }
                    )
                    LinearProgressIndicator(
                        progress = { currentFileProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    Text(
                        text = "${(currentFileProgress * 100).toInt()}%",
                        modifier = Modifier.align(Alignment.End),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Log.d(C411C, "[UI 组合] 当前绑定的 State 内存地址: ${System.identityHashCode(progressState)}")

            Button(
                onClick = {
                    if (isSyncing || isCompleted) return@Button
                    // 在跨越配置变更的作用域中启动耗时队列
                    retainedScope.launch {
                        Log.d(C411C, "[协程内部] 正在修改的 State 内存地址: ${System.identityHashCode(progressState)}")
                        try {
                            isSyncing = true
                            isCompleted = false
                            currentFileIndex = 1
                            // 模拟外层队列循环：依次处理 5 个文件
                            while (currentFileIndex <= totalFiles) {
                                Log.d(C411C, "开始同步第 $currentFileIndex 个文件")
                                currentFileProgress = 0f
                                // 模拟内层单文件上传过程：耗时 1 秒，每 10 毫秒更新 1%
                                for (step in 1..100) {
                                    delay(10.milliseconds)
                                    currentFileProgress = step / 100f

                                    // 在第 3 张图片的 47% 节点，模拟上传过程中出现的未知异常
                                    /*if (currentFileIndex == 3 && currentFileProgress > 47f / 100f) {
                                        throw RuntimeException("Unknow Exception: Current index: $currentFileIndex, Current progress: $currentFileProgress")
                                    }*/
                                }
                                Log.d(C411C, "第 $currentFileIndex 个文件同步完成")
                                // 准备处理下一个文件
                                if (currentFileIndex < totalFiles) {
                                    currentFileIndex++
                                } else {
                                    break
                                }
                            }
                            isCompleted = true
                        } catch (e: Exception) {
                            Log.d(C411C, "同步异常：${e.message}")
                            if (e is CancellationException) throw e
                        } finally {
                            isSyncing = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isCompleted || !isSyncing
            ) {
                Text(text = if (isSyncing) "正在同步中..." else "开始同步")
            }
            // 重置按钮
            OutlinedButton(
                onClick = {
                    currentFileIndex = 0
                    currentFileProgress = 0f
                    isCompleted = false
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isCompleted || !isSyncing
            ) {
                Text("重置队列")
            }

            HorizontalDivider()

            Text(
                text = "物理验证步骤：\n1. 点击启动同步引擎\n2. 当同步到第 2 或第 3 个文件时，疯狂点击模拟器的旋转屏幕按钮（反复横竖屏切换）\n3. 观察现象：UI 重建瞬间，进度条无缝衔接，后台的 while 循环绝对不会断裂，直到 5 个任务全数跑完。",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun rememberRetainedCoroutineScope2(): CoroutineScope {
    val scope = retain { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }

    RetainedEffect(scope) {
        onRetire {
            Log.d(C411C, "宿主页面彻底出栈，清理后台队列协程！")
            scope.cancel()
        }
    }
    return scope
}

/* Output:
Sec11C_Off...SyncScreen  D  [UI 组合] 当前绑定的 State 内存地址: 7874771
Sec11C_Off...SyncScreen  D  [协程内部] 正在修改的 State 内存地址: 7874771
Sec11C_Off...SyncScreen  D  开始同步第 1 个文件
Sec11C_Off...SyncScreen  D  [UI 组合] 当前绑定的 State 内存地址: 7874771
Sec11C_Off...SyncScreen  D  第 1 个文件同步完成
Sec11C_Off...SyncScreen  D  开始同步第 2 个文件
LifeTest                 D  onDestroy:
LifeTest                 D  onCreate:
Sec11C_Off...SyncScreen  D  [UI 组合] 当前绑定的 State 内存地址: 7874771
LifeTest                 D  onDestroy:
LifeTest                 D  onCreate:
Sec11C_Off...SyncScreen  D  [UI 组合] 当前绑定的 State 内存地址: 7874771
Sec11C_Off...SyncScreen  D  第 2 个文件同步完成
Sec11C_Off...SyncScreen  D  开始同步第 3 个文件
Sec11C_Off...SyncScreen  D  第 3 个文件同步完成
Sec11C_Off...SyncScreen  D  开始同步第 4 个文件
Sec11C_Off...SyncScreen  D  第 4 个文件同步完成
Sec11C_Off...SyncScreen  D  开始同步第 5 个文件
Sec11C_Off...SyncScreen  D  第 5 个文件同步完成
Sec11C_Off...SyncScreen  D  [UI 组合] 当前绑定的 State 内存地址: 7874771
 */

@PhonePreviews
@Composable
fun Sec11C_OfflineSyncScreenPreview() {
    CourseComposeTheme {
        Sec11C_OfflineSyncScreen()
    }
}
