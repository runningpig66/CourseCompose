package com.runningpig66.coursecompose.ch04_state

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import kotlinx.coroutines.launch

/**
 * @author runningpig66
 * @date 07-09 星期四
 * @time 5:13
 *
 * 工业级实战（高频转低频）：结合列表滚动与动画，演示 derivedStateOf 作为物理节流阀，完美阻断滑动引起的高频重组轰炸。
 *
 * notes: Sec02_derivedStateOf.md
 */
private const val C402D = "Sec02D_DerivedStateScroll"

@Composable
fun Sec02D_DerivedStateScroll() {
    // 1. 列表状态：内部包含了高频突变的 firstVisibleItemIndex 和 firstVisibleItemScrollOffset
    val listState = rememberLazyListState()
    // 引入协程作用域，用于控制列表的滚动行为
    val coroutineScope = rememberCoroutineScope()

    // 派生状态，将高频的 Index 变化，压缩为一个低频的 Boolean 状态
    val showFab by remember {
        derivedStateOf {
            Log.d(C402D, "[内部运算] derivedStateOf 正在捕捉高频滑动... 当前索引: ${listState.firstVisibleItemIndex}")
            listState.firstVisibleItemIndex > 5
        }
    }

    Log.d(C402D, "[外层大容器] 发生重组 (仅应在初始化时打印一次)")

    Scaffold(
        floatingActionButton = {
            // 3. 只有这个局部 UI 节点直接读取了 showFab，因此只有它会发生局部重组
            Log.d(C402D, "[局部重组] FAB 容器判定重组，当前 showFab: $showFab")

            AnimatedVisibility(
                visible = showFab,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        // 点击回顶：在协程中启动平滑滚动动画
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "回到顶部"
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding(), start = 16.dp, end = 16.dp),
            state = listState,
            contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(100) { index ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "列表项 #$index",
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@PhonePreviews
@Composable
fun Sec02D_DerivedStateScrollPreview() {
    CourseComposeTheme {
        Sec02D_DerivedStateScroll()
    }
}
