package com.runningpig66.coursecompose.ch04_state

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author runningpig66
 * @date 2026-07-02
 * @time 1:21
 *
 * 反面教材示例：演示普通的 remember 与 rememberCoroutineScope 在发生配置变更（如屏幕旋转）时，
 * 导致 UI 状态清零与后台挂起任务被意外取消的生命周期陷阱。
 */
@Composable
fun Sec11A_ConfigChangeTrap() {
    // Warning1: 使用普通的 remember，生命周期仅绑定于当前的 UI 节点
    var progress by remember { mutableIntStateOf(0) }
    var isUploading by remember { mutableStateOf(false) }
    // Warning2: 使用普通的 rememberCoroutineScope，作用域跟随 UI 节点的销毁而取消
    val scope = rememberCoroutineScope()

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
                    scope.launch {
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

@PhonePreviews
@Composable
fun Sec11A_ConfigChangeTrapPreview() {
    CourseComposeTheme {
        Sec11A_ConfigChangeTrap()
    }
}
