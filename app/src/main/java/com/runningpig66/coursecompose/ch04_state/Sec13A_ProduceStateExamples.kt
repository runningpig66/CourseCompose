package com.runningpig66.coursecompose.ch04_state

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author runningpig66
 * @date 2026/07/18
 * @time 2:41
 *
 * 对比手写 MutableState + DisposableEffect 与 produceState，
 * 演示如何将外部 Callback 数据源桥接为 Compose State。
 *
 * notes: 4.13 produceState().md
 */
private const val TAG13 = "Sec13A_ProduceStateExamples"

interface HardwareListener {
    fun onDataReceived(heartRate: Int)
}

object HardwareManager {
    // 使用线程安全集合保存所有监听器
    private val listeners = CopyOnWriteArrayList<HardwareListener>()
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun register(listener: HardwareListener) {
        listeners.add(listener)

        // 当第一个监听器注册时，启动底层硬件轮询协程
        if (job == null || job?.isActive != true) {
            job = scope.launch {
                var mockHeartRate = 60
                while (isActive) {
                    delay(500.milliseconds) // 模拟耗时操作
                    mockHeartRate += 10

                    withContext(Dispatchers.Main.immediate) {
                        // 遍历分发给所有处于活跃状态的监听器
                        listeners.forEach {
                            it.onDataReceived(mockHeartRate)
                        }
                    }
                }
            }
        }
    }

    fun unregister(listener: HardwareListener) {
        listeners.remove(listener)

        if (listeners.isEmpty()) {
            job?.cancel()
            job = null
        }
    }
}

@Composable
fun Sec13A_ProduceStateExamples() {
    Scaffold { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Sec13_ManualStateBridge()
            HorizontalDivider()
            Sec13_ProduceStateBridge()
        }
    }
}

// 1.1 反面案例：纯手写状态桥接
@Composable
fun Sec13_ManualStateBridge() {
    // 痛点 1：状态的声明被孤立在这里。我们必须手动提供一个无意义的初始值（如 0 或 -1），因为硬件数据还没传过来
    var heartRateState by remember { mutableIntStateOf(0) }

    // 痛点 2：生命周期管理与状态赋值分离，产生大量样板代码
    DisposableEffect(Unit) {
        Log.d(TAG13, "Setup1: 硬件传感器已启动，正在监听")
        // 创建回调对象
        val listener = object : HardwareListener {
            override fun onDataReceived(heartRate: Int) {
                heartRateState = heartRate // 在回调中手动给外层的 state 赋值，驱动重组
            }
        }
        // 注册监听
        HardwareManager.register(listener)
        // 解除监听
        onDispose {
            Log.d(TAG13, "Dispose1: 硬件传感器已关闭，释放资源")
            HardwareManager.unregister(listener)
        }
    }

    Column {
        Text(
            text = "[手写桥接]",
            fontWeight = FontWeight.Bold
        )
        if (heartRateState == 0) {
            Text("心率传感器连接中...")
        } else {
            Text("当前心率: $heartRateState BPM")
        }
    }
}

// 1.2 正面案例：使用 produceState 桥接
@Composable
fun Sec13_ProduceStateBridge() {
    // 将状态声明、作用域管理、资源清理高内聚于一个 API
    val heartRateState by produceState(
        initialValue = 0,
        key1 = Unit // 若传入依赖项发生变化，会自动执行 awaitDispose 并重启闭包
    ) {
        Log.d(TAG13, "Setup2: 硬件传感器已启动，正在监听")
        val listener = object : HardwareListener {
            override fun onDataReceived(heartRate: Int) {
                value = heartRate // ProducerScope 提供的 value 属性，赋值即触发重组
            }
        }
        HardwareManager.register(listener)
        // TODO 挂起当前协程，直到组件销毁或 key 发生变化时执行清理逻辑
        awaitDispose {
            Log.d(TAG13, "Dispose2: 硬件传感器已关闭，释放资源")
            HardwareManager.unregister(listener)
        }
    }

    Column {
        Text(
            text = "[produceState 桥接]",
            fontWeight = FontWeight.Bold
        )
        if (heartRateState == 0) {
            Text("心率传感器连接中...")
        } else {
            Text("当前心率: $heartRateState BPM")
        }
    }
}

/* Output:
Setup1: 硬件传感器已启动，正在监听
Setup2: 硬件传感器已启动，正在监听
Dispose1: 硬件传感器已关闭，释放资源
Dispose2: 硬件传感器已关闭，释放资源
...
 */

@PhonePreviews
@Composable
fun Sec13APreview() {
    CourseComposeTheme {
        Sec13A_ProduceStateExamples()
    }
}
