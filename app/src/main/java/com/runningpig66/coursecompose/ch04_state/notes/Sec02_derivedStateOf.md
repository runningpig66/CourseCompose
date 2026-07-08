[TOC]

### 阶段一：痛点引入与核心概念

#### 1. 灾难现场：被无意义重组支配的恐惧 (Sec02A_DerivedStateTrap.kt)

**业务需求：** 页面上有一个很长的列表（`LazyColumn`）。我们需要实现一个非常简单的逻辑：**当列表向下滑动，首个可见项的索引大于 0 时，显示一个“回到顶部”的按钮；否则隐藏它。**

如果你带着传统 Imperative（命令式）编程的思维，或者刚接触 Compose，大概率会写出下面这种代码：

```kotlin
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
```

**发生了什么灾难？**
如果你运行这段代码并疯狂向下滑动列表，观察 Logcat，你会看到极其恐怖的日志刷屏：

```text
===> UI 发生重组了！当前 showButton: true
===> UI 发生重组了！当前 showButton: true
===> UI 发生重组了！当前 showButton: true
===> UI 发生重组了！当前 showButton: true
... (疯狂输出上百条)

```

**底层的物理悖论：**
在 Compose 中，一旦你在 `@Composable` 函数的作用域内直接读取了某个 `State`（例如 `listState.firstVisibleItemIndex`），这个 UI 节点就会**自动订阅**该状态。
当列表滑动时，`firstVisibleItemIndex` 会从 0 变成 1、2、3、4、5... 甚至它的 `ScrollOffset` 在以像素级发生变化。每一次微小的变化，都会触发 Compose 引擎通知 `Sec02A_DerivedStateTrap` 函数重新执行。

但是，对于业务逻辑来说：

* 索引是 `1` 时，`showButton` 是 `true`。
* 索引是 `50` 时，`showButton` 依然是 `true`。
最终的 UI 结果没有任何改变，按钮一直显示着。然而由于你直接监听了上游的“高频数据源”，整个界面的 UI 节点被白白重组了上百次，极其消耗 CPU 和 GPU 资源，严重时直接导致列表掉帧卡顿。

---

#### 2. 破局者：`derivedStateOf` 的核心使命 (Sec02B_DerivedStateFix.kt)

为了解决这种“源头状态高频狂暴变化，但下游 UI 只关心低频运算结果”的痛点，官方提供了专属武器：**`derivedStateOf`**。

它的核心定义极其精准：**它是一个“状态节流阀”与“过滤器”。它能够监听一个或多个源头状态的变化，进行计算，并且只有当“最终的计算结果”发生实质性改变时，才允许重组信号向下游 UI 传递。**

---

#### 3. 基础语法与正确形态

我们将上述灾难代码通过 `derivedStateOf` 加上 `remember` 进行武装，重写为标准的工业级代码：

```kotlin
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
```

**重生后的物理链路：**

1. 你的 Composable UI 现在读取的是 `showButton` 这个**派生状态**，而不是原始的 `listState`。
2. 当列表滑动，`index` 从 1 变成 2、3、4 时，`derivedStateOf` 内部的代码块确实会重新计算（因为它监听了 `index`）。
3. **核弹级优化点：** `derivedStateOf` 计算出新的结果 `true` 后，它会拿这个新结果去跟旧结果 `true` 做对比。系统发现：**`true == true`，最终结果没变！**
4. 于是，`derivedStateOf` 就像一道坚固的物理闸门，直接**拦截**了重组信号，根本不会通知外层的 `Sec02A_DerivedStateFix` 函数执行。

此时你再疯狂滑动列表，Logcat 里的重组日志只会打印两次：

* 第一次：列表刚出来时（索引为 0，值为 `false`）。
* 第二次：列表刚刚向下滑动一点时（索引变为 1，值为 `true`）。
之后不论你怎么往下滑，UI 绝对不再发生任何无意义的重组。

这就是 `derivedStateOf` 存在的全部意义：**高频源 -> 过滤计算 -> 低频结果**。

### 实战一：多状态聚合与局部重组防御 (Sec02C_DerivedStateForm.kt)

在编写登录、注册等表单时，我们通常面临多个输入状态（如用户名、密码、确认密码）。如果按照初学者的写法，把所有的 `var text by remember { mutableStateOf("") }` 都堆在顶层 Composable 中，那么用户每敲击一次键盘，整个表单页面（甚至包括没有修改的控件）都会发生一次完整的重组。

