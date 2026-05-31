# Android Activity 生命周期学习笔记

这份笔记对应本项目的 `MainActivity` 和 `SecondActivity` Demo，重点是把生命周期回调和真实代码行为对应起来，而不是只背 `onCreate -> onStart -> onResume`。

## 1. 这个 Demo 学什么

本项目最初的核心就是 Activity 生命周期。当前首页在进入、离开、跳转、返回和销毁时，会把生命周期事件写入页面日志，并同时输出到 Logcat。

主要观察对象：

- `MainActivity`：应用首页，承载大部分组件学习入口。
- `SecondActivity`：用于观察 Activity 之间切换时的生命周期顺序。
- `LifecycleLogBuffer`：把生命周期事件整理成可显示的文本。

对应代码位置：

```text
app/src/main/java/com/example/lifecycledemo/MainActivity.java
app/src/main/java/com/example/lifecycledemo/SecondActivity.java
app/src/main/java/com/example/lifecycledemo/LifecycleLogBuffer.java
app/src/main/res/layout/activity_main.xml
app/src/main/res/layout/activity_second.xml
```

## 2. Manifest 启动入口

`MainActivity` 是应用入口，因为它在 `AndroidManifest.xml` 中声明了 `MAIN + LAUNCHER`：

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:label="@string/app_name">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

这表示用户点击桌面/应用列表图标后，系统会创建并启动 `MainActivity`。

`SecondActivity` 没有 `LAUNCHER`，它只能由 App 内部显式跳转打开：

```xml
<activity
    android:name=".SecondActivity"
    android:exported="false"
    android:label="第二个页面"
    android:parentActivityName=".MainActivity" />
```

`exported="false"` 表示外部 App 不能直接启动它，这符合学习 Demo 的内部页面定位。

## 3. MainActivity 的启动流程

第一次打开 App 时，核心顺序是：

```text
MainActivity.onCreate()
-> MainActivity.onStart()
-> MainActivity.onResume()
```

项目里每个回调都会做两件事：

```java
Log.d(TAG, ">>> onCreate");
appendLifecycleLog("onCreate");
```

`Log.d()` 用于 Logcat 观察，`appendLifecycleLog()` 用于页面上显示。

### onCreate

`onCreate()` 是 Activity 第一次创建时调用，适合做一次性初始化：

```java
setContentView(R.layout.activity_main);
floatWindowManager = new FloatWindowManager(getApplicationContext());
initViews();
appendLifecycleLog("onCreate");
setupButtons();
```

这里有几个关键点：

- `setContentView()`：把 XML 布局加载成 View 树。
- `initViews()`：通过 `findViewById()` 找到页面控件。
- `setupButtons()`：给按钮绑定点击事件。
- `FloatWindowManager`：悬浮窗管理器在这里创建，但不等于立刻显示悬浮窗。

### onStart

`onStart()` 表示 Activity 对用户可见，但还不一定能交互。

```java
appendLifecycleLog("onStart");
```

它适合理解“页面进入可见状态”。

### onResume

`onResume()` 表示 Activity 已在前台，可以与用户交互。

本项目在这里做了几个有学习意义的动作：

```java
updateMusicUI();
registerReceiver(networkReceiver, filter);
```

也就是说，网络监听这种“页面前台才需要”的资源，在 `onResume()` 注册。

悬浮窗权限从系统设置页返回后，也是在 `onResume()` 再检查：

```java
if (pendingShowFloatWindow) {
    pendingShowFloatWindow = false;
    if (Settings.canDrawOverlays(this)) {
        showFloatWindow();
    }
}
```

这说明：从系统设置页返回 App，并不是走一个“权限回调”，而是 Activity 回到前台后走 `onResume()`，所以要在这里重新判断。

## 4. 跳转到 SecondActivity 时的生命周期

点击“显式跳转到 SecondActivity”后，代码调用：

```java
startActivity(IntentFactory.createSecondActivityIntent(this));
```

典型顺序是：

