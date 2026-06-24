package com.runningpig66.coursecompose.practice

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author runningpig66
 * @date 2026-06-24
 * @time 1:53
 *
 * 演示 LaunchedEffect 的基础用法与安全的生命周期绑定机制。
 * 验证了传入 Unit 时，协程会在组件成功上屏后启动，并在组件被销毁时自动执行 cancel。
 */
private const val TAG55 = "LaunchedTest"

@Composable
fun C55B_LaunchedTest() {
    Scaffold { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            CorrectSplashScreen()
        }
    }
}

@Composable
fun CorrectSplashScreen() {
    var timeLeft by remember { mutableIntStateOf(3) }

    // 1. 传入 Unit 作为 Key，表示这个协程与当前组件“同生共死”，不因重组而重启
    LaunchedEffect(Unit) {
        log("$TAG55 LaunchedEffect 协程安全启动")

        // 2. 天然的 CoroutineScope，可以直接调用 suspend 函数
        while (timeLeft > 0) {
            log("$TAG55 LaunchedEffect 当前 timeLeft: $timeLeft")
            delay(1000.milliseconds)
            timeLeft--
        }

        log("$TAG55 LaunchedEffect 倒计时结束，执行跳转。当前 timeLeft: $timeLeft")
    }

    Text("正确的倒计时：距离进入主页还有 $timeLeft 秒")
}

@PhonePreviews
@Composable
fun C55B_LaunchedTestPreview() {
    CourseComposeTheme {
        C55B_LaunchedTest()
    }
}

/* Output:
System.out               I  0 [main @coroutine#63] LaunchedTest LaunchedEffect 协程安全启动
System.out               I  1015 [main @coroutine#63] LaunchedTest LaunchedEffect 当前 timeLeft: 2
System.out               I  2017 [main @coroutine#63] LaunchedTest LaunchedEffect 当前 timeLeft: 1
System.out               I  3019 [main @coroutine#63] LaunchedTest LaunchedEffect 当前 timeLeft: 0
System.out               I  3019 [main @coroutine#63] LaunchedTest LaunchedEffect 倒计时结束，执行跳转
 */
