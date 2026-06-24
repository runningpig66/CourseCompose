package com.runningpig66.coursecompose.practice

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author runningpig66
 * @date 2026-06-23
 * @time 3:54
 *
 * 演示在组合阶段直接使用 CoroutineScope 开启协程的灾难性反面教材。
 * 验证了缺少生命周期管控会导致协程数量呈指数级爆炸，并在页面退出后引发严重的内存泄漏。
 */
private const val TAG55 = "LaunchedEffectTest"

@Composable
fun C55A_LaunchedEffectPrevious() {
    Scaffold { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            SplashScreen()
        }
    }
}

@SuppressLint("CoroutineCreationDuringComposition") // TEMP
@Composable
fun SplashScreen() {
    var timeLeft by remember { mutableIntStateOf(16) }
    // Warning: Calls to launch should happen inside a LaunchedEffect and not composition
    CoroutineScope(Dispatchers.Main).launch {
        while (timeLeft > Int.MIN_VALUE) {
            delay(1000.milliseconds)
            timeLeft-- // 观察屏幕上 timeLeft 的数值，表明协程数量呈指数级爆炸
            //Log.d(TAG55, "Current timeLeft: $timeLeft")
            log("Current timeLeft: $timeLeft")
        }
    }
    Text("距离进入主页还有 $timeLeft 秒")
}

@PhonePreviews
@Composable
fun C55A_LaunchedEffectPreviousPreview() {
    CourseComposeTheme {
        C55A_LaunchedEffectPrevious()
    }
}
