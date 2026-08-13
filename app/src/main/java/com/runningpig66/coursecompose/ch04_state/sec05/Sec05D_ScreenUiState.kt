package com.runningpig66.coursecompose.ch04_state.sec05

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log
import com.runningpig66.coursecompose.ui.utils.resetLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * @author runningpig66
 * @date 2026/08/12 周三
 * @time 23:08
 *
 * 4.5 Screen UiState：多个 Source State 组合成派生状态实验
 *
 * 练习内容：
 * - 模拟 Repository 数据、搜索 Query、Filter 三个独立状态来源。
 * - 使用 combine() 获取所有 upstream 的最新值并持续重新计算 Screen UiState。
 * - 使用 map / combine / stateIn 构建完整的状态生产链。
 * - 区分 Source State 与 Derived State，避免手动维护重复的 _uiState。
 * - 使用 sealed interface 建模 Loading / Data / Error 等互斥内容状态。
 * - 验证 Repository、Query、Filter 任意一个变化都能自动产生新的 UiState。
 * - 结合 WhileSubscribed 验证页面离开、upstream restart、旧 StateFlow value 与 Loading 的关系。
 * - 初步建立 UDF：State 向下流动，User Event 向 ViewModel 上行。
 *
 * 核心心智模型：
 * Repository Flow + Query + Filter
 *              -> combine
 *              -> Flow<UiState>
 *              -> stateIn
 *              -> StateFlow<UiState>
 *              -> Compose
 *
 * notes: 4.5 ViewMode.md
 */
private const val TAG05D = "Sec05D"

enum class EntryType {
    EXPENSE,
    INCOME
}

enum class LedgerFilter {
    ALL,
    EXPENSE,
    INCOME
}

data class LedgerEntry(
    val id: Int,
    val title: String,
    val amount: Int,
    val type: EntryType
)

sealed interface RepositoryEntriesState {

    data object Loading : RepositoryEntriesState

    data class Data(val entries: List<LedgerEntry>) : RepositoryEntriesState

    data class Error(val message: String) : RepositoryEntriesState
}

sealed interface LedgerContent {

    data object Loading : LedgerContent

    data class Data(val entries: List<LedgerEntry>, val signedTotal: Int) : LedgerContent

    data class Error(val message: String) : LedgerContent
}

data class LedgerUiState(
    val query: String = "",
    val filter: LedgerFilter = LedgerFilter.ALL,
    val content: LedgerContent = LedgerContent.Loading
)

/**
 * @param shouldFail Simulation Repository loading failed
 */
class FakeLedgerRepository(
    private val shouldFail: Boolean = false
) {
    fun observeEntries(): Flow<RepositoryEntriesState> = flow {
        log("$TAG05D Repository START")

        try {
            emit(RepositoryEntriesState.Loading)
            delay(1200.milliseconds)

            if (shouldFail) {
                emit(RepositoryEntriesState.Error(message = "模拟 Repository 加载失败"))
                return@flow
            }

            val initialEntries = listOf(
                LedgerEntry(id = 1, title = "午餐", amount = 28, type = EntryType.EXPENSE),
                LedgerEntry(id = 2, title = "工资", amount = 12000, type = EntryType.INCOME),
                LedgerEntry(id = 3, title = "地铁", amount = 6, type = EntryType.EXPENSE),
                LedgerEntry(id = 4, title = "咖啡", amount = 18, type = EntryType.EXPENSE),
                LedgerEntry(id = 5, title = "退款", amount = 50, type = EntryType.INCOME)
            )
            emit(RepositoryEntriesState.Data(entries = initialEntries))

            // Simulate another change occurring in the database a short while later.
            delay(5.seconds)
            emit(
                RepositoryEntriesState.Data(
                    entries =
                        initialEntries +
                                LedgerEntry(id = 6, title = "晚餐", amount = 42, type = EntryType.EXPENSE)
                )
            )
        } finally {
            log("$TAG05D Repository STOP")
        }
    }
}

class LedgerViewModel : ViewModel() {
    private val repository = FakeLedgerRepository(shouldFail = false)
    private val _query = MutableStateFlow("")
    private val _filter = MutableStateFlow(LedgerFilter.ALL)

