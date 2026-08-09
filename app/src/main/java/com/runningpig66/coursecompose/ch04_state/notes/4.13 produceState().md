[toc]

### 阶段一：痛点引入与核心语法 (Sec13A_ProduceStateExamples.kt)

`produceState` 的核心作用是将非 Compose 体系的异步数据源（如传统 Callback 接口、系统传感器、普通挂起函数）转化为 Compose 可以直接观察并触发重组的 `State<T>`。

为了保证代码结构的规范性，我们将演示文件统一命名为 `Sec13A_ProduceStateExamples.kt`。该文件包含了模拟底层硬件数据源的单例，以及纯手写桥接（反面案例）和 `produceState` 桥接（正面案例）的对比。

#### 完整演示代码

```kotlin
private const val TAG13 = "Sec13A_ProduceStateExamples"

interface HardwareListener {
    fun onDataReceived(heartRate: Int)
}

object HardwareManager {
    // 使用线程安全集合保存所有监听器
    private val listeners = CopyOnWriteArrayList<HardwareListener>()
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun register(listener: HardwareListener) {
        listeners.add(listener)

        // 当第一个监听器注册时，启动底层硬件轮询协程
        if (job == null || job?.isActive != true) {
            job = scope.launch {
                var mockHeartRate = 60
                while (isActive) {
                    delay(500.milliseconds) // 模拟耗时操作
                    mockHeartRate += 10

                    withContext(Dispatchers.Main.immediate) {
                        // 遍历分发给所有处于活跃状态的监听器
                        listeners.forEach {
                            it.onDataReceived(mockHeartRate)
                        }
                    }
                }
            }
        }
    }

    fun unregister(listener: HardwareListener) {
        listeners.remove(listener)
        
        if (listeners.isEmpty()) {
            job?.cancel()
            job = null
        }
    }
}

@Composable
fun Sec13A_ProduceStateExamples() {
    Scaffold { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Sec13_ManualStateBridge()
            HorizontalDivider()
            Sec13_ProduceStateBridge()
        }
    }
}

// 1.1 反面案例：纯手写状态桥接
@Composable
fun Sec13_ManualStateBridge() {
    // 痛点 1：状态的声明被孤立在这里。我们必须手动提供一个无意义的初始值（如 0 或 -1），因为硬件数据还没传过来
    var heartRateState by remember { mutableIntStateOf(0) }

    // 痛点 2：生命周期管理与状态赋值分离，产生大量样板代码
    DisposableEffect(Unit) {
        Log.d(TAG13, "Setup1: 硬件传感器已启动，正在监听")
        // 创建回调对象
        val listener = object : HardwareListener {
            override fun onDataReceived(heartRate: Int) {
                heartRateState = heartRate // 在回调中手动给外层的 state 赋值，驱动重组
            }
        }
        // 注册监听
        HardwareManager.register(listener)
        // 解除监听
        onDispose {
            Log.d(TAG13, "Dispose1: 硬件传感器已关闭，释放资源")
            HardwareManager.unregister(listener)
        }
    }

    Column {
        Text(
            text = "[手写桥接]",
            fontWeight = FontWeight.Bold
        )
        if (heartRateState == 0) {
            Text("心率传感器连接中...")
        } else {
            Text("当前心率: $heartRateState BPM")
        }
    }
}

// 1.2 正面案例：使用 produceState 桥接
@Composable
fun Sec13_ProduceStateBridge() {
    // 将状态声明、作用域管理、资源清理高内聚于一个 APIv
    val heartRateState by produceState(
        initialValue = 0,
        key1 = Unit // 若传入依赖项发生变化，会自动执行 awaitDispose 并重启闭包
    ) {
        Log.d(TAG13, "Setup2: 硬件传感器已启动，正在监听")
        val listener = object : HardwareListener {
            override fun onDataReceived(heartRate: Int) {
                value = heartRate // ProducerScope 提供的 value 属性，赋值即触发重组
            }
        }
        HardwareManager.register(listener)
        // TODO 挂起当前协程，直到组件销毁或 key 发生变化时执行清理逻辑
        awaitDispose {
            Log.d(TAG13, "Dispose2: 硬件传感器已关闭，释放资源")
            HardwareManager.unregister(listener)
        }
    }

    Column {
        Text(
            text = "[produceState 桥接]",
            fontWeight = FontWeight.Bold
        )
        if (heartRateState == 0) {
            Text("心率传感器连接中...")
        } else {
            Text("当前心率: $heartRateState BPM")
        }
    }
}

/* Output:
Setup1: 硬件传感器已启动，正在监听
Setup2: 硬件传感器已启动，正在监听
Dispose1: 硬件传感器已关闭，释放资源
Dispose2: 硬件传感器已关闭，释放资源
...
 */
```

---

#### 1.1 灾难现场复盘：纯手写状态桥接的陷阱

在 `Sec13_ManualStateBridge` 中，虽然功能可以正常运行，但从代码健壮性和可维护性来评估，它暴露了三个具体的工程痛点：

1. **高内聚性被破坏（声明与赋值割裂）：**
状态变量 `heartRateState` 的声明位于顶部，但实际的业务赋值逻辑却深陷在 `DisposableEffect` 内部的回调函数中。在包含数百行代码的复杂 UI 组件里，这种分离会导致数据流向追踪困难。
2. **冗余的样板代码：**
为了实现“监听数据并更新 UI”这一单一诉求，开发者被迫组合调用了 `remember`、`mutableIntStateOf`、`DisposableEffect` 和 `onDispose`。这增加了代码的视觉噪音。
3. **极易引发内存泄漏与协程失控：**
这是最核心的风险。如果开发者在编写 `DisposableEffect` 时遗漏了 `onDispose` 代码块，当当前页面退出、组件从组合树中移除时，`HardwareManager` 依然会持有匿名 `listener` 的强引用。底层的 `Job` 会继续在后台死循环运行，不断向一个已被销毁的 UI 发送数据，造成内存泄漏与 CPU 资源浪费。

---

#### 1.2 破局方案与核心语法：`produceState`

`Sec13_ProduceStateBridge` 展示了 Compose 官方提供的标准解决方案。`produceState` 在底层封装了 `remember` 和 `LaunchedEffect`，将上述所有痛点在一个 API 调用中解决。

**参数与核心机制拆解：**

* **`initialValue` (初始兜底值)**
Compose 的重组是同步执行的，但异步数据源（如硬件传感器）需要时间才能返回首个数据。`initialValue = 0` 强制要求提供一个初始状态，确保组件在第一次组合时有合法数据可供渲染，避免空指针崩溃或渲染异常。
* **`key1`, `key2`, `vararg keys` (重启触发器)**
类似于 `LaunchedEffect` 的 `key` 机制。在当前案例中，由于硬件监听器与特定的外部变量无关，我们传入 `Unit`。如果需求变更为监听特定用户的设备（例如传入 `deviceId` 作为 key），当 `deviceId` 改变时，`produceState` 会自动执行旧设备的注销逻辑，并使用新 ID 重新运行注册逻辑。
* **`ProducerScope` (生产者作用域)**
`produceState` 的尾随闭包运行在 `ProducerScope` 环境中，该作用域本质上是一个协程作用域，并提供了两个关键特性：
1. **内置 `value` 属性写权限：** 开发者无需再手动声明 `MutableState`。直接对作用域内的 `value` 进行赋值（`value = heartRate`），底层会自动将新值推送给 UI 触发重组。
2. **`awaitDispose` 挂起与清理机制：** 对于基于 Callback 的异步注册，闭包末尾**必须调用** `awaitDispose`。调用该函数会立刻挂起当前 `produceState` 启动的内部协程。协程会一直保持挂起状态，直到当前组件离开组合树（出栈销毁）或者传入的 `key` 发生变化。一旦触发销毁，协程恢复执行，进入 `awaitDispose` 闭包内部执行注销逻辑（`HardwareManager.unregister`）。这从机制上强制规范了资源的回收，避免了漏写风险。

### 阶段二：现有心率案例复盘：`produceState()` 到底替代了什么？

先给出这一阶段最重要的结论：你刚才对整个心率案例的主干理解是正确的，而且那个所谓的“反面案例”其实并不应该叫“错误代码”。它是一份**完全可以正常工作的手写状态桥接代码**。只要注册与注销写得正确，它既不会天然内存泄漏，也不存在功能缺陷。

`produceState()` 的意义也确实不像某些讲解说得那么神秘。它没有创造一种你以前做不到的能力。你完全可以不用它，通过 `remember + MutableState + DisposableEffect` 或 `remember + MutableState + LaunchedEffect` 自己实现同样的效果。

真正要理解的是：**`produceState()` 把一种频繁出现的模式抽象成了一个专门的 API——“启动一个与 Composition 生命周期绑定的异步生产者，并把生产出来的数据暴露成 Compose 的 `State<T>`”。**

你之前的讲义正是在做这件事：手写版本先创建 `MutableState`，再用 `DisposableEffect` 注册 `HardwareListener`；`produceState` 版本则直接在一个 API 中完成状态创建、数据写入和生命周期管理。

#### 一、先校准一个重要细节：`DisposableEffect` 不是“第一帧绘制完成后执行”

你刚才复习时有一句话需要修正。你说：

> `DisposableEffect` 是等界面第一帧完整绘制到屏幕之后才执行。

这个心智模型不够准确。

这里不要把 **Composition、应用 Composition 的修改、Layout、Draw、屏幕真正显示出像素** 混成一件事情。

对应用开发来说，更准确而且足够实用的理解是：

> `DisposableEffect` 在这个调用位置成功进入 Composition 后建立副作用；当它离开 Composition，或者 key 发生变化时，旧的副作用会执行 `onDispose`。

不要把它理解成“等第一帧已经被 GPU 画到屏幕上才执行”。**它管理的是 Composition 生命周期，而不是屏幕像素绘制生命周期。**

因此：

```kotlin
DisposableEffect(Unit) {
    register()

    onDispose {
        unregister()
    }
}
```

`Unit` 的真正含义也不是严格意义上的“整个页面永远只执行一次”，而是：

> **只要这个 `DisposableEffect` 的调用位置一直留在当前 Composition 中，它的 key 就不会改变，因此不会因为普通重组而重新启动。**

如果这个 Composable 因为条件判断、Navigation 等原因离开 Composition，再重新进入，那么它仍然会重新注册。

而：

```kotlin
DisposableEffect(deviceId) {
    register(deviceId)

    onDispose {
        unregister(deviceId)
    }
}
```

当 `deviceId` 从 A 变成 B 时，可以理解成：

```text
销毁 A 对应的 effect
        ↓
执行 A 的 onDispose
        ↓
建立 B 对应的新 effect
```

这一部分你原来的理解主干没有问题，只需要把“第一帧绘制结束”改成“进入 / 离开 Composition”即可。

---

#### 二、手写版本实际上做了三件完全不同的事情

重新看你现在已有的 `Sec13A_ProduceStateExamples.kt`：

```kotlin
var heartRateState by remember {
    mutableIntStateOf(0)
}

DisposableEffect(Unit) {
    val listener = object : HardwareListener {
        override fun onDataReceived(heartRate: Int) {
            heartRateState = heartRate
        }
    }

    HardwareManager.register(listener)

    onDispose {
        HardwareManager.unregister(listener)
    }
}
```

不要把它看成一坨代码。实际上它完成了三个独立工作。

第一件事情是：

```kotlin
remember {
    mutableIntStateOf(0)
}
```

这是在 **创建 Compose 状态容器**。

它负责回答：

> “当前心率是多少？”

而且当 `.value` 改变之后，读取过这个 State 的 Compose Scope 可以失效并在需要时重新组合。

第二件事情是：

```kotlin
HardwareManager.register(listener)
```

这是在 **连接外部数据源**。

`HardwareManager` 根本不知道什么叫 Compose，也不知道什么叫 Recomposition。

它只知道：

```text
我拿到新的心率
    ↓
调用 listener.onDataReceived(...)
```

这就是非常传统的 Android / Java Callback 世界。

第三件事情是：

```kotlin
onDataReceived(heartRate) {
    heartRateState = heartRate
}
```

这里才发生整个案例最关键的一步：

```text
非 Compose 世界的数据
        ↓
Callback
        ↓
heartRateState.value = 新数据
        ↓
Compose State 世界
        ↓
读取 State 的 UI 可以重组
```

所以如果以后让我给你这个模式起一个准确的名字，我会称它为：

**External State → Compose State Adapter**

或者中文：

**外部状态源到 Compose State 的桥接 / 适配。**

这比单纯叫“异步请求”更准确，因为外部数据源未必是网络请求。

它可以是：

```text
Android Callback
传感器
LocationManager
ConnectivityManager
BroadcastReceiver
第三方 SDK Listener
普通 suspend API
Flow
RxJava
甚至你自己实现的异步数据源
```

它们共同的特点只有一个：

> **原始数据源不是 Compose `State<T>`，但 UI 最终想要一个 `State<T>`。**

这才是 `produceState()` 这个 API 存在的背景。

---

#### 三、现在把同样的三件事压进 `produceState()`

你的正面案例是：

```kotlin
val heartRateState by produceState(
    initialValue = 0,
    key1 = Unit
) {
    val listener = object : HardwareListener {
        override fun onDataReceived(heartRate: Int) {
            value = heartRate
        }
    }

    HardwareManager.register(listener)

    awaitDispose {
        HardwareManager.unregister(listener)
    }
}
```

现在不要先研究源码，只从 API 表面观察。

原来你需要自己声明：

```kotlin
var heartRateState by remember {
    mutableIntStateOf(0)
}
```

现在变成：

```kotlin
produceState(initialValue = 0) {
    ...
}
```

而 `produceState()` 的返回类型是：

```kotlin
State<T>
```

在当前案例中就是概念上的：

```kotlin
State<Int>
```

因此如果不使用属性委托，可以写成：