```text
MainActivity.onPause()
SecondActivity.onCreate()
SecondActivity.onStart()
SecondActivity.onResume()
MainActivity.onStop()
```

这个顺序很重要：

- `MainActivity.onPause()` 先执行，因为 MainActivity 失去前台交互。
- `SecondActivity` 创建并进入前台。
- 当 MainActivity 完全不可见后，才进入 `onStop()`。

如果新的 Activity 是半透明的，MainActivity 可能只到 `onPause()`，不一定进入 `onStop()`。所以不要把 `onPause()` 和 `onStop()` 简单理解成“永远一起出现”。

## 5. 从 SecondActivity 返回 MainActivity

按返回键从 SecondActivity 回到 MainActivity，典型顺序是：

```text
SecondActivity.onPause()
MainActivity.onRestart()
MainActivity.onStart()
MainActivity.onResume()
SecondActivity.onStop()
SecondActivity.onDestroy()
```

这里可以学到两个点：

### onRestart

`onRestart()` 只在 Activity 已经 `onStop()` 后重新回到前台时调用。

如果 Activity 是第一次启动，不会调用 `onRestart()`。

### onDestroy

`SecondActivity` 被返回键关闭后，会走 `onDestroy()`。这表示这个 Activity 实例结束了。

但是要注意：`onDestroy()` 不只代表“用户主动关闭”。配置变化、系统回收等场景也可能导致 Activity 销毁。

## 6. MainActivity 暂停、停止和销毁

### onPause

`onPause()` 表示 Activity 失去焦点，但可能仍可见。

本项目在这里注销网络广播：

```java
if (networkReceiver != null) {
    unregisterReceiver(networkReceiver);
    networkReceiver = null;
    NetworkChangeReceiver.setNetworkChangeListener(null);
}
```

这是一种典型的生命周期绑定资源管理：

```text
onResume 注册
onPause 注销
```

这样能避免页面不在前台时还继续接收回调，也能避免内存泄漏。

### onStop

`onStop()` 表示 Activity 完全不可见。

适合停止一些只在可见时需要的工作，比如动画、刷新、相机预览等。

### onDestroy

`onDestroy()` 表示 Activity 即将销毁。

本项目在这里做了两类释放：

```java
if (isBound) {
    unbindService(serviceConnection);
    isBound = false;
}
```

绑定的 Service 要解绑，否则 Activity 销毁后还持有连接，容易泄漏。

```java
if (floatWindowManager != null) {
    floatWindowManager.remove();
}
```

当前 Demo 中悬浮窗归 `MainActivity` 管理，所以 Activity 销毁时要移除悬浮窗。这个点和悬浮窗文档是连起来的：

```text
Activity 持有 FloatWindowManager
-> Activity.onDestroy()
-> FloatWindowManager.remove()
-> WindowManager.removeView()
```

如果未来希望悬浮窗退出页面后仍存在，就不应该由 Activity 持有，而应该交给前台 Service。

## 7. 生命周期日志是怎么显示的

页面日志不是直接拼在 Activity 里，而是交给 `LifecycleLogBuffer`：

```java
private final LifecycleLogBuffer lifecycleLogBuffer = new LifecycleLogBuffer();
```

每次生命周期回调发生时：

```java
lifecycleLogBuffer.append(time, eventName);
updateLifecycleLogText();
```

`LifecycleLogBuffer` 的作用是：

- 保存事件顺序。
- 统一格式化日志文本。
- 方便写本地单元测试。

这和悬浮窗里的 `FloatWindowEdgeController` 思路类似：把纯逻辑从 Android UI 类里拆出来，普通 JUnit 就能测。

## 8. 和悬浮窗功能的关系

生命周期 Demo 和悬浮窗 Demo 不是割裂的，它们正好能互相解释。

### Activity 管理悬浮窗

当前实现：

```text
MainActivity.onCreate()
-> 创建 FloatWindowManager

点击按钮
-> FloatWindowManager.show()
-> WindowManager.addView()

MainActivity.onDestroy()
-> FloatWindowManager.remove()
-> WindowManager.removeView()
```

