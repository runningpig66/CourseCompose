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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
 * 探究 DisposableEffect 结合变量 Key 发生变化时的“先销毁、后重启”机制。
 * 同时作为重组作用域的“极限测试靶场”，验证了内联函数及状态读取位置对重组范围的影响。
 *
 * notes: C54_DisposableEffect.md
 */
private const val TAG54 = "DisposableTest2"
var externalCount3 = 0

@Composable
fun C54A_DisposableTest2() {
    var clickCount by remember { mutableIntStateOf(0) }
    var showChild by remember { mutableStateOf(true) }

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
            Text("clickCount: $clickCount")
            Text("externalCount: $externalCount3")
            Button(onClick = { clickCount++ }) { Text("clickCount++") }

            if (showChild) {
                Text("我是一个可能需要注册硬件监听的子组件 externalCount: $externalCount3")

                SideEffect {
                    externalCount3++
                    Log.d(TAG54, "(SideEffect) externalCount: $externalCount3")
                }

                DisposableEffect(clickCount) {
                    externalCount3++

                    // Setup 阶段：在这里执行申请资源、注册监听等操作
                    Log.d(TAG54, "(DisposableEffect) 成功打开硬件传感器！externalCount: $externalCount3")
                    // 模拟获得了一个传感器实例
                    // val sensor = SensorManager.open()

                    onDispose {
                        // Dispose 阶段：在这里执行释放资源、注销监听等收尾操作
                        Log.d(TAG54, "(DisposableEffect) 传感器已安全关闭。externalCount: $externalCount3")
                        // 模拟释放传感器
                        // sensor.close()
                    }
                }
            }
        }
    }
}

@PhonePreviews
@Composable
fun C54ADisposableTest2Preview() {
    CourseComposeTheme {
        C54A_DisposableTest2()
    }
}