```kotlin
val heartRateState = produceState(
    initialValue = 0
) {
    ...
}

Text("${heartRateState.value}")
```

而你当前写的是：

```kotlin
val heartRateState by produceState(...)
```

由于 Kotlin 的属性委托帮你调用了 `State<T>` 的 `getValue()`，所以局部变量 `heartRateState` 本身看到的是：

```kotlin
Int
```

于是可以直接：

```kotlin
Text("$heartRateState")
```

这里你的理解也是正确的：

> `produceState()` 返回的不是普通 `Int`，而是 `State<Int>`；`by` 只是把里面的 `.value` 解包出来。

因此下面读取 `heartRateState` 的 Composable 仍然建立了 Compose 状态读取关系。内部状态变化后，UI 仍然可以发生相应更新。

---

#### 四、那么 `value = heartRate` 到底是什么？

这就是 `produceState()` 很漂亮的一点。

手写版中你有一个外部变量：

```kotlin
heartRateState = heartRate
```

而 `produceState` 的 producer 里面直接写：

```kotlin
value = heartRate
```

这里的 `value` 并不是一个神秘的局部变量。

`produceState` 给它的 producer 提供了一个 **`ProduceStateScope<T>`**。

顺便修正一下旧讲义中的术语：严格来说这里叫 `ProduceStateScope`，不是泛称的 `ProducerScope`。

这个 Scope 本身就拥有可以修改当前状态的 `value`。

所以可以先形成这样一个心智模型：

```text
produceState<Int>
        │
        ├── 对外
        │     State<Int>
        │
        │     UI 只能观察
        │
        └── 对内 producer
              ProduceStateScope<Int>
                    │
                    └── value = xxx
                         可以生产新状态
```

这就是它名字里 **produce State** 的含义。

外面的消费者看到：

```kotlin
State<Int>
```

里面的生产者则拥有：

```kotlin
value = ...
```

这其实已经体现了一种非常清楚的职责划分：

```text
producer：负责生产
State：负责暴露
Composable：负责消费
```

你的旧讲义也已经提到了 `value` 是 producer 向 Compose 状态写入数据的入口。

---

#### 五、所以它真的只是“少写一个 MutableState”吗？

现在可以正式回答你刚才最大的疑问。

**从“能力”上说，你的判断非常接近事实：是的，没有 `produceState()`，完全能写。**

甚至可以把你现在的两份代码画成：

```text
手写版本

remember
   ↓
MutableState
   ↑
Callback
   ↑
DisposableEffect
   ↓
onDispose
```

而 `produceState` 是：

```text
produceState
   │
   ├── State<T>
   │
   ├── producer
   │      ↓
   │    value = x
   │
   └── 生命周期
          ↓
      awaitDispose
```

所以它没有魔法。

但如果只总结成：

> “就是少写了一个 `mutableStateOf`。”

又低估它了。

因为它真正封装的是一个**模式**：

```text
某个与 Composition 生命周期绑定的生产任务
                    ↓
不断产生 T
                    ↓
转换成 State<T>
                    ↓
提供给声明式 UI
```

也就是说，API 不是因为那四五行代码太累才存在，而是 Compose Runtime 给这种模式起了一个明确的名字：

```kotlin
produceState()
```

这在声明式 API 设计中非常重要。

看到：

```kotlin
val location by produceState(...)
```

熟悉 Compose 的程序员应该能够立即理解：

> “这里正在把某种外部 / 异步数据生产过程转成 Compose State。”

而看到：

```kotlin
remember { mutableStateOf(...) }

DisposableEffect(...) {
    ...
}
```

你必须继续往下读，才能知道这个 `MutableState` 与这个 Effect 究竟是不是同一个逻辑。

所以它带来的最大价值之一其实是：

**表达意图。**

---

#### 六、原来的“反面案例”并不危险，错误的是把它描述成天然会泄漏

这里需要纠正旧讲义的一处措辞。

旧讲义把手写方案的问题描述成“极易引发内存泄漏与协程失控”，理由是开发者可能忘记注销 Listener。

这个说法讲得有点过头。

你的代码：

```kotlin
DisposableEffect(Unit) {
    HardwareManager.register(listener)

    onDispose {
        HardwareManager.unregister(listener)
    }
}
```

本身是非常标准的代码。

**没有任何泄漏问题。**

只有当你错误地写成：

```kotlin
DisposableEffect(Unit) {
    HardwareManager.register(listener)

    onDispose {
        // 忘了 unregister
    }
}
```

才可能出现资源泄漏。

但注意：

`produceState()` 并不能从物理上禁止程序员犯同一种错误。

你一样可以：

```kotlin
produceState(initialValue = 0) {
    HardwareManager.register(listener)

    awaitDispose {
        // 同样忘了 unregister
    }
}
```

照样泄漏。

所以不要建立：

```text
DisposableEffect = 危险
produceState = 安全
```

这种错误认识。

准确关系是：

```text
DisposableEffect
    → 通用的“建立副作用 + 清理副作用”

produceState
    → 专门的“执行异步生产过程 + 输出 State<T>”
```

如果你的目的仅仅是：

> 注册 Listener，并在离开 Composition 时注销。

那么：

```kotlin
DisposableEffect
```

本来就是最合适的工具之一。

如果你的目的进一步变成：

> 注册 Listener，而且 Listener 返回的数据本身就是这个 Composable 要消费的状态。

这时候：

```kotlin
produceState
```

就开始非常贴合问题本身了。

这是二者真正的边界。

---

#### 七、你为什么现在“完全没有感觉到它和协程有什么关系”？

这个感觉非常正常，因为当前案例把协程藏得太好了。

你的数据真正从哪里产生？

是：

```kotlin
HardwareManager
```

而你模拟的 `HardwareManager` 自己就拥有：

```kotlin
CoroutineScope(Dispatchers.Default)
```

随后它自己：

```kotlin
scope.launch {
    while (isActive) {
        delay(...)
        ...
    }
}
```

因此从 UI 的角度看：

```text
produceState
     ↓
register(listener)
     ↓
然后什么都没干
     ↓
HardwareManager 自己在后台产生数据
     ↓
callback
     ↓
value = heartRate
```

所以你的直觉自然会问：

> **“`produceState` 自己的协程在哪里？我压根没用它执行心率任务啊。”**

非常好。

这恰恰说明下一阶段必须讲协程。

当前阶段你只需要先知道一件事情：

`produceState` 的：

```kotlin
{
    ...
}
```

不是普通 lambda。

它的生产代码本质上是：

```kotlin
suspend ProduceStateScope<T>.() -> Unit
```

也就是说：

> **整个 producer 本身运行在一个由 Compose 生命周期管理的协程中。**

只不过现在真正产生心率的工作已经由 `HardwareManager` 自己干掉了。

所以 `produceState` 的那个协程目前看起来像个“空壳生命周期容器”。

而这一句：

```kotlin
awaitDispose {
    HardwareManager.unregister(listener)
}
```

正是这个案例中它与协程真正连接起来的地方。为什么叫 **await**？它究竟在 await 什么？为什么程序执行到这里不会立刻 `unregister()`？它挂起以后挂在哪里？页面离开 Composition 后，到底是谁让它结束？key 改变以后为什么旧 Listener 能被注销？**这些问题全部属于下一阶段。**

---

#### 八、阶段二结束后应该形成的最终模型

现在暂时不要把 `produceState()` 想复杂。

把它牢牢记成下面这张图就够了：

```text
             外部非 Compose 数据源
                     │
                     │ Callback / suspend / etc.
                     ▼
        ┌─────────────────────────┐
        │      produceState       │
        │                         │
        │   producer              │
        │      │                  │
        │      └── value = data   │
        │                         │
        └───────────┬─────────────┘
                    │
                    ▼
                 State<T>
                    │
                    ▼
               Compose UI
                    │
                    ▼
                  重组
```

因此一句可以直接写进笔记里的定义是：

> **`produceState()` 是一个 Composable 状态适配 API：它启动一个与当前 Composition 生命周期绑定的异步 producer，并将 producer 产生的数据暴露为 Compose 可观察的 `State<T>`。**

而你原来的：

```kotlin
remember + MutableState + DisposableEffect
```

则是这个模式的一种**手动实现**。

所以今天最开始那个问题，现在答案已经很明确：

> **你的手写版本功能完全没问题。`produceState()` 不是为了修复它，而是把“外部数据源 → Compose State”这一完整模式变成一个具有明确语义的标准抽象。**

现有的 `Sec13A_ProduceStateExamples.kt` 已经足够承担这一阶段的实验，因此**阶段二不新增 `Sec13B` 文件**。下一阶段开始研究协程、`ProduceStateScope` 和 `awaitDispose` 时，再根据实验需要决定是否创建 `Sec13B_...kt`。

#### 下一阶段：阶段三——`ProduceStateScope`、协程生命周期与 `awaitDispose`

下一阶段会专门解决目前最大的悬念：

```text
producer 为什么必须是 suspend？
           ↓
produceState 的协程到底在哪里？
           ↓
awaitDispose 到底挂起了什么？
           ↓
key 改变 / 离开 Composition
为什么能终止旧 producer？
           ↓
如果数据源本来就是 suspend 函数，
为什么反而不需要 awaitDispose？
```

这一部分结束后，`produceState()` 才算真正从“会照着写”进入“知道它为什么这样设计”。

### 阶段三：`ProduceStateScope`、协程生命周期与 `awaitDispose`

这一阶段解决上一阶段留下的核心问题：为什么 `produceState()` 明明看起来只是“注册 Listener、收到数据后修改 `value`”，它却偏偏建立在协程之上？`awaitDispose()` 又究竟在等待什么？

答案实际上已经藏在你贴出的函数签名里：

```kotlin
@Composable
public fun <T> produceState(
    initialValue: T,
    key1: Any?,
    producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    LaunchedEffect(key1) {
        ProduceStateScopeImpl(result, coroutineContext).producer()
    }
    return result
}

public interface ProduceStateScope<T> : MutableState<T>, CoroutineScope {
    public suspend fun awaitDispose(onDispose: () -> Unit): Nothing
}
```

现在先不继续进入 `ProduceStateScopeImpl` 和 `awaitDispose()` 的实现源码，那是下一阶段的事情。这一阶段只利用这几十行公开结构，把整个运行模型建立起来。

#### 一、`producer` 根本不是普通 Lambda，而是一个挂起扩展 Lambda

`produceState()` 最值得注意的参数不是 `initialValue`，甚至不是 `key1`，而是最后这个：

```kotlin
producer: suspend ProduceStateScope<T>.() -> Unit
```

如果把 Kotlin 语法拆开来看，它同时包含三个信息。

第一，`suspend` 表明这段代码运行在协程环境中，因此内部可以直接调用 `delay()`、挂起网络请求、`collect()` 等挂起函数。

第二，`ProduceStateScope<T>.()` 表明它是一个**带接收者的扩展 Lambda**。因此当我们写：

```kotlin
produceState(initialValue = 0) {
    value = 100
}
```

这里的 `value` 并不是某个凭空出现的局部变量，而是隐式接收者 `this: ProduceStateScope<Int>` 提供的属性。

第三，`ProduceStateScope<T>` 自己又同时继承了：

```kotlin
MutableState<T>
CoroutineScope
```

所以这个 Scope 同时拥有两种能力：作为 `MutableState<T>`，它可以通过 `value = ...` 生产新的 Compose 状态；作为 `CoroutineScope`，它又拥有 `coroutineContext`，处于一个真正的协程生命周期之中。

于是可以把它理解为：

```text
ProduceStateScope<T>

    MutableState<T> 这一面
        └── value = xxx
            把数据写进 Compose State

    CoroutineScope 这一面
        └── coroutineContext
            挂起函数
            子协程
            协程取消
            生命周期
```

这正是 `produceState()` 和普通的“帮我创建一个 MutableState”之间最重要的差异。

---

#### 二、`produceState()` 的协程到底在哪里？

你之前之所以没有感觉到协程，是因为真正产生心率的任务写在了 `HardwareManager` 里面：

```kotlin
private val scope = CoroutineScope(Dispatchers.Default)

scope.launch {
    while (isActive) {
        delay(...)
        ...
    }
}
```

于是从 UI 看起来，`produceState()` 只是注册了一个 Listener，似乎完全没有启动任何协程。

但你现在贴出的源码已经直接给出了答案：

```kotlin
LaunchedEffect(key1) {
    ProduceStateScopeImpl(
        result,
        coroutineContext
    ).producer()
}
```

也就是说，调用：

```kotlin
produceState(...) {
    // 我们写的代码
}
```

概念上相当于 Compose 在幕后执行：

```kotlin
val result = remember {
    mutableStateOf(initialValue)
}

LaunchedEffect(key1) {
    // 在这个 LaunchedEffect 协程里面
    // 执行我们的 producer
}

return result
```

因此 `produceState()` 的生命周期实际上建立在你已经学过的 `LaunchedEffect` 上。

这时候整个模型就连起来了：

```text
Composable 进入 Composition
        │
        ▼
remember 创建 State<T>
        │
        ▼
LaunchedEffect(key) 启动 Compose 管理的协程
        │
        ▼
执行 producer
        │
        ├── suspend 函数
        ├── value = ...
        └── awaitDispose(...)
```

所以 `produceState()` 并没有发明另外一套副作用生命周期系统。至少从你当前版本贴出的源码来看，它直接建立在 `remember + LaunchedEffect` 之上。

这也是为什么前面学习 `LaunchedEffect()` 很重要：如果已经彻底理解 `LaunchedEffect`，`produceState` 的一半其实已经学完了。

---

#### 三、Callback 模型真正的问题：`register()` 会立即返回

重新回到心率案例：

