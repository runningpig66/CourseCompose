package com.runningpig66.coursecompose.practice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import android.util.Log
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-06-22
 * @time 6:27
 *
 * 演示在 Composable 组件中极其干净地动态注册与注销 Android 系统级广播（如飞行模式切换）。
 * 包含在初始化时主动拉取真实系统快照的“防脱节”闭环机制。
 *
 * notes: C54_DisposableEffect.md
 */
private const val TAG54 = "BroadcastTest"

@Composable
fun C54E_BroadcastTest() {
    val context = LocalContext.current

    // 在状态初始化的瞬间，主动向 Android 系统的 Settings.Global 数据库查询一次真实的当前状态。
    var isAirplaneModeOn by remember {
        mutableStateOf(
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0
            ) != 0
        )
    }

    Scaffold { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(text = if (isAirplaneModeOn) "飞行模式已开启：网络已断开" else "飞行模式已关闭：网络连接正常")

            // 将 context 作为 Key 传入。只要 Context 不变（通常整个 Activity 生命周期内不会变），这个广播就在页面可见时存活。
            DisposableEffect(context) {
                Log.d(TAG54, "Setup: UI 挂载成功，向 Android 系统注册飞行模式广播")

                // 创建一个标准的原生 BroadcastReceiver
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        if (intent?.action == Intent.ACTION_AIRPLANE_MODE_CHANGED) {
                            // 从系统的 Intent 中提取飞行模式的当前布尔值
                            val isTurnedOn = intent.getBooleanExtra("state", false)
                            Log.d(TAG54, "收到系统广播：飞行模式切换为: $isTurnedOn")

                            // 更新 Compose 状态，驱动 UI 重组
                            // 注意：这里由于我们直接修改的是 mutableStateOf 委托的变量，所以完美避开了旧值陷阱，每次都能正确更新。
                            isAirplaneModeOn = isTurnedOn
                        }
                    }
                }

                // 定义需要监听的频道（IntentFilter），并真正向底层注册
                val filter = IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                context.registerReceiver(receiver, filter)

                // 当 UI 离开组合时，注销广播，防止内存泄漏和系统资源浪费
                onDispose {
                    Log.d(TAG54, "Dispose: 界面被销毁（或退出），安全解除广播监听")
                    context.unregisterReceiver(receiver)
                }
            }
        }
    }
}

@PhonePreviews
@Composable
fun C54E_BroadcastTestPreview() {
    CourseComposeTheme {
        C54E_BroadcastTest()
    }
}
