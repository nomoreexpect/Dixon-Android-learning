# Android 悬浮窗学习笔记

这份笔记对应本项目的 `FloatWindowManager` Demo，重点是窗口建立和管理流程，而不是只看卡片 UI。

## 1. AndroidManifest.xml 权限配置

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

`SYSTEM_ALERT_WINDOW` 用于允许 App 显示在其他应用上层。它不是普通的运行时危险权限，不能只靠 `requestPermissions()` 弹系统权限框；通常要把用户带到系统设置页，让用户手动打开“显示在其他应用上层”。

本项目的配置位置：

```xml
<!-- 系统悬浮窗权限：允许使用 TYPE_APPLICATION_OVERLAY 添加应用外悬浮窗。-->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

## 2. 悬浮窗权限检查与授权跳转代码

`MainActivity` 中先检查：

```java
if (!Settings.canDrawOverlays(this)) {
    pendingShowFloatWindow = true;
    openOverlayPermissionSettings();
    return;
}
```

跳转设置页：

```java
Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
startActivity(intent);
```

有些系统对 `ACTION_MANAGE_OVERLAY_PERMISSION` 或 `ACTION_MANAGE_OVERLAY_PERMISSION + package:包名` 的兼容性不好，可能打开悬浮窗权限页后马上退回。当前项目改成优先打开最稳定的“应用详情页”，让用户从应用详情里的“权限/特殊权限/显示在其他应用上层”进入授权；如果应用详情页打不开，再兜底到通用悬浮窗列表和包名页。

用户从设置页返回后，`MainActivity.onResume()` 再次检查权限。如果已经允许，就调用 `showFloatWindow()`。

## 3. FloatWindowManager 代码职责

本项目新增 `FloatWindowManager`，它只负责窗口本身：

- 创建悬浮窗 View。
- 构造 `WindowManager.LayoutParams`。
- 调用 `WindowManager.addView()`。
- 拖动时调用 `WindowManager.updateViewLayout()`。
- 关闭或页面销毁时调用 `WindowManager.removeView()`。

Activity 不直接操作 `WindowManager`，这样初学时更容易看清楚职责边界：

```java
floatWindowManager = new FloatWindowManager(getApplicationContext());
floatWindowManager.show();
floatWindowManager.remove();
```

## 4. addView / updateViewLayout / removeView 示例

添加窗口：

```java
windowManager.addView(floatView, layoutParams);
```

更新位置：

```java
layoutParams.x = x;
layoutParams.y = y;
windowManager.updateViewLayout(floatView, layoutParams);
```

移除窗口：

```java
windowManager.removeView(floatView);
```

对应到项目：

- `FloatWindowManager.show()` 调用 `addView`。
- `FloatWindowManager.updatePosition()` 调用 `updateViewLayout`。
- `FloatWindowManager.remove()` 调用 `removeView`。

## 5. 每段代码的关键注释

### 创建 View

```java
private View createFloatView() {
    LinearLayout card = new LinearLayout(appContext);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(dp(14), dp(12), dp(14), dp(12));

    TextView title = new TextView(appContext);
    title.setText("悬浮窗 Demo");

    TextView close = new TextView(appContext);
    close.setText("关闭");
    close.setOnClickListener(v -> remove());

    card.addView(title);
    card.addView(close);
    attachDragBehavior(card);
    return card;
}
```

这一步只是普通 View 创建。它还不是窗口，真正进入窗口系统发生在 `addView()`。

### 构造 LayoutParams

```java
WindowManager.LayoutParams params = new WindowManager.LayoutParams();
params.width = WindowManager.LayoutParams.WRAP_CONTENT;
params.height = WindowManager.LayoutParams.WRAP_CONTENT;
params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
params.format = PixelFormat.TRANSLUCENT;
params.gravity = Gravity.START | Gravity.TOP;
params.x = dp(80);
params.y = dp(160);
```

这一步告诉系统：“我要创建一个什么类型的窗口，它能不能抢焦点，背景如何合成，从哪里计算坐标。”

### 拖动更新

```java
FloatWindowPositionTracker.Position position =
        positionTracker.moveTo(event.getRawX(), event.getRawY());