所以当前 Demo 的结论是：

- MainActivity 只是 `onPause/onStop` 时，悬浮窗可以继续存在。
- MainActivity 真正 `onDestroy` 时，悬浮窗会被移除。

### 为什么之前拖边缘会触发 destroy

之前拖悬浮窗到屏幕边缘时，系统手势可能识别成返回操作：

```text
拖到系统返回手势区域
-> 系统触发 Back
-> MainActivity 关闭
-> MainActivity.onDestroy()
-> FloatWindowManager.remove()
```

后来通过边界限制、点击/拖动区分、收球吸附，避免普通拖动误触系统返回。

这正是生命周期和窗口系统的交叉点：窗口行为可能间接影响 Activity 的生命周期。

## 9. 常见误区

### onCreate 不是每次回到页面都会执行

从 SecondActivity 返回 MainActivity 时，如果 MainActivity 没被销毁，一般是：

```text
onRestart -> onStart -> onResume
```

不会重新走 `onCreate()`。

### onDestroy 不一定可靠保存数据

不要把重要数据只放在 `onDestroy()` 保存。系统可能在极端情况下直接杀进程，不保证你的保存逻辑一定执行。

更可靠的方式：

- 重要数据及时持久化。
- UI 临时状态用 `onSaveInstanceState()` 或 ViewModel。
- 数据层用 Room/DataStore 等持久化方案。

### onPause 不能做太重的事

`onPause()` 会阻塞下一个 Activity 进入前台。这里应该做轻量工作，比如暂停动画、注销监听、保存少量状态。

### 注册和注销要成对

本项目网络广播是：

```text
onResume 注册
onPause 注销
```

如果只注册不注销，可能导致重复回调或内存泄漏。

## 10. 如何用 Logcat 验证

过滤 `Lifecycle-Main`：

```powershell
adb logcat | Select-String Lifecycle-Main
```

或者在 Android Studio Logcat 中搜索：

```text
Lifecycle-Main
```

重点观察：

- 第一次启动：`onCreate -> onStart -> onResume`
- 跳转 SecondActivity：`onPause -> onStop`
- 返回 MainActivity：`onRestart -> onStart -> onResume`
- 退出 MainActivity：`onPause -> onStop -> onDestroy`

SecondActivity 可以过滤：

```text
Lifecycle-Second
```

## 11. 如何用页面验证

打开 App 后，首页“生命周期日志”区域会显示事件序号、时间和回调名称。

建议按这个顺序操作：

1. 打开 App，观察 MainActivity 首次启动顺序。
2. 点击“显式跳转到 SecondActivity”，观察 MainActivity 暂停/停止。
3. 按返回键，观察 MainActivity 重启回前台。
4. 点击清空日志，再重复一次。
5. 开启悬浮窗后按返回退出页面，观察 `onDestroy` 和悬浮窗移除。

## 12. 后续可以继续补什么

当前生命周期 Demo 已经能讲清楚基础回调。下一步可以继续加：

- `onSaveInstanceState()`：旋转屏幕或进程重建后恢复 UI 状态。
- `ViewModel`：让生命周期日志在配置变化后不丢。
- Fragment 生命周期：对比 Activity 和 Fragment 的回调顺序。
- ProcessLifecycleOwner：观察整个 App 进入前台/后台。
- LifecycleObserver：用观察者方式响应生命周期，而不是把所有逻辑写在 Activity 里。

## 13. 学习重点总结

- `onCreate` 适合初始化 View 和一次性对象。
- `onStart` 表示可见。
- `onResume` 表示可交互，适合注册前台监听。
- `onPause` 表示失去焦点，适合做轻量释放。
- `onStop` 表示完全不可见。
- `onRestart` 只在从停止状态回到前台时出现。
- `onDestroy` 用于最终释放，但不要把重要数据只押在这里。
- 生命周期不是孤立知识，它会影响 Service 绑定、BroadcastReceiver 注册、悬浮窗 remove、权限授权返回等真实业务逻辑。
