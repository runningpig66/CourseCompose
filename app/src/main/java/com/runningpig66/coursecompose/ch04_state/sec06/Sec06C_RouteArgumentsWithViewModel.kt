package com.runningpig66.coursecompose.ch04_state.sec06

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author runningpig66
 * @date 2026/08/15 周六
 * @time 1:24
 *
 * Navigation 3 路由参数与 NavEntry ViewModel 作用域练习。
 *
 * 将 Route 中的 noteId 传给 ViewModel，并验证不同 NavEntry
 * 拥有独立 ViewModel；同时观察 Entry 出栈后的 ViewModel 清理行为。
 */
private const val TAG06C = "Sec06C"

@Serializable
private sealed interface Sec06CRoute : NavKey {
    @Serializable
    data object NoteList : Sec06CRoute

    @Serializable
    data class NoteDetail(
        val noteId: Long,
    ) : Sec06CRoute
}

private data class NoteRecord(
    val id: Long,
    val title: String,
    val content: String
)

private interface NoteRepository {
    suspend fun getNote(noteId: Long): NoteRecord?
}

private class FakeNoteRepository : NoteRepository {
    private val notes = mapOf(
        101L to NoteRecord(
            id = 101L,
            title = "学习 Navigation 3",
            content = "整理 NavKey、NavEntry 与 ViewModel 的职责。",
        ),
        102L to NoteRecord(
            id = 102L,
            title = "Calendar Note",
            content = "设计日历首页与当天详情页。",
        ),
        103L to NoteRecord(
            id = 103L,
            title = "Room 实战",
            content = "之后让 Repository 从 Room 提供真正的数据流。",
        )
    )

    override suspend fun getNote(noteId: Long): NoteRecord? {
        delay(1200.milliseconds)
        return notes[noteId]
    }
}

private data class NoteDetailUiState(
    val noteId: Long,
    val isLoading: Boolean = false,
    val note: NoteRecord? = null,
    val loadCount: Int = 0,
    val errorMessage: String? = null
)

private class NoteDetailViewModel(
    private val noteId: Long,
    private val repository: NoteRepository
) : ViewModel() {

    val instanceId = System.identityHashCode(this)

    private val _uiState = MutableStateFlow(NoteDetailUiState(noteId = noteId))
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    init {
        log(TAG06C, "ViewModel CREATE | noteId=$noteId | vm=$instanceId")
        loadNote()
    }

    fun reload() {
        if (_uiState.value.isLoading) return
        loadNote()
    }

    private fun loadNote() {
        viewModelScope.launch {
            val nextLoadCount = _uiState.value.loadCount + 1
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }
            log(TAG06C, "LOAD START | noteId=$noteId | vm=$instanceId | count=$nextLoadCount")
            val note = repository.getNote(noteId)
            if (note != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        note = note,
                        loadCount = nextLoadCount
                    )
                }
                log(TAG06C, "LOAD SUCCESS | noteId=$noteId | vm=$instanceId | count=$nextLoadCount")
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadCount = nextLoadCount,
                        errorMessage = "找不到 noteId=$noteId"
                    )
                }
            }
        }
    }

    override fun onCleared() {
        log(TAG06C, "ViewModel CLEARED | noteId=$noteId | vm=$instanceId")
    }

    class Factory(
        private val noteId: Long,
        private val repository: NoteRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NoteDetailViewModel(
                noteId = noteId,
                repository = repository
            ) as T
        }
    }
}

@Composable
fun RouteArgumentsWithViewModelDemo() {
    val backStack = rememberNavBackStack(Sec06CRoute.NoteList)
    val repository = remember { FakeNoteRepository() }

    Scaffold { innerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(innerPadding),
            onBack = {
                val removedKey = backStack.removeLastOrNull()
                log(TAG06C, "POP -> $removedKey | backStack=${backStack.toDebugString()}")
            },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = entryProvider {
                entry<Sec06CRoute.NoteList> {
                    NoteListScreen(
                        onOpenNote = { noteId ->
                            val key = Sec06CRoute.NoteDetail(noteId = noteId)
                            backStack.add(key)
                            log(TAG06C, "PUSH -> $key | backStack=${backStack.toDebugString()}")
                        }
                    )
                }
                entry<Sec06CRoute.NoteDetail> { key ->
                    NoteDetailRoute(
                        noteId = key.noteId,
                        repository = repository,
                        onOpenNote = { noteId ->
                            val key = Sec06CRoute.NoteDetail(noteId = noteId)
                            backStack.add(key)
                            log(TAG06C, "PUSH -> $key | backStack=${backStack.toDebugString()}")
                        },
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
            }
        )
    }
}

@Composable
private fun NoteListScreen(
    onOpenNote: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Note List")

        HorizontalDivider()

        Button(
            onClick = { onOpenNote(101L) }, // 直接硬编码啊
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "打开 Note 101")
        }

        Button(
            onClick = { onOpenNote(102L) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "打开 Note 102")
        }

        Button(
            onClick = { onOpenNote(103L) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "打开 Note 103")
        }
    }
}

