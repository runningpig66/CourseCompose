package com.runningpig66.coursecompose.practice

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-06-22
 * @time 1:37
 *
 * 真实复现 Compose 极其危险的“闭包旧值陷阱（Stale Closure）”。
 * 演示状态被上提后，当闭包捕获基础类型（Int）且 Key 为 Unit 时，底层硬件监听器彻底与最新 UI 脱节的致命 Bug。
 *
 * notes: C54_DisposableEffect.md
 */
private const val TAG54 = "StaleClosureTest"

@Composable
fun C54C_StaleClosureTrap2() {
    // 报警阈值，初始为 100
    var threshold by remember { mutableIntStateOf(100) }

    Scaffold { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("当前报警阈值：$threshold")
            Button(onClick = { threshold += 20 }) {
                Text("提高阈值（+20）")
            }
            HardwareMonitorChild(threshold)
        }
    }
}

@Composable
fun HardwareMonitorChild(thresholdParameter: Int) {
    // 传入 Unit，保证整个页面生命周期内，硬件只初始化一次
    DisposableEffect(Unit) {
        Log.d(TAG54, "Setup: 硬件传感器已启动，正在监听")

        val listener = object : HardwareListener {
            override fun onDataReceived(heartRate: Int) {
                // 依赖陷阱：闭包内部捕获的是传进来的基本类型 Int
                if (heartRate > thresholdParameter) {
                    Log.d(TAG54, "警告：当前心率 $heartRate， 超过阈值 $thresholdParameter")
                } else {
                    Log.d(TAG54, "安全：当前心率 $heartRate， 未超过超过阈值 $thresholdParameter")
                }
            }
        }
        HardwareManager.register(listener)

        onDispose {
            Log.d(TAG54, "Dispose: 硬件传感器已关闭，释放资源")
            HardwareManager.unregister(listener)
        }
    }
}

@PhonePreviews
@Composable
fun C54C_StaleClosureTrap2Preview() {
    CourseComposeTheme {
        C54C_StaleClosureTrap2()
    }
}
