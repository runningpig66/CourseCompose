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
 * @time 16:41
 *
 * Navigation 3 类型安全路由参数基础练习。
 *
 * 使用 NavKey data class 携带年月日等简单参数，
 * 验证参数随 Route 入栈，并在对应 entry 中通过强类型 Key 读取。
 */
private const val TAG06A = "Sec06A"

@Serializable
private sealed interface Sec06ARoute : NavKey {
    @Serializable
    data object Calendar : Sec06ARoute

    @Serializable
    data class DayDetail(
        val year: Int,
        val month: Int,
        val day: Int,
    ) : Sec06ARoute
}

private data class DemoDay(
    val year: Int,
    val month: Int,
    val day: Int,
    val noteCount: Int,
)

private val demoDays = listOf(
    DemoDay(year = 2026, month = 8, day = 14, noteCount = 3),
    DemoDay(year = 2026, month = 8, day = 15, noteCount = 1),
    DemoDay(year = 2026, month = 9, day = 1, noteCount = 5),
)

@Composable
fun TypeSafeRouteArgumentsDemo() {
    val backStack = rememberNavBackStack(Sec06ARoute.Calendar)

    Scaffold { innerPadding ->
        NavDisplay(
            modifier = Modifier.padding(innerPadding),
            backStack = backStack,
            onBack = {
                val removedKey = backStack.removeLastOrNull()
                log(TAG06A, "POP -> $removedKey | backStack = ${backStack.joinToString(" -> ")}")
            },
            entryProvider = entryProvider {
                entry<Sec06ARoute.Calendar> {
                    CalendarScreen(
                        backStackText = backStack.joinToString(" -> "),
                        onDayClick = { day ->
                            val key = Sec06ARoute.DayDetail(year = day.year, month = day.month, day = day.day)
                            backStack.add(key)
                            log(TAG06A, "PUSH -> $key | backStack = ${backStack.joinToString(" -> ")}")
                        }
                    )
                }

                entry<Sec06ARoute.DayDetail> { key ->
                    DayDetailScreen(
                        key = key,
                        backStackText = backStack.joinToString(" -> "),
                        onBack = {
                            val removedKey = backStack.removeLastOrNull()
                            log(TAG06A, "UI POP -> $removedKey | backStack = ${backStack.joinToString(" -> ")}")
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun CalendarScreen(
    backStackText: String,
    onDayClick: (DemoDay) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Calendar")
        Text(text = "当前返回栈：\n$backStackText")

        HorizontalDivider()

        demoDays.forEach { day ->
            Button(
                onClick = { onDayClick(day) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${day.year}-${day.month}-${day.day}  (${day.noteCount} 条记录)"
                )
            }
        }
    }
}

@Composable
private fun DayDetailScreen(
    key: Sec06ARoute.DayDetail,
    backStackText: String,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Day Detail")

        Text(text = "收到的 NavKey：\n$key")

        Text(
            text = "year = ${key.year}\n" +
                    "month = ${key.month}\n" +
                    "day = ${key.day}"
        )

        HorizontalDivider()

        Text(text = "当前返回栈：\n$backStackText")

        Button(onClick = onBack) {
            Text("返回 Calendar")
        }
    }
}

@PhonePreviews
@Composable
private fun CalendarScreenPreview() {
    CourseComposeTheme {
        Surface {
            CalendarScreen(
                backStackText = "Calendar",
                onDayClick = {}
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
                key = Sec06ARoute.DayDetail(year = 2026, month = 8, day = 14),
                backStackText = "Calendar -> DayDetail(year=2026, month=8, day=14)",
                onBack = {}
            )
        }
    }
}

/*@PhonePreviews
@Composable
private fun Sec06APreview() {
    CourseComposeTheme {
        Surface {
            TypeSafeRouteArgumentsDemo()
        }
    }
}*/
