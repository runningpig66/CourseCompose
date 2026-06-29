package com.runningpig66.coursecompose.ch04_state

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
 * @date 2026-06-30
 * @time 3:35
 *
 * notes: Sec12_rememberCoroutineScope.md
 */
@Composable
fun Sec12A_ScopeInCallback() {
    val scope = rememberCoroutineScope()

    Scaffold { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Button(onClick = {
                scope.launch {
                    performNetworkRequest()
                    println("请求完成，更新 UI 状态")
                }
            }) {
                Text("发起网络请求")
            }
        }
    }
}

suspend fun performNetworkRequest() {
    println("开始请求...")
    delay(3000.milliseconds)
}

@PhonePreviews
@Composable
fun Sec12A_ScopeInCallbackPreview() {
    CourseComposeTheme {
        Sec12A_ScopeInCallback()
    }
}