```kotlin
produceState(
    initialValue = 0,
    key1 = Unit
) {
    val listener = object : HardwareListener {
        override fun onDataReceived(heartRate: Int) {
            value = heartRate
        }
    }

    HardwareManager.register(listener)

    awaitDispose {
        HardwareManager.unregister(listener)
    }
}
```

这里一定要把 `register()` 和一个真正的挂起任务区别开。

执行：

```kotlin
HardwareManager.register(listener)
```

只是告诉外部系统：

> “以后有数据的时候，请调用这个 Listener。”

注册动作本身很快就结束了。

如果我们把后面的 `awaitDispose()` 删除：

```kotlin
produceState(initialValue = 0) {
    val listener = ...

    HardwareManager.register(listener)
}
```

那么 producer 的执行过程就是：

```text
创建 Listener
    ↓
register(listener)
    ↓
register() 返回
    ↓
producer 执行到末尾
    ↓
producer 协程正常结束
```

可是这里出现了一个严重的问题：**producer 协程结束，不代表 HardwareManager 自动注销 Listener。**

`HardwareManager` 仍然持有那个 Listener，以后照样可以不断执行：

```kotlin
listener.onDataReceived(...)
```

而且当 Composable 离开 Composition 时，已经没有一段仍然挂起的 producer 生命周期可以承担“这里应该注销”的职责。

这正是 Callback 型 API 和普通挂起函数最大的不同。

Callback API 通常是：

```text
register()
    ↓
立即返回
    ↓
未来很长一段时间不断 Callback
    ↓
必须显式 unregister()
```

而 `produceState()` 需要的是：

```text
producer 开始
    ↓
register()
    ↓
━━━━━━━━━━━━━━━━━━━━━━
   producer 仍然活着
━━━━━━━━━━━━━━━━━━━━━━
    ↓
Composition 生命周期结束
    ↓
unregister()
    ↓
producer 结束
```

`awaitDispose()` 就是中间那根生命周期支柱。

---

#### 四、`awaitDispose()` 到底在 await 什么？

名字 `awaitDispose` 可以直接拆成：

**await disposal —— 等待这个 producer 被处置。**

调用：

```kotlin
awaitDispose {
    HardwareManager.unregister(listener)
}
```

以后，当前协程会**挂起**。

注意，是挂起，不是阻塞线程。

这一点对学过协程以后应该特别熟悉。它并不是：

```text
while (true) {
    一直占着线程傻等
}
```

而更接近：

```text
保存当前协程的执行状态
        ↓
释放当前线程
        ↓
等待“生命周期结束 / 协程取消”事件
```

因此一个 `produceState` 可以挂在那里几分钟甚至几小时，并不会因为 `awaitDispose()` 而一直占据一个线程。

此时真正工作的其实是外面的 `HardwareManager`：

```text
produceState coroutine
        │
        ├── 已注册 listener
        │
        └── awaitDispose 挂起中
                     ↑
                     │
HardwareManager ── callback ──→ value = heartRate
```

这也终于解释了你之前的困惑：

> “我明明看不到 `produceState` 的协程在干活。”

没错。**在 Callback 场景中，这个协程主要负责的不是“生产数据所需要的线程工作”，而是维持生产者与 Composition 之间的生命周期关系。**

真正的数据什么时候来，是 HardwareManager 的事情；Compose 这边的 producer 负责说：“只要我的 UI 生命周期还有效，这个订阅就有效。”

---

#### 五、页面离开 Composition 时，到底发生了什么？

因为源码内部是：

```kotlin
LaunchedEffect(key1) {
    ProduceStateScopeImpl(...).producer()
}
```

所以当 `produceState()` 的调用位置离开 Composition 时，底层对应的 `LaunchedEffect` 协程会被取消。

此时执行链可以理解成：

```text
Composable 离开 Composition
        ↓
LaunchedEffect 生命周期结束
        ↓
producer 所在协程收到取消
        ↓
awaitDispose 感知到 producer 被处置
        ↓
执行 onDispose
        ↓
HardwareManager.unregister(listener)
        ↓
外部数据源解绑
        ↓
producer 协程彻底结束
```

这就是 `awaitDispose()` 和协程真正连接起来的地方。

所以它不是一个与 `DisposableEffect.onDispose` 毫无关系的神秘 API。两者处理的工程问题其实非常相似：

```kotlin
DisposableEffect(Unit) {
    register(listener)

    onDispose {
        unregister(listener)
    }
}
```

与：

```kotlin
produceState(initialValue = 0) {
    register(listener)

    awaitDispose {
        unregister(listener)
    }
}
```

都表达了“建立长期资源，并在 Composition 生命周期结束时释放”。

区别在于前者是通用副作用 API；后者发生在一个**正在生产 `State<T>` 的协程 producer** 里面，因此清理机制以挂起函数 `awaitDispose()` 的形式出现。

---

#### 六、这里需要纠正旧讲义对 `awaitDispose()` 的一个细微表述

你之前的旧笔记中有一种表述，大意是：

> “协程一直挂起，当页面离开后协程恢复，然后进入 `awaitDispose` 的闭包执行注销逻辑。”

作为直观理解勉强可以，但严格来说不够准确。

你贴出的接口已经暴露了一个非常重要的事实：

```kotlin
public suspend fun awaitDispose(
    onDispose: () -> Unit
): Nothing
```

注意返回值：

```kotlin
Nothing
```

这意味着 `awaitDispose()` **不会正常返回一个结果，让代码继续向下执行。**

也就是说，这种代码在逻辑上没有正常的后续路径：

```kotlin
awaitDispose {
    unregister()
}

// 不应该期待执行到这里
```

更准确的心智模型应该是：

> `awaitDispose()` 将 producer 挂起，直到 producer 因离开 Composition、key 改变等原因被取消；取消发生时执行 `onDispose` 清理逻辑，随后整个 producer 结束。

所以不要把它想成：

```text
暂停
↓
收到信号
↓
正常恢复
↓
执行清理
↓
继续执行 awaitDispose 后面的代码
```

而应该理解成：

```text
暂停
↓
producer 被取消
↓
执行清理逻辑
↓
producer 生命周期终结
```

至于这个 `Nothing` 在底层是如何实现出来的，我们下一阶段看 `awaitDispose()` 实现源码时再验证，现在不提前钻进去。

---

#### 七、key 改变以后为什么旧 Listener 能被注销？

现在把：

```kotlin
key1 = Unit
```

换成更有实际意义的：

```kotlin
key1 = deviceId
```

例如：

```kotlin
val heartRate by produceState(
    initialValue = 0,
    key1 = deviceId
) {
    val listener = ...

    HardwareManager.register(listener)

    awaitDispose {
        HardwareManager.unregister(listener)
    }
}
```

假设第一次：

```text
deviceId = "Watch-A"
```

于是启动 producer A：

```text
LaunchedEffect("Watch-A")
        ↓
register(A)
        ↓
awaitDispose
```

后来用户把手表切换成：

```text
deviceId = "Watch-B"
```

对于 `LaunchedEffect` 而言，key 改变代表原来的 effect 已经失效。因此旧 producer 会被取消，其 `awaitDispose` 清理逻辑被触发，随后新的 key 对应的 producer 建立：

```text
Watch-A producer
        ↓
取消
        ↓
awaitDispose → unregister(A)

Watch-B producer
        ↓
register(B)
        ↓
awaitDispose
```

这和你之前学习 `LaunchedEffect(key)` 时的模型完全一致：

> **key 定义的是这一次异步工作的身份。**

只不过这里取消的工作不仅仅可能是一个 `delay()` 或网络请求，它还可能代表一个外部 Listener 注册关系。

---

#### 八、一个非常容易忽略的细节：key 改变不会自动把 State 重置为 `initialValue`

这一点值得单独记下来，而且从你贴出的源码马上可以推出来。

源码是：

```kotlin
val result = remember {
    mutableStateOf(initialValue)
}

LaunchedEffect(key1) {
    ...
}
```

注意：

```kotlin
remember { ... }
```

没有使用：

```kotlin
remember(key1) { ... }
```

因此 `key1` 改变的时候，重启的是 `LaunchedEffect`，**不是 `result` 这个 State 对象。**

假设：

```text
initialValue = 0

Watch-A 最后一次心率 = 83
```

然后：

```text
deviceId:
Watch-A → Watch-B
```

旧 producer 被取消，新 producer 启动，但是 State 仍然可能暂时保持：

```text
83
```

直到 Watch-B 第一次产生新数据。

它不会因为 key 改变自动重新执行：

```kotlin
mutableStateOf(initialValue)
```

所以如果产品需求是：

> 切换设备以后立刻显示“连接中”，而不是暂时显示上一块手表的 83 BPM。

那么 producer 开始时应该显式重置状态，例如：

```kotlin
produceState(
    initialValue = HeartRateUiState.Loading,
    key1 = deviceId
) {
    value = HeartRateUiState.Loading

    // 开始新的数据生产过程
}
```

这是 `produceState()` 很容易在实际项目里踩到的一个边缘点。

`initialValue` 的主要职责是**第一次创建这个 State 时的初值**，而不是“每次 key 变化时自动恢复的值”。

---

#### 九、为什么普通挂起函数反而不需要 `awaitDispose()`？

这是理解 `awaitDispose` 最有效的对照实验。

假设外部 API 本来就是挂起函数：

```kotlin
suspend fun loadUser(userId: Long): User
```

我们完全可以写：

```kotlin
val user by produceState<User?>(
    initialValue = null,
    key1 = userId
) {
    value = loadUser(userId)
}
```

这里没有：

```kotlin
awaitDispose()
```

为什么？

因为整个异步任务本身就在 producer 协程里面：

```text
produceState coroutine
        ↓
loadUser(userId)
        ↓
挂起等待网络
        ↓
返回 User
        ↓
value = user
        ↓
producer 正常结束
```

如果网络请求还没结束时 Composable 离开 Composition：

```text
Composition 离开
        ↓
LaunchedEffect 协程取消
        ↓
producer 被取消
        ↓
正在执行的可取消挂起操作随之取消
```

这里没有一个已经逃逸到协程之外、需要手动 `unregister()` 的长期资源，因此根本不需要 `awaitDispose()`。

这可以形成一条非常实用的判断规则：

> **如果数据生产过程本身就是挂起式的，并且取消 producer 协程就足以停止数据生产，通常不需要 `awaitDispose()`；如果调用 `register()` 后外部系统会脱离当前协程继续向 Callback 推送数据，那么通常需要 `awaitDispose()` 做配对注销。**

例如：

```text
suspend API
    → 通常直接调用

Flow.collect()
    → 通常直接 collect

Callback register/unregister
    → 通常 awaitDispose

Listener add/remove
    → 通常 awaitDispose

Observer observe/removeObserver
    → 通常 awaitDispose
```

这里先只建立原则。Flow 在现代 Android 中是否应该直接使用 `produceState` 收集，我们留到兄弟 API 和架构边界那一阶段再谈。

---

#### 十、`ProduceStateScope` 作为 `CoroutineScope` 还有什么意义？

因为：

```kotlin
ProduceStateScope<T> : CoroutineScope
```

所以 producer 不只是能调用挂起函数，它本身还有协程上下文。

例如理论上可以：

```kotlin
produceState(initialValue = ...) {
    launch {
        ...
    }

    launch {
        ...
    }
}
```

这些子协程会挂在 producer 对应的协程生命周期下面。当这个 `produceState` 离开 Composition 或 key 改变导致 producer 被取消时，结构化并发关系下的子协程也会随之取消。

但实际项目中不要因为“这里有 CoroutineScope”就把大量业务并发全部塞进去。`produceState` 仍然首先是一个 **UI 状态适配器**，不是新的 ViewModel，也不是 Repository 的替代品。

后面阶段还会专门讨论这个架构边界。

---

#### 十一、实操：新建 `Sec13B_ProduceStateLifecycle.kt`

这一阶段值得增加第二个实验文件，因为单纯看原来的 `Unit` key 很难直观看出协程取消和 `awaitDispose()` 的作用。

文件名：

`Sec13B_ProduceStateLifecycle.kt`

下面这份实验代码故意实现两个效果：

1. 点击按钮在 `Watch-A` 和 `Watch-B` 之间切换，观察旧 producer 的清理与新 producer 的启动。
2. 每个设备第一次数据故意延迟 1.5 秒，用来观察一个非常关键的现象：**key 改变后 State 不会自动恢复 `initialValue`。**

