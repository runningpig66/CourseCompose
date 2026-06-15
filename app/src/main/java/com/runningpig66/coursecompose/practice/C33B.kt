package com.runningpig66.coursecompose.practice

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningpig66.coursecompose.ui.theme.CourseComposeAnimateAsStateTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-06-15
 * @time 0:53
 *
 * 33. Transition-延伸：AnimatedContent() 练习
 *
 * 本类演示了如何使用 AnimatedContent 实现多步骤表单的局部切换。
 * 通过监听 currentStep 状态的改变，在高度不同的子组件之间进行过渡。
 * 并在 transitionSpec 中根据步骤的增减计算滑动方向，同时配置尺寸过渡的动画。
 */
@Composable
fun C33B() {
    // 记录当前所处的步骤编号
    var currentStep by remember { mutableIntStateOf(1) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 承载内容切换的外部容器
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                // 监听 targetState 的变化并执行相应的出入场动画
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        val duration = 300
                        // 对比目标状态与初始状态，决定元素的进出方向
                        if (targetState > initialState) {
                            // 前进 (Next)：新组件从右侧进入，旧组件向左侧退出
                            slideIntoContainer(SlideDirection.Start, tween(duration)) +
                                    fadeIn(tween(duration)) togetherWith
                                    slideOutOfContainer(SlideDirection.Start, tween(duration)) +
                                    fadeOut(tween(duration))
                        } else {
                            // 后退 (Back)：新组件从左侧进入，旧组件向右侧退出
                            slideIntoContainer(SlideDirection.End, tween(duration)) +
                                    fadeIn(tween(duration)) togetherWith
                                    slideOutOfContainer(SlideDirection.End, tween(duration)) +
                                    fadeOut(tween(duration))
                        } using SizeTransform(clip = false) { initialSize, targetSize ->
                            Log.d(
                                TAG, "${System.currentTimeMillis()} C33B: " +
                                        "initialSize: $initialSize, targetSize: $targetSize"
                            )
                            /*spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )*/
                            // 配置尺寸过渡的动画规格
                            tween(
                                durationMillis = 300,
                                easing = FastOutSlowInEasing
                            )
                        }
                    },
                    //contentAlignment = Alignment.TopCenter,
                    label = "Stepper",
                    //contentKey = { it.step }
                ) { targetState ->
                    // 根据状态挂载具体的界面组件
                    when (targetState) {
                        1 -> StepOneContent()
                        2 -> StepTwoContent()
                        3 -> StepThreeContent()
                    }
                }
            }
            // 底部控制按钮栏
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(top = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { if (currentStep > 1) currentStep-- },
                    enabled = currentStep > 1
                ) {
                    Text(text = "Previous")
                }
                Button(
                    onClick = { if (currentStep < 3) currentStep++ },
                    enabled = currentStep < 3
                ) {
                    Text("Next")
                }
            }
        }
    }
}

@Preview
@Composable
private fun StepOneContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Step 1: 基本信息",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
        // 模拟一个很矮的输入区
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(top = 16.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
        ) {
            Text(
                text = "姓名、手机号输入框...",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Preview
@Composable
private fun StepTwoContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Step 2: 详细资料",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
        // 模拟一个极高的输入区
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(top = 16.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
        ) {
            Text(
                text = "地址、身份证、多行文本区...",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Preview
@Composable
private fun StepThreeContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Step 3: 完成注册",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
        // 模拟一个中等高度的完成界面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(top = 16.dp)
                .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(8.dp))
        ) {
            Text(
                text = "成功！",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@PhonePreviews
@Composable
fun C33BPreview() {
    CourseComposeAnimateAsStateTheme {
        C33B()
    }
}
