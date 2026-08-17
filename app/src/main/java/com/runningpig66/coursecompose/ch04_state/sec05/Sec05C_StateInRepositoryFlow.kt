package com.runningpig66.coursecompose.ch04_state.sec05

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.logD
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.seconds

/**
 * @author runningpig66
 * @date 2026/08/12 周三
 * @time 2:41
 *
 * 4.5 stateIn()：Repository Cold Flow -> ViewModel StateFlow 实验
 *
 * 练习内容：
 * - Fake Repository 提供 cold Flow，模拟真实 Room / Repository 数据流。
 * - 使用 stateIn() 将 Flow 转换为 ViewModel 范围内共享的 StateFlow。
 * - 理解 stateIn() 为什么需要 CoroutineScope，以及 viewModelScope 的实际作用。
 * - 理解 SharingStarted.WhileSubscribed(5_000) 的启动、停止与重新订阅行为。
 * - 验证 subscriber 消失后 upstream collection 可以停止，但 StateFlow 和最后的 value 仍然存在。
 * - 验证重新出现 subscriber 后 cold Flow 会被重新 collect，并重新执行生产逻辑。
 * - 理解 initialValue 为什么能够在 upstream 第一条数据到达前立即提供 UI 状态。
 *
 * 核心心智模型：
 * Cold Flow -> stateIn(viewModelScope, SharingStarted, initialValue)
 *           -> shared StateFlow -> Compose
 *
 * notes: 4.5 ViewMode.md
 */
private const val TAG05C = "Sec05C"

data class RepositoryTick(
    val session: Int,
    val tick: Int
)

class FakeCounterRepository {
    private var sessionCount = 0

    fun observeTicks(): Flow<RepositoryTick> = flow {
        val session = ++sessionCount

        logD("$TAG05C Repository 上游 START，session=$session")

        try {
            var tick = 0
            while (currentCoroutineContext().isActive) {
                delay(1.seconds)
                tick++

                logD("$TAG05C Repository emit：session=$session, tick=$tick")

                emit(RepositoryTick(session = session, tick = tick))
            }
        } finally {
            logD("$TAG05C Repository 上游 STOP，session=$session")
        }
    }
}

data class StateInUiState(
    val session: Int = 0,
    val tick: Int = 0,
    val message: String = "等待 UI 订阅"
)

class StateInViewModel : ViewModel() {
    private val repository = FakeCounterRepository()

    val uiState: StateFlow<StateInUiState> =
        repository.observeTicks()
            .map { repositoryTick ->
                logD(
                    "$TAG05C ViewModel map：" +
                            "session=${repositoryTick.session}, " +
                            "tick=${repositoryTick.tick}"
                )

                StateInUiState(
                    session = repositoryTick.session,
                    tick = repositoryTick.tick,
                    message = "正在接收 Repository 数据"
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = StateInUiState()
            )

    init {
        logD("$TAG05C ViewModel 创建：${this.hashCode()}")
    }

    override fun onCleared() {
        logD("$TAG05C ViewModel onCleared")
    }
}

@Composable
fun StateInRepositoryRoute(
    viewModel: StateInViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SideEffect {
        logD("$TAG05C Compose：session=${uiState.session}, tick=${uiState.tick}")
    }

    StateInRepositoryScreen(uiState = uiState)
}

@Composable
private fun StateInRepositoryScreen(uiState: StateInUiState) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Repository Session：${uiState.session}",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Tick：${uiState.tick}",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(text = uiState.message)
        }
    }
}

@PhonePreviews
@Composable
private fun Sec05CPreview() {
    CourseComposeTheme {
        StateInRepositoryScreen(uiState = StateInUiState())
    }
}
