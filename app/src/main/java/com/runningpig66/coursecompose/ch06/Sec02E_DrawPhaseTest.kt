package com.runningpig66.coursecompose.ch06

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import com.runningpig66.coursecompose.ui.utils.log

/**
 * @author runningpig66
 * @date 2026/08/30
 * @time 2:51
 */
private const val TAG02D = "DrawPhaseTest"

@Composable
fun DrawPhaseStateExperiment() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            DrawOnlyStatePanel()
            HorizontalDivider()
            CompositionStatePanel()
        }
    }
}

@Composable
private fun DrawOnlyStatePanel() {
    val alphaState = remember { mutableFloatStateOf(1f) }

    log(TAG02D, "DRAW-ONLY -> COMPOSITION")

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "State value is read inside DrawScope")

        Button(
            onClick = {
                alphaState.floatValue = if (alphaState.floatValue == 1f) 0.25f else 1f
            }
        ) {
            Text(text = "Toggle circle alpha")
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val currentAlpha = alphaState.floatValue

            log(TAG02D, "DRAW-ONLY -> DRAW, alpha=$currentAlpha")

            drawRect(color = Color(0xFF454647))

            drawCircle(
                color = Color(0xFF1976D2),
                radius = 40.dp.toPx(),
                center = center,
                alpha = currentAlpha
            )
        }
    }
}

@Composable
private fun CompositionStatePanel() {
    val alphaState = remember { mutableFloatStateOf(1f) }

    // 注意：State 在 Composition 中被读取。
    val currentAlpha = alphaState.floatValue

    log(TAG02D, "COMPOSITION-READ -> COMPOSITION, alpha=$currentAlpha")

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "State value is read during Composition")

        Button(
            onClick = {
                alphaState.floatValue = if (alphaState.floatValue == 1f) 0.25f else 1f
            }
        ) {
            Text(text = "Toggle circle alpha")
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            log(TAG02D, "COMPOSITION-READ -> DRAW")

            drawRect(color = Color(0xFF454647))

            drawCircle(
                color = Color(0xFFE53935),
                radius = 40.dp.toPx(),
                center = center,
                alpha = currentAlpha
            )
        }
    }
}

@PhonePreviews
@Composable
fun DrawPhaseStateExperimentPreview() {
    CourseComposeTheme {
        DrawPhaseStateExperiment()
    }
}