    val uiState: StateFlow<LedgerUiState> =
        combine(
            repository.observeEntries(),
            _query,
            _filter
        ) { repositoryState, query, filter ->
            log("$TAG05D combine 执行：query=$query, filter=$filter")

            when (repositoryState) {
                is RepositoryEntriesState.Loading -> {
                    LedgerUiState(
                        query = query,
                        filter = filter,
                        content = LedgerContent.Loading
                    )
                }

                is RepositoryEntriesState.Error -> {
                    LedgerUiState(
                        query = query,
                        filter = filter,
                        content = LedgerContent.Error(message = repositoryState.message)
                    )
                }

                is RepositoryEntriesState.Data -> {
                    val normalizedQuery = query.trim()

                    val visibleEntries = repositoryState.entries
                        .filter { ledgerEntry ->
                            when (filter) {
                                LedgerFilter.ALL -> true
                                LedgerFilter.EXPENSE -> ledgerEntry.type == EntryType.EXPENSE
                                LedgerFilter.INCOME -> ledgerEntry.type == EntryType.INCOME
                            }
                        }
                        .filter { ledgerEntry ->
                            normalizedQuery.isBlank() ||
                                    ledgerEntry.title.contains(other = normalizedQuery, ignoreCase = true)
                        }

                    val signedTotal = visibleEntries.sumOf { ledgerEntry ->
                        when (ledgerEntry.type) {
                            EntryType.EXPENSE -> -ledgerEntry.amount
                            EntryType.INCOME -> ledgerEntry.amount
                        }
                    }

                    LedgerUiState(
                        query = query,
                        filter = filter,
                        content = LedgerContent.Data(
                            entries = visibleEntries,
                            signedTotal = signedTotal
                        )
                    )
                }
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = LedgerUiState()
            )

    init {
        resetLog()
        log("$TAG05D ViewModel 创建：${hashCode()}")
    }

    fun onQueryChange(newQuery: String) {
        log("$TAG05D onQueryChange：$newQuery")
        _query.value = newQuery
    }

    fun onFilterChange(newFilter: LedgerFilter) {
        log("$TAG05D onFilterChange：$newFilter")
        _filter.value = newFilter
    }

    override fun onCleared() {
        log("$TAG05D ViewModel onCleared")
    }
}

@Composable
fun ScreenUiStateRoute(
    viewModel: LedgerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SideEffect {
        log(
            "$TAG05D Compose：" +
                    "query=${uiState.query}, " +
                    "filter=${uiState.filter}, " +
                    "content=${uiState.content::class.simpleName}"
        )
    }

    ScreenUiStateScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onFilterChange = viewModel::onFilterChange
    )
}

@Composable
private fun ScreenUiStateScreen(
    uiState: LedgerUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (LedgerFilter) -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "搜索账单") },
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LedgerFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.filter == filter,
                        onClick = { onFilterChange(filter) },
                        label = {
                            Text(
                                text = when (filter) {
                                    LedgerFilter.ALL -> "全部"
                                    LedgerFilter.EXPENSE -> "支出"
                                    LedgerFilter.INCOME -> "收入"
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            when (val content = uiState.content) {
                is LedgerContent.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is LedgerContent.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = content.message,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }

                is LedgerContent.Data -> {
                    Text(
                        text = "当前结果：${content.entries.size} 条",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "当前筛选合计：${content.signedTotal} 元",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    if (content.entries.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "没有匹配结果", style = MaterialTheme.typography.titleLarge)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = content.entries,
                                key = { it.id }
                            ) { entry ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = entry.title,
                                        style = MaterialTheme.typography.titleLarge
                                    )

                                    Text(
                                        text = when (entry.type) {
                                            EntryType.INCOME -> "+${entry.amount} 元"
                                            EntryType.EXPENSE -> "-${entry.amount} 元"
                                        },
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@PhonePreviews
@Composable
private fun Sec05DPreview() {
    CourseComposeTheme {
        ScreenUiStateScreen(
            uiState = LedgerUiState(
                query = "",
                filter = LedgerFilter.ALL,
                content = LedgerContent.Data(
                    entries = listOf(
                        LedgerEntry(id = 1, title = "午餐", amount = 28, type = EntryType.EXPENSE),
                        LedgerEntry(id = 2, title = "工资", amount = 12000, type = EntryType.INCOME)
                    ),
                    signedTotal = 11972
                )
            ),
            onQueryChange = {},
            onFilterChange = {}
        )
    }
}
