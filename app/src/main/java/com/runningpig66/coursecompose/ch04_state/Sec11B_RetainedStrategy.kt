package com.runningpig66.coursecompose.ch04_state

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.RetainedEffect
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author runningpig66
 * @date 2026-07-02
 * @time 2:01
 *
 * 基础防御策略：引入 rememberSaveable 保护静态数据，并封装基于 retain 的协程作用域。
 * 演示如何让状态和活对象跨越配置变更存活，并在页面真正彻底销毁时安全释放资源。
 *
 * notes: Sec11_RetainedEffect.md
 */
private const val C411B = "Sec11B_RetainedStrategy"

@Composable
fun Sec11B_RetainedStrategy() {
    // 1. 纯数据状态持久化：使用 rememberSaveable 抵抗配置变更
    var progress by rememberSaveable { mutableIntStateOf(0) }
    var isUploading by rememberSaveable { mutableStateOf(false) }
    // 2. 活对象/后台任务持久化：使用自定义的 retained 作用域
    val retainedScope = rememberRetainedCoroutineScope()

    Scaffold { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "当前进度：$progress%",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (isUploading) return@Button
                    // 使用免疫配置变更的作用域启动耗时任务
                    retainedScope.launch {
                        try {
                            isUploading = true
                            progress = 0
                            // 模拟耗时上传操作：每 100 毫秒增加 1%
                            while (progress < 100) {
                                delay(100.milliseconds)
                                progress += 1
                            }
                        } finally {
                            // 如果协程被正常结束或异常取消，都会重置状态
                            isUploading = false
                        }
                    }
                },
                enabled = !isUploading
            ) {
                Text(text = if (isUploading) "正在上传中" else "点击开始上传")
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "物理验证步骤：\n1. 点击开始上传\n2. 当进度走到 30%~50% 时，旋转模拟器屏幕（或切换系统深色模式）\n3. 观察进度值与后台协程是否存活",
                modifier = Modifier.background(color = MaterialTheme.colorScheme.secondaryContainer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

// 自定义跨越配置变更的协程作用域
@Composable
fun rememberRetainedCoroutineScope(): CoroutineScope {
    // 核心 1：利用 retain 将 Scope 存入 Activity 级别的 NonConfigurationInstances 容器中
    // 即使 UI 树反复销毁重建，SupervisorJob 也会作为 Root Job 继续存活
    val scope = retain { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }

    // 核心 2：利用 RetainedEffect 监听页面的真正死亡（出栈），而不是配置变更导致的假死
    // 传入 scope 作为 key，确保当前对象被精准监视
    RetainedEffect(scope) {
        onRetire {
            Log.d(C411B, "页面销毁，Cancel 后台任务。")
            scope.cancel()
        }
    }
    return scope
}

@PhonePreviews
@Composable
fun Sec11B_RetainedStrategyPreview() {
    CourseComposeTheme {
        Sec11B_RetainedStrategy()
    }
}
