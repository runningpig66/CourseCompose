package com.runningpig66.coursecompose.ch04_state.sec05

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.logD
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * @author runningpig66
 * @date 2026/08/13 周四
 * @time 4:33
 *
 * 4.5 ViewModel / StateFlow 常见错误与边缘 Case 实验
 *
 * 练习内容：
 * - 验证 UiState 内部使用 MutableList 并原地修改为什么可能不触发 StateFlow 更新。
 * - 观察“数据已经改变，但直到无关重组发生后 UI 才突然刷新”的典型错误现象。
 * - 区分 MutableStateFlow 自身线程安全与内部可变对象线程安全。
 * - 强化不可变 UiState + copy / update 的状态更新方式。
 * - 理解为什么 MutableStateFlow 不应该直接向 UI 暴露写权限。
 * - 区分属于 ViewModel 的业务协程与属于 Compose/UI 的瞬时工作。
 * - 理解 Flow 异常应在 stateIn() 前转换为明确的 UI Error State。
 * - 区分 State 与一次性 Event，避免把 StateFlow 当作事件队列使用。
 *
 * 核心原则：
 * ViewModel 应拥有明确的 Source State 与业务事件，
 * 并稳定地产生不可变、可解释的 Screen UiState。
 *
 * notes: 4.5 ViewMode.md
 */
private const val TAG05E = "Sec05E"

data class BadMutableListState(
    val items: MutableList<String> = mutableListOf()
)

data class GoodImmutableListState(
    val items: List<String> = emptyList()
)

class PitfallViewModel : ViewModel() {
    private val _badListState = MutableStateFlow(BadMutableListState())
    val badListState = _badListState.asStateFlow()

    private val _goodListState = MutableStateFlow(GoodImmutableListState())
    val goodListState = _goodListState.asStateFlow()

    private val _unrelatedTick = MutableStateFlow(0)
    val unrelatedTick = _unrelatedTick.asStateFlow()

    fun addBadItem() {
        val state = _badListState.value

        state.items.add("Bad Item ${state.items.size + 1}")

        logD("$TAG05E BAD 内部已经修改：size=${state.items.size}")

        // 即使这样重新赋回去也救不了。还是同一个对象，而且 equals 仍然相等。
        _badListState.value = state
    }

    fun addGoodItem() {
        _goodListState.update { oldState ->
            oldState.copy(
                items = oldState.items + "Good Item ${oldState.items.size + 1}"
            )
        }

        logD("$TAG05E GOOD 更新：size=${_goodListState.value.items.size}")
    }

    fun forceUnrelatedChange() {
        _unrelatedTick.update { it + 1 }
        logD("$TAG05E 制造无关状态变化：tick=${_unrelatedTick.value}")
    }
}

@Composable
fun ViewModelPitfallsRoute(viewModel: PitfallViewModel = viewModel()) {
    val badState by viewModel.badListState.collectAsStateWithLifecycle()
    val goodState by viewModel.goodListState.collectAsStateWithLifecycle()
    val unrelatedTick by viewModel.unrelatedTick.collectAsStateWithLifecycle()

    SideEffect {
        logD(
            "$TAG05E Compose：" +
                    "bad=${badState.items.size}, " +
                    "good=${goodState.items.size}, " +
                    "tick=$unrelatedTick"
        )
    }

    ViewModelPitfallsScreen(
        badCount = badState.items.size,
        goodCount = goodState.items.size,
        unrelatedTick = unrelatedTick,
        onBadAdd = viewModel::addBadItem,
        onGoodAdd = viewModel::addGoodItem,
        onUnrelatedChange = viewModel::forceUnrelatedChange
    )
}

@Composable
private fun ViewModelPitfallsScreen(
    badCount: Int,
    goodCount: Int,
    unrelatedTick: Int,
    onBadAdd: () -> Unit,
    onGoodAdd: () -> Unit,
    onUnrelatedChange: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "错误 MutableList 数量：$badCount",
                style = MaterialTheme.typography.titleLarge
            )
            Button(onClick = onBadAdd) {
                Text("错误：原地修改 MutableList")
            }

            HorizontalDivider()

            Text(
                text = "正确不可变 List 数量：$goodCount",
                style = MaterialTheme.typography.titleLarge
            )
            Button(onClick = onGoodAdd) {
                Text("正确：产生新的不可变状态")
            }

            HorizontalDivider()

            Text(
                text = "无关状态：$unrelatedTick",
                style = MaterialTheme.typography.titleLarge
            )
            Button(onClick = onUnrelatedChange) {
                Text("制造一次无关重组")
            }
        }
    }
}

@PhonePreviews
@Composable
private fun Sec05EPreview() {
    CourseComposeTheme {
        ViewModelPitfallsScreen(
            badCount = 2,
            goodCount = 3,
            unrelatedTick = 0,
            onBadAdd = {},
            onGoodAdd = {},
            onUnrelatedChange = {}
        )
    }
}
