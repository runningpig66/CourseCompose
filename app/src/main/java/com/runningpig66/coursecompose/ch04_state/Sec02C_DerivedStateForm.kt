package com.runningpig66.coursecompose.ch04_state

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

/**
 * @author runningpig66
 * @date 07-09 星期四
 * @time 4:12
 *
 * 工业级实战（多状态聚合）：在复杂表单场景下，将多个高频输入状态聚合并派生为单一低频状态，实现视图的精准局部重组。
 *
 * notes: Sec02_derivedStateOf.md
 */
private const val C402C = "Sec02C_DerivedStateForm"

// 状态持有者 (StateHolder) 将零散的状态和高开销的派生计算逻辑剥离出 UI 函数，实现逻辑与视图的解耦。
class RegistrationFormState {
    // 基础状态源：高频变化
    var username by mutableStateOf("")
    var password by mutableStateOf("")

    // 派生状态：多状态聚合。监听上述两个状态，只要其中一个变化，闭包就会重新计算。
    val isSubmitEnabled by derivedStateOf {
        Log.d(C402C, "[内部运算] derivedStateOf 正在执行校验计算...")
        val isUserValid = username.length >= 4
        val isPwdValid = password.length >= 6 && password.any { it.isDigit() }

        isUserValid && isPwdValid
    }
}

@Composable
fun Sec02C_DerivedStateForm() {
    // 仅在首次组合时创建 StateHolder
    val formState = remember { RegistrationFormState() }

    Log.d(C402C, "[外层大容器] 发生重组 (仅应在初始化时打印一次) <====")

    Scaffold { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 输入框各自监听自身关心的状态，实现极致的局部重组
            UsernameInputField(formState)
            PasswordInputField(formState)

            Spacer(Modifier.height(32.dp))

            // 提交按钮只监听低频的派生状态
            SubmitButton(formState)
        }
    }
}

@Composable
private fun UsernameInputField(state: RegistrationFormState) {
    Log.d(C402C, "[局部重组] UsernameInputField 重新绘制")
    OutlinedTextField(
        value = state.username,
        onValueChange = { state.username = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("用户名（最少 4 位）") },
    )
}

@Composable
private fun PasswordInputField(state: RegistrationFormState) {
    Log.d(C402C, "[局部重组] PasswordInputField 重新绘制")
    OutlinedTextField(
        value = state.password,
        onValueChange = { state.password = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("密码 (最少 6 位，需包含数字)") },
        // TODO
        visualTransformation = PasswordVisualTransformation()
    )
}

@Composable
private fun SubmitButton(state: RegistrationFormState) {
    // 这里的读取动作（state.isSubmitEnabled）建立了与 derivedStateOf 的依赖
    Log.d(C402C, "[局部重组] SubmitButton 重新绘制，当前状态: ${state.isSubmitEnabled}")
    Button(
        onClick = { /*TODO*/ },
        modifier = Modifier.fillMaxWidth(),
        enabled = state.isSubmitEnabled
    ) {
        Text("注册")
    }
}

@PhonePreviews
@Composable
fun Sec02C_DerivedStateFormPreview() {
    CourseComposeTheme {
        Sec02C_DerivedStateForm()
    }
}
