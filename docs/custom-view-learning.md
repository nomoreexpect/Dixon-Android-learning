# 自定义 View 学习笔记

这篇文档对应项目里的 **View 学习 Demo**。它不只是讲 `onDraw()` 怎么画图，而是把 Android 测试开发经常遇到的 View 定位、点击、可见性、坐标问题，和底层 View 创建、测量、布局、绘制、事件分发链路放在一起理解。

## 这个 Demo 学什么

本 Demo 重点观察两条线。

Framework 原理线：

```text
Activity.startActivity()
→ ViewDemoActivity.onCreate()
→ setContentView()
→ LayoutInflater 解析 XML
→ 创建 DebugLearningView
→ DecorView 挂到 Window
→ ViewRootImpl 接管 View 树
→ measure
→ layout
→ draw
→ Choreographer / VSYNC 驱动下一帧
```

测试开发线：

```text
测试脚本查找 View
→ resource-id / text / contentDescription / className
→ 判断 visible / enabled / clickable / bounds
→ 必要时滚动到可见区域
→ 注入点击或触摸坐标
→ dispatchTouchEvent()
→ onTouchEvent()
→ performClick()
→ 页面状态或日志验证结果
```

## 项目入口

- 首页入口：`MainActivity`
- 入口按钮：`打开 View 学习 Demo`
- Demo 页面：`ViewDemoActivity`
- 自定义 View：`DebugLearningView`
- 页面布局：`app/src/main/res/layout/activity_view_demo.xml`
- 学习文档：`docs/custom-view-learning.md`
- 架构图：`docs/custom-view-architecture.svg`

## Manifest / Gradle 配置

View 本身不需要特殊权限，也不需要新增 Gradle 依赖。本次只是在 Manifest 注册一个学习页面：

```xml
<activity
    android:name=".ViewDemoActivity"
    android:exported="false"
    android:label="View 学习 Demo"
    android:parentActivityName=".MainActivity" />
```

含义：

- `android:name=".ViewDemoActivity"`：声明这个 Activity 可以被应用内部启动。
- `android:exported="false"`：不允许其他应用直接启动这个学习页面。
- `android:parentActivityName=".MainActivity"`：让系统知道它的上一级页面，ActionBar 返回更自然。

## 核心类与职责

| 类/文件 | 职责 |
| --- | --- |
| `MainActivity` | 首页新增第 ⑥ 个模块，点击按钮进入 View 学习页面。 |
| `ViewDemoActivity` | 管理 Demo 页面，接收 `DebugLearningView` 的回调日志，展示测试视角状态。 |
| `DebugLearningView` | 自定义 View，记录 attach、measure、draw、touch、click 等关键回调。 |
| `ViewDemoLogBuffer` | 限制日志条数并格式化输出，避免页面文本无限增长。 |
| `DebugViewState` | 保存尺寸模式、绘制次数、最后触摸点等可测试状态。 |
| `ViewDemoLogBufferTest` | 测试日志编号、截断和清空行为。 |
| `DebugViewStateTest` | 测试尺寸切换、绘制计数和触摸点格式化。 |

## View 是什么

初学者可以先把 View 理解成一块可以被测量、摆放、绘制、接收事件的矩形区域。

它至少关心这些问题：

- 我多大：`onMeasure()`
- 我放在哪里：`layout()` / `onLayout()`
- 我画什么：`onDraw()`
- 我能不能被点：`dispatchTouchEvent()` / `onTouchEvent()`
- 我是否能被测试找到：`id`、`text`、`contentDescription`、`className`
- 我是否真的能被操作：`visibility`、`enabled`、`clickable`、屏幕 bounds

普通 `View` 自己没有子 View，所以 `onLayout()` 默认不做什么。真正负责摆放子 View 的通常是 `ViewGroup`，例如 `LinearLayout`、`FrameLayout`、`ConstraintLayout`、`RecyclerView`。