```kotlin
/**
 * @author runningpig66
 * @date 2026/08/08 周六
 * @time 0:44
 */
private const val TAG13B = "Sec13B"

fun interface HeartRateListener {
    fun onHeartRateChanged(heartRate: Int)
}

object MockHeartRateSensor {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jobs = ConcurrentHashMap<HeartRateListener, Job>()

    fun register(deviceId: String, listener: HeartRateListener) {
        log("$TAG13B register: $deviceId")

        val job = scope.launch {
            // 意図的に新しいデバイスの最初のデータを少し遅らせて到着させる
            delay(1500.milliseconds)
            var heartRate = if (deviceId == "Watch-A") 60 else 90
            while (isActive) {
                withContext(Dispatchers.Main.immediate) {
                    listener.onHeartRateChanged(heartRate)
                }
                heartRate++
                delay(1000.milliseconds)
            }
        }
        jobs[listener] = job
    }

    fun unregister(listener: HeartRateListener) {
        log("$TAG13B unregister")
        jobs.remove(listener)?.cancel()
    }
}

@Composable
fun Sec13B_ProduceStateLifecycle() {
    var deviceId by remember { mutableStateOf("Watch-A") }
    val heartRate by produceState(initialValue = 0, key1 = deviceId) {
        log("$TAG13B producer START: $deviceId")
        // value = 0 // オプション：デバイスを切り替えた後に心拍数を初期化する

        val listener = HeartRateListener { newHeartRate ->
            log("$TAG13B callback: $deviceId -> $newHeartRate")
            value = newHeartRate
        }
        MockHeartRateSensor.register(deviceId, listener)

        awaitDispose {
            log("$TAG13B producer DISPOSE: $deviceId")
            MockHeartRateSensor.unregister(listener)
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Current Device: $deviceId")
            Text(text = "Current HeartRate: $heartRate")
            Button(onClick = {
                deviceId = if (deviceId == "Watch-A") "Watch-B" else "Watch-A"
            }) {
                Text(text = "Switch Device")
            }
        }
    }
}

// もう1つの約10行の小さな実験をやってみましょう。producerがそもそもコルーチンであることを証明するものです。
@Composable
fun SuspendProducerExample(userId: Int) {
    val text by produceState(initialValue = "Loading...", key1 = userId) {
        log("$TAG13B load start: userId = $userId")
        delay(3000.milliseconds)
        value = "User $userId loaded"
        log("$TAG13B load finished: userId=$userId")
    }
    Text(text = text)
}

/* Output:
9716 [main @coroutine#125] Sec13B producer START: Watch-A
9716 [main @coroutine#125] Sec13B register: Watch-A
11220 [main @coroutine#129] Sec13B callback: Watch-A -> 60
12223 [main @coroutine#129] Sec13B callback: Watch-A -> 61
12751 [main @coroutine#125] Sec13B producer DISPOSE: Watch-B
12751 [main @coroutine#125] Sec13B unregister
12752 [main @coroutine#135] Sec13B producer START: Watch-B
12752 [main @coroutine#135] Sec13B register: Watch-B
14255 [main @coroutine#136] Sec13B callback: Watch-B -> 90
15259 [main @coroutine#136] Sec13B callback: Watch-B -> 91
16260 [main @coroutine#135] Sec13B producer DISPOSE: Watch-B
16260 [main @coroutine#135] Sec13B unregister
 */

@PhonePreviews
@Composable
fun Sec13BPreview() {
    CourseComposeTheme {
        Sec13B_ProduceStateLifecycle()
    }
}
```

第一次运行时，大体可以观察到：

```text
producer START: Watch-A
register: Watch-A

// 1.5 秒以后

callback: Watch-A -> 60
callback: Watch-A -> 61
callback: Watch-A -> 62
...
```

假设当前已经显示：

```text
Watch-A
62 BPM
```

此时点击“切换设备”，日志会出现类似：

```text
producer DISPOSE: Watch-A
unregister

producer START: Watch-B
register: Watch-B
```

这里先不要死抠日志之间极细微的调度时序，重要的是观察生命周期语义：**旧 key 对应的 producer 被取消并清理，新 key 对应的 producer 建立。**

然后还有第二个非常重要的现象。点击按钮以后，界面上的设备已经变成：

```text
Watch-B
```

但是在 Watch-B 第一条数据到来之前，你很可能暂时仍然看到：

```text
62 BPM
```

而不是：

```text
0 BPM
```

约 1.5 秒以后才变成：

```text
90 BPM
```

这正好实证了前面通过源码推导出来的结论：

> **key 改变会重启 producer，但不会重新创建 `remember` 保存的 State，因此不会自动恢复 `initialValue`。**

如果希望每次换设备立即重置，可以在 producer 一开始显式写：

```kotlin
value = 0
```

于是新的生命周期就变成：

```text
Watch-A → Watch-B
        ↓
旧 producer dispose
        ↓
新 producer start
        ↓
value = 0
        ↓
显示“连接中”
        ↓
Watch-B 第一条 Callback
        ↓
value = 90
```

实际商业项目里通常不会直接拿 `0` 表示加载中，而会使用明确的 UI 状态，例如 `Loading / Success / Error`，这一点后面的架构实战再处理。

---

#### 十二、再做一个十行左右的小实验：证明 producer 本身就是协程

无需单独再创建 `Sec13C`。可以暂时在同一个实验文件里加入：

```kotlin
@Composable
fun SuspendProducerExample(
    userId: Int
) {
    val text by produceState(
        initialValue = "Loading...",
        key1 = userId
    ) {
        Log.d(
            TAG13B,
            "load start: userId=$userId"
        )

        delay(3000)

        value = "User $userId loaded"

        Log.d(
            TAG13B,
            "load finished: userId=$userId"
        )
    }

    Text(text)
}
```

这里没有 `rememberCoroutineScope()`，没有自己 `launch()`，却能直接调用：

```kotlin
delay(3000)
```

原因现在已经非常明确：`producer` 本身就是 `suspend ProduceStateScope<T>.() -> Unit`，而它实际上由内部 `LaunchedEffect` 协程执行。

如果 3 秒还没结束时 `userId` 改变：

```text
userId = 1
    ↓
正在 delay(3000)

userId 变成 2
    ↓
userId=1 的 producer 被取消
    ↓
新的 producer(userId=2) 启动
    ↓
重新 delay(3000)
    ↓
value = "User 2 loaded"
```

这里同样不需要 `awaitDispose()`，因为没有任何注册到外部世界的 Listener 要手动解绑。producer 协程被取消，本身就足以终止这次任务。

---

#### 十三、本阶段必须真正掌握的心智模型

到这里，`produceState()` 已经不能再只理解成“帮我们省一个 `MutableState`”。

它实际上同时组合了两套你之前已经学过的能力：

```text
remember + MutableState
        │
        │ 负责
        ▼
保存和暴露 Compose State

LaunchedEffect
        │
        │ 负责
        ▼
与 Composition 生命周期绑定的协程
```

`ProduceStateScope` 再把两者接起来：

```text
                 produceState
                      │
          ┌───────────┴───────────┐
          │                       │
          ▼                       ▼
      State<T>                Coroutine
          │                       │
          │                       │
          └──────────┬────────────┘
                     ▼
            ProduceStateScope<T>
                     │
                  value = x
```

而 `awaitDispose()` 专门解决的是 Callback 世界里最特殊的生命周期缺口：

```text
register()
    ↓
函数立即返回
    ↓
外部系统仍持续工作
```

因此它通过挂起 producer，把这个外部注册关系重新绑定到 Compose 管理的协程生命周期上：

```text
register
    ↓
awaitDispose
    ↓
producer 保持挂起
    ↓
Composition 离开 / key 改变
    ↓
producer 取消
    ↓
unregister
```

最终可以把本阶段压缩成三句话：

> **第一，`produceState` 的 producer 本身就是一个 Compose 管理的协程，它的生命周期来自内部的 `LaunchedEffect`。**

> **第二，对于普通 suspend 数据源，producer 协程本身就可以承担执行和取消，因此通常不需要 `awaitDispose()`；对于 Callback / Listener 型数据源，注册动作会立即返回，而外部订阅仍持续存在，因此需要 `awaitDispose()` 把注销动作绑定到 producer 的取消生命周期。**

> **第三，key 改变会取消旧 producer 并启动新 producer，但不会重新创建 `remember` 保存的 State，因此 `initialValue` 不会因为 key 改变而自动重新赋值。**

#### 下一阶段：阶段四——`produceState()` 极简源码下钻

下一阶段保持源码范围很克制。你已经提供了 `produceState()` 和 `ProduceStateScope` 的源码，接下来真正值得补齐的只剩几个关键节点：`ProduceStateScopeImpl` 如何同时代理 `MutableState` 和携带 `CoroutineScope`；`awaitDispose()` 为什么返回 `Nothing`、取消时究竟如何触发清理；以及从源码角度验证 `value` 相同值时的状态合并行为。

届时如果 Android Studio 中能够继续点进 `ProduceStateScopeImpl` 与 `awaitDispose()` 的实现，把那两小段源码贴出来即可，不需要把整个 Compose Runtime 文件全部搬过来。

### 阶段四：`produceState()` 极简源码下钻——彻底理清“到底有几个协程”

这一阶段只研究真正决定 `produceState()` 运行模型的几行源码。最核心的问题并不是某个类继承了几个接口，而是要彻底区分三个概念：**CoroutineScope、正在运行的 Coroutine，以及 suspend 函数的调用环境。**

你现在产生“好像有两条协程线”的困惑非常典型，因为源码里确实同时出现了 `LaunchedEffect` 提供的 `CoroutineScope`，以及 `ProduceStateScopeImpl` 实现的 `CoroutineScope`。但最终结论先明确下来：

> **这里没有两个并列运行的 producer 协程。正常情况下只有一条主协程：`LaunchedEffect` 创建的协程。`ProduceStateScopeImpl` 并没有再创建第二条协程，它只是把这条现有协程的 `coroutineContext` 保存下来并作为 `CoroutineScope` 暴露出去。**

后面的源码分析全部围绕这句话展开。

---

#### 一、先修正 `LaunchedEffect` 的三个生命周期方法名称

你刚才复习 `LaunchedEffect` 时，整体运行模型记得很好，不过方法名稍微混淆了。`RememberObserver` 最核心的三个回调是：

`onRemembered()`、`onForgotten()`、`onAbandoned()`。

因此可以把 `LaunchedEffect` 的生命周期粗略理解为：effect 对象进入 Composition 并被 remembered 后，在 `onRemembered()` 中启动协程；当它因为调用位置离开 Composition 或对应的 remember 身份被替换而 forgotten 时，在 `onForgotten()` 中取消原来的 Job。

你刚才说的“进入组合启动，离开组合取消”这个心智模型没有问题。真正需要改掉的只是 `unremembered / unforgotten` 这两个名称。

对于本课来说也不需要重新下钻 `LaunchedEffectImpl`。只需要继续保留这个已经学过的结论：

> **`LaunchedEffect` 提供了一条由 Composition 管理生命周期的协程。**

接下来所有事情都发生在这条协程上。

---

#### 二、从 `produceState()` 源码逐句看运行链

你提供的源码非常短：

```kotlin
@Composable
public fun <T> produceState(
    initialValue: T,
    key1: Any?,
    producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
    val result = remember { mutableStateOf(initialValue) }

    LaunchedEffect(key1) {
        ProduceStateScopeImpl(
            result,
            coroutineContext
        ).producer()
    }

    return result
}
```

第一句：

```kotlin
val result = remember {
    mutableStateOf(initialValue)
}
```

只负责创建并记住真正的 Compose `MutableState<T>`。

所以从源码角度已经可以确认上一阶段的判断：

> `produceState()` 返回给 UI 的 State 并不神秘，本体就是一个由 `remember { mutableStateOf(initialValue) }` 创建出来的状态对象。

真正值得研究的是第二句：

```kotlin
LaunchedEffect(key1) {
    ProduceStateScopeImpl(
        result,
        coroutineContext
    ).producer()
}
```

为了避免 Lambda 语法把事情藏起来，可以把它概念上拆成：

```kotlin
LaunchedEffect(key1) {

    val scope = ProduceStateScopeImpl(
        state = result,
        coroutineContext = coroutineContext
    )

    scope.producer()
}
```

此时最重要的执行关系已经非常清楚了：

```text
LaunchedEffect
    │
    │ 创建并运行 Coroutine A
    ▼
Coroutine A
    │
    ├── 创建普通 Kotlin 对象 ProduceStateScopeImpl
    │
    └── 用这个对象作为 receiver 调用 producer
```

注意中间第二步只是：

```kotlin
ProduceStateScopeImpl(...)
```

**普通对象构造。**

这里没有 `launch`，没有 `async`，没有 `coroutineScope {}`，也没有任何新的 coroutine builder。

因此：

> **构造 `ProduceStateScopeImpl` 本身绝不会创建第二条协程。**

这就是解开你当前疑问的第一把钥匙。

---

#### 三、`CoroutineScope` 对象 ≠ Coroutine

这是这一节最值得记住的区别。

你刚才的推理大致是：

> `ProduceStateScopeImpl` 实现了 `CoroutineScope`，所以有了这个对象以后，即使在同步代码里，它似乎也可以自己调用挂起的 `producer()`。

**这里是不成立的。**

`CoroutineScope` 本质上非常简单。它最核心的东西只是：

```kotlin
public interface CoroutineScope {
    public val coroutineContext: CoroutineContext
}
```

也就是说，一个 `CoroutineScope` 对象本质上只是：

> **“我手里保存着一份 CoroutineContext。”**

它不是一条正在执行的协程，也不会因为这个对象存在就让当前线程突然进入协程。

举一个非常关键的反例。假设：

```kotlin
val scope = CoroutineScope(Dispatchers.Default)

suspend fun loadData() {
    // ...
}
```

在普通同步函数中你仍然不能写：

```kotlin
fun normalFunction() {
    scope.loadData()
}
```

即使假设 `loadData()` 是 `CoroutineScope` 的 suspend 扩展函数，**它仍然是 suspend 函数**，普通同步代码不能直接调用。

编译器依然会告诉你：

```text
Suspend function should be called only
from a coroutine or another suspend function
```

有了 `CoroutineScope`，你能做的是：

```kotlin
scope.launch {
    loadData()
}
```

这里真正创建协程的是 `launch`，不是 `scope` 本身。

所以必须牢牢记住：

> **CoroutineScope 是“启动和组织协程所需要的上下文载体”，Coroutine 才是真正执行 suspend 代码的东西。**

这两个概念不能混在一起。

---

#### 四、为什么 `scope.producer()` 可以调用？

现在回到：

```kotlin
scope.producer()
```

其中 `producer` 的类型是：

```kotlin
suspend ProduceStateScope<T>.() -> Unit
```

这里同时存在两个彼此独立的维度。

第一个维度是 **receiver**。`ProduceStateScopeImpl` 作为 receiver，决定 producer 内部的 `this` 是谁，因此你才能直接访问：

```kotlin
value
coroutineContext
launch { ... }
awaitDispose { ... }
```

第二个维度是 **suspend 调用环境**。决定这个 `producer()` 能不能被调用的，不是 receiver 实现了 `CoroutineScope`，而是：

```kotlin
scope.producer()
```

这句话本身正写在：

```kotlin
LaunchedEffect {
    ...
}
```

