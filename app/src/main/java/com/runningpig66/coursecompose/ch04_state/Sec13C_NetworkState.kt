package com.runningpig66.coursecompose.ch04_state

import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log

/**
 * @author runningpig66
 * @date 2026/08/08 周六
 * @time 23:15
 *
 * 使用 ConnectivityManager.NetworkCallback 演示真实 Android 场景，
 * 将系统网络连接状态通过 produceState 转换为 Compose State。
 *
 * notes: 4.13 produceState().md
 */
private const val TAG13C = "Sec13C"

enum class NetWorkState {
    Available,
    Unavailable
}

@Composable
fun rememberNetworkState(): State<NetWorkState> {
    val context = LocalContext.current

    val connectivityManager = remember(context) {
        context.applicationContext.getSystemService(ConnectivityManager::class.java)
    }

    return produceState(
        initialValue = NetWorkState.Unavailable,
        key1 = connectivityManager
    ) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                log("$TAG13C onAvailable")
                value = NetWorkState.Available
            }

            override fun onLost(network: Network) {
                log("$TAG13C onLost")
                value = NetWorkState.Unavailable
            }
        }

        log("$TAG13C registerNetworkCallback")
        connectivityManager.registerDefaultNetworkCallback(callback)

        awaitDispose {
            log("$TAG13C unregisterNetworkCallback")
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}

@Composable
fun Sec13C_NetworkStateExample() {
    val networkState by rememberNetworkState()

    Scaffold { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (networkState) {
                    NetWorkState.Available -> "Network available"
                    NetWorkState.Unavailable -> "Network unavailable"
                }
            )
        }
    }
}

/* Output:
0 [main @coroutine#63] Sec13C registerNetworkCallback
5 [ConnectivityThread] Sec13C onAvailable
5392 [ConnectivityThread] Sec13C onLost
6322 [ConnectivityThread] Sec13C onAvailable
12382 [main @coroutine#63] Sec13C unregisterNetworkCallback
 */

@PhonePreviews
@Composable
fun Sec13Preview() {
    CourseComposeTheme {
        Sec13C_NetworkStateExample()
    }
}