updatePosition(position.x, position.y);
```

`MotionEvent.getRawX()/getRawY()` 是屏幕坐标。拖动时计算出新的 `x/y`，再交给 `updateViewLayout()`。

## 6. LayoutParams 关键参数解释

| 参数 | 本项目取值 | 含义 |
| --- | --- | --- |
| `width/height` | `WRAP_CONTENT` | 悬浮窗大小由卡片内容决定 |
| `type` | `TYPE_APPLICATION_OVERLAY` | Android 8.0+ 的应用悬浮窗类型 |
| `flags` | `FLAG_NOT_FOCUSABLE` | 不抢 Activity 的输入焦点 |
| `flags` | `FLAG_LAYOUT_IN_SCREEN` | 允许窗口在屏幕范围内布局 |
| `format` | `PixelFormat.TRANSLUCENT` | 卡片外区域可以透明 |
| `gravity` | `START | TOP` | `x/y` 从屏幕左上角方向计算 |
| `x/y` | 初始 `80dp/160dp` | 初始位置，拖动时持续更新 |

`type` 很关键。`targetSdkVersion` 是 34，所以不能使用旧的 `TYPE_PHONE`、`TYPE_SYSTEM_ALERT` 之类类型；Android 8.0 以后普通 App 悬浮窗应使用 `TYPE_APPLICATION_OVERLAY`。

## 7. Activity Window、Dialog、PopupWindow、Toast、悬浮窗的区别

| 类型 | 谁创建 | 依附对象 | 是否需要悬浮窗权限 | 生命周期特点 |
| --- | --- | --- | --- | --- |
| Activity Window | Activity | Activity | 否 | Activity 创建和销毁时跟随变化 |
| Dialog | Dialog/Activity | Activity 的 window token | 否 | Activity 销毁前必须 dismiss |
| PopupWindow | PopupWindow | 某个 View 或 Activity | 否 | 通常只在当前页面内显示 |
| Toast | 系统 Toast 机制 | 系统短暂展示 | 否 | 短时间提示，不适合交互 |
| 悬浮窗 | WindowManager | WindowManagerService | 是 | 可显示在 App 窗口之上，可跨页面 |

简单记法：

- Dialog/PopupWindow 更像“页面里面的临时窗口”。
- 悬浮窗更像“交给系统窗口管理器托管的独立窗口”。

## 8. 常见异常

### BadTokenException

常见原因：

- 使用了不合适的窗口 `type`。
- 没有悬浮窗权限。
- 用 Activity token 添加窗口时 Activity 已经销毁。

本项目使用 `TYPE_APPLICATION_OVERLAY` 和 application context，并在添加前检查权限，能减少这类问题。

### WindowLeaked

窗口依附的 Activity 已经销毁，但窗口没有移除。Dialog 很常见，悬浮窗也可能发生类似泄漏。

本项目在 `MainActivity.onDestroy()` 调用：

```java
if (floatWindowManager != null) {
    floatWindowManager.remove();
}
```

### 重复 addView

同一个 View 不能重复 `addView()`。否则会报错。

本项目用：

```java
if (isShowing()) {
    return true;
}
```

### removeView 时 View 未 attach

如果 View 已经被移除，再次移除可能抛 `IllegalArgumentException`。

本项目在 `remove()` 中捕获它，并清空引用。

### SecurityException

没有 overlay 权限时添加 `TYPE_APPLICATION_OVERLAY` 可能失败。本项目先用 `Settings.canDrawOverlays()` 做前置检查，并在 `addView` 外层捕获运行时异常。

## 9. 通过 Logcat 验证窗口创建

过滤日志：

```shell
adb logcat | grep FloatWindow
```

Windows PowerShell 可用：

```powershell
adb logcat | Select-String FloatWindow
```

重点观察：

- `addView: type=... flags=... x=... y=...`
- `addView: success`
- `updateViewLayout: x=... y=...`
- `removeView: success`

如果没有权限，应该看到：

```text
show: overlay permission denied
```

## 10. 通过 dumpsys window 验证窗口创建

查看窗口列表：

```shell
adb shell dumpsys window | grep -i lifecycledemo
adb shell dumpsys window windows
```

Windows PowerShell 可用：

```powershell
adb shell dumpsys window | Select-String lifecycledemo
adb shell dumpsys window windows
```

悬浮窗显示后，窗口系统中应该能看到属于 `com.example.lifecycledemo` 的窗口记录。关闭悬浮窗后，再查应该消失。

## 11. 完整窗口建立流程

### 1. Activity/Service

本 Demo 从 `MainActivity` 触发：

```java
btnFloatWindow.setOnClickListener(v -> toggleFloatWindow());
```

如果真实业务希望 Activity 退出后悬浮窗继续存在，可以把所有权移动到前台 `FloatWindowService`。

### 2. 创建悬浮窗 View

`FloatWindowManager.createFloatView()` 创建普通 Android View。此时它还只是内存里的 View 对象。

### 3. 构造 WindowManager.LayoutParams

`createLayoutParams()` 指定窗口类型、焦点行为、透明格式、初始位置。

### 4. WindowManager.addView()

调用：

```java
windowManager.addView(floatView, layoutParams);
```

这是 View 进入窗口系统的关键入口。

### 5. 创建 ViewRootImpl

应用进程内会为这个窗口创建 `ViewRootImpl`。它负责：

- 管理 View 树。
- 发起 measure/layout/draw。
- 接收输入事件。
- 和系统窗口服务通信。

### 6. Binder 通知 WindowManagerService

`ViewRootImpl` 通过 Binder IPC 请求系统进程里的 `WindowManagerService` 添加窗口。

### 7. WMS 管理层级、焦点和 Surface

`WindowManagerService` 会检查：

- 这个 App 是否有权限。
- 窗口 `type` 放在哪个层级。
- 是否能获取焦点。
- 输入事件区域如何分发。
- Surface 如何分配和管理。

### 8. SurfaceFlinger 合成显示

View 绘制结果进入 Surface。最后 `SurfaceFlinger` 把 App、悬浮窗、状态栏、导航栏等多个 Surface 合成成屏幕上的最终画面。

完整链路：

```text
Activity/Service
-> 创建悬浮窗 View
-> 构造 WindowManager.LayoutParams
-> 设置 type/flags/format/gravity/x/y
-> WindowManager.addView()
-> ViewRootImpl 接管 View 树
-> Binder 通知 WindowManagerService
-> WMS 管理窗口层级、焦点、输入和 Surface
-> SurfaceFlinger 合成显示
```

## 12. 退出页面后悬浮窗是否继续存在

当前 Demo 是 Activity-owned：

- 跳转到 `SecondActivity` 时，`MainActivity` 会走 `onPause()`、`onStop()`，但没有销毁，所以悬浮窗可能继续存在。
- `MainActivity` 真正 `onDestroy()` 时会调用 `remove()`，悬浮窗会被移除。

这正好用来学习：窗口是否继续存在，取决于谁持有并管理它。

## 13. 是否需要 Service 管理悬浮窗

学习 Demo：不需要 Service。`MainActivity + FloatWindowManager` 已经能讲清楚核心链路。

真实业务：通常需要前台 Service，尤其是这些场景：

- Activity 关闭后仍要显示悬浮窗。
- 悬浮窗是长期运行能力，比如辅助工具、悬浮控制器。
- 需要更明确的后台存活和用户可见通知。

如果改成 `FloatWindowService`，关系会变成：

```text
MainActivity 只负责申请权限和 startForegroundService()
FloatWindowService 负责 show/update/remove
Service.onDestroy() 负责最终 removeView()
```

这样悬浮窗就不再依附某个 Activity，而是依附 Service 的运行状态。

## 14. 学习重点总结

- `View` 本身不是窗口，`addView()` 才把它交给窗口系统。
- `LayoutParams.type` 决定窗口层级和权限要求。
- `flags` 决定是否抢焦点、如何接收输入、如何参与布局。
- 拖动不是移动 View 本身，而是更新窗口的 `LayoutParams.x/y`。
- 添加了窗口就必须移除，否则容易泄漏。
- Activity 管理适合 Demo；Service 管理适合长期悬浮窗。
- App 侧看到的是 `WindowManager`，系统侧真正管理的是 `WindowManagerService`，最终显示由 `SurfaceFlinger` 合成。