这个 suspend Lambda 中。

也就是说，**当前已经处于 LaunchedEffect 创建的 Coroutine A 里了。**

因此调用链是：

```text
LaunchedEffect 创建 Coroutine A
        ↓
Coroutine A 执行 block
        ↓
创建 ProduceStateScopeImpl
        ↓
Coroutine A 调用 suspend producer()
        ↓
producer 继续运行在 Coroutine A 中
```

没有发生：

```text
Coroutine A
   ↓
ProduceStateScopeImpl
   ↓
突然创建 Coroutine B
```

这一步非常重要。

调用 suspend 函数不会自动创建协程。

例如你以前学过：

```kotlin
launch {
    delay(1000)
    loadData()
    flow.collect()
}
```

`delay()`、`loadData()`、`collect()` 都可能是 suspend 函数，但并不是每调用一次 suspend 函数就创建一条协程。

它们只是：

> **在当前协程中执行，并且拥有挂起当前协程的能力。**

`producer()` 也是完全相同的道理。

---

#### 五、为什么 `ProduceStateScopeImpl` 还非得实现 `CoroutineScope`？

现在反过来就能理解这个设计了。

接口是：

```kotlin
public interface ProduceStateScope<T> :
    MutableState<T>,
    CoroutineScope
```

所以 producer：

```kotlin
produceState(...) {

}
```

内部既拥有状态能力，也拥有协程 Scope 能力。

关键在实现类：

```kotlin
private class ProduceStateScopeImpl<T>(
    state: MutableState<T>,
    override val coroutineContext: CoroutineContext,
) : ProduceStateScope<T>,
    MutableState<T> by state
```

构造它的时候，传进去的是：

```kotlin
ProduceStateScopeImpl(
    result,
    coroutineContext
)
```

而这个 `coroutineContext` 是在哪里读取到的？

就在：

```kotlin
LaunchedEffect {
    ...
}
```

内部。

所以它实际上拿的是**当前 LaunchedEffect 协程的 CoroutineContext**。

可以画成：

```text
LaunchedEffect Coroutine A

CoroutineContext A
    │
    ├── Job A
    ├── Dispatcher / ContinuationInterceptor
    ├── CoroutineName 等
    └── 其他 Context Element
             │
             │ 原样交给
             ▼
ProduceStateScopeImpl
    │
    └── coroutineContext = CoroutineContext A
```

所以不是：

```text
LaunchedEffect 有 Scope A

ProduceStateScopeImpl 又创建 Scope B
```

而是：

```text
同一个 CoroutineContext A

一方面：
LaunchedEffect 当前正在使用它运行 Coroutine A

另一方面：
ProduceStateScopeImpl 把它暴露成 CoroutineScope
```

这两个“Scope 感”其实指向的是**同一棵协程生命周期树**。

这就是你刚才感觉“两条线”的真正来源。

---

#### 六、那在 producer 里面调用 `launch {}` 会发生什么？

例如：

```kotlin
produceState(initialValue = 0) {

    launch {
        // child work
    }

    awaitDispose {
        // cleanup
    }
}
```

为什么这里可以直接写 `launch`？

因为 receiver：

```kotlin
this
```

是：

```kotlin
ProduceStateScopeImpl
```

而它实现了：

```kotlin
CoroutineScope
```

所以 Kotlin 可以解析到：

```kotlin
CoroutineScope.launch(...)
```

此时 `launch` 才真正创建一条新的 Coroutine B。

运行关系变成：

```text
LaunchedEffect
    │
    ▼
Coroutine A —— producer
    │
    ├── value = ...
    │
    ├── awaitDispose(...)
    │
    └── launch { ... }
             │
             ▼
         Coroutine B
```

而因为 `ProduceStateScopeImpl.coroutineContext` 就是 Coroutine A 当前使用的 Context，因此 Coroutine B 会纳入这套 Job 生命周期关系之中。

当 `LaunchedEffect` 对应的工作被取消时，这些结构化的子协程也会随之取消。

所以真正出现“第二条协程”的条件不是：

```kotlin
ProduceStateScopeImpl(...)
```

而是你自己明确调用：

```kotlin
launch { ... }
```

这与正常协程代码完全一致。

---

#### 七、现在来看 `awaitDispose()`：它监听的到底是哪条协程？

源码是：

```kotlin
override suspend fun awaitDispose(
    onDispose: () -> Unit
): Nothing {
    try {
        suspendCancellableCoroutine<Nothing> {}
    } finally {
        onDispose()
    }
}
```

答案现在其实已经非常直接了：

> **它挂起和监听的就是当前正在执行 producer 的那一条 LaunchedEffect 协程，也就是前面的 Coroutine A。**

并不存在“ProduceStateScopeImpl 自己的另一条协程”。

调用链如下：

```text
Coroutine A
    │
    └── producer()
          │
          ├── register(listener)
          │
          └── awaitDispose()
                 │
                 └── suspendCancellableCoroutine
                        │
                        ▼
                  Coroutine A 挂起
```

因此被挂起来的从始至终就是 **Coroutine A**。

---

#### 八、`suspendCancellableCoroutine` 是什么？

你以前协程课程中学过它，这里只恢复本课需要的知识，不重新开一节协程源码课。

` suspendCancellableCoroutine<T>` 是 Kotlin Coroutines 提供的一个非常重要的底层桥接工具。它的典型用途是：

> **把 Callback API 转换成 suspend API。**

经典模式大概是：

```kotlin
suspend fun awaitSomething(): Result =
    suspendCancellableCoroutine { continuation ->

        api.request(
            callback = { result ->
                continuation.resume(result)
            }
        )

        continuation.invokeOnCancellation {
            api.cancel()
        }
    }
```

当执行到 `suspendCancellableCoroutine` 时，当前协程可以真正挂起；未来某个 Callback 再通过 `Continuation` 把它恢复。

而 `awaitDispose()` 做了一件非常聪明、甚至有一点“故意耍赖”的事情：

```kotlin
suspendCancellableCoroutine<Nothing> {}
```

Lambda 里面：

```kotlin
{}
```

**什么都没做。**

既没有：

```kotlin
continuation.resume(...)
```

也没有别的正常恢复路径。

因此这条协程一旦执行到这里：

```text
Coroutine A
    ↓
suspendCancellableCoroutine
    ↓
挂起
    ↓
没人 resume
    ↓
正常情况下永远不会继续
```

这正是它想要的结果。

---

#### 九、为什么泛型偏偏是 `Nothing`？

源码不是：

```kotlin
suspendCancellableCoroutine<Unit> {}
```

而是：

```kotlin
suspendCancellableCoroutine<Nothing> {}
```

这是一个非常漂亮的 Kotlin 类型表达。

`Nothing` 表示：

> **这段计算不存在正常返回的值。**

因此：

```kotlin
awaitDispose(...)
```

自己的返回类型也是：

```kotlin
Nothing
```

它在类型层面直接告诉你：

> “不要期待我正常返回。”

所以正常模型不是：

```text
awaitDispose()
    ↓
等一会
    ↓
返回
    ↓
继续执行下一行
```

而是：

```text
awaitDispose()
    ↓
挂起
    ↓
一直等到协程取消
    ↓
清理
    ↓
整个 producer 结束
```

这也是为什么 `awaitDispose()` 通常就是 callback producer 的终点。

---

#### 十、那么取消发生后，为什么一定能进入 `finally`？

这里正是 `suspendCancellableCoroutine` 名字中的 **Cancellable** 发挥作用的地方。

假设现在：

```kotlin
key1 = "Watch-A"
```

producer 已经运行到：

```kotlin
suspendCancellableCoroutine<Nothing> {}
```

于是 Coroutine A 正处于可取消的挂起状态。

现在用户切换：

```text
Watch-A → Watch-B
```

于是 `LaunchedEffect` 的 key 变化，旧 effect 对应的 Job 被取消。

取消沿着当前 coroutine context 传播到：

```kotlin
suspendCancellableCoroutine
```

这个可取消挂起点。

协程因此从“永久等待正常 resume”转入**取消展开（cancellation unwinding）**。

概念上可以理解为：

```text
Job.cancel()
    ↓
suspendCancellableCoroutine 感知取消
    ↓
当前挂起以 CancellationException 的形式终止
    ↓
控制流开始向外展开
    ↓
finally 必须执行
```

于是：

```kotlin
finally {
    onDispose()
}
```

执行。

也就最终调用：

```kotlin
MockHeartRateSensor.unregister(listener)
```

因此完整链条终于可以一次画到底：

```text
Watch-A producer
      │
      ▼
LaunchedEffect Coroutine A
      │
      ▼
register(listenerA)
      │
      ▼
awaitDispose()
      │
      ▼
suspendCancellableCoroutine
      │
      └──── Coroutine A 挂起
                     │
                     │ key: A → B
                     ▼
             LaunchedEffect 取消 Job A
                     │
                     ▼
        suspendCancellableCoroutine 感知取消
                     │
                     ▼
                  finally
                     │
                     ▼
            unregister(listenerA)
                     │
                     ▼
             Coroutine A 结束
```

随后新的 key 对应的新 `LaunchedEffect` 工作启动新的 producer。

**这就是整个 `produceState + awaitDispose` Callback 桥接的核心流转。**

---

#### 十一、为什么不能随便换成一个普通“永远挂起”的函数？

你刚才提到了一个很好的直觉：

> “这里是不是故意写一个挂起占位，为了将来能够进入 finally？”

基本方向是对的，但可以再精确一点。

它确实需要一个能够：

**挂起当前 producer，并且能响应当前 Job 取消的 suspension point。**

如果这个挂起点无法感知取消，那么 `LaunchedEffect` Job 被取消以后，producer 就无法及时从挂起状态退出，`finally` 自然也无法正常按照预期完成资源释放。

`suspendCancellableCoroutine` 正好同时提供：

```text
挂起
+
感知 Job cancellation
```

所以这里才使用它。

真正关键的不是“必须有一行代码才能进入 finally”，而是：

> **必须把 producer 停在一个可取消的挂起点上，这样 Composition 生命周期通过 Job cancellation 才能唤起清理过程。**

这个表述更加准确。

---

#### 十二、你运行出的 `DISPOSE: Watch-B` 非常值得研究

你实际运行得到：

```text
Sec13B producer START: Watch-A
Sec13B register: Watch-A

...

Sec13B producer DISPOSE: Watch-B
Sec13B unregister

Sec13B producer START: Watch-B
```

而我上一阶段预期写的是：

```text
producer DISPOSE: Watch-A
```

你的实际结果是对的，而且这里暴露出了一个非常好的 Compose/Kotlin 闭包问题。

看原代码：

```kotlin
var deviceId by remember {
    mutableStateOf("Watch-A")
}

val heartRate by produceState(
    initialValue = 0,
    key1 = deviceId
) {
    log("$TAG13B producer START: $deviceId")

    // ...

    awaitDispose {
        log("$TAG13B producer DISPOSE: $deviceId")
        MockHeartRateSensor.unregister(listener)
    }
}
```

关键在：

```kotlin
deviceId
```

它不是一个被永久冻结成 `"Watch-A"` 的普通常量。

它背后来自：

```kotlin
MutableState<String>
```

而 `awaitDispose` 的清理 Lambda 又捕获了这个外部状态。

当用户点击按钮时，首先发生：

```text
deviceId State:
Watch-A → Watch-B
```

之后旧 producer 因 key 改变被取消。

而旧 producer 的清理代码此时再读取：

```kotlin
deviceId
```

拿到的已经可能是当前状态：

```text
Watch-B
```

所以日志出现：

```text
producer DISPOSE: Watch-B
```

这并不意味着：

> “Compose 错把 Watch-B 的 producer dispose 了。”

实际上被注销的仍然是旧 producer 创建的：

```kotlin
listener
```

所以：

```kotlin
MockHeartRateSensor.unregister(listener)
```

仍然注销的是正确的旧 Listener。

**错的只是日志标签。**

---

#### 十三、怎么让日志准确显示旧 producer 属于 Watch-A？

最简单的方法就是在 producer 生命周期刚开始时拍一张不可变快照：

```kotlin
val heartRate by produceState(
    initialValue = 0,
    key1 = deviceId
) {
    val activeDeviceId = deviceId

    log("$TAG13B producer START: $activeDeviceId")

    val listener = HeartRateListener { newHeartRate ->
        log("$TAG13B callback: $activeDeviceId -> $newHeartRate")
        value = newHeartRate
    }

    MockHeartRateSensor.register(
        activeDeviceId,
        listener
    )

    awaitDispose {
        log("$TAG13B producer DISPOSE: $activeDeviceId")
        MockHeartRateSensor.unregister(listener)
    }
}
```

现在：

```kotlin
activeDeviceId
```

是这个 producer 启动那一刻读取出来的普通 `String`。

于是旧 producer 生命周期始终记录：

```text
Watch-A
```

新的 producer 始终记录：

```text
Watch-B
```

日志就会更加符合生命周期语义：

```text
producer START: Watch-A
register: Watch-A

producer DISPOSE: Watch-A
unregister

producer START: Watch-B
register: Watch-B
```

这个现象实际上与你已经学过的 `rememberUpdatedState()` 那一课存在联系：**闭包到底应该读取“当前最新值”，还是应该冻结“这一次任务启动时对应的值”？**

在这里，我们想描述“这次 producer 属于哪一台设备”，所以应该冻结启动时的值。

这也是为什么你实际跑代码非常有价值。如果只是读代码，这个细节很容易直接漏过去。

---

#### 十四、`MutableState<T> by state` 又在干什么？

实现类还有这一句：

```kotlin
private class ProduceStateScopeImpl<T>(
    state: MutableState<T>,
    override val coroutineContext: CoroutineContext,
) : ProduceStateScope<T>,
    MutableState<T> by state
```

这里不需要深入 Kotlin delegation 源码，因为你已经学过 Kotlin。

