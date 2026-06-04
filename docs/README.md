# 文档索引

这里整理项目中的学习文档、设计说明和示意图。

## Activity 生命周期

- [Activity 生命周期学习笔记](activity-lifecycle-learning.md)

## 悬浮窗

- [悬浮窗学习笔记](floating-window-learning.md)
- [悬浮窗整体框架图](floating-window-architecture.svg)
- [悬浮窗设计说明](superpowers/specs/2026-05-29-float-window-design.md)
- [悬浮窗实现计划](superpowers/plans/2026-05-29-float-window.md)
- [收球功能实现计划](superpowers/plans/2026-05-31-float-window-collapse.md)

## View 学习

- [自定义 View 学习笔记](custom-view-learning.md)
- [自定义 View 整体框架图](custom-view-architecture.svg)
- [自定义 View 设计说明](superpowers/specs/2026-06-04-custom-view-design.md)
- [自定义 View 实现计划](superpowers/plans/2026-06-04-custom-view.md)

## 项目说明

- [根 README](../README.md)
- [Android 学习能力扩展文档构建 Prompt](android-learning-doc-prompt.md)
- [Android 学习专题文档模板](templates/android-learning-feature-doc-template.md)

## 维护建议

- 新增较大的学习模块时，在根 README 的“功能模块”和“后续学习方向”同步更新。
- 新增专题文档时，在本文件中增加入口。
- 新增能力时优先复制 `android-learning-doc-prompt.md` 中的 Prompt，并按 `templates/android-learning-feature-doc-template.md` 补齐专题文档。
- 如果需要发布 APK 或版本说明，可以后续补充 `CHANGELOG.md`。
