package com.runningpig66.coursecompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.runningpig66.coursecompose.practice.C35
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CourseComposeTheme {
                AnimationShowcaseApp()
            }
        }
    }
}

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Home : Route

    @Serializable
    data object TweenDemo : Route

    @Serializable
    data object SnapDemo : Route

    @Serializable
    data object KeyframesDemo : Route

    @Serializable
    data object PracticeDemo : Route

    @Serializable
    data object SpringDemo : Route

    @Serializable
    data object RepeatableDemo1 : Route

    @Serializable
    data object RepeatableDemo2 : Route

    @Serializable
    data object WaveLoadingDemo : Route

    @Serializable
    data object RadarPulseDemo : Route

    @Serializable
    data object ExponentialDecayDemo : Route

    @Serializable
    data object DecayTimelineDemo : Route
}

// 1. 定义一个数据类来绑定路由和展示文本
data class DemoItem(val route: Route, val title: String)

// 2. 集中维护配置表 (未来增删页面，只需在这里改一行代码)
val animationDemos = listOf(
    DemoItem(Route.PracticeDemo, "0. PracticeDemo"),
    DemoItem(Route.TweenDemo, "1. TweenSpec 赛道图鉴"),
    DemoItem(Route.SnapDemo, "2. SnapSpec 降级测试"),
    DemoItem(Route.KeyframesDemo, "3. KeyframesSpec 密码抖动"),
    DemoItem(Route.SpringDemo, "4. SpringSpec"),
    DemoItem(Route.RepeatableDemo1, "5. RepeatableSpec1"),
    DemoItem(Route.RepeatableDemo2, "6. RepeatableSpec2"),
    DemoItem(Route.WaveLoadingDemo, "7. WaveLoadingDemo"),
    DemoItem(Route.RadarPulseDemo, "8. RadarPulseDemo"),
    DemoItem(Route.ExponentialDecayDemo, "9. ExponentialDecayDemo"),
    DemoItem(Route.DecayTimelineDemo, "10. DecayTimelineDemo"),
)

@Composable
fun AnimationShowcaseApp() {
    val backStack = rememberNavBackStack(Route.Home)

    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<Route.Home> { HomeIndexScreen { route -> backStack.add(route) } }
            entry<Route.PracticeDemo> { C35() }
            entry<Route.TweenDemo> { TweenEasingRaceDemo() }
            entry<Route.SnapDemo> { SnapDegradeDemo() }
            entry<Route.KeyframesDemo> { KeyframesShakeDemo() }
            entry<Route.SpringDemo> { SpringPlaygroundDemo() }
            entry<Route.RepeatableDemo1> { RepeatablePlaygroundDemo() }
            entry<Route.RepeatableDemo2> { RepeatableOffsetDemo() }
            entry<Route.WaveLoadingDemo> { WaveLoadingDemo() }
            entry<Route.RadarPulseDemo> { RadarPulseDemo() }
            entry<Route.ExponentialDecayDemo> { ExponentialDecayDemo() }
            entry<Route.DecayTimelineDemo> { DecayTimelineDemo() }
        }
    )
}

@Composable
fun HomeIndexScreen(onNavigateTo: (Route) -> Unit) {
    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(animationDemos) { demoItem ->
                Button(
                    onClick = { onNavigateTo(demoItem.route) },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text(text = demoItem.title)
                }
            }
        }
    }
}

@PhonePreviews
@Composable
fun HomeIndexScreenPreview() {
    CourseComposeTheme {
        AnimationShowcaseApp()
    }
}
