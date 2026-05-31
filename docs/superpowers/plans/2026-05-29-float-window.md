# Floating Window Learning Demo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an Activity-triggered Android floating window learning demo that shows overlay permission handling, `WindowManager.addView`, dragging via `updateViewLayout`, closing via `removeView`, and the underlying window pipeline.

**Architecture:** Keep the first version Activity-owned: `MainActivity` owns the demo trigger and lifecycle cleanup, while a focused `FloatWindowManager` owns overlay View creation and WindowManager calls. Extract drag math into a pure Java `FloatWindowPositionTracker` so movement behavior can be tested with local JUnit before touching Android window APIs.

**Tech Stack:** Java, Android XML layouts, Android `WindowManager`, `Settings.canDrawOverlays`, JUnit 4 local unit tests, Gradle Android plugin 8.5.2.

---

## File Structure

- Create `app/src/main/java/com/example/lifecycledemo/FloatWindowPositionTracker.java`: pure Java drag-position calculator.
- Create `app/src/test/java/com/example/lifecycledemo/FloatWindowPositionTrackerTest.java`: JUnit tests for drag-position behavior.
- Create `app/src/main/java/com/example/lifecycledemo/FloatWindowManager.java`: creates the floating card, configures `WindowManager.LayoutParams`, calls `addView`, `updateViewLayout`, and `removeView`.
- Modify `app/src/main/AndroidManifest.xml`: add `SYSTEM_ALERT_WINDOW`.
- Modify `app/src/main/res/layout/activity_main.xml`: add a floating-window learning section with status text and toggle button.
- Modify `app/src/main/java/com/example/lifecycledemo/MainActivity.java`: initialize manager, check overlay permission, launch settings, show/remove overlay, and clean up in `onDestroy`.
- Create `docs/floating-window-learning.md`: beginner-friendly explanation covering the requested Android window chain, parameters, component differences, errors, and verification commands.

## Verification Commands

