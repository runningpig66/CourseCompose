package com.runningpig66.coursecompose.ch04_state.sec05

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.seconds

/**
 * @author runningpig66
 * @date 2026/08/11 周二
 * @time 4:50
 *
 * 4.5 StateFlow 核心行为与 Compose 生命周期收集实验
 *
 * 练习内容：
 * - 验证 StateFlow 始终存在 current value，新 collector 会立即收到最新状态。
 * - 验证 StateFlow 的 equality-based conflation，相等状态不会重复发射。
 * - 验证快速生产时慢 collector 可能跳过中间状态，只观察到最新状态。
 * - 对比 value 直接赋值与 update { } 的使用场景。
 * - 理解 update { } 的原子 read-modify-write 与 CAS / retry 语义。
 * - 使用 collectAsStateWithLifecycle() 验证 STARTED 生命周期下的收集与停止。
 * - 区分 ViewModel 中的数据生产生命周期与 Compose UI collector 生命周期。
 *
 * 核心心智模型：
 * StateFlow 表示“当前状态”，不是历史记录或必须逐条消费的事件队列。
 *
 * notes: 4.5 ViewMode.md
 */
const val TAG05B = "Sec05B" // log("$TAG05B ")

data class StateFlowLabUiState(
    val count: Int = 0,
    val message: String = "初始状态"
)

class StateFlowLabViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StateFlowLabUiState())
    val uiState: StateFlow<StateFlowLabUiState> = _uiState.asStateFlow()

    private var lateCollectorJob: Job? = null
    private var lifecycleTickerJob: Job? = null

    /*init {
        log("$TAG05B ViewModel 创建")

        // 注意：现在还没有启动下面两个 collector。StateFlow 已经拥有自己的当前值。
        log("$TAG05B collector 启动前 value = ${_uiState.value}")

        // Collector A：快速 collector
        viewModelScope.launch {
            uiState.collect { state ->
                log("$TAG05B Collector A 收到：count=${state.count}, message=${state.message}")
            }
        }

        // Collector B：故意设计成慢 collector
        viewModelScope.launch {
            uiState.collect { state ->
                log("$TAG05B Collector B 开始处理：count=${state.count}")
                delay(1200.milliseconds)
                log("$TAG05B Collector B 处理完成：count=${state.count}")
            }
        }
    }*/

    // 普通 +1
    fun increment() {
        _uiState.update { oldState ->
            oldState.copy(
                count = oldState.count + 1,
                message = "普通 +1"
            )
        }
    }

    // 设置相等状态
    fun setEqualValue() {
        val oldState = _uiState.value
        val newState = oldState.copy()

        log("$TAG05B 准备设置相等对象：old == new → ${oldState == newState}")

        _uiState.value = newState
    }

    // 连续快速更新 5 次
    fun burstUpdate() {
        viewModelScope.launch {
            repeat(5) { index ->
                _uiState.update { oldState ->
                    oldState.copy(
                        count = oldState.count + 1,
                        message = "连续更新 ${index + 1}/5"
                    )
                }

                log("$TAG05B Producer 写入：count=${_uiState.value.count}")

                // 给快速 collector 一个运行机会，后面专门观察慢 collector 会发生什么。
                yield()
            }
        }
    }

    // 启动新的 Collector C
    fun startLateCollector() {
        if (lateCollectorJob != null) {
            log("$TAG05B Collector C 已经启动过")
            return
        }

        log("$TAG05B 即将启动 Collector C，当前 value=${_uiState.value}")

        lateCollectorJob = viewModelScope.launch {
            uiState.collect { state ->
                log("$TAG05B Collector C 收到：count=${state.count}, message=${state.message}")
            }
        }
    }

    // 直接读取当前 value
    fun printCurrentValue() {
        log("$TAG05B 直接读取 value：${_uiState.value}")
    }

    fun startLifecycleTicker() {
        if (lifecycleTickerJob?.isActive == true) return

        lifecycleTickerJob = viewModelScope.launch {
            log("$TAG05B LifecycleTicker 开始")

            while (isActive) {
                delay(1.seconds)

                _uiState.update { oldState ->
                    val newCount = oldState.count + 1

                    oldState.copy(
                        count = newCount,
                        message = "ViewModel 持续生产：$newCount"
                    )
                }

                log("$TAG05B Producer 写入 StateFlow：count=${_uiState.value.count}")
            }
        }
    }

    fun stopLifecycleTicker() {
        lifecycleTickerJob?.cancel()
        lifecycleTickerJob = null

        log("$TAG05B LifecycleTicker 停止")
    }

    override fun onCleared() {
        log("$TAG05B ViewModel onCleared")
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun StateFlowBehaviorRoute(
    viewModel: StateFlowLabViewModel = viewModel()
) {
    val observedUiStateFlow = remember(viewModel) {
        viewModel.uiState.onEach { state ->
            log("$TAG05B Route collector 真正收到：count=${state.count}")
        }
    }

    val uiState by observedUiStateFlow.collectAsStateWithLifecycle(
        // ERROR: StateFlow.value should not be called within composition
        initialValue = viewModel.uiState.value
    )

    //-val uiState = viewModel.uiState.value

    SideEffect {
        log("$TAG05B Compose 完成一次组合：count=${uiState.count}")
    }

    // val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StateFlowBehaviorScreen(
        uiState = uiState,
        onIncrement = viewModel::increment,
        onSetEqualValue = viewModel::setEqualValue,
        onBurstUpdate = viewModel::burstUpdate,
        onStartLateCollector = viewModel::startLateCollector,
        onPrintCurrentValue = viewModel::printCurrentValue,
        onStartLifecycleTicker = viewModel::startLifecycleTicker,
        onStopLifecycleTicker = viewModel::stopLifecycleTicker
    )
}

@Composable
private fun StateFlowBehaviorScreen(
    uiState: StateFlowLabUiState,
    onIncrement: () -> Unit,
    onSetEqualValue: () -> Unit,
    onBurstUpdate: () -> Unit,
    onStartLateCollector: () -> Unit,
    onPrintCurrentValue: () -> Unit,
    onStartLifecycleTicker: () -> Unit,
    onStopLifecycleTicker: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "count = ${uiState.count}",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = uiState.message,
                style = MaterialTheme.typography.bodyLarge
            )

            Button(onClick = onIncrement) { Text("普通 +1") }
            Button(onClick = onSetEqualValue) { Text("设置相等状态") }
            Button(onClick = onBurstUpdate) { Text("连续快速更新 5 次") }
            Button(onClick = onStartLateCollector) { Text("启动新的 Collector C") }
            Button(onClick = onPrintCurrentValue) { Text("直接读取当前 value") }
            Button(onClick = onStartLifecycleTicker) { Text("开始 Lifecycle 测试") }
            Button(onClick = onStopLifecycleTicker) { Text("停止 Lifecycle 测试") }
        }
    }
}

@PhonePreviews
@Composable
private fun Sec05BPreview() {
    CourseComposeTheme {
        StateFlowBehaviorScreen(
            uiState = StateFlowLabUiState(count = 3, message = "Preview 状态"),
            onIncrement = {},
            onSetEqualValue = {},
            onBurstUpdate = {},
            onStartLateCollector = {},
            onPrintCurrentValue = {},
            onStartLifecycleTicker = {},
            onStopLifecycleTicker = {}
        )
    }
}
