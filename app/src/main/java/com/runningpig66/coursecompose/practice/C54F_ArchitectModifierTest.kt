package com.runningpig66.coursecompose.practice

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-06-23
 * @time 1:25
 *
 * 架构师视角的极佳实践：利用 Modifier.composed 和 LocalActivity 将复杂的 Window 常亮管控机制封装为自定义修饰符。
 * 实现业务层“傻瓜式”链式调用且绝对防漏的底层收敛。
 *
 * notes: C54_DisposableEffect.md
 */
// ====================================================================
// 【底层架构区】：这部分代码通常写在公司的 common/utils 模块中
// ====================================================================
/**
 * 架构师避坑点 1：Context 剥洋葱机制
 * 在 Compose 中，LocalContext.current 拿到的不一定直接是 Activity，
 * 它可能被系统包裹了一层 ContextThemeWrapper。如果直接强转 (context as Activity) 会直接 Crash。
 * 必须通过 while 循环一层层把真实的 Activity 剥出来。
 */
/*fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        // 如果它是 Wrapper 但不是 Activity，你的 while 循环会无限死循环！必须加上这行：
        context = context.baseContext
    }
    return null
}*/

/**
 * 架构师避坑点 2：利用 Modifier.composed {} 将生命周期封装进修饰符
 * Modifier.composed 是一个魔法 API，它允许我们在一个普通的 Modifier 扩展函数里，
 * 使用 LocalContext 和 DisposableEffect 这种只有 @Composable 环境才能用的东西。
 */
fun Modifier.keepScreenOn(): Modifier = composed {
    val window = LocalActivity.current?.window

    // 如果 window 为空，或者 window 发生了改变，副作用会自动重启/清理
    DisposableEffect(window) {
        // Setup: 当拥有该 Modifier 的 UI 节点进入组合时，强行给系统 Window 挂上常亮 Flag
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            // Dispose: 当 UI 节点离开组合时，极其安全地清理掉常亮 Flag，绝不漏电
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    // 返回 Modifier 本身，保证链式调用不断裂
    this
}

// ====================================================================
// 【业务开发区】：这部分是普通的 UI 开发人员写的代码
// ====================================================================
@Composable
fun C54F_ArchitectModifierTest() {
    var isVideoPlaying by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Modifier 封装测试")
            Button(onClick = { isVideoPlaying = !isVideoPlaying }) {
                Text(text = if (isVideoPlaying) "关闭视频播放器" else "打开视频播放器")
            }
            if (isVideoPlaying) {
                // 请看这里的业务层代码，是多么的干净、优雅、傻瓜式！
                // 业务开发根本不需要知道什么是 DisposableEffect，什么是 WindowManager
                // 他只需要在需要的组件上挂一个 .keepScreenOn() 就可以了！
                Box(
                    Modifier
                        .size(200.dp)
                        .background(Color.Black)
                        .keepScreenOn()
                ) {
                    Text(
                        text = "视频播放中\n你可以放下手机观察\n此时屏幕绝对不会变暗息屏",
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@PhonePreviews
@Composable
fun C54F_ArchitectModifierTestPreview() {
    CourseComposeTheme {
        C54F_ArchitectModifierTest()
    }
}