@Composable
private fun NoteDetailRoute(
    noteId: Long,
    repository: NoteRepository, // 1.1 为什么不去问 viewModel 上传事件？UI拿仓库直接操作？
    onOpenNote: (Long) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: NoteDetailViewModel = viewModel( // 1.2 好吧，就当作是模拟 Hilt 依赖注入自动生成了
        // 1.2 可是，为什么要把仓库传进来，不直接在这里创建 fake 仓库？
        /* 因为那只是把问题藏得更深。Composable 不应该负责随手构建 Repository；
        否则它开始承担依赖创建、生命周期和测试替换职责。
        这个 Demo 把 FakeNoteRepository 放在外层 remember，
        只是为了模拟“应用已经有一个 Repository 实例”，然后手工完成 Factory 接线。
        正式项目换 Hilt 后，这段人工接线就消失了。*/
        factory = NoteDetailViewModel.Factory(
            noteId = noteId,
            repository = repository
        )
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NoteDetailScreen(
        uiState = uiState,
        viewModelInstanceId = viewModel.instanceId,
        onReload = viewModel::reload,
        onOpenNote = onOpenNote, // 1.3 一部分事件委托给 ViewModel，一部分事件委托给 Navigation
        onBack = onBack,
    )
}

@Composable
private fun NoteDetailScreen(
    uiState: NoteDetailUiState,
    viewModelInstanceId: Int,
    onReload: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Note Detail")

        Text(
            text = "noteId = ${uiState.noteId}\n" +
                    "ViewModel = $viewModelInstanceId\n" +
                    "加载次数 = ${uiState.loadCount}"
        )

        HorizontalDivider()

        when {
            uiState.isLoading && uiState.note == null -> {
                CircularProgressIndicator()
                Text(text = "正在加载 Note ${uiState.noteId}...")
            }

            uiState.errorMessage != null -> {
                Text(text = uiState.errorMessage)
            }

            uiState.note != null -> {
                Text(text = "标题：${uiState.note.title}")
                Text(text = "正文：${uiState.note.content}")
                if (uiState.isLoading) {
                    Text(text = "正在重新加载...")
                }
            }
        }

        HorizontalDivider()

        Button(
            onClick = onReload,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading,
        ) {
            Text(text = "重新加载当前 Note")
        }

        if (uiState.noteId != 102L) {
            Button(
                onClick = { onOpenNote(102L) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "继续打开 Note 102")
            }
        }

        if (uiState.noteId != 103L) {
            Button(
                onClick = { onOpenNote(103L) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "继续打开 Note 103")
            }
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "返回上一页")
        }
    }
}

private fun List<NavKey>.toDebugString(): String {
    return mapIndexed { index, key ->
        "[$index] $key"
    }.joinToString()
}

@PhonePreviews
@Composable
private fun NoteListScreenPreview() {
    CourseComposeTheme {
        Surface {
            NoteListScreen(onOpenNote = {})
        }
    }
}

@PhonePreviews
@Composable
private fun NoteDetailScreenPreview() {
    CourseComposeTheme {
        Surface {
            NoteDetailScreen(
                uiState = NoteDetailUiState(
                    noteId = 100L,
                    isLoading = true,
                    note = NoteRecord(
                        id = 101L,
                        title = "学习 Navigation 3",
                        content = "整理 NavKey、NavEntry 与 ViewModel 的职责。",
                    ),
                    loadCount = 1,
                    errorMessage = null,
                ),
                viewModelInstanceId = 12345,
                onReload = {},
                onOpenNote = {},
                onBack = {}
            )
        }
    }
}
