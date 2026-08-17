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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log
import kotlinx.serialization.Serializable

/**
 * @author runningpig66
 * @date 2026/08/14 周五
 * @time 21:40
 *
 * Navigation 3 返回栈与 NavKey 行为实验。
 *
 * 验证同一路由类型可多次入栈、相等的 NavKey 也可重复存在，
 * 并观察 push / pop、Key 值语义以及配置变化后的返回栈恢复。
 */
private const val TAG06B = "Sec06B"

@Serializable
private sealed interface Sec06BRoute : NavKey {
    @Serializable
    data object Calendar : Sec06BRoute

    @Serializable
    data class DayDetail(
        val year: Int,
        val month: Int,
        val day: Int,
    ) : Sec06BRoute
}

private data class Sec06BDemoDay(
    val year: Int,
    val month: Int,
    val day: Int,
)

private val sec06BDays = listOf(
    Sec06BDemoDay(2026, 8, 14),
    Sec06BDemoDay(2026, 8, 15),
    Sec06BDemoDay(2026, 9, 1),
)

@Composable
fun RouteBackStackExperiment() {
    val backStack = rememberNavBackStack(Sec06BRoute.Calendar)

    /* 故意只用 remember，而不是 rememberSaveable / ViewModel。
    它代表“与 Navigation 无关的一份普通页面状态”，后面专门用来和 rememberNavBackStack 的恢复能力做对照。*/
    var calendarRevision by remember { mutableIntStateOf(0) }

    Scaffold { innerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(innerPadding),
            onBack = {
                val removedKey = backStack.removeLastOrNull()
                log(TAG06B, "SYSTEM POP -> $removedKey | backStack = ${backStack.toDebugString()}")
            },
            entryProvider = entryProvider {
                entry<Sec06BRoute.Calendar> {
                    CalendarScreen(
                        calendarRevision = calendarRevision,
                        backStackText = backStack.toDebugString(),
                        onOpenDay = { day ->
                            val key = Sec06BRoute.DayDetail(
                                year = day.year,
                                month = day.month,
                                day = day.day
                            )
                            backStack.add(key)
                            log(TAG06B, "PUSH -> ${key.toIdentityString()} | backStack = ${backStack.toDebugString()}")
                        }
                    )
                }

                entry<Sec06BRoute.DayDetail> { key ->
                    DayDetailScreen(
                        key = key,
                        calendarRevision = calendarRevision,
                        backStackText = backStack.toDebugString(),
                        onOpenNextDay = {
                            val nextKey = key.nextDemoDay()
                            backStack.add(nextKey)
                            log(
                                TAG06B, "PUSH NEXT -> ${nextKey.toIdentityString()} | " +
                                        "backStack = ${backStack.toDebugString()}"
                            )
                        },
                        onRepeatSameKey = {
                            val duplicateKey = key.copy()
                            log(
                                TAG06B, "DUPLICATE TEST | " +
                                        "old=${key.toIdentityString()} | " +
                                        "new=${duplicateKey.toIdentityString()} | " +
                                        "old == new = ${key == duplicateKey} | " +
                                        "old === new = ${key === duplicateKey}"
                            )
                            backStack.add(duplicateKey)
                            log(TAG06B, "PUSH DUPLICATE -> backStack = ${backStack.toDebugString()}")
                        },
                        onChangeCalendarRevision = {
                            calendarRevision++
                            log(TAG06B, "calendarRevision -> $calendarRevision | currentKey = $key")
                        },
                        onBack = {
                            val removedKey = backStack.removeLastOrNull()
                            log(TAG06B, "UI POP -> $removedKey | backStack = ${backStack.toDebugString()}")
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun CalendarScreen(
    calendarRevision: Int,
    backStackText: String,
    onOpenDay: (Sec06BDemoDay) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Calendar")

        Text(text = "Calendar 普通状态 revision = $calendarRevision")

        HorizontalDivider()

        Text(text = "当前返回栈：\n$backStackText")

        HorizontalDivider()

        sec06BDays.forEach { day ->
            Button(
                onClick = { onOpenDay(day) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "打开 ${day.year}-${day.month}-${day.day}")
            }
        }
    }
}

@Composable
private fun DayDetailScreen(
    key: Sec06BRoute.DayDetail,
    calendarRevision: Int,
    backStackText: String,
    onOpenNextDay: () -> Unit,
    onRepeatSameKey: () -> Unit,
    onChangeCalendarRevision: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Day Detail")

        Text(text = "当前 Key：\n${key.toIdentityString()}")

        Text(text = "Calendar revision = $calendarRevision")

        HorizontalDivider()

        Text(text = "当前返回栈：\n$backStackText")

        HorizontalDivider()

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenNextDay,
        ) {
            Text(text = "继续进入下一个 DayDetail")
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onChangeCalendarRevision,
        ) {
            Text(text = "修改 Calendar 的普通状态 +1")
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onRepeatSameKey,
        ) {
            Text(text = "实验：再次压入完全相同的 Key")
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onBack,
        ) {
            Text(text = "Pop 当前 Key")
        }
    }
}

private fun Sec06BRoute.DayDetail.nextDemoDay(): Sec06BRoute.DayDetail {
    return when {
        year == 2026 && month == 8 && day == 14 ->
            Sec06BRoute.DayDetail(2026, 8, 15)

        year == 2026 && month == 8 && day == 15 ->
            Sec06BRoute.DayDetail(2026, 9, 1)

        else ->
            Sec06BRoute.DayDetail(2026, 8, 14)
    }
}

private fun List<NavKey>.toDebugString(): String {
    return mapIndexed { index, key ->
        "[$index] $key"
    }.joinToString(separator = "\n")
}

private fun Any.toIdentityString(): String {
    return "$this | " +
            "hashCode=${hashCode()} | " +
            "identity=${System.identityHashCode(this)}"
}

@PhonePreviews
@Composable
private fun CalendarScreenPreview() {
    CourseComposeTheme {
        Surface {
            CalendarScreen(
                calendarRevision = 100,
                backStackText = "test",
                onOpenDay = {}
            )
        }
    }
}

@PhonePreviews
@Composable
private fun DayDetailScreenPreview() {
    CourseComposeTheme {
        Surface {
            DayDetailScreen(
                key = Sec06BRoute.DayDetail(2026, 8, 14),
                calendarRevision = 100,
                backStackText = "test",
                onOpenNextDay = {},
                onRepeatSameKey = {},
                onChangeCalendarRevision = {},
                onBack = {}
            )
        }
    }
}