它意味着：

> `ProduceStateScopeImpl` 自己不重新实现一套 `MutableState`，而是把 `MutableState` 的操作全部委托给传进来的 `state`。

而传进来的 `state` 正是：

```kotlin
result
```

也就是最开始：

```kotlin
val result = remember {
    mutableStateOf(initialValue)
}
```

所以 producer 内部：

```kotlin
value = heartRate
```

实际最终修改的是同一个：

```kotlin
result.value
```

然后函数外面：

```kotlin
return result
```

又把同一个 State 返回给 UI。

因此状态链条可以精确画成：

```text
remember
   │
   ▼
MutableState<T> result
   │
   ├───────────────────────────┐
   │                           │
   │ 返回给 Composable         │ 委托给
   ▼                           ▼
State<T>                ProduceStateScopeImpl
                               │
                               ▼
                         value = newValue
                               │
                               ▼
                         result.value =
```

所以这里完全不存在两个 State。

和协程问题一样：

> **看起来出现了两个对象角色，但实际上它们共享同一个底层资源。**

协程方面共享同一个 `CoroutineContext`；状态方面共享同一个 `MutableState result`。

这也是这段源码设计得非常漂亮的地方。

---

#### 十五、相同的 `value` 为什么可能不会重组？

这件事不是你本次贴出的 `ProduceStateScopeImpl` 源码本身定义的，而是来自它委托的：

```kotlin
mutableStateOf(initialValue)
```

的正常 Compose State 行为。

所以：

```kotlin
value = 80
value = 80
value = 80
```

默认情况下并不意味着必须触发三次有效状态变化。

`produceState()` 没有自己重新设计一套状态通知机制，它最终还是修改：

```kotlin
result.value
```

因此遵守正常 `MutableState` 的 mutation policy。

这也意味着 `produceState` 天然具有所谓的 **state conflation** 特性：如果生产者不断产生与当前状态等价的值，Compose 没有必要仅仅因为“又赋值了一次”就重新组合消费者。

这部分无需继续钻 `SnapshotMutableStateImpl`；知道它来自普通 `MutableState` 的语义即可。

---

#### 十六、现在把整个源码压缩成真正需要记住的版本

完整源码虽然只有十几行，但真正值得长期记住的其实就是下面这套模型：

```text
produceState()
       │
       ├── remember { mutableStateOf(initialValue) }
       │               │
       │               └── 真正的数据容器
       │
       └── LaunchedEffect(key)
                │
                └── 创建 Composition 管理的 Coroutine A
                         │
                         ▼
               ProduceStateScopeImpl
                   │             │
                   │             └── coroutineContext
                   │                 = Coroutine A 的 Context
                   │
                   └── MutableState by result
                         │
                         ▼
                     value = xxx
```

Callback 场景再接上：

```text
Coroutine A
    │
    ▼
producer()
    │
    ├── register(listener)
    │
    ▼
awaitDispose()
    │
    ▼
suspendCancellableCoroutine<Nothing> {}
    │
    │ 挂起 Coroutine A
    │
    ▼
等待 LaunchedEffect Job 被取消
    │
    ▼
finally
    │
    ▼
onDispose()
    │
    ▼
unregister(listener)
```

所以现在可以精确回答你最开始那个“到底谁取消谁”的问题：

> **真正负责发起取消的是 `LaunchedEffect` 所管理的 Job。`ProduceStateScopeImpl` 不负责创建第二条 producer 协程，也不独立发起另一套取消。它只是持有当前 `LaunchedEffect` 协程的 `coroutineContext`。`awaitDispose()` 又通过 `suspendCancellableCoroutine` 挂在这同一条协程上，因此当 `LaunchedEffect` 的 Job 因离开 Composition 或 key 改变而取消时，`awaitDispose()` 感知同一次 cancellation，执行 `finally → onDispose()`。**

这就是一条线，而不是两条线：

```text
Composition
    ↓
LaunchedEffect Job
    ↓
producer
    ↓
ProduceStateScopeImpl
    ↓
awaitDispose
```

`ProduceStateScopeImpl` 是这条线里的“能力包装器”，不是另外一台协程发动机。

---

#### 十七、这一阶段之后，对源码应该掌握到什么程度？

到这里已经足够。

没有必要继续为了 `produceState()` 去追：

`LaunchedEffectImpl` 的全部实现、`RememberObserver` 的 Runtime 调度、`Recomposer`、SlotTable、Snapshot 内部数据结构、`suspendCancellableCoroutine` 的 continuation 实现。

那些东西当然都能继续看，但已经明显偏离“理解并熟练使用 `produceState()`”这个目标。

这一课真正值得看的源码，到这里实际上已经完成。

你应该能够脱离源码回答四个问题：

**为什么 producer 能调用 suspend 函数？** 因为它运行在 `LaunchedEffect` 创建的协程中，本身也是 suspend Lambda。

**为什么 `ProduceStateScopeImpl` 又实现 `CoroutineScope`？** 因为它把当前 `LaunchedEffect` 协程的 `coroutineContext` 暴露给 producer，使 producer 可以基于同一生命周期启动结构化子协程。

**为什么 `awaitDispose()` 能等到页面退出？** 因为它通过 `suspendCancellableCoroutine` 把当前 producer 协程挂起，而这个协程对应的 Job 正由 `LaunchedEffect` 跟随 Composition 管理。

**为什么取消之后一定执行注销？** 因为 cancellation 使可取消挂起点结束，控制流展开时执行 `finally { onDispose() }`。

如果这四件事情已经完全通了，源码部分就应该果断结束，而不是继续往 Compose Runtime 地下室挖。

#### 下一阶段：阶段五——把容易打架的 API 一次分干净

接下来回到原教学大纲，不再继续源码下钻。阶段五会系统建立 `DisposableEffect`、`LaunchedEffect`、`rememberCoroutineScope`、`produceState` 以及 Flow → Compose State 这一组 API 的选择边界，并回答真正项目里最重要的问题：**什么时候外部数据应该直接在 Compose 层用 `produceState` 桥接，什么时候应该进入 ViewModel + StateFlow，什么时候使用 `produceState` 反而是在破坏架构。**

阶段六再做实际业务落地与最终场景题验收，因此原大纲剩余内容没有丢，只是源码部分因为你本轮提出的疑问多拆了一次。

### 阶段五：把容易打架的 API 一次分干净——`DisposableEffect`、`LaunchedEffect`、`rememberCoroutineScope`、`produceState` 与 Flow

经过前四个阶段以后，`produceState()` 本身的运行机制已经基本结束。接下来最重要的不是继续记它的参数，而是把它放回 Compose 的整个副作用体系中：面对一个真实需求时，究竟应该选哪个 API。

这也是 `produceState()` 最容易学歪的地方。因为如果只看“能够实现什么功能”，这几个 API 之间存在很大的能力重叠。例如 `LaunchedEffect + mutableStateOf` 完全可以手写出 `produceState`；`DisposableEffect + MutableState` 也能桥接 Callback；`rememberCoroutineScope` 也可以启动协程。**区别不在于“能不能实现”，而在于这个 API 所表达的任务模型是什么。**

#### 一、先建立一张总地图：Compose 到底提供了哪几种“副作用任务”

可以先把这一组 API 按照“是什么东西触发工作”分成三类。

| API                           | 谁触发工作                   |     是否协程 |                         是否天然有清理 | 主要输出           |
| ----------------------------- | ----------------------- | -------: | ------------------------------: | -------------- |
| `DisposableEffect`            | 进入 Composition / key 改变 |        否 |                   是，`onDispose` | 副作用本身          |
| `LaunchedEffect`              | 进入 Composition / key 改变 |        是 |                          通过协程取消 | 副作用本身          |
| `rememberCoroutineScope`      | 用户事件等命令式调用              |        是 |          Scope 随 Composition 取消 | 副作用本身          |
| `produceState`                | 进入 Composition / key 改变 |        是 | 协程取消；Callback 可用 `awaitDispose` | **`State<T>`** |
| `collectAsStateWithLifecycle` | Composition + Lifecycle |     内部处理 |                            内部处理 | **`State<T>`** |
| `snapshotFlow`                | Compose State 发生变化      | 需要在协程中收集 |                         取决于收集协程 | **`Flow<T>`**  |

其中最值得观察的是最后一列。

前面几个副作用 API 的核心诉求都是：

> “我要执行一件事情。”

而 `produceState` 的核心诉求是：

> “我要**产生一个 State**。”

这就是它在整个 Compose API 家族里的位置。

---

#### 二、`DisposableEffect` vs `produceState`：关键看“Listener 的数据是不是 UI 状态”

先从我们已经写过的心率例子开始。

如果需求只是：

> 页面出现时注册一个 Listener，页面消失时注销 Listener。

例如注册一个分析 SDK：

```kotlin
DisposableEffect(Unit) {
    analytics.registerScreen("Home")

    onDispose {
        analytics.unregisterScreen("Home")
    }
}
```

这里没有任何数据需要交给 Compose UI。

这种情况下使用：

```kotlin
produceState(...)
```

反而很奇怪，因为你根本没有 State 要 produce。

所以 `DisposableEffect` 非常适合：

```text
register / unregister
addListener / removeListener
addObserver / removeObserver
绑定 / 解绑某种外部资源
```

而现在把需求改变一点：

> 注册心率监听器，而且 Listener 返回的心率就是当前 UI 要显示的状态。

这时你如果手写：

```kotlin
var heartRate by remember {
    mutableIntStateOf(0)
}

DisposableEffect(deviceId) {
    val listener = HeartRateListener {
        heartRate = it
    }

    sensor.register(deviceId, listener)

    onDispose {
        sensor.unregister(listener)
    }
}
```

完全正确。

`produceState` 只是把问题表达得更加直接：

```kotlin
val heartRate by produceState(
    initialValue = 0,
    key1 = deviceId
) {
    val listener = HeartRateListener {
        value = it
    }

    sensor.register(deviceId, listener)

    awaitDispose {
        sensor.unregister(listener)
    }
}
```

两者能力上高度重叠，但语义不同。

前者读起来是：

> “创建一个 State；另外做一个需要清理的副作用；副作用顺便修改 State。”

后者读起来就是：

> “从这个外部数据源生产一个 State。”

因此可以形成一个非常实用的判断：

> **如果“注册—监听—注销”本身就是目的，优先考虑 `DisposableEffect`；如果这个监听过程的核心目的就是持续把外部数据转换为一个 `State<T>`，`produceState` 更贴切。**

---

#### 三、`LaunchedEffect` vs `produceState`：这是最容易混淆的一对

因为你已经看过源码，所以现在知道：

```kotlin
produceState(...)
```

内部本来就是：

```kotlin
remember { mutableStateOf(...) }

LaunchedEffect(...) {
    ...
}
```

于是问题自然出现：

> 那我直接用 `LaunchedEffect` 不就行了吗？

当然可以。

例如有一个挂起函数：

```kotlin
suspend fun loadUser(userId: Long): User
```

完全可以这样写：

```kotlin
var user by remember {
    mutableStateOf<User?>(null)
}

LaunchedEffect(userId) {
    user = loadUser(userId)
}
```

也可以写：

```kotlin
val user by produceState<User?>(
    initialValue = null,
    key1 = userId
) {
    value = loadUser(userId)
}
```

功能几乎一样。

真正的选择标准仍然是**意图**。

`LaunchedEffect` 的语义是：

> 当这个 Composable / key 进入某种状态时，启动一个 suspend side effect。

它不要求最终产生 State。例如：

```kotlin
LaunchedEffect(Unit) {
    analytics.logScreenOpened()
}
```

或者：

```kotlin
LaunchedEffect(message) {
    snackbarHostState.showSnackbar(message)
}
```

这些操作都不是为了“生产一个值给 UI”。

而 `produceState` 的 API 形状已经强行限定了：

```kotlin
initialValue
        ↓
producer
        ↓
value = ...
        ↓
State<T>
```

所以它表达的是一个更窄、更明确的问题：

> **我运行异步工作，就是为了产生这个 Compose State。**

因此不要记：

> `produceState` 比 `LaunchedEffect` 强。

应该记：

> **`produceState` 比 `LaunchedEffect` 专。**

`LaunchedEffect` 是通用工具；`produceState` 是“异步数据 → State”的专用适配器。

---

#### 四、`rememberCoroutineScope` 和 `produceState` 的根本区别：命令式事件 vs 声明式状态

这是旧大纲中特别列出来的一组比较，而且确实值得保留。

假设有：

```kotlin
val scope = rememberCoroutineScope()

Button(
    onClick = {
        scope.launch {
            snackbarHostState.showSnackbar("Saved")
        }
    }
) {
    Text("Save")
}
```

为什么不能简单替换成 `LaunchedEffect` 或 `produceState`？

因为这里什么时候启动协程，不是由 Composition 状态本身直接决定，而是由：

```kotlin
onClick
```

这个**命令式事件**决定。

`rememberCoroutineScope()` 的作用是：

> 给这个 Composable 提供一个生命周期受 Composition 管理的 `CoroutineScope`，让我以后在某个事件回调里自行决定什么时候 `launch()`。

因此：

```text
点击按钮
   ↓
onClick
   ↓
scope.launch
```

这是事件驱动。

而：

```kotlin
produceState(
    initialValue = ...,
    key1 = userId
) {
    ...
}
```

是：

```text
Composition 中需要这个 State
        ↓
producer 自动启动

userId 改变
        ↓
旧 producer 自动取消
        ↓
新 producer 自动启动
```

这是声明式生命周期驱动。

所以可以把二者压缩成一句非常重要的话：

> **`rememberCoroutineScope` 回答的是“事件发生以后，我要启动协程”；`produceState` 回答的是“只要这个 UI 需要存在，我就需要维护这份异步状态”。**

例如：

