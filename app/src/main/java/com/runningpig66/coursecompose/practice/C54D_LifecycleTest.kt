package com.runningpig66.coursecompose.practice

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-06-22
 * @time 5:12
 *
 * 演示在 Compose 中优雅接管传统 Android Lifecycle 的标准工业级写法。
 * 利用 LocalLifecycleOwner 与 Navigation 的状态机补课机制，实现精准的页面前后台状态（如视频暂停/恢复）管控。
 *
 * notes: C54_DisposableEffect.md
 */
private const val TAG54 = "LifecycleTest"

@Composable
fun C54D_LifecycleTest() {
    var isPlaying by remember { mutableStateOf(false) }
    var currentEvent by remember { mutableStateOf("INITIALIZED") }

    val lifecycleOwner = LocalLifecycleOwner.current

    Scaffold { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("当前生命周期事件: $currentEvent")
            Text("播放器状态: ${if (isPlaying) "正在播放视频..." else "视频已暂停"}")

            DisposableEffect(lifecycleOwner) {
                Log.d(TAG54, "Setup: 准备向 Android 宿主注册生命周期观察者")

                val observer = LifecycleEventObserver { _, event ->
                    currentEvent = event.name

                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            Log.d(TAG54, "${event.name} (App 回到前台，恢复播放)")
                            isPlaying = true
                        }

                        Lifecycle.Event.ON_PAUSE -> {
                            Log.d(TAG54, "${event.name} (App 退到后台，暂停播放)")
                            isPlaying = false
                        }

                        else -> {
                            Log.d(TAG54, event.name)
                        }
                    }
                }

                lifecycleOwner.lifecycle.addObserver(observer)

                onDispose {
                    Log.d(TAG54, "Dispose: 界面被销毁，解除生命周期观察者绑定")
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
        }
    }
}

@PhonePreviews
@Composable
fun C54D_LifecycleTestPreview() {
    CourseComposeTheme {
        C54D_LifecycleTest()
    }
}

/* Output:
LifecycleTest            D  Setup: 准备向 Android 宿主注册生命周期观察者
LifecycleTest            D  ON_CREATE
LifecycleTest            D  ON_START
LifecycleTest            D  ON_RESUME (App 回到前台，恢复播放)
LifecycleTest            D  ON_PAUSE (App 退到后台，暂停播放)
LifecycleTest            D  ON_STOP
LifecycleTest            D  ON_START
LifecycleTest            D  ON_RESUME (App 回到前台，恢复播放)
LifecycleTest            D  ON_PAUSE (App 退到后台，暂停播放)
LifecycleTest            D  ON_STOP
LifecycleTest            D  ON_DESTROY
LifecycleTest            D  Dispose: 界面被销毁，解除生命周期观察者绑定
 */
