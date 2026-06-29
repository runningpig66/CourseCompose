package com.runningpig66.coursecompose.ch04_state

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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.practice.HardwareListener
import com.runningpig66.coursecompose.practice.HardwareManager
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-06-30
 * @time 1:58
 *
 * notes: Sec10A_rememberUpdatedState.md
 */
private const val C410A = "C4_10A_StaleClosureTrap3"

@Composable
fun HardwareMonitorScreen() {
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
            HardwareMonitor(threshold)
        }
    }
}

@Composable
fun HardwareMonitor(threshold: Int) {
    val currentThreshold by rememberUpdatedState(threshold)

    DisposableEffect(Unit) {
        val listener = object : HardwareListener {
            override fun onDataReceived(heartRate: Int) {
                if (heartRate > currentThreshold) {
                    Log.d(C410A, "警告：当前心率 $heartRate， 超过阈值 $currentThreshold")
                } else {
                    Log.d(C410A, "安全：当前心率 $heartRate， 未超过超过阈值 $currentThreshold")
                }
            }
        }
        HardwareManager.register(listener)

        onDispose {
            Log.d(C410A, "Dispose: 硬件传感器已关闭，释放资源")
            HardwareManager.unregister(listener)
        }
    }
}

@PhonePreviews
@Composable
fun HardwareMonitorScreenPreview() {
    CourseComposeTheme {
        HardwareMonitorScreen()
    }
}