## setContentView 到 ViewRootImpl

在 `ViewDemoActivity.onCreate()` 中调用：

```java
setContentView(R.layout.activity_view_demo);
```

这行代码背后大致发生：

```text
AppCompatActivity.setContentView()
→ LayoutInflater 读取 activity_view_demo.xml
→ 根据 XML 标签创建 ScrollView / LinearLayout / TextView / DebugLearningView
→ 形成一棵 View 树
→ Activity 的 Window 持有 DecorView
→ DecorView 包住应用内容区域
→ WindowManager 把 DecorView 添加到窗口系统
→ ViewRootImpl 成为 View 树和 WMS 之间的桥
→ ViewRootImpl.performTraversals()
→ performMeasure()
→ performLayout()
→ performDraw()
```

学习重点：

- Activity 不是直接画到屏幕上的，真正绘制的是 View 树。
- Window 是 Activity 和窗口系统之间的抽象。
- DecorView 是应用窗口最外层的根 View。
- ViewRootImpl 不是 View，但它负责调度 View 树的遍历。
- WMS 管窗口层级、焦点、输入和 Surface；SurfaceFlinger 负责最终合成显示。

## measure：View 怎么知道自己多大

`DebugLearningView.onMeasure()` 做了三件事：

```text
读取期望尺寸
→ 根据父容器给的 MeasureSpec 计算最终尺寸
→ setMeasuredDimension(width, height)
```

`MeasureSpec` 有三个常见模式：

| 模式 | 含义 | 常见来源 |
| --- | --- | --- |
| `EXACTLY` | 父容器已经确定具体大小 | `match_parent` 或固定 `100dp` |
| `AT_MOST` | 最大不能超过某个尺寸 | `wrap_content` |
| `UNSPECIFIED` | 父容器不限制大小 | 少见，常见于某些滚动测量场景 |

本 Demo 的自定义 View 写在 XML 里：

```xml
<com.example.lifecycledemo.DebugLearningView
    android:id="@+id/debug_learning_view"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content" />
```

因此它会告诉父容器：“我想用自己的期望尺寸，但不能超过父容器允许的最大空间。”

## layout：View 被摆到哪里

测量完成后，父容器会调用子 View 的 `layout(left, top, right, bottom)`。这一步决定 View 在父容器坐标系里的位置和最终宽高。

测试开发里常见的“控件存在但点不到”，很多时候就和 layout 结果有关：

- View 在屏幕外，需要先滚动。
- View 宽高是 0，脚本能找到 id，但没有可点击区域。
- View 被其他窗口或遮罩覆盖，坐标点击落到了别的控件上。
- 父容器裁剪了子 View，导致实际可见区域变小。

## draw：View 怎么画出来

`DebugLearningView.onDraw()` 做了这些事：

- 记录绘制次数。
- 画绿色圆角矩形背景。
- 画当前尺寸模式、绘制次数、触摸坐标。
- 如果有触摸点，画一个橙色圆点。

注意：

- `onDraw()` 只负责画当前状态，不应该做网络、数据库、大量计算。
- 不要在 `onDraw()` 里频繁 `requestLayout()`，容易造成反复布局和卡顿。
- `Canvas` 是绘制命令的载体，真正显示还要经过硬件加速、RenderThread、Surface、SurfaceFlinger 等流程。

## invalidate 和 requestLayout 的区别

Demo 页面有两个按钮：

```text
invalidate
requestLayout
```

它们的含义不同：

| API | 作用 | 常见结果 |
| --- | --- | --- |
| `invalidate()` | 标记 View 需要重绘 | 触发新的 `onDraw()` |
| `requestLayout()` | 标记 View 需要重新测量和布局 | 触发 `onMeasure()`、layout，之后可能 draw |

判断方式：

- 只是颜色、文字、进度、触摸点变了：通常用 `invalidate()`。
- 宽高、边距、内容尺寸、布局约束变了：通常用 `requestLayout()`。

