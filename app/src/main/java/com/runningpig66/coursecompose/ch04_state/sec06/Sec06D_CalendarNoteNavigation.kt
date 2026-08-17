package com.runningpig66.coursecompose.ch04_state.sec06

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEffect
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable

/**
 * @author runningpig66
 * @date 2026/08/16 周日
 * @time 3:02
 *
 * Calendar Note 导航原型与 Navigation Result 练习。
 *
 * 模拟 Calendar → Detail → Edit → Picker 的页面流程，
 * 区分前向 Route 参数、页面自身 UiState 与子页面返回 Result，
 * 并练习 Navigation 3 的 ResultEventBus / ResultEffect 通信模型。
 */
private const val TAG06D = "Sec06D"

@Serializable
private sealed interface Sec06DRoute : NavKey {

    @Serializable
    data object Calendar : Sec06DRoute

    @Serializable
    data class DayDetail(
        val year: Int,
        val month: Int,
        val day: Int,
    ) : Sec06DRoute

    @Serializable
    data class EditRecord(
        val recordId: Long,
    ) : Sec06DRoute

    @Serializable
    data class ColorPicker(
        val recordId: Long,
        val initialColor: RecordColor,
    ) : Sec06DRoute
}

@Serializable
private enum class RecordColor {
    RED,
    GREEN,
    BLUE,
    YELLOW,
}

private data class ColorPickerResult(
    val color: RecordColor,
)

private data class EditRecordUiState(
    val recordId: Long,
    val title: String,
    val color: RecordColor,
)

private class EditRecordViewModel(
    private val recordId: Long,
) : ViewModel() {
    val instanceId = System.identityHashCode(this)

    private val _uiState = MutableStateFlow(
        EditRecordUiState(
            recordId = recordId,
            title = "Record $recordId",
            color = RecordColor.BLUE,
        )
    )
    val uiState: StateFlow<EditRecordUiState> = _uiState.asStateFlow()

    init {
        log(TAG06D, "EditViewModel CREATE | recordId=$recordId | vm=$instanceId")
    }

    fun onColorSelected(color: RecordColor) {
        log(TAG06D, "ViewModel RECEIVE COLOR | recordId=$recordId | color=$color | vm=$instanceId")

        _uiState.update {
            it.copy(color = color)
        }
    }

    override fun onCleared() {
        log(TAG06D, "EditViewModel CLEARED | recordId=$recordId | vm=$instanceId")
    }

    class Factory(private val recordId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditRecordViewModel(recordId = recordId) as T
        }
    }
}