不仅如此，如果我们在顶层简单地写一句 `val isEnabled = username.length > 6 && password.length > 6`，这其实就是典型的“脱裤子放屁”——因为顶层函数本来就在高频重组，这句话每次都会跟着重新计算，根本起不到任何性能优化作用。**工业级的标准解法是：** 将状态下沉至独立的状态持有者（StateHolder）类中，并利用 `derivedStateOf` 建立一条只暴露给特定 UI 的低频“过滤通道”。

请看下面的实战代码，这是大厂在处理复杂表单时标准的单向数据流与局部重组架构：

```kotlin
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
```

#### 物理运行链路解析

当你将这段代码跑在设备上，并在“用户名”输入框中输入字符 `A`、`B`、`C` 时，请严密观察 Logcat 的物理表现：

1. **外层容器的物理隔离：** `[外层大容器]` 的日志**绝对不会**再次打印。因为外部函数根本没有读取 `username`，它只是传递了 `formState` 这个对象的内存引用。
2. **高频输入的局部流转：** 每敲击一次键盘，底层 `username` 发生突变，快照系统精准定位到 `UsernameInputField`，触发输入框自身的局部重组。
3. **`derivedStateOf` 的拦截防御：**
   * 每敲击一次，你都会看到 `[内部运算] derivedStateOf 正在执行校验计算...` 的日志。这说明只要源状态发生改变，派生状态的闭包**一定会**重新执行。
   * 但是，当它算出最终结果依然是 `false`，并与上一次的旧缓存 `false` 相比发现没有变化时，它直接拦截了重组信号。
   * 因此，`[局部重组] SubmitButton 重新绘制` 的日志**绝对不会打印**。
4. **防线突破（低频流转）：** 当你输入完毕满足所有条件的那一瞬间，`derivedStateOf` 计算出结果为 `true`。此时 Diff 校验发现与旧缓存不一致，它立刻将重组信号放行给 `SubmitButton`。此时，提交按钮才会触发唯一的一次有效重组并变为可点击状态。

这个案例彻底展示了 `derivedStateOf` 作为多状态聚合点的威力：它允许后端逻辑高频执行验证，但确保前端只有在关键节点（状态反转时）才产生极低频的重组消耗。

### 实战二：高频转低频滚动防抖交互 (Sec02D_DerivedStateScroll.kt)

在真实的商业项目中，除了表单，最容易引发重组灾难的就是**滚动列表（Scrollable Lists）**。列表的偏移量（Offset）和可见项索引（Index）在手指滑动时是以屏幕刷新率（如 120Hz）在疯狂突变的。如果 UI 层直接监听这种高频状态，会导致极其严重的掉帧。

我们用一个非常经典的工业级场景——**“滑动超过 5 个 Item 后，渐浮现出『回到顶部』悬浮按钮”**，来彻底展示 `derivedStateOf` 是如何作为“防波堤”，将上游的狂风骤雨（高频状态）转化为下游的徐徐微风（低频状态）的。

请看这套极其精细、自带协程滚动的实战代码：

```kotlin
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
```

#### 物理运行与性能剖析

当你把这段代码跑起来，用手指在屏幕上疯狂上下滑动时，请仔细观察底层的物理日志。这就是 `derivedStateOf` 的核心护城河：

1. **外层 UI 绝对静默：**
`====> [外层大容器] 发生重组 <====` 这句话，在整个滑动生命周期中**只会出现一次**。这意味着，哪怕 GPU 正在以 120 帧渲染你的滑动，Compose 的 UI 树结构并没有发生任何无意义的重建。
2. **高频监听与过滤（The Filter）：**
当你从索引 0 滑动到 5 的过程中，你会看到日志疯狂输出：
`[内部运算] derivedStateOf 正在捕捉高频滑动... 当前索引: 1`
`[内部运算] derivedStateOf 正在捕捉高频滑动... 当前索引: 2`
这说明快照系统（Snapshot System）精准捕捉到了 `listState` 的变化，并**强制派生闭包重新执行**。
但重点来了：闭包每次算出结果 `false` 后，跟旧缓存 `false` 相比发现没变化，直接把重组信号“掐死”在摇篮里。
3. **越过阈值的低频释放（The Flip）：**
当你的手指继续上滑，索引变成 6 的那一瞬间：
* 闭包算出结果：`true`。
* Diff 校验发现：`true != false`（旧缓存）。
* 物理防线放行！系统精准引爆了只读取了 `showFab` 的那个下游节点。
* 于是，你看到了唯一的有效刷新：`[局部重组] FAB 容器判定重组，当前 showFab: true`。

