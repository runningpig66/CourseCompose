package com.runningpig66.coursecompose

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 2026-05-28
 * @time 4:34
 *
 * 演示 SnapSpec 在 Jetpack Compose 架构中的多态特性与工程降级价值。
 *
 * SnapSpec (瞬间吸附规范) 的核心作用在于保持 AnimationSpec API 签名的统一。
 * 在企业级应用架构中，动画的降级策略（如针对低配设备或省电模式关闭过渡效果）
 * 应当由配置层通过多态注入，从而避免在底层 UI 组件中硬编码大量的逻辑分支代码。
 */
@Composable
fun SnapDegradeDemo() {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            DegradableExpandableCard()
        }
    }
}

@Composable
fun DegradableExpandableCard(modifier: Modifier = Modifier) {
    // 环境配置标识：控制当前组件的动画降级策略
    var isPowerSaveMode by remember { mutableStateOf(false) }
    // 业务状态标识：控制卡片的展开与收缩
    var isExpanded by remember { mutableStateOf(false) }
    // 策略注入：依据环境标识动态生成对应的 AnimationSpec 实例
    // 从而实现底层业务交互代码与具体动画执行策略的解耦
    val currentSpec: AnimationSpec<Dp> = if (isPowerSaveMode) snap() else tween(durationMillis = 300)
    // 高度状态驱动：应用动态生成的 AnimationSpec
    val cardHeight by animateDpAsState(
        targetValue = if (isExpanded) 250.dp else 80.dp,
        animationSpec = currentSpec, // 将动态决定的 Spec 注入
        label = "CardHeight"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "系统省电模式 (关闭过渡动画)", modifier = Modifier.weight(1f))
            Switch(
                checked = isPowerSaveMode,
                onCheckedChange = { isPowerSaveMode = it }
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight) // 绑定高度状态
                .background(Color(0xFF3F51B5), RoundedCornerShape(12.dp))
                .clickable {
                    // 触发业务状态跃迁，系统将根据 currentSpec 的具体实现（过渡或瞬间吸附）自动执行
                    isExpanded = !isExpanded
                }
                .padding(16.dp)
        ) {
            Text(
                text = if (isExpanded) "点击收起\n\n(详细数据...)" else "点击展开查看详情",
                color = Color.White
            )
        }
    }
}

@PhonePreviews
@Composable
fun SnapDegradePreview() {
    CourseComposeTheme {
        SnapDegradeDemo()
    }
}