```text
点击“保存”
→ rememberCoroutineScope

点击“删除”
→ rememberCoroutineScope

点击以后显示 Snackbar
→ rememberCoroutineScope

页面需要实时显示当前网络状态
→ produceState 候选

页面需要实时显示某个 Callback 数据源
→ produceState 候选
```

---

#### 五、但现代 Android 项目真正最大的竞争对手不是这些 API，而是 `ViewModel + StateFlow`

到这里才来到 `produceState()` 在生产项目中最重要的问题。

假设我们的记账 App 有：

```kotlin
data class AccountUiState(
    val income: Long,
    val expense: Long,
    val transactions: List<Transaction>
)
```

ViewModel 暴露：

```kotlin
val uiState: StateFlow<AccountUiState>
```

UI 层通常应该做的是：

```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

而不是：

```kotlin
val uiState by produceState(...) {
    viewModel.uiState.collect {
        value = it
    }
}
```

后者虽然技术上可以工作，但属于**重新手写一个已经存在的标准桥梁**。

在 Android Compose 项目中，如果 Flow 来自 ViewModel，通常优先使用生命周期感知的：

```kotlin
collectAsStateWithLifecycle()
```

它直接表达：

```text
Flow / StateFlow
        ↓
按照 Android Lifecycle 收集
        ↓
Compose State
```

因此这条边界非常重要：

> **已经是 Flow 的东西，不要因为刚学会 `produceState` 就全部拿它包一遍。**

`produceState` 更有价值的场景往往是：

```text
Callback
Listener
某个 suspend API
外部 SDK
其他不是 Compose State 的数据源
```

需要在 **Compose 边界附近** 临时适配成 `State<T>`。

---

#### 六、那么 suspend API 是放在 `produceState`，还是 ViewModel？

上一阶段举过：

```kotlin
val user by produceState<User?>(
    initialValue = null,
    key1 = userId
) {
    value = loadUser(userId)
}
```

这个例子是合法的，用来解释 API 也很好。

但不能因此推导：

> “以后 Repository 网络请求都直接在 Composable 里 `produceState`。”

这就涉及架构边界。

假设：

```kotlin
repository.getTransactions()
```

是记账 App 的核心业务请求，那么它涉及：

* Loading；
* Error；
* Retry；
* 缓存；
* 数据库；
* 用户登录状态；
* 多个页面共享；
* 配置变更；
* UI 离开后是否继续；
* 测试。

这种状态明显属于 ViewModel / domain / data 层负责管理，而不是某一个 Composable 的临时生命周期。

因此更合理的是：

```text
Repository
    ↓
ViewModel
    ↓
StateFlow<UiState>
    ↓
collectAsStateWithLifecycle()
    ↓
Composable
```

而不是：

```text
Composable
    ↓
produceState
    ↓
Repository
```

后者不是绝对禁止，但对于核心业务状态通常说明架构层次正在倒置。

所以 `produceState` 的关键边界可以概括成：

> **它适合“UI 边界的数据适配”，而不是“业务状态中心”。**

---

#### 七、一个非常典型的 `produceState` 正当场景

例如某个 Composable 需要加载图片之外的第三方对象，而第三方 SDK 只提供 Callback：

```kotlin
interface MapSdk {
    fun requestLocation(
        callback: LocationCallback
    )

    fun removeLocationCallback(
        callback: LocationCallback
    )
}
```

而这个状态只服务于当前 UI 元素。

这种情况下写一个可复用的 Composable State Adapter 很合理：

```kotlin
@Composable
fun rememberSdkLocation(
    sdk: MapSdk
): State<Location?> {
    return produceState<Location?>(
        initialValue = null,
        key1 = sdk
    ) {
        val callback = LocationCallback { location ->
            value = location
        }

        sdk.requestLocation(callback)

        awaitDispose {
            sdk.removeLocationCallback(callback)
        }
    }
}
```

调用者只看到：

```kotlin
val location by rememberSdkLocation(sdk)
```

这里 `produceState` 的优势就非常明显了。

外面的 UI 不需要知道：

```text
callback
register
unregister
awaitDispose
```

它只需要知道：

```text
这里有一个 Compose State<Location?>
```

这实际上是一种很漂亮的**适配层封装**。

---

#### 八、而什么时候应该直接用 `DisposableEffect`？

假设地图页面需要把一个对象绑定到 SDK：

```kotlin
DisposableEffect(mapView) {
    mapView.onStart()

    onDispose {
        mapView.onStop()
    }
}
```

这里没有状态要返回，直接 `DisposableEffect`。

再比如：

```kotlin
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        ...
    }

    lifecycleOwner.lifecycle.addObserver(observer)

    onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
    }
}
```

如果你只是执行生命周期事件，不需要把 observer 输出转换为 State，同样没有必要使用 `produceState`。

所以遇到 Listener 时，不要形成：

```text
Listener = produceState
```

正确判断是：

```text
Listener
   │
   ├── 只是建立/解除副作用
   │       → DisposableEffect
   │
   └── Listener 的值需要成为 State<T>
           → produceState 是候选
```

---

#### 九、`snapshotFlow` 正好是 `produceState` 的反方向

课程目录把：

`4.13 produceState`
`4.14 snapshotFlow`

放在一起其实非常合理。课程本身也明确把两者定义为两个相反的转换方向：`produceState` 是“非 Compose 状态转换成 Compose 状态”，而下一节 `snapshotFlow` 是“Compose 状态转换成协程 Flow”。

可以先建立方向图：

```text
外部数据世界
Callback / suspend / ...
        │
        │ produceState
        ▼
Compose State<T>
        │
        │ snapshotFlow
        ▼
Flow<T>
```

例如：

```kotlin
var searchQuery by remember {
    mutableStateOf("")
}
```

如果只是：

```kotlin
Text(searchQuery)
```

当然直接读取 State。

但如果需求变成：

> 用户输入搜索内容以后，转换成 Flow，使用 `debounce()`、`distinctUntilChanged()` 等 Flow 操作符，然后执行搜索。

这时候 `snapshotFlow` 才有意义：

```kotlin
LaunchedEffect(Unit) {
    snapshotFlow { searchQuery }
        .debounce(300)
        .collect { query ->
            // 执行副作用
        }
}
```

我们下一课会专门学习它，因此现在不展开语义和陷阱。

这里只需要形成这对关系：

> `produceState` 是 **进入 Compose State 世界**；`snapshotFlow` 是 **从 Compose State 世界进入 Flow 世界**。

---

#### 十、五个真实需求，应该怎么选？

现在直接做一次实战判断。

**场景 1：页面进入以后注册广播接收器，页面退出以后注销，但是广播内容不需要显示。**

选择：

`DisposableEffect`

因为核心需求是资源注册/注销。

---

**场景 2：页面进入以后启动一个每秒更新一次的倒计时，离开页面自动停止。**

选择：

`LaunchedEffect`

例如：

```kotlin
LaunchedEffect(Unit) {
    while (isActive) {
        delay(1.seconds)
        // 执行倒计时逻辑
    }
}
```

核心是生命周期绑定的 suspend side effect。

如果“倒计时结果本身就是这个 API 想返回的 State”，也完全可以进一步封装成 `produceState`。这里再次说明二者并不存在不可逾越的能力边界。

---

**场景 3：用户点击保存按钮以后调用 suspend 保存函数，然后显示 Snackbar。**

选择：

`rememberCoroutineScope`

因为：

```text
用户点击
→ 才启动任务
```

这是事件驱动，而不是 Composition 自动驱动。

---

**场景 4：系统 ConnectivityManager 通过 Callback 持续告诉当前 UI 网络连接状态，Composable 想直接获得一个 `State<Boolean>`。**

选择：

`produceState`

因为这就是：

```text
Callback
→ Compose State
```

最标准的适配问题。

---

**场景 5：ViewModel 已经暴露：**

```kotlin
val uiState: StateFlow<UiState>
```

选择：

```kotlin
collectAsStateWithLifecycle()
```

而不是重新用 `produceState` 收集。

这几个场景如果已经可以不看答案直接选出来，说明这一组 API 的边界基本建立了。

---

#### 十一、一个可以长期使用的选择流程

以后不用背表格。遇到问题按照这个顺序判断就行。

首先问：

> **我要不要得到一个新的 Compose `State<T>`？**

如果不要，只是在执行副作用，那么继续判断：如果任务是“进入建立、离开清理”的资源关系，用 `DisposableEffect`；如果是随着 Composition/key 自动运行的挂起任务，用 `LaunchedEffect`；如果是点击、拖拽、回调等事件发生以后才主动启动协程，用 `rememberCoroutineScope`。

如果答案是“我要得到 `State<T>`”，继续问：

> **数据源是不是已经是 ViewModel 暴露出来的 Flow / StateFlow？**

如果是，Android UI 通常使用：

```kotlin
collectAsStateWithLifecycle()
```

如果不是，而是 Callback、Listener 或适合绑定当前 Composition 生命周期的 suspend producer，那么：

```kotlin
produceState()
```

就是非常值得考虑的工具。

最后再问一个架构问题：

> **这份状态只是当前 UI 的适配状态，还是 App 的核心业务状态？**

如果是核心业务状态，应该认真考虑把状态管理提升到 ViewModel / domain / repository，而不是因为 `produceState` 很方便就把业务逻辑塞进 Composable。

---

#### 十二、`produceState` 最容易出现的四种误用

第一种是**所有 Listener 都用 `produceState`**。没有需要输出的 State 时，`DisposableEffect` 往往更准确。

第二种是**所有 suspend 函数都用 `produceState`**。如果只是执行副作用，用 `LaunchedEffect`；如果属于业务数据加载，往往应该在 ViewModel。

第三种是**拿 `produceState` 手工收集已经存在的 StateFlow**。现代 Android 中已有专门的生命周期感知收集 API，没有必要重复造桥。

第四种是**把整个 Repository 甚至业务流程塞进 producer**：

```kotlin
produceState(...) {
    // 查数据库
    // 请求服务器
    // 修改用户
    // 更新缓存
    // 提交订单
    // 处理几十种业务状态
}
```

这种代码即使能运行，也已经失去了 `produceState` 作为 UI 状态适配器的意义。

---

#### 十三、本阶段最终需要留下的心智模型

如果以后已经忘掉这一课的大部分内容，只要还记得下面这几句话就够了。

`DisposableEffect`：

> **我要建立一个与 Composition 生命周期绑定、需要成对清理的副作用。**

`LaunchedEffect`：

> **我要启动一个随着 Composition/key 自动启动和取消的挂起任务。**

`rememberCoroutineScope`：

> **我要等某个事件发生以后，自己决定什么时候启动协程。**

`produceState`：

> **我要启动一个随着 Composition/key 管理生命周期的 producer，并把结果暴露成 `State<T>`。**

`collectAsStateWithLifecycle`：

> **我已经有 Flow / StateFlow，现在只需要按照 Android Lifecycle 把它作为 Compose State 消费。**

`snapshotFlow`：

> **我已经有 Compose State，现在想把它转换成 Flow，进入 Flow 的操作符和异步处理体系。**

到这里，`produceState()` 的 API 定位已经完整了。

#### 下一阶段：阶段六——真实项目边界、最终实战与验收

阶段六将作为 `produceState()` 的最后一个阶段，不再继续研究源码。重点会放在两个方面：第一，用实际 Android/记账 App 的角度判断 `produceState` 应该处在 Clean Architecture 的什么位置，哪些状态应该交给 ViewModel；第二，通过几道需求题进行最终验收，并把整个 `produceState` 压缩成一份面试和日后复习都能使用的总结。

阶段六结束后，`4.13 produceState()` 正式收尾，随后就可以进入 `4.14 snapshotFlow()`。

### 阶段六：真实项目边界、最终实战与验收——`produceState()` 收尾

前五个阶段已经把 `produceState()` 的两个核心问题解决了：第一，它在机制上就是 `remember { mutableStateOf(...) } + LaunchedEffect(...)` 的专用组合；第二，它在 API 设计上的定位，是把一个与当前 Composition 生命周期绑定的数据生产过程适配成 `State<T>`。这一阶段不再继续向源码下钻，而是完成最后一件更重要的事情：**把这个 API 放进真实 Android 项目和 Clean Architecture 中，知道什么时候真的应该写它。**

---

#### 一、先给 `produceState()` 划出真正的工程边界

判断一段逻辑适不适合放进 `produceState()`，可以连续问三个问题。

第一个问题：**这段代码最终是不是为了给当前 Compose UI 提供一个 `State<T>`？** 如果答案是否定的，只是注册资源、发送埋点、显示 Snackbar 等，那么前一阶段已经知道有更合适的 Effect API。

第二个问题：**这个数据生产过程是否天然应该随着当前 Composable 的存在而存在？** 例如当前页面显示时监听某个 Android Framework Callback，页面不再需要这个状态以后就停止监听，这非常符合 `produceState`。

第三个问题，也是架构上最重要的：**这究竟是 UI 边界上的临时适配状态，还是 App 真正的业务状态？**

可以画出两个完全不同的方向：

```text
适合 produceState：

Android Framework / 第三方 SDK
            ↓
       Callback API
            ↓
       produceState
            ↓
        State<T>
            ↓
      当前 Compose UI
```

而记账 App 的核心业务数据通常应该是：

```text
Room / Network
      ↓
Repository
      ↓
UseCase / Domain
      ↓
ViewModel
      ↓
StateFlow<UiState>
      ↓
collectAsStateWithLifecycle()
      ↓
