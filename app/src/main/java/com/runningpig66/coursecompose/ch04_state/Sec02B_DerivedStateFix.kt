package com.runningpig66.coursecompose.ch04_state

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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-07-05
 * @time 6:49
 *
 * 基础防御策略：使用 derivedStateOf 将高频的源状态过滤为低频的布尔派生状态，成功拦截无效重组。
 *
 * notes: Sec02_derivedStateOf.md
 */
private const val C402B = "Sec02A_DerivedStateTrap"

@Composable
fun Sec02B_DerivedStateFix() {
    val listState = rememberLazyListState()
    // 使用 derivedStateOf 将高频的源状态，派生为低频的新状态
    val showButton by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    Log.d(C402B, "Recompose 1, Current showButton: $showButton")

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

            Log.d(C402B, "Recompose 2, Current showButton: $showButton")

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
...
 */

@PhonePreviews
@Composable
fun Sec02B_DerivedStateFixPreview() {
    CourseComposeTheme {
        Sec02B_DerivedStateFix()
    }
}
