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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author runningpig66
 * @date 2026-06-22
 * @time 1:37
 *
 * 演示因 Kotlin 属性委托（by 关键字）意外避开“旧值陷阱”的特殊案例。
 * 证明当闭包捕获的是 State 对象的内存引用时，即使 Key 为 Unit，也始终能安全读取到最新状态。
 *
 * notes: C54_DisposableEffect.md
 */
private const val TAG54 = "StaleClosureTest"

@Composable
fun C54C_StaleClosureTrap1() {
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

            // 传入 Unit，保证整个页面生命周期内，硬件只初始化一次
            DisposableEffect(Unit) {
                Log.d(TAG54, "Setup: 硬件传感器已启动，正在监听")

                val listener = object : HardwareListener {
                    override fun onDataReceived(heartRate: Int) {
                        // 隐式依赖陷阱：在这里读取了外部的 threshold
                        if (heartRate > threshold) {
                            Log.d(TAG54, "警告：当前心率 $heartRate， 超过阈值 $threshold")
                        } else {
                            Log.d(TAG54, "安全：当前心率 $heartRate， 未超过超过阈值 $threshold")
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
    }
}

// 模拟的底层接口和管理类
interface HardwareListener {
    fun onDataReceived(heartRate: Int)
}

object HardwareManager {
    private var currentListener: HardwareListener? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun register(listener: HardwareListener) {
        currentListener = listener
        Log.d(TAG54, "[HardwareManager] 收到注册请求，开始发射心率数据")

        job?.cancel() // 确保没有遗留的任务

        /*job = scope.launch {
            // 只要任务没被取消，就无限循环模拟硬件脉冲
            while (isActive) {
                delay(2000.milliseconds) // 每隔 2 秒产生一次新数据
                val mockHeartRate = 110 // 模拟当前真实心率固定为 110

                // 切换回主线程进行回调（真实的 Android 硬件回调通常也在主线程或指定 Looper）
                withContext(Dispatchers.Main) {
                    currentListener?.onDataReceived(mockHeartRate)
                }
            }
        }*/

        job = scope.launch(Dispatchers.Main) {
            flow {
                var mockHeartRate2 = 100
                while (true) {
                    emit(mockHeartRate2)
                    delay(2000.milliseconds)
                    mockHeartRate2 += 10
                }
            }
                .flowOn(Dispatchers.Default)
                .collect { currentListener?.onDataReceived(it) }
        }
    }

    fun unregister(listener: HardwareListener) {
        if (currentListener === listener) {
            Log.d(TAG54, "[HardwareManager] 收到注销请求，停止发射心率数据")
            job?.cancel()
            job = null
            currentListener = null
        }
    }
}

@PhonePreviews
@Composable
fun C54C_StaleClosureTrap1Preview() {
    CourseComposeTheme {
        C54C_StaleClosureTrap1()
    }
}
