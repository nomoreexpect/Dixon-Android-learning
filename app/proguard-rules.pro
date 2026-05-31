# 生命周期 Demo 的 ProGuard 规则
# 学习项目保持所有类可见，方便 Logcat 查看真实类名
-keep class com.example.lifecycledemo.** { *; }
