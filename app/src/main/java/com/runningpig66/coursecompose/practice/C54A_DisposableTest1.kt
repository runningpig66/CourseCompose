package com.runningpig66.coursecompose.practice

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-06-19
 * @time 6:23
 *
 * 演示 DisposableEffect 的基础结构与严格时序。验证在 UI 节点进入组合 (Setup) 和离开组合 (onDispose) 时，回调被精准触发的底层逻辑。
 *
 * notes: C54_DisposableEffect.md
 */
private const val TAG54 = "DisposableTest1"

@Composable
fun C54A_DisposableTest1() {
    var showChild by remember { mutableStateOf(true) }
    var sensorType by remember { mutableStateOf("心率传感器") }

    // 本例中的 clickCount 的最小重组作用域是哪里？
    Scaffold { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("显示子组件")
                Switch(checked = showChild, onCheckedChange = { showChild = it })
            }
            Button(onClick = { sensorType = if (sensorType == "心率传感器") "血压传感器" else "心率传感器" }) {
                Text("切换传感器类型（当前：$sensorType）")
            }
            if (showChild) {
                // 当 showChild 为 true 时，ChildWidget 进入组合
                // 当 showChild 为 false 时，ChildWidget 离开组合
                // // 将传感器类型作为参数传递给子组件
                ChildWidgetWithKey(sensorType)
                //ChildWidget()
            }
        }
    }
}

@Composable
fun ChildWidgetWithKey(sensorType: String) {
    Text("当前正在监听：$sensorType")

    // Change: 将 Unit 替换为外部传入的变量 sensorType
    DisposableEffect(sensorType) {
        // Setup 阶段：在这里执行申请资源、注册监听等操作
        Log.d(TAG54, "(DisposableEffect) Setup: 打开设备硬件 [$sensorType]")
        // 模拟获得了一个传感器实例
        // val sensor = SensorManager.open()

        onDispose {
            // Dispose 阶段：在这里执行释放资源、注销监听等收尾操作
            Log.d(TAG54, "(DisposableEffect) Dispose: 关闭设备硬件 [$sensorType]")
            // 模拟释放传感器
            // sensor.close()
        }
    }
}

@Composable
fun ChildWidget() {
    Text("我是一个可能需要注册硬件监听的子组件")
    // 引入 DisposableEffect，传入 Unit 作为参数
    DisposableEffect(Unit) {
        // Setup 阶段：在这里执行申请资源、注册监听等操作
        Log.d(TAG54, "(DisposableEffect) Setup: ChildWidget 进入组合，成功打开硬件传感器！")
        // 模拟获得了一个传感器实例
        // val sensor = SensorManager.open()

        onDispose {
            // Dispose 阶段：在这里执行释放资源、注销监听等收尾操作
            Log.d(TAG54, "(DisposableEffect) Dispose: ChildWidget 离开组合，传感器已安全关闭。")
            // 模拟释放传感器
            // sensor.close()
        }
    }
}

@PhonePreviews
@Composable
fun C54ADisposableTestPreview() {
    CourseComposeTheme {
        C54A_DisposableTest1()
    }
}