**架构总结：**在这个实战中，`derivedStateOf` 扮演了一个物理层面的“降频器”。没有它，你的 FAB 容器会被上千次的滑动信号反复轰炸；有了它，一切繁杂的数据流都在底层被默默消化，只有当业务逻辑发生实质反转（越过第 5 项）时，才会掀起唯一的涟漪。这就是现代客户端性能优化的至高境界。

### 阶段三：降维补漏与架构选择总结

在实际开发和面试中，关于 `derivedStateOf` 的考察往往集中在它的使用边界和误区。如果不加区分地使用，反而会造成性能损耗。

#### 1. 滥用陷阱：不需要过滤频率的简单逻辑

**常见误区：** 开发者在了解 `derivedStateOf` 可以派生状态后，会将所有的逻辑判断都包裹起来。

**反面案例：**

```kotlin
@Composable
fun UserProfile() {
    var age by remember { mutableStateOf(0) }
    
    // 错误示范：没有起到过滤作用，反而增加了开销
    val isAdult by remember { derivedStateOf { age >= 18 } } 

    Button(onClick = { age++ }) {
        Text("当前年龄: $age")
    }
    
    if (isAdult) {
        Text("已成年")
    }
}
```

**底层分析：**
`derivedStateOf` 内部会创建一个独立的 State 对象，并维护一套依赖追踪和结果缓存机制。
在这个例子中，`age` 是通过点击按钮触发的低频操作（点击一次改变一次）。当 `age` 改变时，整个 `UserProfile` 已经因为 `age` 的变化触发了重组。此时 `derivedStateOf` 并不能减少重组次数，反而因为创建了额外的缓存对象和依赖追踪记录，增加了内存和 CPU 开销。

**正确做法：**
对于此类低频且简单的计算，直接在函数体中声明即可：
`val isAdult = age >= 18`
如果计算逻辑耗时，应使用带参数的 `remember`：
`val isAdult = remember(age) { age >= 18 }`

#### 2. 失效陷阱：闭包内读取非 State 对象

**常见误区：** 在 `derivedStateOf` 的计算块中混用普通的 Kotlin 变量或标准集合。

**反面案例：**

```kotlin
@Composable
fun FilteredList() {
    // 普通的 ArrayList
    val normalList = ArrayList<String>() 
    
    // 错误示范：快照系统无法追踪普通集合的变化
    val processedName by remember { 
        derivedStateOf { normalList.filter { it.length > 3 } } 
    }
}
```

**底层分析：**
Compose 的状态监听机制依赖于 `Snapshot` 系统对 `State` 对象的读取（Read）拦截。当 `derivedStateOf` 执行其闭包时，如果读取的是普通的 `ArrayList` 或 `Int`，由于它们没有被 Compose 的代理机制包装，快照系统无法建立依赖图谱。
后续如果外部代码向 `normalList` 添加了数据，`derivedStateOf` 无法感知到变化，闭包不会重新执行，导致界面数据停止更新（静默失效）。

**正确做法：**
闭包内读取的变量，必须是 `State<T>` 类型。如果是集合，必须使用 `SnapshotStateList`（如 `mutableStateListOf`）。

#### 3. 架构选择标准：`remember(key)` vs `derivedStateOf`

这是状态管理中最容易混淆的两个 API。在业务开发中，选择标准可以归纳为以下一条明确的规则：**比较源状态的变化频率与结果的变化频率。**

* **使用 `remember(key)` 的条件：输入频率 = 输出频率。**
当源状态（Key）发生变化时，派生结果**必然**也发生变化，且源状态本身属于低频更新。
*用途*：缓存耗时的计算过程，避免在每次普通重组时重复计算。
*示例*：根据用户 ID 查询本地数据库并格式化用户信息。
* **使用 `derivedStateOf` 的条件：输入频率 > 输出频率。**
当源状态处于高频变化（如滚动偏移量、动画进度、连续的文本输入），但你需要的派生结果是低频变化的（如是否越过某个布尔值阈值）。
*用途*：作为重组拦截器，阻断无意义的下游刷新。
*示例*：滑动列表超过 100 像素后显示悬浮按钮。

#### 第 4.2 节总结

1. `derivedStateOf` 是 Compose 中处理多对一状态聚合与高频过滤的标准工具。
2. 它的物理机制是通过对比新旧计算结果是否一致，来决定是否向下游作用域发送重组失效（Invalidate）信号。
3. 它必须配合 `remember` 一起使用以保证对象在重组中存活。
4. 在应用层，避免将它当作常规的逻辑运算替代品，只有在明确需要“降低状态变更频率”以保护渲染性能时，才引入该 API。