Composable
```

所以 `produceState()` **不应该因为方便，就成为一个缩小版 ViewModel。**

例如未来你的记账首页需要显示“本月收入、本月支出、预算余额、交易列表”。即使技术上完全可以在 Composable 中写：

```kotlin
val uiState by produceState(
    initialValue = HomeUiState.Loading
) {
    val transactions = repository.queryTransactions()
    val budget = repository.queryBudget()

    value = HomeUiState.Success(
        transactions = transactions,
        budget = budget
    )
}
```

这并不是值得采用的项目架构。这里的数据属于页面真正的业务状态，需要配置变更后的持续管理、测试、错误处理、多个数据源组合，甚至可能被其他页面使用。它应该由 ViewModel 管。

反过来，如果一个局部 Composable 需要把某个系统 Callback 暂时转成 Compose State，并且这个状态离开当前 UI 后就没有存在价值，那么 `produceState()` 就非常自然。

---

#### 二、最后做一个真正的 Android Framework 案例：网络连接状态

之前的 `Sec13A` 和 `Sec13B` 都使用了模拟的心率传感器。它们非常适合教学，但这一课结束前值得真正接一次 Android Framework Callback。

按照你现有文件命名方式，新建：

`Sec13C_NetworkState.kt`

这个案例使用 `ConnectivityManager.NetworkCallback`。它的结构与你已经研究透的心率案例几乎完全相同，但这次数据源是真的 Android 系统 API。

```kotlin
/**
 * @author runningpig66
 * @date 2026/08/08 周六
 * @time 23:15
 */
private const val TAG13C = "Sec13C"

enum class NetWorkState {
    Available,
    Unavailable
}

@Composable
fun rememberNetworkState(): State<NetWorkState> {
    val context = LocalContext.current

    val connectivityManager = remember(context) {
        context.applicationContext.getSystemService(ConnectivityManager::class.java)
    }

    return produceState(
        initialValue = NetWorkState.Unavailable,
        key1 = connectivityManager
    ) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                log("$TAG13C onAvailable")
                value = NetWorkState.Available
            }

            override fun onLost(network: Network) {
                log("$TAG13C onLost")
                value = NetWorkState.Unavailable
            }
        }

        log("$TAG13C registerNetworkCallback")
        connectivityManager.registerDefaultNetworkCallback(callback)

        awaitDispose {
            log("$TAG13C unregisterNetworkCallback")
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}

@Composable
fun Sec13C_NetworkStateExample() {
    val networkState by rememberNetworkState()

    Scaffold { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (networkState) {
                    NetWorkState.Available -> "Network available"
                    NetWorkState.Unavailable -> "Network unavailable"
                }
            )
        }
    }
}

/* Output:
0 [main @coroutine#63] Sec13C registerNetworkCallback
5 [ConnectivityThread] Sec13C onAvailable
5392 [ConnectivityThread] Sec13C onLost
6322 [ConnectivityThread] Sec13C onAvailable
12382 [main @coroutine#63] Sec13C unregisterNetworkCallback
 */

@PhonePreviews
@Composable
fun Sec13Preview() {
    CourseComposeTheme {
        Sec13C_NetworkStateExample()
    }
}

```

为了使用网络状态 API，Manifest 中还需要：

```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

运行时可以尝试打开和关闭 Wi-Fi、移动数据，观察日志与 UI。具体设备、系统版本和当前默认网络切换过程可能造成回调时序上的差异，所以这里关注的不是“每切一次 Wi-Fi 必须精确打印几条日志”，而是下面这条结构。

```text
Composable 需要网络状态
        ↓
produceState 创建 State
        ↓
注册 NetworkCallback
        ↓
系统网络发生变化
        ↓
onAvailable / onLost
        ↓
value = ...
        ↓
Compose State 更新
        ↓
UI 更新
```

当这个 Composable 不再需要该状态时：

```text
离开 Composition
        ↓
LaunchedEffect 对应协程取消
        ↓
awaitDispose 的 finally
        ↓
unregisterNetworkCallback()
```

到现在这个流程应该已经非常熟悉了。

还有一个工程细节值得知道：这里的 `Available` 表达的是 Android 网络连接层面的可用状态，并不能简单等同于“某个服务器一定访问成功”或者“互联网绝对畅通”。如果产品真正需要判断服务是否可达，应当有更高层的数据/业务设计，而不能把 `NetworkCallback` 当作服务器健康检查。

---

#### 三、为什么这里适合封装成 `rememberNetworkState()`？

外部调用者现在只看到：

```kotlin
val networkState by rememberNetworkState()
```

它完全不知道内部存在：

`ConnectivityManager`、`NetworkCallback`、`registerDefaultNetworkCallback()`、`unregisterNetworkCallback()`、`awaitDispose()`。

这其实体现出了 `produceState()` 在工程中最好的一种使用姿势：**把某种非 Compose API 包装成一个 Compose 风格的状态 API。**

调用端得到的体验就像：

```kotlin
val networkState by rememberNetworkState()
val scrollState = rememberScrollState()
val pagerState = rememberPagerState(...)
```

内部具体怎么监听 Android Framework，是这个 adapter 自己的责任。

这里 `produceState()` 就不仅仅是在“省掉 `MutableState`”，而是在帮助你建立一个非常干净的 API 边界：

```text
Imperative / Callback API
           ↓
    Compose Adapter
           ↓
   Declarative State API
```

这也是我认为这个 API 真正值得学习的地方。

---

#### 四、但同一个“网络状态”需求，也可能不应该使用 `produceState`

这里正好用同一个案例理解架构边界。

假设网络状态只是当前一个小组件需要：

> “网络断开时，在当前页面顶部显示一个 Offline Banner。”

而页面离开以后完全不需要继续维护这份状态。

那么：

```kotlin
rememberNetworkState()
```

非常合理。

但假设以后你的记账 App 规定：

* 整个 App 都要感知网络状态；
* Repository 根据网络状态调整同步策略；
* 离线时交易先写 Room；
* 联网以后自动同步云端；
* ViewModel 根据同步状态展示错误；
* 多个页面都需要知道同步状态。

这时候“网络状态”已经不再只是 Compose UI 的局部适配状态了。

架构更可能变成：

```text
ConnectivityManager
        ↓
ConnectivityObserver
        ↓
Flow<ConnectivityState>
        ↓
Repository / SyncManager / ViewModel
        ↓
StateFlow<UiState>
        ↓
collectAsStateWithLifecycle()
```

此时如果 UI 再自己创建：

```kotlin
rememberNetworkState()
```

反而会产生多个状态源，甚至让 UI 和真正的同步系统观察不同的网络状态。

所以同一个 Android API，**不能脱离业务上下文判断“应该用 produceState”。**

判断标准始终是状态所有权。

---

#### 五、对于你的 Clean Architecture，`produceState()` 应该放在哪一层？

如果以后项目按照类似：

```text
data/
domain/
ui/
```

组织，`produceState()` 本身属于 Compose Runtime API，因此通常应该待在 **UI/Compose 边界**。

例如：

```text
ui/
  component/
    NetworkState.kt

ui/
  utils/
    RememberNetworkState.kt
```

或者某个明确属于 UI 平台适配的目录。

不要把：

```kotlin
@Composable
fun rememberSomething(): State<...>
```

塞进：

```text
domain/
```

Domain 层理论上甚至不应该知道 `@Composable`、`State<T>`、`produceState()` 的存在。

Domain 面对的应该是普通 Kotlin 模型、接口、Flow/suspend 等适合独立于 UI 框架的抽象。

这一点对于以后如果希望部分 data/domain 代码能够向 KMP 迁移尤其重要，因为：

```text
Compose State<T>
```

不应该成为核心业务接口的默认语言。

所以一个比较健康的边界是：

```text
Domain
    不认识 Compose

Data
    不依赖 Compose UI 状态

ViewModel
    通常暴露 StateFlow / Flow

Compose UI
    在最外层把这些数据消费为 State

UI 特有的外部 API
    必要时 produceState 进行局部适配
```

这套边界比单纯记住 `produceState()` 的函数签名重要得多。

---

#### 六、最后补一个很实际的问题：到底什么时候应该自己封装 `produceState`？

日常开发里，你其实不会每天手写：

```kotlin
produceState(...)
```

很多成熟库已经替你完成类似转换。

例如某个库如果已经提供：

```kotlin
@Composable
fun rememberXXXState(...)
```

或者专门的 Flow → State API，就优先使用成熟 API，而不是为了展示自己理解源码再重新实现一套。

`produceState()` 更像 Compose 给开发者准备的一个**底层状态适配积木**。当现有世界中出现：

```text
某个 SDK
某个 Android Callback
某个异步数据源
```

而现成 Compose Adapter 又不存在时，你就知道：

> “这里可以自己写一个很薄的 `rememberXXXState()`。”

这才是实际项目中非常漂亮的用法。

---

#### 七、最后做一次场景验收

现在假设没有前一阶段的表格，只看需求来判断。

**场景 A**

```text
进入聊天页后注册 screenshot listener，
离开聊天页注销。
截图发生后只发送 analytics event，
UI 不显示任何 screenshot 状态。
```

选择 `DisposableEffect`。

因为核心是注册与注销资源，没有 State 要生产。

---

**场景 B**

```text
进入某个页面以后，每隔 5 秒执行一次 suspend 刷新任务；
退出页面立即停止。
```

优先考虑 `LaunchedEffect`。

核心诉求是一个随着 Composition 生命周期运行的挂起副作用。

---

**场景 C**

```text
用户点击“保存交易”按钮以后调用 suspend saveTransaction()。
```

由事件触发，因此 UI 层通常使用 `rememberCoroutineScope()` 启动相应协程；而如果使用 ViewModel，则更常见的是把点击事件交给 ViewModel，由 `viewModelScope` 执行业务操作。

注意后一种在你的正式项目里会更常见。

---

**场景 D**

```text
一个只存在于当前页面的 SDK 提供 Listener：
addListener(listener)
removeListener(listener)

Listener 返回的数据需要实时显示在当前 Composable。
```

这是 `produceState()` 最典型的候选场景。

---

**场景 E**

```text
TransactionViewModel 已经暴露：

StateFlow<TransactionUiState>
```

不要再写 `produceState`。

使用生命周期感知的 Flow → Compose State API。

---

**场景 F**

```text
用户滚动 LazyColumn；
希望观察 firstVisibleItemIndex，
转换成 Flow 后执行 debounce / filter / collect。
```

这里答案已经来到下一课：

`snapshotFlow()`。

现在只需要知道方向，不需要提前学习它的具体机制。

---

**场景 G**

```text
Room 查询交易数据，
计算本月总支出，
请求服务器同步，
处理错误与重试，
最后生成 HomeUiState。
```

虽然能够用 `produceState()` 写出来，但这属于 ViewModel / Repository 等业务架构的职责。不要把它塞进 Composable。

如果上面这些场景不依赖死记 API 名字也能完成判断，那么 `produceState()` 的应用层理解已经足够扎实。

---

#### 八、面试时如果被问：`produceState()` 是什么？

不需要把我们两晚学习的内容全部倒出来。一个成熟、简洁的回答可以是：

> `produceState` 用于把非 Compose 的异步数据源转换成 Compose 的 `State<T>`。它返回一个由 Compose 管理的 State，并在一个与 Composition 生命周期绑定的协程中执行 producer。key 改变时旧 producer 会被取消并重新启动。对于 Callback 类型的数据源，可以使用 `awaitDispose` 在 producer 被取消时注销 Listener。它适合 UI 边界上的状态适配；如果数据已经由 ViewModel 以 Flow 或 StateFlow 暴露，通常直接使用相应的 Flow-to-State API，而不是再用 `produceState` 包一层。

如果面试官继续问源码，再补：

> 它内部核心就是 `remember { mutableStateOf(initialValue) }` 加 `LaunchedEffect(keys)`，`ProduceStateScopeImpl` 把这个 MutableState 委托出去，同时携带当前协程的 CoroutineContext；`awaitDispose` 利用可取消挂起点，在协程取消时通过 `finally` 执行清理。

讲到这里已经相当充分。

---

#### 九、4.13 `produceState()` 最终知识压缩

经过这六个阶段，可以把整课压缩成一个完整模型：

```text
                 非 Compose 数据源
        Callback / suspend / SDK / Framework
                         │
                         ▼
                  produceState()
                         │
        ┌────────────────┴───────────────┐
        │                                │
        ▼                                ▼
remember { MutableState<T> }     LaunchedEffect(keys)
        │                                │
        │                                ▼
        │                         producer coroutine
        │                                │
        │                     ProduceStateScope<T>
        │                                │
        │                     value = newValue
        │                                │
        └────────────────◀───────────────┘
        │
        ▼
     State<T>
        │
        ▼
   Compose UI
```

如果数据源是 Callback，再增加：

```text
register(listener)
        ↓
awaitDispose()
        ↓
suspendCancellableCoroutine 挂起 producer
        ↓
离开 Composition / key 改变
        ↓
LaunchedEffect Job 取消
        ↓
finally
        ↓
unregister(listener)
```

最后只需要长期保留五个结论：

1. **`produceState()` 的职责是把异步/外部数据生产过程适配成 Compose `State<T>`。**
2. **producer 是 suspend Lambda，真正运行在内部 `LaunchedEffect` 管理的协程中。**
3. **`ProduceStateScope` 同时提供 `MutableState` 写能力和 `CoroutineScope`，但它本身不是另外一条协程。**
4. **Callback/Listener 类型的长期注册通常使用 `awaitDispose()` 配对释放资源；普通 suspend producer 通常不需要。**
5. **`produceState()` 最适合 UI 边界适配，而不是承担 ViewModel、Repository 和核心业务状态管理。**

至此，**4.13《从 `produceState()` 说起：非 Compose 状态转换成 Compose 状态》可以正式结束。**

下一课 `4.14 snapshotFlow()` 会沿着恰好相反的方向继续：

```text
produceState:
外部世界 → Compose State

snapshotFlow:
Compose State → Flow
```

而且下一课不会默认按照 `produceState()` 这次的源码深度展开。先把 `snapshotFlow` 的使用动机、Snapshot 读取观察和 Flow 行为建立清楚，再判断有没有值得看的短源码。
