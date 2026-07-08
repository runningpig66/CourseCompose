package com.runningpig66.coursecompose.ch04_state

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-07-05
 * @time 4:27
 *
 * 反面教材：直接在 UI 组合层读取高频变化的列表滚动状态，导致产生大量无意义重组的性能陷阱。
 *
 * notes: Sec02_derivedStateOf.md
 */
private const val C402A = "Sec02A_DerivedStateTrap"

@SuppressLint("FrequentlyChangingValue")
@Composable
fun Sec02A_DerivedStateTrap() {
    // 获取列表的滑动状态（内部包含高频变化的属性）
    val listState = rememberLazyListState()
    // Warning: 直接在组合范围内读取高频变化的状态，进行逻辑判断
    val showButton = listState.firstVisibleItemIndex > 0

    // 如果注释掉 listState 相关的使用，滑动列表只会出现一次重组（初始组合）
    Log.d(C402A, "Recompose 1, Current showButton: $showButton")

    Scaffold { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp,
                    top = innerPadding.calculateTopPadding(),
                    end = 16.dp
                )
        ) {
            LazyColumn(
                modifier = Modifier.align(Alignment.Center),
                state = listState,
                contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(100) { index ->
                    Text(
                        text = "这是第 $index 项",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Log.d(C402A, "Recompose 2, Current showButton: $showButton")

            if (showButton) {
                Button(
                    onClick = { /* 回到顶部逻辑 */ },
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text("回到顶部")
                }
            }
        }
    }
}

/* Output:
Recompose 1, Current showButton: false
Recompose 2, Current showButton: false
Recompose 1, Current showButton: true
Recompose 2, Current showButton: true
Recompose 1, Current showButton: true
Recompose 2, Current showButton: true
...
 */

@PhonePreviews
@Composable
fun Sec02A_DerivedStateTrapPreview() {
    CourseComposeTheme {
        Sec02A_DerivedStateTrap()
    }
}
