package com.runningpig66.coursecompose.practice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-06-16
 * @time 4:11
 *
 * 36. Modifier.composed() 和 ComposedModifier
 */
@Composable
fun C36() {
    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {

        }
    }
}

@PhonePreviews
@Composable
fun C36Preview() {
    CourseComposeTheme {
        C36()
    }
}