@Composable
fun CalendarNoteNavigationPrototype() {
    val backStack = rememberNavBackStack(Sec06DRoute.Calendar)

    Scaffold { innerPadding ->
        NavDisplay(
            modifier = Modifier.padding(innerPadding),
            backStack = backStack,
            onBack = {
                val removedKey = backStack.removeLastOrNull()
                log(TAG06D, "SYSTEM POP -> $removedKey | backStack=${backStack.toDebugString()}")
            },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
                rememberResultEventBusNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<Sec06DRoute.Calendar> {
                    CalendarPrototypeScreen(
                        onOpenDay = { year, month, day ->
                            val key = Sec06DRoute.DayDetail(
                                year = year,
                                month = month,
                                day = day
                            )
                            backStack.add(key)
                            log(TAG06D, "PUSH -> $key | backStack=${backStack.toDebugString()}")
                        }
                    )
                }
                entry<Sec06DRoute.DayDetail> { key ->
                    DayDetailPrototypeScreen(
                        year = key.year,
                        month = key.month,
                        day = key.day,
                        onEditRecord = { recordId ->
                            val editKey = Sec06DRoute.EditRecord(recordId)
                            backStack.add(editKey)
                            log(TAG06D, "PUSH -> $editKey | backStack=${backStack.toDebugString()}")
                        },
                        onBack = {
                            val removedKey = backStack.removeLastOrNull()
                            log(TAG06D, "UI POP -> $removedKey | backStack=${backStack.toDebugString()}")
                        }
                    )
                }
                entry<Sec06DRoute.EditRecord> { key ->
                    EditRecordRoute(
                        recordId = key.recordId,
                        onOpenColorPicker = { currentColor ->
                            val pickerKey = Sec06DRoute.ColorPicker(
                                recordId = key.recordId,
                                initialColor = currentColor
                            )
                            backStack.add(pickerKey)
                            log(TAG06D, "PUSH -> $pickerKey | backStack=${backStack.toDebugString()}")
                        },
                        onBack = {
                            val removedKey = backStack.removeLastOrNull()
                            log(TAG06D, "UI POP -> $removedKey | backStack=${backStack.toDebugString()}")
                        }
                    )
                }
                entry<Sec06DRoute.ColorPicker> { key ->
                    val resultBus = LocalResultEventBus.current

                    ColorPickerScreen(
                        recordId = key.recordId,
                        initialColor = key.initialColor,
                        onColorSelected = { selectedColor ->
                            val result = ColorPickerResult(color = selectedColor)
                            log(TAG06D, "SEND RESULT -> $result")
                            resultBus.sendResult(result = result)

                            val removedKey = backStack.removeLastOrNull()
                            log(TAG06D, "POP AFTER RESULT -> $removedKey | backStack=${backStack.toDebugString()}")
                        },
                        onCancel = {
                            val removedKey = backStack.removeLastOrNull()
                            log(TAG06D, "CANCEL -> $removedKey | no result")
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun CalendarPrototypeScreen(
    onOpenDay: (Int, Int, Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Calendar")

        Text(text = "选择日期，进入当天记录")

        HorizontalDivider()

        Button(
            onClick = { onOpenDay(2026, 8, 14) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "2026-08-14")
        }

        Button(
            onClick = { onOpenDay(2026, 8, 15) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "2026-08-15")
        }
    }
}

@Composable
private fun DayDetailPrototypeScreen(
    year: Int,
    month: Int,
    day: Int,
    onEditRecord: (Long) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Day Detail")

        Text(text = "日期：$year-$month-$day")

        HorizontalDivider()

        Button(
            onClick = { onEditRecord(101L) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "编辑 Record 101")
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onEditRecord(102L) },
        ) {
            Text(text = "编辑 Record 102")
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "返回")
        }
    }
}

@Composable
private fun EditRecordRoute(
    recordId: Long,
    onOpenColorPicker: (RecordColor) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel: EditRecordViewModel = viewModel(
        factory = EditRecordViewModel.Factory(
            recordId = recordId
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ResultEffect<ColorPickerResult> { result ->
        log(
            TAG06D,
            "RESULT EFFECT -> " +
                    "recordId=$recordId | " +
                    "result=$result | " +
                    "vm=${viewModel.instanceId}"
        )
        viewModel.onColorSelected(color = result.color)
    }

    EditRecordScreen(
        uiState = uiState,
        viewModelInstanceId = viewModel.instanceId,
        onChooseColor = {
            onOpenColorPicker(uiState.color)
        },
        onBack = onBack,
    )
}

@Composable
private fun EditRecordScreen(
    uiState: EditRecordUiState,
    viewModelInstanceId: Int,
    onChooseColor: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Edit Record")

        Text(
            text = "recordId = ${uiState.recordId}\n" +
                    "ViewModel = $viewModelInstanceId"
        )

        HorizontalDivider()

        Text(text = "标题：${uiState.title}")

        Text(text = "当前颜色：${uiState.color}")

        Button(
            onClick = onChooseColor,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "选择颜色")
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "返回")
        }
    }
}

@Composable
private fun ColorPickerScreen(
    recordId: Long,
    initialColor: RecordColor,
    onColorSelected: (RecordColor) -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Color Picker")

        Text(text = "recordId = $recordId")

        Text(text = "进入时颜色：$initialColor")

        HorizontalDivider()

        RecordColor.entries.forEach { color ->
            Button(
                onClick = { onColorSelected(color) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (color == initialColor) {
                        "$color（当前）"
                    } else {
                        color.name
                    }
                )
            }
        }

        Button(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "取消，不返回结果")
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
fun CalendarPrototypeScreenPreview() {
    CourseComposeTheme {
        Surface {
            CalendarPrototypeScreen(
                onOpenDay = { _, _, _ -> }
            )
        }
    }
}

@PhonePreviews
@Composable
fun DayDetailPrototypeScreenPreview() {
    CourseComposeTheme {
        Surface {
            DayDetailPrototypeScreen(
                year = 2025,
                month = 8,
                day = 26,
                onEditRecord = {},
                onBack = {}
            )
        }
    }
}

@PhonePreviews
@Composable
fun EditRecordScreenPreview() {
    CourseComposeTheme {
        Surface {
            EditRecordScreen(
                uiState = EditRecordUiState(
                    recordId = 108,
                    title = "Record 108",
                    color = RecordColor.BLUE,
                ),
                viewModelInstanceId = 12345,
                onChooseColor = {},
                onBack = {}
            )
        }
    }
}

@PhonePreviews
@Composable
fun ColorPickerScreenPreview() {
    CourseComposeTheme {
        Surface {
            ColorPickerScreen(
                recordId = 108L,
                initialColor = RecordColor.BLUE,
                onColorSelected = {},
                onCancel = {}
            )
        }
    }
}
