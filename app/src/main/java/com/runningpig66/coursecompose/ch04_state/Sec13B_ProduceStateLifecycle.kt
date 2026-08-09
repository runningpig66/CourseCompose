package com.runningpig66.coursecompose.ch04_state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author runningpig66
 * @date 2026/08/08 周六
 * @time 0:44
 *
 * 演示 produceState 的生命周期、key 变化后的 producer 重启，
 * 以及 awaitDispose 如何完成 Listener 的注册与注销。
 *
 * notes: 4.13 produceState().md
 */
private const val TAG13B = "Sec13B"

fun interface HeartRateListener {
    fun onHeartRateChanged(heartRate: Int)
}

object MockHeartRateSensor {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jobs = ConcurrentHashMap<HeartRateListener, Job>()

    fun register(deviceId: String, listener: HeartRateListener) {
        log("$TAG13B register: $deviceId")

        val job = scope.launch {
            // 意図的に新しいデバイスの最初のデータを少し遅らせて到着させる
            delay(1500.milliseconds)
            var heartRate = if (deviceId == "Watch-A") 60 else 90
            while (isActive) {
                withContext(Dispatchers.Main.immediate) {
                    listener.onHeartRateChanged(heartRate)
                }
                heartRate++
                delay(1000.milliseconds)
            }
        }
        jobs[listener] = job
    }

    fun unregister(listener: HeartRateListener) {
        log("$TAG13B unregister")
        jobs.remove(listener)?.cancel()
    }

    fun normalFunction() {
        scope.launch {
            loadData()
        }
    }

    suspend fun loadData() {}
}

@Composable
fun Sec13B_ProduceStateLifecycle() {
    var deviceId by remember { mutableStateOf("Watch-A") }
    val heartRate by produceState(initialValue = 0, key1 = deviceId) {
        val activeDeviceId = deviceId

        log("$TAG13B producer START: $activeDeviceId")
        // value = 0 // オプション：デバイスを切り替えた後に心拍数を初期化する

        val listener = HeartRateListener { newHeartRate ->
            log("$TAG13B callback: $activeDeviceId -> $newHeartRate")
            value = newHeartRate
        }
        MockHeartRateSensor.register(activeDeviceId, listener)

        awaitDispose {
            log("$TAG13B producer DISPOSE: $activeDeviceId")
            MockHeartRateSensor.unregister(listener)
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Current Device: $deviceId")
            Text(text = "Current HeartRate: $heartRate")
            Button(onClick = {
                deviceId = if (deviceId == "Watch-A") "Watch-B" else "Watch-A"
            }) {
                Text(text = "Switch Device")
            }
        }
    }
}

// もう1つの約10行の小さな実験をやってみましょう。producerがそもそもコルーチンであることを証明するものです。
@Composable
fun SuspendProducerExample(userId: Int) {
    val text by produceState(initialValue = "Loading...", key1 = userId) {
        log("$TAG13B load start: userId = $userId")
        delay(3000.milliseconds)
        value = "User $userId loaded"
        log("$TAG13B load finished: userId=$userId")
    }
    Text(text = text)
}

/* Output:
0 [main @coroutine#69] Sec13B producer START: Watch-A
8 [main @coroutine#69] Sec13B register: Watch-A
1544 [main @coroutine#71] Sec13B callback: Watch-A -> 60
2557 [main @coroutine#71] Sec13B callback: Watch-A -> 61
3141 [main @coroutine#69] Sec13B producer DISPOSE: Watch-A
3142 [main @coroutine#69] Sec13B unregister
3154 [main @coroutine#83] Sec13B producer START: Watch-B
3159 [main @coroutine#83] Sec13B register: Watch-B
4675 [main @coroutine#84] Sec13B callback: Watch-B -> 90
5688 [main @coroutine#84] Sec13B callback: Watch-B -> 91
6609 [main @coroutine#83] Sec13B producer DISPOSE: Watch-B
6614 [main @coroutine#83] Sec13B unregister
 */

@PhonePreviews
@Composable
fun Sec13BPreview() {
    CourseComposeTheme {
        Sec13B_ProduceStateLifecycle()
    }
}
