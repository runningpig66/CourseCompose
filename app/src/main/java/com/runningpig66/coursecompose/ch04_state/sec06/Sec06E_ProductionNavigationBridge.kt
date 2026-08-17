package com.runningpig66.coursecompose.ch04_state.sec06

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.runningpig66.coursecompose.ui.utils.log
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author runningpig66
 * @date 2026/08/17 周一
 * @time 3:14
 *
 * Navigation 3 从教学 Demo 过渡到正式 App 架构的练习。
 *
 * 练习 Hilt Assisted Injection 将运行时路由参数传入 ViewModel，
 * 并探索大型 App 中按 feature 拆分 Route、Entry Builder 与 Navigation Host 的生产级组织方式。
 */
// Data
data class Record(
    val id: Long,
    val title: String,
    val amount: Long,
)

interface RecordRepository {
    fun observeRecord(recordId: Long): Flow<Record?>
}

// TODO 1 FakeRecordRepository 需要使用 @Singleton 标记吗？bindRecordRepository() 使用使用了该注解了呀
@Singleton
class FakeRecordRepository @Inject constructor() : RecordRepository {
    private val records = MutableStateFlow(
        mapOf(
            101L to Record(id = 101L, title = "午餐", amount = 1800L),
            102L to Record(id = 102L, title = "交通", amount = 420L),
        )
    )

    override fun observeRecord(recordId: Long): Flow<Record?> {
        return records.map { map -> map[recordId] }
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface RecordRepositoryModule {
    @Binds
    @Singleton
    fun bindRecordRepository(impl: FakeRecordRepository): RecordRepository
}

// Domain
class ObserveRecordUseCase @Inject constructor(
    private val repository: RecordRepository
) {
    operator fun invoke(recordId: Long): Flow<Record?> {
        return repository.observeRecord(recordId)
    }
}

// UI
sealed interface RecordDetailUiState {
    data class Loading(
        val recordId: Long
    ) : RecordDetailUiState

    data class Success(
        val record: Record
    ) : RecordDetailUiState

    data class NotFound(
        val recordId: Long
    ) : RecordDetailUiState
}

@HiltViewModel(assistedFactory = RecordDetailViewModel.Factory::class)
class RecordDetailViewModel @AssistedInject constructor(
    @Assisted
    private val recordId: Long,
    private val observeRecord: ObserveRecordUseCase
) : ViewModel() {
    val instanceId = System.identityHashCode(this)

    val uiState: StateFlow<RecordDetailUiState> = observeRecord(recordId)
        .map { record ->
            if (record == null) {
                RecordDetailUiState.NotFound(recordId = recordId)
            } else {
                RecordDetailUiState.Success(record = record)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RecordDetailUiState.Loading(recordId = recordId)
        )

    init {
        log("RecordVM", "CREATE | recordId=$recordId | vm=$instanceId")
    }

    override fun onCleared() {
        log("RecordVM", "CLEARED | recordId=$recordId | vm=$instanceId")
    }

    @AssistedFactory
    interface Factory {
        fun create(recordId: Long): RecordDetailViewModel
    }
}

@Serializable
data class RecordDetailKey(val recordId: Long) : NavKey

@Composable
private fun RecordDetailRoute(
    recordId: Long,
    onEdit: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel = hiltViewModel<RecordDetailViewModel, RecordDetailViewModel.Factory>(
        creationCallback = { factory ->
            factory.create(recordId = recordId)
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    /* TODO
    RecordDetailScreen(
        uiState = uiState,
        onEdit = onEdit,
        onBack = onBack,
    ) */
}

class AppNavigator(
    private val navigateAction: (NavKey) -> Unit,
    private val backAction: () -> Unit
) {
    fun navigate(key: NavKey) {
        navigateAction(key)
    }

    fun back() {
        backAction()
    }
}
