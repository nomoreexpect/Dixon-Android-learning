# Android 组件学习 Demo

这是一个面向 Android 基础学习的本地 Demo 项目。项目最初从 Activity 生命周期示例开始，现在已经扩展到 Intent、Service、BroadcastReceiver、ContentProvider、WindowManager 悬浮窗、权限跳转、通知与本地测试等内容。

应用安装后的显示名称是 **Android组件Demo**。

## 功能模块

- **Activity 生命周期**：记录 `onCreate`、`onStart`、`onResume`、`onPause`、`onStop`、`onDestroy` 等回调。
- **Intent 示例**：显式跳转、打开网页、打开拨号盘、系统分享。
- **前台 Service**：模拟音乐播放，展示 `startForegroundService`、绑定 Service、通知。
- **BroadcastReceiver**：动态监听网络变化。
- **ContentProvider**：暴露本地数据查询入口。
- **WindowManager 悬浮窗**：申请悬浮窗权限，创建 overlay，支持拖动、关闭、收球、贴边吸附动画。
- **本地单元测试**：覆盖生命周期日志、Intent 构造、悬浮窗拖动、吸边与权限跳转规则。

## 项目结构

```text
.
├── app/
│   ├── src/main/java/com/example/lifecycledemo/
│   │   ├── MainActivity.java
│   │   ├── SecondActivity.java
│   │   ├── MusicService.java
│   │   ├── NetworkChangeReceiver.java
│   │   ├── LocalDataProvider.java
│   │   ├── FloatWindowManager.java
│   │   ├── FloatWindowEdgeController.java
│   │   └── FloatWindowDragGestureTracker.java
│   ├── src/main/res/
│   └── src/test/java/com/example/lifecycledemo/
├── docs/
│   ├── floating-window-learning.md
│   ├── floating-window-architecture.svg
│   └── superpowers/
├── build.gradle
├── settings.gradle
└── gradlew / gradlew.bat
```

## 环境要求

- Android Studio
- JDK 17 或 Android Studio 自带 JBR
- Android SDK 34
- minSdkVersion 26
- targetSdkVersion 34

## 构建 APK

Windows PowerShell:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

构建产物：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 安装到设备

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

安装后在应用列表中搜索：

```text
Android组件Demo
```

注意：Android 不一定会自动把新安装应用放到桌面主屏。如果桌面找不到，请到应用抽屉/应用列表里搜索，或长按应用图标手动添加到主屏。

## 运行测试

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest
```

## 悬浮窗权限

悬浮窗功能使用：

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

运行时会通过 `Settings.canDrawOverlays()` 检查权限，并跳转到系统设置页授权。授权后点击页面里的“开启悬浮窗”即可显示 WindowManager overlay。

当前 Demo 中悬浮窗由 `MainActivity` 持有：

- Activity 存活时，悬浮窗可以显示、拖动、收球、关闭。
- Activity 销毁时，`onDestroy()` 会调用 `removeView()` 移除悬浮窗。
- 如果未来要让悬浮窗退出页面后仍常驻，应迁移到前台 Service 管理。

## 悬浮窗核心链路

```text
Activity
→ FloatWindowManager
→ 创建悬浮窗 View
→ 构造 WindowManager.LayoutParams
→ WindowManager.addView()
→ ViewRootImpl 接管 View 树
→ Binder 通知 WindowManagerService
→ WMS 管理窗口层级、焦点、Surface
→ SurfaceFlinger 合成显示
```

拖动时：

```text
MotionEvent
→ FloatWindowPositionTracker 计算位置
→ FloatWindowEdgeController 限制边界 / 判断吸边
→ WindowManager.updateViewLayout()
```

## 相关文档

- [悬浮窗学习笔记](docs/floating-window-learning.md)
- [悬浮窗整体框架图](docs/floating-window-architecture.svg)
- [文档索引](docs/README.md)

## 后续学习方向

建议继续在本项目中逐步加入：

- RecyclerView 组件目录
- Fragment 与返回栈
- ViewModel / LiveData 管理 UI 状态
- Room 数据库学习笔记模块
- WorkManager 后台任务
- Notification 操作按钮
- Dialog / PopupWindow / BottomSheet / 悬浮窗对比
- 自定义 View 与触摸事件分发
