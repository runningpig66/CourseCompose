package com.runningpig66.coursecompose.ch04_state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026/07/17
 * @time 2:24
 *
 * 架构边界划分：演示在单向数据流（UDF）架构中，UI 视觉状态（rememberSaveable）与核心业务状态（ViewModel）的标准职责协同。
 *
 * notes: Sec03_rememberSaveable().md
 */
// 1. 核心业务层：使用 ViewModel 和 SavedStateHandle 托管业务状态
class SearchViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val KEY_SEARCH_QUERY = "search_query"

    // 业务状态：搜索关键字。使用 SavedStateHandle 存储，底层自动对接系统的 Bundle 恢复机制。
    // 即使输入框所在的 UI 节点被卸载，只要页面没关，搜索关键字就不会丢失。
    val searchQuery = savedStateHandle.getStateFlow(KEY_SEARCH_QUERY, "")

    fun updateQuery(newQuery: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = newQuery
    }

    fun performSearch() {
        // 基于 savedStateHandle 中的值发起网络请求...
    }
}

// 2. UI 渲染层：纯视觉交互状态使用 rememberSaveable
@Composable
fun Sec03D_ArchitectureChoiceScreen(viewModel: SearchViewModel = viewModel()) {
    // 从 ViewModel 收集业务状态
    // val query by viewModel.searchQuery.collectAsState()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    ArchitectureChoiceScreenContent(
        query,
        viewModel::updateQuery,
        viewModel::performSearch
    )
}

@Composable
fun ArchitectureChoiceScreenContent(
    query: String,
    updateQuery: (String) -> Unit,
    performSearch: () -> Unit
) {
    // 纯 UI 状态：高级筛选面板是否处于展开状态。
    // 使用 rememberSaveable，确保进程死亡重启后，面板依然保持之前的开/合视觉状态。
    // 如果把这个状态放到 ViewModel 中，会导致 ViewModel 臃肿且包含 UI 逻辑。
    var isFilterPanelExpanded by rememberSaveable { mutableStateOf(true) }

    // 瞬时 UI 状态：普通的 remember。
    // 仅用于当前界面的短期交互（如按钮的按下效果），进程死亡后丢失也无所谓。
    var showTooltip by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { updateQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("输入搜索内容") }
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(onClick = { performSearch() }) {
                    Text(
                        text = "搜索",
                        modifier = Modifier.widthIn(min = 50.dp),
                        textAlign = TextAlign.Center
                    )
                }
                Button(onClick = { isFilterPanelExpanded = !isFilterPanelExpanded }) {
                    Text(
                        text = if (isFilterPanelExpanded) "收起筛选" else "展开筛选",
                        modifier = Modifier.widthIn(min = 50.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (isFilterPanelExpanded) {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "类别：数码产品",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "价格区间：0 - 1000",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@PhonePreviews
@Composable
fun ArchitectureChoiceScreenContentPreview() {
    CourseComposeTheme {
        ArchitectureChoiceScreenContent(
            query = "",
            updateQuery = {},
            performSearch = {})
    }
}