本 Demo 里“切换尺寸”会改变自定义 View 的期望宽高，所以它调用 `requestLayout()`。

## 触摸事件分发

触摸事件大致这样走：

```text
InputDispatcher
→ Activity.dispatchTouchEvent()
→ Window / DecorView
→ ViewGroup.dispatchTouchEvent()
→ 子 View.dispatchTouchEvent()
→ OnTouchListener
→ View.onTouchEvent()
→ performClick()
```

`DebugLearningView` 覆盖了：

- `dispatchTouchEvent()`：记录事件先到达 View。
- `onTouchEvent()`：记录 View 消费事件，并保存最后触摸点。
- `performClick()`：在 `ACTION_UP` 后触发点击语义。

为什么 `performClick()` 重要：

- Android Lint 会提醒自定义 View 处理触摸时应该实现点击语义。
- 无障碍服务、测试工具和普通点击语义都依赖它。
- 如果只处理 `ACTION_UP` 但不调用 `performClick()`，有些自动化或无障碍场景会变得不稳定。

## 测试开发怎么看 View

测试开发通常先问五个问题。

1. 能不能找到？

常用定位信息：

- `resource-id`
- `text`
- `contentDescription`
- `className`
- 父子层级
- RecyclerView 列表位置

本 Demo 的自定义 View 有：

```text
resource-id: debug_learning_view
contentDescription: 自定义 View 学习画布，可点击和拖动观察触摸事件
```

2. 找到了是否可见？

需要确认：

- `visibility == VISIBLE`
- 宽高大于 0
- 在当前屏幕可见范围内
- 没被弹窗、悬浮窗、系统权限页、键盘遮挡

3. 能不能点击？

需要确认：

- `enabled == true`
- `clickable == true`
- 目标中心点落在可见 bounds 内
- 父容器没有拦截事件
- `ACTION_DOWN` 后事件没有被取消

4. 坐标是不是对的？

自动化点击失败常见原因：

- 取的是父容器坐标，不是屏幕坐标。
- ScrollView 滚动后坐标变化。
- 动画过程中 View 的位置还在变。
- 状态栏、导航栏、刘海、安全区域导致坐标偏移。
- 横竖屏或分辨率不同，硬编码坐标失效。

5. 状态是否能断言？

普通 TextView 可以断言 text，自定义 View 不一定有文本。自定义 View 想更容易测试，可以：

- 设置稳定 id。
- 设置有意义的 `contentDescription`。
- 把复杂状态抽成普通 Java/Kotlin 类测试。
- 页面上暴露调试状态文本。
- 在 Logcat 输出关键事件。

本 Demo 就把 `DebugViewState` 抽成纯 Java 类，并用页面上的“测试开发视角”展示当前状态。

## 常见异常和问题

| 现象 | 常见原因 | 排查方式 |
| --- | --- | --- |
| 找不到 View | id 写错、页面没打开、View 还没创建、列表项未滚动出来 | 看 Layout Inspector、`uiautomator dump`、页面日志 |
| 找到但点击失败 | View 不可见、宽高为 0、被遮挡、未 enabled/clickable | 看 bounds、visibility、enabled、clickable |
| 点击坐标偏移 | 使用硬编码坐标、滚动后位置变化、状态栏/导航栏偏移 | 优先用控件定位点击，少用裸坐标 |
| 自定义 View 没反应 | `onTouchEvent(ACTION_DOWN)` 返回 false 或父容器拦截 | 看 `dispatchTouchEvent` / `onTouchEvent` 日志 |
| 点击语义不完整 | 处理触摸但没调用 `performClick()` | 在 `ACTION_UP` 后调用 `performClick()` |
| UI 卡顿 | `onDraw()` 做耗时操作或频繁创建对象 | Logcat、Profile、减少绘制内分配 |
| 反复 layout | `onDraw()` 或布局过程中不断 `requestLayout()` | 检查日志是否 measure/layout 高频出现 |
| `requestLayout()` 后没看到变化 | 尺寸没变、父容器约束不允许变大 | 看 `MeasureSpec` 和 `onMeasure` 结果 |

