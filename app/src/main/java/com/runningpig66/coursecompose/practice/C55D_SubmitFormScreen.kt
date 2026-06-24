package com.runningpig66.coursecompose.practice

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author runningpig66
 * @date 2026-06-24
 * @time 5:56
 *
 * 避坑指南：演示将 LaunchedEffect 违规用于普通回调事件（如 onClick）时的典型编译器报错。
 * 明确了副作用 API 的使用边界，并引出 rememberCoroutineScope 作为点击事件开协程的正确破局方案。
 */
@Composable
fun C55D_SubmitFormScreen() {
    Scaffold { innerPadding ->
        Box(
            Modifier
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            //IncorrectSubmit()
            CorrectSubmit()
        }
    }
}

/*@Composable
fun IncorrectSubmit() {
    var isLoading by remember { mutableStateOf(false) }

    Button(onClick = {
        isLoading = true
        // 试图在点击事件里直接使用 LaunchedEffect
        // Warning: @Composable invocations can only happen from the context of a @Composable function
        LaunchedEffect(Unit) {
            delay(1000.milliseconds)
            isLoading = false
        }
    }) {
        Text(text = if (isLoading) "正在提交..." else "点击提交")
    }
}*/

@Composable
fun CorrectSubmit() {
    var isLoading by remember { mutableStateOf(false) }

    // 在 Composable 环境中，提前申请一个与当前组件生命周期绑定的协程作用域
    val scope = rememberCoroutineScope()

    Button(onClick = {
        // 正确做法：在普通回调中，使用提前申请好的 scope 来 launch 协程
        scope.launch {
            isLoading = true
            // 模拟耗时网络提交
            delay(1000.milliseconds)
            isLoading = false
        }
    }) {
        Text(text = if (isLoading) "正在提交..." else "点击提交")
    }
}

@PhonePreviews
@Composable
fun C55D_SubmitFormScreenPreview() {
    CourseComposeTheme {
        C55D_SubmitFormScreen()
    }
}
