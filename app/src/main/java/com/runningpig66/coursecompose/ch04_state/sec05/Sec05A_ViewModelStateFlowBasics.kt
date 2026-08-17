package com.runningpig66.coursecompose.ch04_state.sec05

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * @author runningpig66
 * @date 2026/08/10 周一
 * @time 5:09
 *
 * 4.5 ViewModel + StateFlow 基础与生命周期实验
 *
 * 练习内容：
 * - 使用 MutableStateFlow / StateFlow 保存并暴露 Screen UI State。
 * - 使用 viewModelScope 启动属于 ViewModel 的业务协程。
 * - Compose 通过 collectAsStateWithLifecycle() 收集 StateFlow。
 * - 使用 Route / Screen 拆分 ViewModel 连接层与纯 UI。
 * - 观察重组、屏幕旋转、Activity 重建、Back 退出时 ViewModel 的生命周期。
 * - 验证配置变化时 ViewModelStore 保留 ViewModel，viewModelScope 中的任务可以继续执行。
 * - 验证 ViewModel 真正 clear 时 viewModelScope 被取消以及 onCleared() 的执行。
 *
 * 核心心智模型：
 * UI Event -> ViewModel -> viewModelScope -> StateFlow -> Compose State -> UI
 *
 * notes: 4.5 ViewMode.md
 */
const val TAG05A = "Sec05A" // log("$TAG05A ")

data class LoadUiState(
    val isLoading: Boolean = false,
    val message: String = "还没有加载数据",
    val loadCount: Int = 0
)

class LoadViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoadUiState())
    val uiState: StateFlow<LoadUiState> = _uiState.asStateFlow()

    init {
        logD("$TAG05A ViewModel 创建：${this.hashCode()}")
    }

    override fun onCleared() {
        logD("$TAG05A ViewModel onCleared：${this.hashCode()}")
    }

    fun loadData() {
        if (_uiState.value.isLoading) return

        logD("$TAG05A 收到 UI 的 loadData() 事件")

        viewModelScope.launch {
            logD("$TAG05A 协程开始执行")

            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    message = "正在加载..."
                )

                logD("$TAG05A StateFlow -> Loading")

                delay(8.seconds)

                val newCount = _uiState.value.loadCount + 1

                _uiState.value = LoadUiState(
                    isLoading = false,
                    message = "第 $newCount 次加载成功",
                    loadCount = newCount
                )

                logD("$TAG05A StateFlow -> Success，第 $newCount 次")
            } finally {
                logD("$TAG05A loadData 协程 finally")
            }
        }
    }
}

@Composable
fun ViewModelStateFlowBasicsRoute(
    viewModel: LoadViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SideEffect {
        logD("$TAG05A Route 完成一次组合，ViewModel=${viewModel.hashCode()}")
    }

    ViewModelStateFlowBasicsScreen(
        uiState = uiState,
        onLoadClick = viewModel::loadData
    )
}

@Composable
private fun ViewModelStateFlowBasicsScreen(
    uiState: LoadUiState,
    onLoadClick: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 24.dp, alignment = Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = uiState.message,
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "成功加载次数：${uiState.loadCount}",
                style = MaterialTheme.typography.bodyLarge
            )

            if (uiState.isLoading) {
                CircularProgressIndicator()
            }

            Button(
                onClick = onLoadClick,
                enabled = !uiState.isLoading
            ) {
                Text(text = if (uiState.isLoading) "加载中" else "加载数据")
            }
        }
    }
}

@PhonePreviews
@Composable
fun Sec05APreview() {
    CourseComposeTheme {
        ViewModelStateFlowBasicsScreen(
            uiState = LoadUiState(),
            onLoadClick = {}
        )
    }
}
