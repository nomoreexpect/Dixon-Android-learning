# <能力名称> 学习笔记

> 将 `<能力名称>`、`<feature-name>`、类名、命令和路径替换为本次新增能力的真实内容。

## 这个 Demo 学什么

说明这个能力要帮助初学者理解什么问题。

示例：

- 组件如何创建和销毁。
- 系统服务如何获取和调用。
- 权限、生命周期、线程或进程边界在哪里。
- 用户操作如何一步步传到 Android Framework。

## 项目入口

- 入口页面：`<Activity 或 Fragment>`
- 触发控件：`<按钮/菜单/列表项>`
- 核心类：`<核心管理类>`
- 相关布局：`<layout xml>`

## Manifest / Gradle 配置

```xml
<!-- 在这里放 Manifest 权限、Activity、Service、Provider 或 Receiver 配置 -->
```

```gradle
// 在这里放新增依赖或 SDK 配置
```

说明每一项配置为什么需要，什么时候不需要。

## 核心类与职责

| 类/文件 | 职责 |
| --- | --- |
| `<EntryActivity>` | 负责页面入口、按钮点击、生命周期释放 |
| `<FeatureManager>` | 封装核心能力，避免 Activity 过重 |
| `<FeatureState>` | 保存可测试的状态或计算逻辑 |
| `<FeatureTest>` | 覆盖纯逻辑或规则判断 |

## 完整调用链路

```text
用户操作
→ Activity / Fragment
→ Manager / ViewModel / Service
→ 创建 View / 构造请求 / 调用系统 API
→ Android Framework 关键类
→ 系统服务或渲染/存储/通知/后台任务模块
→ 回调结果
→ UI 更新或资源释放
```

把本能力最重要的一条链路写清楚，尽量覆盖从应用层到 Framework 的关键节点。

## 关键代码片段

```java
// 放最小必要代码片段，不要整篇复制源码。
```

代码说明：

- 这段代码在哪里被调用。
- 关键参数代表什么。
- 哪些地方和生命周期、权限、线程或系统服务有关。

## 生命周期关系

说明这个能力和页面生命周期的关系：

- `onCreate()`：初始化什么。
- `onStart()` / `onResume()`：注册或恢复什么。
- `onPause()` / `onStop()`：暂停或解绑什么。
- `onDestroy()`：必须释放什么。

如果能力退出页面后仍需存在，说明是否需要 `Service`，以及为什么。

## 权限、线程和系统服务

- 权限：是否需要运行时权限、特殊权限或 Manifest 权限。
- 线程：哪些操作必须在主线程，哪些可以在后台线程。
- 系统服务：是否使用 `getSystemService()`，对应服务负责什么。
- 跨进程：是否涉及 Binder、系统服务或 Framework 回调。

## 常见异常与误区

| 问题 | 常见原因 | 解决方式 |
| --- | --- | --- |
| `<异常或现象>` | `<原因>` | `<处理方式>` |

优先写初学者真实容易遇到的问题，例如权限未授权、生命周期泄漏、重复注册、主线程阻塞、Context 使用错误。

## 验证方式

### 单元测试

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest --console=plain
```

### 构建 APK

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug --console=plain
```

### 设备验证

```powershell
adb logcat | findstr <Tag>
adb shell dumpsys <service-name>
```

说明期望看到什么日志、状态或系统输出。

## 和已有模块的关系

说明这个能力和项目里已有的 Activity 生命周期、Service、权限、悬浮窗、通知、测试等内容如何关联。

## 后续扩展

- 扩展方向 1
- 扩展方向 2
- 扩展方向 3

## 学习重点总结

用几句话总结本能力最值得记住的点：

- 应用层入口是什么。
- Framework 关键类是什么。
- 生命周期释放点在哪里。
- 最常见的问题如何排查。

