package com.runningpig66.coursecompose.ch04_state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 07-09 星期四
 * @time 6:39
 *
 * 基础防线验证：对照演示 remember 与 rememberSaveable 在面临 Android 系统进程死亡（Process Death）时的状态保留差异。
 *
 * notes: Sec03_rememberSaveable().md
 */
@Composable
fun Sec02E_ProcessDeathTest() {
    // 对照组 A：使用普通 remember 的数据仅保存在 Compose 的插槽表（内存）中
    var normalText by remember { mutableStateOf("") }

    // 对照组 B：使用 rememberSaveable 的数据会同步至系统的 SavedStateRegistry (最终存入 Bundle)
    var saveableText by rememberSaveable { mutableStateOf("") }

    Scaffold { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("验证步骤：\n1. 在两个输入框中输入内容。\n2. 将应用退至后台（按 Home 键）。\n3. 通过 ADB 或 Logcat 强制杀死进程。\n4. 从多任务列表（Recents）中重新打开应用。")

            OutlinedTextField(
                value = normalText,
                onValueChange = { normalText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("普通 remember (进程死亡后丢失)") }
            )

            OutlinedTextField(
                value = saveableText,
                onValueChange = { saveableText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("rememberSaveable (进程死亡后保留)") }
            )
        }
    }
}

@PhonePreviews
@Composable
fun Sec02E_ProcessDeathTestPreview() {
    CourseComposeTheme {
        Sec02E_ProcessDeathTest()
    }
}
