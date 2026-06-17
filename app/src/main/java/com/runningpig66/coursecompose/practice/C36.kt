package com.runningpig66.coursecompose.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews
import kotlinx.coroutines.launch

/**
 * @author runningpig66
 * @date 2026-06-16
 * @time 4:11
 *
 * 36. Modifier.composed() 和 ComposedModifier
 *
 * 普通的 Modifier 是无状态的。如果需要封装一个自带内部状态（或需要协程、上下文）的修饰符，就需要使用 composed()。
 * - 延迟执行：composed() 会返回一个 ComposedModifier 节点，它本身只是一个壳，里面装了一个工厂函数。
 * 这个工厂函数在声明时不会执行，只有当它被传递给具体的组件（如 Box），在底层准备布局时才会被解包执行。
 * - 状态隔离：由于每次被组件挂载时都会独立执行一次工厂函数，所以如果多个组件复用同一个 composed 修饰符，
 * 它们会在各自的插槽区域独立 remember 一份状态，互不干扰。
 * - 业务场景：日常写页面排版时一般不需要用它，它主要用于开发通用的自定义修饰符（通常写成扩展函数的形式）。
 */
@Composable
fun C36() {
    // 这里的 modifier1 只是一个包装了工厂函数的空壳。当它传给下面的 Box 和 Text 时，会在它们各自的作用域内独立执行工厂函数。
    // 点击改变状态时，旧的 Modifier 不会被修改（它是不可变的），而是触发工厂函数重新执行，生成新的 Modifier 替换掉旧的。
    val modifier1 = Modifier.composed { // 有状态的 Modifier
        //var padding = 8.dp
        var padding by remember { mutableStateOf(8.dp) }
        padding(padding).clickable { padding = 0.dp }
    }

    //Modifier.Node()

    // modifier2 的 padding 状态是直接写在 C36 里的。这意味着 Modifier 是立刻被创建出来的，且状态属于外部的 C36 函数。
    var padding by remember { mutableStateOf(16.dp) }
    val modifier2 = Modifier
        .padding(padding)
        .clickable { padding = 0.dp } // Modifier 创建时间不同

    Scaffold { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            // modifier1 测试：状态独立。Box 和 Text 各自拥有一份 8.dp 的独立状态，点击互不影响，且不会导致 C36 重组。
            Box(Modifier.background(Color.Blue) then modifier1)
            Text("running", Modifier.background(Color.Green) then modifier1)
        }

        /*Column(Modifier.padding(innerPadding)) {
            // modifier2 测试：状态共享。Box 和 Text 共享了外部定义的 16.dp 状态。
            // 只要点击其中一个，会导致整个 C36 重新执行，两者的边距会同时变成 0。
            Box(Modifier.background(Color.Blue) then modifier2)
            Text("pig", Modifier.background(Color.Green) then modifier2)
        }*/
    }
}

// 实际开发中最常用的方式：通过扩展函数封装内部逻辑，对外提供干净的 API
fun Modifier.paddingJumpModifier() = composed {
    var padding by remember { mutableStateOf(8.dp) }
    padding(padding).clickable { padding = 0.dp }
}

// 需要使用 LaunchedEffect 等 Composable 作用域 API 时
fun Modifier.coroutineModifier() = composed {
    LaunchedEffect(Unit) {
        launch {
            // ...
        }
    }
    this // 如果没有额外修饰，返回 this 即可
}

// 需要获取 CompositionLocal 上下文时
fun Modifier.localModifier() = composed {
    val context = LocalContext.current
    this
}

@PhonePreviews
@Composable
fun C36Preview() {
    CourseComposeTheme {
        C36()
    }
}
