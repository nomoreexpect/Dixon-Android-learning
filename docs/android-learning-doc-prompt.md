# Android 学习能力扩展文档构建 Prompt

这个 Prompt 用来固定本项目新增 Android 学习能力时的交付方式：不只写代码，也要同步补齐学习文档、入口索引、核心链路、验证方式和后续扩展方向。

适用场景：

- 新增一个 Android 组件或系统能力 Demo，例如 Fragment、RecyclerView、Room、WorkManager、Notification、自定义 View。
- 扩展已有能力，例如继续优化悬浮窗、补生命周期实验、增加权限或系统服务相关案例。
- 希望每次能力扩展后，项目都保持“能运行、能学习、能复盘”的状态。

## 可直接复制的 Prompt

```text
你正在扩展一个 Android 组件学习 Demo 项目。

新增能力：<填写能力名称>
项目路径：<填写本地项目路径>
目标页面/入口：<填写 Activity、Fragment 或模块>
能力定位：<学习 Demo / 可复用业务能力 / 二者兼顾>

请基于项目源码完成代码实现，并同步构建学习文档。要求：

1. 先分析项目结构
   - 判断项目语言、架构、minSdkVersion、targetSdkVersion、Manifest 配置。
   - 搜索已有相关代码，例如 Activity、Service、BroadcastReceiver、ContentProvider、WindowManager、Dialog、PopupWindow、ViewModel、Room、WorkManager。
   - 说明新增能力应该放在哪些类、包、页面或模块中，以及原因。

2. 实现功能
   - 遵循项目现有 Java/XML/Gradle 风格。
   - 把 UI、业务逻辑、系统 API 调用和可测试纯逻辑尽量拆清楚。
   - 对涉及权限、生命周期、线程、系统服务或跨进程通信的部分写清楚边界。

3. 同步创建学习文档
   - 新建 docs/<feature-name>-learning.md。
   - 文档面向 Android 初学者，但关键概念必须准确。
   - 至少包含：学习目标、项目入口、Manifest/Gradle 配置、核心类职责、完整调用链路、关键 API 参数、生命周期关系、常见异常、Logcat/adb/dumpsys 验证方式、后续扩展方向。

4. 同步更新索引
   - 更新 README.md 的功能模块、项目结构、相关文档或后续学习方向。
   - 更新 docs/README.md 的文档索引。
   - 如有系统级链路或类关系，生成 docs/<feature-name>-architecture.svg 或补充 Mermaid 图。

5. 补测试与验证
   - 对可测试的纯逻辑补 JUnit 测试。
   - 运行：
     $env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
     .\gradlew.bat :app:testDebugUnitTest --console=plain
     .\gradlew.bat :app:assembleDebug --console=plain
   - 如涉及设备行为，补充 adb、Logcat 或 dumpsys 验证命令。

6. 最终交付
   - 总结新增/修改的代码文件。
   - 总结新增/修改的文档文件。
   - 明确测试结果和仍需人工验证的点。
   - 提交并推送到 remote。
```

## 文档命名约定

- 专题学习文档：`docs/<topic>-learning.md`
- 架构或链路图：`docs/<topic>-architecture.svg`
- 设计说明：`docs/superpowers/specs/YYYY-MM-DD-<topic>-design.md`
- 实现计划：`docs/superpowers/plans/YYYY-MM-DD-<topic>.md`
- 通用模板：`docs/templates/android-learning-feature-doc-template.md`

`<topic>` 使用英文短横线命名，例如 `floating-window`、`activity-lifecycle`、`work-manager`、`custom-view-touch`。

## 学习文档必须覆盖的内容

- 这个能力解决什么学习问题。
- 在当前项目中从哪里进入。
- Manifest、Gradle、权限或依赖做了什么配置。
- 哪些类参与，每个类负责什么。
- 从用户点击到系统 API 的完整调用链。
- Android Framework 关键类关系，例如 Activity、View、Window、WindowManager、Service、Binder、WMS、Surface。
- 生命周期如何开始、暂停、恢复、销毁。
- 什么时候需要 Service、线程、协程、Handler 或后台任务。
- 常见异常和排查方式。
- 如何用 Logcat、adb shell、dumpsys 或单元测试验证。
- 后续可以继续扩展什么。

## Definition Of Done

新增学习能力只有同时满足下面条件，才算完整：

- 功能可以运行，核心流程可被用户触发。
- 关键代码路径有注释或文档解释。
- `README.md` 能让新人知道项目新增了什么。
- `docs/README.md` 能找到专题文档。
- 专题文档能解释“为什么这样做”，而不只是贴代码。
- 可测试逻辑有单元测试，或说明为什么只能人工验证。
- 构建和测试命令有明确结果。
- Git 提交记录能看出本次扩展的主题。

## 不要这样做

- 只实现 UI，不解释 Android 系统链路。
- 只在聊天里讲，仓库里没有文档沉淀。
- 新增代码后忘记更新 README 和 docs 索引。
- 把系统权限、生命周期释放、异常场景留空。
- 把所有逻辑塞进 Activity，导致后续无法复用和测试。
- 没有验证就宣称功能完成。

