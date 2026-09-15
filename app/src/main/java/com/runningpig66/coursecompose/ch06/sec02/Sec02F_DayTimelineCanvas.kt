package com.runningpig66.coursecompose.ch06.sec02

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import kotlin.math.roundToInt

/**
 * @author runningpig66
 * @date 2026/09/13
 * @time 23:48
 */
private data class TimelineEventUi(
    val title: String,
    val startMinute: Int,
    val endMinute: Int,
    val color: Color
)

@Composable
fun Sec02E_DayTimelineCanvas() {
    // 为了让绘制逻辑本身足够干净，我们显示 08:00 ~ 18:00 共 10 小时
    val timelineStartMinute = 8 * 60
    val timelineEndMinute = 18 * 60

    var currentMinute by remember { mutableIntStateOf(10 * 60 + 30) }

    // 三个事件块模拟真实日程
    val events = listOf(
        TimelineEventUi(
            title = "Compose study",
            startMinute = 9 * 60,
            endMinute = 10 * 60 + 15,
            color = Color(0xFF42A5F5)
        ),
        TimelineEventUi(
            title = "Lunch",
            startMinute = 11 * 60 + 30,
            endMinute = 12 * 60 + 20,
            color = Color(0xFFFFB74D)
        ),
        TimelineEventUi(
            title = "App development",
            startMinute = 14 * 60 + 10,
            endMinute = 16 * 60,
            color = Color(0xFF66BB6A)
        )
    )

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Current time: ${formatMinuteOfDay(currentMinute)}")

            Slider(
                value = currentMinute.toFloat(),
                onValueChange = { currentMinute = it.roundToInt() },
                valueRange = timelineStartMinute.toFloat()..timelineEndMinute.toFloat(),
                steps = 19
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(580.dp)
            ) {
                val startHour = timelineStartMinute / 60
                val endHour = timelineEndMinute / 60
                val totalMinutes = timelineEndMinute - timelineStartMinute

                val timeAxisWidth = 32.dp.toPx()
                val contentLeft = timeAxisWidth + 12.dp.toPx()

                val eventLeft = contentLeft + 8.dp.toPx()
                val eventRight = size.width - 12.dp.toPx()

                fun minuteToY(minute: Int): Float {
                    val minuteFromStart = minute - timelineStartMinute
                    val progress = minuteFromStart.toFloat() / totalMinutes.toFloat()
                    return progress * size.height
                }

                // ---------- Background ----------
                drawRect(color = Color(0xFFB9FEBC))

                // ---------- Time axis ----------
                drawLine(
                    color = Color(0xFF2196F3),
                    start = Offset(x = contentLeft, y = 0f),
                    end = Offset(x = contentLeft, y = size.height),
                    strokeWidth = 1.dp.toPx()
                )

                // ---------- Hour grid ----------
                for (hour in startHour..endHour) {
                    val hourMinute = hour * 60
                    val y = minuteToY(hourMinute)

                    drawLine(
                        color = Color(0xFF337EE0),
                        start = Offset(x = contentLeft, y = y),
                        end = Offset(x = size.width, y = y),
                        strokeWidth = 1.dp.toPx()
                    )

                    drawLine(
                        color = Color(0xFF757575),
                        start = Offset(x = contentLeft - 8.dp.toPx(), y = y),
                        end = Offset(x = contentLeft, y = y),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // ---------- Half-hour grid ----------
                for (hour in startHour until endHour) {
                    val halfHourMinute = hour * 60 + 30
                    val y = minuteToY(halfHourMinute)

                    drawLine(
                        color = Color(0xFF80BEF7),
                        start = Offset(x = contentLeft, y = y),
                        end = Offset(x = size.width - 16.dp.toPx(), y = y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // ---------- Event blocks ----------
                events.forEach { event ->
                    val visibleStart = maxOf(event.startMinute, timelineStartMinute)
                    val visibleEnd = minOf(event.endMinute, timelineEndMinute)

                    // 可以直接过滤 18:30 → 20:00 这种完全不在当前时间轴里的事件
                    if (visibleStart < visibleEnd) {
                        val eventTop = minuteToY(visibleStart) + 3.dp.toPx()
                        val eventBottom = minuteToY(visibleEnd) - 3.dp.toPx()
                        val eventHeight = eventBottom - eventTop

                        drawRoundRect(
                            color = event.color,
                            topLeft = Offset(x = eventLeft, y = eventTop),
                            size = Size(
                                width = eventRight - eventLeft,
                                height = eventHeight
                            ),
                            cornerRadius = CornerRadius(8.dp.toPx()),
                            alpha = 0.78f
                        )

                        drawRoundRect(
                            color = event.color,
                            topLeft = Offset(x = eventLeft, y = eventTop),
                            size = Size(
                                width = 5.dp.toPx(),
                                height = eventHeight
                            ),
                            cornerRadius = CornerRadius(3.dp.toPx())
                        )
                    }
                }

                // ---------- Current time ----------
                if (currentMinute in timelineStartMinute..timelineEndMinute) {
                    val currentY = minuteToY(currentMinute)

                    drawLine(
                        color = Color(0xFFE53935),
                        start = Offset(x = contentLeft, y = currentY),
                        end = Offset(x = size.width, y = currentY),
                        strokeWidth = 2.dp.toPx()
                    )

                    drawCircle(
                        color = Color(0xFFE53935),
                        radius = 5.dp.toPx(),
                        center = Offset(x = contentLeft, y = currentY)
                    )
                }
            }

            events.forEach { event ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = event.color,
                                shape = CircleShape
                            )
                    )

                    Text(
                        text = formatMinuteOfDay(event.startMinute) +
                                " - " +
                                formatMinuteOfDay(event.endMinute) +
                                "  ${event.title}"
                    )
                }
            }
        }
    }
}

private fun formatMinuteOfDay(minute: Int): String {
    val hour = minute / 60
    val minutePart = minute % 60
    return "%02d:%02d".format(hour, minutePart)
}

@PhonePreviews
@Composable
fun Sec02E_DayTimelineCanvasPreview() {
    CourseComposeTheme {
        Sec02E_DayTimelineCanvas()
    }
}