Use Android Studio's bundled JBR for this workspace:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :app:testDebugUnitTest
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :app:assembleDebug
```

Expected final result:

- Unit tests pass.
- Debug APK builds.
- Manual device/emulator verification can create, drag, close, and dump the overlay window.

---

### Task 1: Add Tested Drag Position Calculator

**Files:**
- Create: `app/src/test/java/com/example/lifecycledemo/FloatWindowPositionTrackerTest.java`
- Create: `app/src/main/java/com/example/lifecycledemo/FloatWindowPositionTracker.java`

- [ ] **Step 1: Write the failing unit test**

Create `app/src/test/java/com/example/lifecycledemo/FloatWindowPositionTrackerTest.java`:

```java
package com.example.lifecycledemo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FloatWindowPositionTrackerTest {

    @Test
    public void moveTo_returnsWindowPositionOffsetByFingerDelta() {
        FloatWindowPositionTracker tracker = new FloatWindowPositionTracker();

        tracker.startDrag(100f, 200f, 80, 240);
        FloatWindowPositionTracker.Position position = tracker.moveTo(135f, 260f);

        assertEquals(115, position.x);
        assertEquals(300, position.y);
    }

    @Test
    public void moveTo_supportsDraggingLeftAndUp() {
        FloatWindowPositionTracker tracker = new FloatWindowPositionTracker();

        tracker.startDrag(300f, 400f, 160, 220);
        FloatWindowPositionTracker.Position position = tracker.moveTo(250f, 350f);

        assertEquals(110, position.x);
        assertEquals(170, position.y);
    }

    @Test
    public void moveTo_roundsSubPixelMovementToNearestInteger() {
        FloatWindowPositionTracker tracker = new FloatWindowPositionTracker();

        tracker.startDrag(10.2f, 20.2f, 5, 7);
        FloatWindowPositionTracker.Position position = tracker.moveTo(11.8f, 23.9f);

        assertEquals(7, position.x);
        assertEquals(11, position.y);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :app:testDebugUnitTest --tests "com.example.lifecycledemo.FloatWindowPositionTrackerTest"
```

Expected: FAIL because `FloatWindowPositionTracker` does not exist.

- [ ] **Step 3: Write minimal implementation**

Create `app/src/main/java/com/example/lifecycledemo/FloatWindowPositionTracker.java`:

```java
package com.example.lifecycledemo;

/**
 * Calculates floating-window movement from raw touch coordinates.
 *
 * This class has no Android View dependency, so the drag math can be tested with
 * normal local unit tests. WindowManager still owns the real on-screen update.
 */
public class FloatWindowPositionTracker {

    private float downRawX;
    private float downRawY;
    private int downWindowX;
    private int downWindowY;

    /**
     * Records the finger position and the window position at ACTION_DOWN.
     */
    public void startDrag(float rawX, float rawY, int windowX, int windowY) {
        downRawX = rawX;
        downRawY = rawY;
        downWindowX = windowX;
        downWindowY = windowY;
    }

    /**
     * Returns the new window position for the current finger location.
     */
    public Position moveTo(float rawX, float rawY) {
        int nextX = downWindowX + Math.round(rawX - downRawX);
        int nextY = downWindowY + Math.round(rawY - downRawY);
        return new Position(nextX, nextY);
    }

    public static final class Position {
        public final int x;
        public final int y;

        public Position(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :app:testDebugUnitTest --tests "com.example.lifecycledemo.FloatWindowPositionTrackerTest"
```

Expected: PASS.

---

### Task 2: Add FloatWindowManager

**Files:**
- Create: `app/src/main/java/com/example/lifecycledemo/FloatWindowManager.java`

- [ ] **Step 1: Create the WindowManager owner class**

Create `app/src/main/java/com/example/lifecycledemo/FloatWindowManager.java`:

```java
package com.example.lifecycledemo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Owns the learning-demo floating window.
 *
 * MainActivity decides when the demo should start or stop. This class owns the
 * lower-level window work: create View, build LayoutParams, add/update/remove.
 */
public class FloatWindowManager {

    private static final String TAG = "FloatWindow";

    private final Context appContext;
    private final WindowManager windowManager;
    private final FloatWindowPositionTracker positionTracker = new FloatWindowPositionTracker();

    private View floatView;
    private WindowManager.LayoutParams layoutParams;

    public FloatWindowManager(Context context) {
        appContext = context.getApplicationContext();
        windowManager = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
    }

    public boolean isShowing() {
        return floatView != null;
    }

    /**
     * Creates and adds the overlay window.
     *
     * addView is the key API call. Internally Android creates a ViewRootImpl,
     * then ViewRootImpl talks to WindowManagerService through Binder.
     */
    public boolean show() {
        if (isShowing()) {
            Log.d(TAG, "show: already added, ignore duplicate addView");
            return true;
        }

        if (!Settings.canDrawOverlays(appContext)) {
            Log.w(TAG, "show: overlay permission denied, cannot add TYPE_APPLICATION_OVERLAY");
            return false;
        }

        floatView = createFloatView();
        layoutParams = createLayoutParams();

        try {
            Log.d(TAG, "addView: type=" + layoutParams.type
                    + ", flags=" + layoutParams.flags
                    + ", x=" + layoutParams.x
                    + ", y=" + layoutParams.y);
            windowManager.addView(floatView, layoutParams);
            Log.d(TAG, "addView: success");
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "addView: failed", e);
            floatView = null;
            layoutParams = null;
            return false;
        }
    }

    /**
     * Updates LayoutParams.x/y and asks WindowManager to move the existing window.
     */
    public void updatePosition(int x, int y) {
        if (floatView == null || layoutParams == null) {
            Log.w(TAG, "updatePosition: ignored because floating window is not showing");
            return;
        }

        layoutParams.x = x;
        layoutParams.y = y;
        windowManager.updateViewLayout(floatView, layoutParams);
        Log.d(TAG, "updateViewLayout: x=" + x + ", y=" + y);
    }

    /**
     * Removes the overlay View from WindowManager.
     */
    public void remove() {
        if (floatView == null) {
            Log.d(TAG, "remove: no floating window to remove");
            return;
        }

        try {
            Log.d(TAG, "removeView: start");
            windowManager.removeView(floatView);
            Log.d(TAG, "removeView: success");
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "removeView: view was not attached", e);
        } finally {
            floatView = null;
            layoutParams = null;
        }
    }

    private WindowManager.LayoutParams createLayoutParams() {
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
        return params;
    }

    private View createFloatView() {
        LinearLayout card = new LinearLayout(appContext);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(createCardBackground());
        card.setElevation(dp(8));

        TextView title = new TextView(appContext);
        title.setText("悬浮窗 Demo");
        title.setTextColor(Color.parseColor("#1B1B1F"));
        title.setTextSize(15);
        title.setTypeface(Typeface.DEFAULT_BOLD);

        TextView body = new TextView(appContext);
        body.setText("拖动卡片会调用 updateViewLayout");
        body.setTextColor(Color.parseColor("#44474F"));
        body.setTextSize(12);
        body.setPadding(0, dp(6), 0, dp(8));

        TextView close = new TextView(appContext);
        close.setText("关闭");
        close.setGravity(Gravity.CENTER);
        close.setTextColor(Color.WHITE);
        close.setTextSize(13);
        close.setTypeface(Typeface.DEFAULT_BOLD);
        close.setPadding(dp(12), dp(7), dp(12), dp(7));
        close.setBackground(createCloseBackground());
        close.setOnClickListener(v -> remove());

        card.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        card.addView(body, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        card.addView(close, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        attachDragBehavior(card);
        return card;
    }

    private void attachDragBehavior(View dragTarget) {
        dragTarget.setOnTouchListener((view, event) -> {
            if (layoutParams == null) {
                return false;
            }

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    positionTracker.startDrag(
                            event.getRawX(),
                            event.getRawY(),
                            layoutParams.x,
                            layoutParams.y
                    );
                    return true;

                case MotionEvent.ACTION_MOVE:
                    FloatWindowPositionTracker.Position position =
                            positionTracker.moveTo(event.getRawX(), event.getRawY());
                    updatePosition(position.x, position.y);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    return true;

                default:
                    return false;
            }
        });
    }

    private GradientDrawable createCardBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.WHITE);
        drawable.setCornerRadius(dp(8));
        drawable.setStroke(dp(1), Color.parseColor("#D7DAE0"));
        return drawable;
    }

    private GradientDrawable createCloseBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor("#D32F2F"));
        drawable.setCornerRadius(dp(6));
        return drawable;
    }

    private int dp(int value) {
        float density = appContext.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
```

- [ ] **Step 2: Compile after adding the class**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :app:compileDebugJavaWithJavac
```

Expected: PASS. If `Settings.canDrawOverlays` or `TYPE_APPLICATION_OVERLAY` imports fail, fix imports before continuing.

---

### Task 3: Add Manifest Permission and Main Screen Controls

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/layout/activity_main.xml`

- [ ] **Step 1: Add overlay permission**

In `app/src/main/AndroidManifest.xml`, add this permission under the existing permission block:

```xml
    <!-- 系统悬浮窗权限：允许使用 TYPE_APPLICATION_OVERLAY 添加应用外悬浮窗。-->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

- [ ] **Step 2: Add the UI section**

In `app/src/main/res/layout/activity_main.xml`, insert this section after the foreground Service controls and before the BroadcastReceiver section:

```xml
        <!-- ═══════════════════════════════════════════
             区块 3: WindowManager 悬浮窗
             ═══════════════════════════════════════════ -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="③ WindowManager（悬浮窗）"
            android:textSize="15sp"
            android:textStyle="bold"
            android:textColor="#FFFFFF"
            android:background="#607D8B"
            android:padding="10dp"
            android:layout_marginTop="8dp" />

        <TextView
            android:id="@+id/tv_float_window_status"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="悬浮窗未显示"
            android:textSize="14sp"
            android:background="#FFFFFF"
            android:padding="10dp"
            android:layout_marginBottom="4dp" />

        <Button
            android:id="@+id/btn_float_window"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="开启悬浮窗"
            android:backgroundTint="#607D8B"
            android:layout_marginBottom="12dp" />
```

Renumber the following BroadcastReceiver and ContentProvider headings from `③/④` to `④/⑤`.

- [ ] **Step 3: Compile resources**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :app:processDebugResources
```

Expected: PASS.

---

### Task 4: Integrate Permission Check and Window Lifecycle in MainActivity

**Files:**
- Modify: `app/src/main/java/com/example/lifecycledemo/MainActivity.java`

- [ ] **Step 1: Add imports**

Add:

```java
import android.provider.Settings;
```

`android.net.Uri` already exists in this file and should be reused for the settings Intent.

- [ ] **Step 2: Add fields**

Near the existing UI fields, add:

```java
    // 悬浮窗相关
    private FloatWindowManager floatWindowManager;
    private TextView tvFloatWindowStatus;
    private Button btnFloatWindow;
    private boolean pendingShowFloatWindow = false;
```

- [ ] **Step 3: Initialize views and manager**

In `onCreate`, after `setContentView(...)`, initialize the manager before `initViews()`:

```java
        floatWindowManager = new FloatWindowManager(getApplicationContext());
```

In `initViews()`, add:

```java
        tvFloatWindowStatus = findViewById(R.id.tv_float_window_status);
```

- [ ] **Step 4: Add button setup**

In `setupButtons()`, add after the Service button setup:

```java
        // ── WindowManager 悬浮窗 Demo ──
        btnFloatWindow = findViewById(R.id.btn_float_window);
        btnFloatWindow.setOnClickListener(v -> toggleFloatWindow());
```

- [ ] **Step 5: Add overlay permission and show/remove helpers**

Add these methods before the `Service 控制` section:

```java
    // ================================================================
    //  WindowManager 悬浮窗控制
    // ================================================================

    private void toggleFloatWindow() {
        if (floatWindowManager != null && floatWindowManager.isShowing()) {
            floatWindowManager.remove();
            updateFloatWindowUI();
            return;
        }

        if (!Settings.canDrawOverlays(this)) {
            pendingShowFloatWindow = true;
            openOverlayPermissionSettings();
            return;
        }

        showFloatWindow();
    }

    private void showFloatWindow() {
        if (floatWindowManager == null) {
            return;
        }

        boolean shown = floatWindowManager.show();
        if (!shown) {
            Toast.makeText(this, "悬浮窗创建失败，请检查权限和 Logcat", Toast.LENGTH_SHORT).show();
        }
        updateFloatWindowUI();
    }

    private void openOverlayPermissionSettings() {
        Toast.makeText(this, "请允许“显示在其他应用上层”权限", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
        );
        startActivitySafely(intent, "无法打开悬浮窗权限设置页");
    }

    private void updateFloatWindowUI() {
        if (btnFloatWindow == null || tvFloatWindowStatus == null || floatWindowManager == null) {
            return;
        }

        if (floatWindowManager.isShowing()) {
            btnFloatWindow.setText("关闭悬浮窗");
            tvFloatWindowStatus.setText("悬浮窗已显示：拖动卡片会触发 updateViewLayout");
        } else {
            btnFloatWindow.setText("开启悬浮窗");
            tvFloatWindowStatus.setText("悬浮窗未显示");
        }
    }
```

- [ ] **Step 6: Handle permission return in onResume**

In `onResume()`, after the existing network receiver setup and log line, add:

```java
        if (pendingShowFloatWindow) {
            pendingShowFloatWindow = false;
            if (Settings.canDrawOverlays(this)) {
                showFloatWindow();
            } else {
                Toast.makeText(this, "未授予悬浮窗权限", Toast.LENGTH_SHORT).show();
                updateFloatWindowUI();
            }
        } else {
            updateFloatWindowUI();
        }
```

- [ ] **Step 7: Remove the Activity-owned overlay in onDestroy**

In `onDestroy()`, before or after unbinding the Service, add:

```java
        if (floatWindowManager != null) {
            floatWindowManager.remove();
        }
```

- [ ] **Step 8: Compile MainActivity**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :app:compileDebugJavaWithJavac
```

Expected: PASS.

---

### Task 5: Add Beginner-Focused Learning Notes

**Files:**
- Create: `docs/floating-window-learning.md`

- [ ] **Step 1: Write the learning document**

Create `docs/floating-window-learning.md` with these sections:

```markdown
# Android 悬浮窗学习笔记

## 1. Manifest 权限

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

`SYSTEM_ALERT_WINDOW` 不是普通运行时权限，用户需要在系统设置页手动允许“显示在其他应用上层”。

## 2. 权限检查与授权跳转

```java
if (!Settings.canDrawOverlays(this)) {
    Intent intent = new Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + getPackageName())
    );
    startActivity(intent);
}
```

## 3. WindowManager 管理类

本项目使用 `FloatWindowManager`。它持有 `WindowManager`、悬浮窗 `View` 和 `WindowManager.LayoutParams`，并负责：

- `addView`: 把 View 添加为系统窗口。
- `updateViewLayout`: 更新窗口位置。
- `removeView`: 移除窗口，避免泄漏。

## 4. add/update/remove 示例

```java
windowManager.addView(floatView, layoutParams);
layoutParams.x = newX;
layoutParams.y = newY;
windowManager.updateViewLayout(floatView, layoutParams);
windowManager.removeView(floatView);
```

## 5. LayoutParams 关键参数

- `type`: Android 8.0+ 使用 `TYPE_APPLICATION_OVERLAY`。
- `flags`: `FLAG_NOT_FOCUSABLE` 表示悬浮窗不抢输入焦点。
- `format`: `PixelFormat.TRANSLUCENT` 允许透明背景。
- `gravity`: `START | TOP` 让 `x/y` 从左上角计算。
- `x/y`: 窗口初始位置，也是在拖动时更新的位置。

## 6. 与其他窗口形式的区别

| 类型 | 依附对象 | 是否需要悬浮窗权限 | 能否跨 Activity/应用显示 |
| --- | --- | --- | --- |
| Activity Window | Activity | 否 | 否 |
| Dialog | Activity Window token | 否 | 否 |
| PopupWindow | 某个 View/Activity | 否 | 否 |
| Toast | 系统 Toast 机制 | 否 | 短暂显示 |
| 悬浮窗 | WindowManagerService | 是 | 可以 |

## 7. 常见异常

- `BadTokenException`: window type/token 不正确，或者没有权限。
- `WindowLeaked`: Activity 销毁前没有移除窗口。
- 重复 `addView`: 同一个 View 被添加两次。
- `IllegalArgumentException`: 移除一个未 attach 的 View。
- `SecurityException`: 未授予 overlay 权限。

## 8. Logcat 验证

```shell
adb logcat | grep FloatWindow
```

观察 `addView`、`updateViewLayout`、`removeView` 日志。

## 9. dumpsys window 验证

```shell
adb shell dumpsys window | grep -i lifecycledemo
adb shell dumpsys window windows
```

创建悬浮窗后应该能看到属于 `com.example.lifecycledemo` 的窗口；关闭后应消失。

## 10. 完整链路

`MainActivity` 检查权限并调用 `FloatWindowManager.show()`。

`FloatWindowManager` 创建 View 和 `LayoutParams`，调用 `WindowManager.addView()`。

应用进程内会创建 `ViewRootImpl`。`ViewRootImpl` 负责把 View 树接入窗口系统，并通过 Binder 请求系统进程里的 `WindowManagerService` 添加窗口。

`WindowManagerService` 判断窗口类型、权限、层级、焦点和输入区域，并为窗口分配/管理 Surface。

应用把 View 绘制到 Surface，最后由 `SurfaceFlinger` 把这个 Surface 和其他应用、状态栏、导航栏等 Surface 合成到屏幕。

## 11. 是否需要 Service

本 Demo 不需要 Service，因为悬浮窗由 `MainActivity` 触发和清理，适合学习 `addView/update/remove`。

真实业务中，如果悬浮窗要在 Activity 退出后继续存在，应该把窗口所有权移到前台 `FloatWindowService`。Service 负责创建、更新、移除窗口，并用前台通知说明后台运行原因。
```

- [ ] **Step 2: Confirm the document exists**

Run:

```powershell
Test-Path 'docs\floating-window-learning.md'
```

Expected: `True`.

---

### Task 6: Full Verification and Manual Test Checklist

**Files:**
- Verify all modified files.

- [ ] **Step 1: Run unit tests**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :app:testDebugUnitTest
```

Expected: all local tests pass.

- [ ] **Step 2: Build debug APK**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Manual Android verification**

On an emulator or device:

```shell
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.lifecycledemo/.MainActivity
adb logcat | grep FloatWindow
```

Manual checks:

- Tap `开启悬浮窗` without permission and confirm system settings opens.
- Grant "display over other apps".
- Return to app and confirm the floating card appears.
- Drag the card and confirm `updateViewLayout` logs changing `x/y`.
- Tap `关闭` on the card and confirm it disappears.
- Reopen the card, navigate to `SecondActivity`, and observe that the overlay can remain while `MainActivity` is stopped.
- Finish `MainActivity` and confirm `onDestroy()` removes the overlay.

Use:

```shell
adb shell dumpsys window | grep -i lifecycledemo
adb shell dumpsys window windows
```

Expected:

- The overlay is listed after `addView`.
- The overlay is gone after close or Activity destruction.