## Logcat 验证

过滤 tag：

```powershell
adb logcat | findstr View-Demo
```

你会看到类似：

```text
View-Demo: onMeasure | desired=720x480, result=720x480, widthSpec=AT_MOST(984), heightSpec=UNSPECIFIED(0)
View-Demo: onDraw | mode=compact, desired=720x480, drawCount=1, lastTouch=none
View-Demo: dispatchTouch | ACTION_DOWN, x=120.0, y=80.0
View-Demo: onTouch | ACTION_DOWN, consumed=true, x=120.0, y=80.0
```

观察建议：

- 刚进入页面，看首次 `onMeasure` / `onSizeChanged` / `onDraw`。
- 点 `invalidate`，重点看是否新增 `onDraw`。
- 点 `requestLayout`，重点看是否新增 `onMeasure`。
- 拖动自定义 View，重点看 `ACTION_DOWN` / `ACTION_MOVE` / `ACTION_UP`。

## adb 和 dumpsys 验证

确认当前窗口：

```powershell
adb shell dumpsys window | findstr ViewDemoActivity
```

导出当前界面节点：

```powershell
adb shell uiautomator dump /sdcard/window.xml
adb shell cat /sdcard/window.xml | findstr debug_learning_view
```

如果能看到 `resource-id` 或 `content-desc`，说明自动化工具可以通过语义信息定位它。注意：`uiautomator dump` 对自定义绘制内容不会知道你 Canvas 里画了什么，它只能看到 View 节点属性。

## Layout Inspector 验证

在 Android Studio 中打开 Layout Inspector，可以检查：

- `DecorView`
- `ScrollView`
- `LinearLayout`
- `DebugLearningView`
- 实际宽高
- 可见性
- id
- bounds

这比只看 XML 更真实，因为 XML 只是声明，最终尺寸和位置要等 measure/layout 后才能确定。

## 和已有模块的关系

本项目之前已经有 Activity 生命周期和 WindowManager 悬浮窗：

- Activity 生命周期说明页面什么时候创建和销毁。
- WindowManager 悬浮窗说明 View 可以不只存在于 Activity 内容区，也可以被加到独立窗口层级。
- View 学习模块补上“页面里控件如何创建、绘制、响应触摸、被测试定位”这一层。

三者串起来看：

```text
Activity 生命周期
→ setContentView 创建普通页面 View 树
→ ViewRootImpl 驱动普通窗口里的 View 绘制
→ WindowManager 也可以 addView 创建应用外 overlay
→ 测试工具最终面对的仍然是窗口和 View 节点
```

## 后续扩展

可以继续往这些方向加：

- 自定义 `ViewGroup`：真正实现 `onLayout()`，理解子 View 摆放。
- 事件分发专项：Activity、ViewGroup、View 三层日志对比。
- RecyclerView 测试：列表复用、滚动定位、item 点击。
- Espresso / UIAutomator 示例：写实际自动化测试用例。
- Layout Inspector 专项：结合截图标注 View 树。
- 性能专项：Choreographer、掉帧、过度绘制、`onDraw()` 分配对象。

## 学习重点总结

- View 是可测量、可布局、可绘制、可接收事件的 UI 基本单元。
- `setContentView()` 之后才会有 View 树，ViewRootImpl 负责后续遍历。
- `invalidate()` 偏绘制，`requestLayout()` 偏测量和布局。
- 触摸事件先分发，再由目标 View 决定是否消费。
- 测试开发不要只看 id，还要看可见性、可点击性、bounds、遮挡、滚动和坐标。
- 自定义 View 想好测，最好提供稳定 id、`contentDescription`、日志和可测试状态模型。

