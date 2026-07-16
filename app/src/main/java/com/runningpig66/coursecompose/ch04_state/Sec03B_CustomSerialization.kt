package com.runningpig66.coursecompose.ch04_state

import android.os.Parcelable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import kotlinx.parcelize.Parcelize

/**
 * @author runningpig66
 * @date 07-09 星期四
 * @time 7:
 *
 * 自定义序列化实战：演示如何通过 @Parcelize、listSaver 以及自定义 Saver 接口，打破系统基础类型限制，安全保存复杂数据对象。
 *
 * notes: Sec03_rememberSaveable().md
 */
@Composable
fun Sec03B_CustomSerialization() {
    Scaffold { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Sec03B_ParcelizeSaver
            Sec03B_ParcelizeSaver()
            // 2. Sec03C_ListSaver
            Sec03B_ListSaver()
            // 3. Sec03C_CustomSaver
            Sec03B_CustomSaver()
        }
    }
}

// 1. Sec03B_ParcelizeSaver
@Parcelize
data class UserProfile(
    val id: String,
    val username: String,
    val age: Int
) : Parcelable

@Composable
fun Sec03B_ParcelizeSaver() {
    // 方案 A：直接使用，autoSaver 能够自动识别 Parcelable 接口
    var userProfile by rememberSaveable {
        mutableStateOf(UserProfile("1001", "runningpig", 30))
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "当前用户: ${userProfile.username}, 年龄: ${userProfile.age}",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = {
            // 状态变更后，进程死亡重建时，新的属性会被完整保留
            userProfile = userProfile.copy(age = userProfile.age + 1)
        }) { Text("生日快乐") }
    }
}

// 2. Sec03C_ListSaver
// 假设这是第三方库的实体类，无法添加 @Parcelize 注解
data class ThirdPartyTask(
    val title: String,
    val isCompleted: Boolean
)

// 手动定义一个 Saver，指明：1. 原始类型 (ThirdPartyTask); 2. 存储类型 (Any，通常为基础类型的 List)
val TaskListSaver = listSaver<ThirdPartyTask, Any>(
    // 拆解：将复杂对象转为一个基础类型的 List
    save = { task -> listOf(task.title, task.isCompleted) },
    // 组装：从 List 的指定索引中读取数据，重新构造对象
    restore = { restoredList ->
        ThirdPartyTask(
            title = restoredList[0] as String,
            isCompleted = restoredList[1] as Boolean
        )
    }
)

@Composable
fun Sec03B_ListSaver() {
    // 注意：当使用 mutableStateOf 时，必须将自定义的 Saver 传给 stateSaver 参数
    // 如果不使用 mutableStateOf，而是普通的 rememberSaveable，则传给 saver 参数
    var task by rememberSaveable(stateSaver = TaskListSaver) {
        mutableStateOf(ThirdPartyTask("Learn Compose", false))
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "任务: ${task.title}, 状态: ${if (task.isCompleted) "已完成" else "未完成"}",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = {
            task = task.copy(isCompleted = !task.isCompleted)
        }) { Text("切换状态") }
    }
}

// 3. Sec03C_CustomSaver
data class Coordinate(val x: Float, val y: Float)
data class Location(val name: String, val coordinate: Coordinate)

// 直接定义一个底层 Saver 对象
val LocationSaver = Saver<Location, String>(
    // 拆解：将其转为系统支持的 String 类型，例如转为特定格式的字符串（或 JSON 字符串）
    save = { location -> "${location.name},${location.coordinate.x},${location.coordinate.y}" },
    // 组装：解析字符串并重建嵌套对象
    restore = { savedString ->
        val parts = savedString.split(",")
        if (parts.size == 3) {
            Location(
                name = parts[0],
                coordinate = Coordinate(parts[1].toFloat(), parts[2].toFloat())
            )
        } else {
            null // 解析失败时返回 null
        }
    }
)

@Composable
fun Sec03B_CustomSaver() {
    var location by rememberSaveable(stateSaver = LocationSaver) {
        mutableStateOf(Location("Base", Coordinate(100f, 200f)))
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "位置：${location.name} (${location.coordinate.x}, ${location.coordinate.y})")
        Button(onClick = {
            location = location.copy(coordinate = Coordinate(location.coordinate.x + 10f, location.coordinate.y))
        }) { Text("向右移动") }
    }
}

@PhonePreviews
@Composable
fun Sec03B_CustomSerializationPreview() {
    CourseComposeTheme {
        Sec03B_CustomSerialization()
    }
}
