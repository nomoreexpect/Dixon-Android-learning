# Android Floating Window Learning Demo Design

## Context

Project path: `E:\WorkSpace\AndroidLifecycleDemo`

The app is a single-module Android application:

- Module: `app`
- Language/UI: Java + XML layouts
- Architecture: ordinary `Activity` demo, with lifecycle-focused examples
- `compileSdk`: 34
- `minSdk`: 26
- `targetSdk`: 34
- Main package: `com.example.lifecycledemo`

Existing related code:

- `MainActivity` demonstrates Activity lifecycle, Intent, dynamic BroadcastReceiver, foreground Service binding, and ContentProvider usage.
- `MusicService` is a foreground media playback Service and already demonstrates `startForegroundService`, `bindService`, and Service lifecycle.
- `AndroidManifest.xml` already declares network, foreground service, media playback foreground service, and notification permissions.
- No existing `WindowManager`, `Dialog`, `PopupWindow`, `TYPE_APPLICATION_OVERLAY`, `addView`, `updateViewLayout`, or `removeView` implementation exists.

This feature should be added to `MainActivity` and a new `FloatWindowManager` class in the `app` module.

## Goal

Add a learning-focused floating window demo that teaches the real Android window creation and management flow:

`Activity/Service -> create floating View -> build WindowManager.LayoutParams -> set type/flags/format/gravity/x/y -> WindowManager.addView() -> ViewRootImpl -> Binder -> WindowManagerService -> Surface -> SurfaceFlinger`

The demo should:

- Open from a button on `MainActivity`.
- Show a simple card-like floating window.
- Support dragging.
- Support closing from the floating card.
- Demonstrate `updateViewLayout` when moving the window.
- Demonstrate `removeView` when closing or when `MainActivity` is destroyed.
- Explain when a Service is needed and when it is not.

## Recommended Approach

Use `MainActivity + FloatWindowManager` first.

This is best for the current project because the app is a learning demo focused on component lifecycles. Keeping the floating window tied to `MainActivity` makes the lifecycle relationship explicit:

- User taps button in `MainActivity`.
- `MainActivity` checks overlay permission.
- `FloatWindowManager` creates and adds the floating View through `WindowManager`.
- Dragging updates `LayoutParams.x/y` with `updateViewLayout`.
- Closing or leaving `MainActivity` removes the View.

The floating window should not continue after `MainActivity` exits in the first demo. That behavior avoids accidental leaks and makes `removeView` easy to observe. The code comments should explain that a real business floating window that must survive page exit should move ownership to a foreground `FloatWindowService`.

## Alternatives

### Activity-owned FloatWindowManager

Pros:

- Smallest change.
- Best teaching value for `addView/updateViewLayout/removeView`.
- Easy to connect to `MainActivity` lifecycle logs.
- Avoids foreground service notification complexity.

Cons:

- The floating window is removed when the Activity is destroyed.
- It is not suitable for a long-running production overlay.

### Foreground FloatWindowService

Pros:

- Floating window can continue after Activity exits.
- Closer to real-world apps such as assistive tools, screen widgets, or persistent controls.

Cons:

- Requires foreground Service notification management.
- More lifecycle states to explain at once.
- On Android 8.0+ the service must call `startForeground()` quickly.

### Dialog or PopupWindow

Pros:

- No system overlay permission needed.
- Good for in-app temporary UI.

Cons:

- Not a true application overlay.
- Depends on an Activity window token.
- Does not teach `TYPE_APPLICATION_OVERLAY` or system overlay window management.

## Manifest Changes

Add:

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

No new Service is needed for the first demo.

If the feature is later expanded to survive Activity exit, add a `FloatWindowService` declaration and consider using a foreground Service.

## MainActivity Changes

Add one floating-window learning section to `activity_main.xml` near the existing lifecycle and Service sections.

Add fields:

```java
private FloatWindowManager floatWindowManager;
```

Initialize:

```java
floatWindowManager = new FloatWindowManager(getApplicationContext());
```

Add button handling:

- If overlay permission is not granted, open the system overlay settings page with `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`.
- If permission is granted, call `floatWindowManager.show()`.
- If already shown, either show a Toast or remove it depending on button text.

Add lifecycle cleanup:

```java
if (floatWindowManager != null) {
    floatWindowManager.remove();
}
```

Use `onDestroy()` for the Activity-owned demo. Mention that using `onStop()` would remove the window when jumping to `SecondActivity`, while using `onDestroy()` allows the user to observe whether it remains across temporary Activity transitions.

## FloatWindowManager Design

Responsibilities:

- Hold an application `Context`.
- Get `WindowManager` from `Context.WINDOW_SERVICE`.
- Keep references to the floating `View` and `WindowManager.LayoutParams`.
- Guard against duplicate `addView`.
- Build the card UI programmatically or inflate from XML.
- Add close behavior.
- Add drag behavior.
- Call `addView`, `updateViewLayout`, and `removeView`.
- Log each important operation.

Use application context for system overlay windows, not an Activity context. This avoids holding the Activity longer than needed. The View must still be removed explicitly.

## LayoutParams

Use:

```java
WindowManager.LayoutParams params = new WindowManager.LayoutParams();
params.width = WindowManager.LayoutParams.WRAP_CONTENT;
params.height = WindowManager.LayoutParams.WRAP_CONTENT;
params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
params.format = PixelFormat.TRANSLUCENT;
params.gravity = Gravity.START | Gravity.TOP;
params.x = 80;
params.y = 240;
```

Parameter intent:

- `type`: identifies the window layer/category. For Android 8.0+ app overlays, use `TYPE_APPLICATION_OVERLAY`.
- `flags`: controls focus, touch, layout, and interaction behavior.
- `format`: enables transparent background around the card.
- `gravity`: makes `x/y` measured from the top-left screen area.
- `x/y`: initial position and later drag position.

## Drag Behavior

Use `OnTouchListener`:

- On `ACTION_DOWN`, remember raw touch position and current `params.x/y`.
- On `ACTION_MOVE`, compute delta.
- Update `params.x/y`.
- Call `windowManager.updateViewLayout(floatView, params)`.
- On `ACTION_UP`, distinguish click from drag if needed.

All `WindowManager` calls should run on the main thread because they operate on Views.

## Close Behavior

The card contains a close button. Clicking it calls:

```java
remove();
```

`remove()` must:

- Check `floatView != null`.
- Check whether the view is attached or catch `IllegalArgumentException`.
- Call `windowManager.removeView(floatView)`.
- Clear `floatView` and `layoutParams`.

## Error Handling

Common issues to explain in code and notes:

- `BadTokenException`: wrong window type, missing permission, or trying to attach a window with an invalid Activity token.
- `WindowLeaked`: Activity-owned window was not removed before Activity finished.
- Duplicate `addView`: adding the same View twice causes an exception; guard with `isShowing()`.
- `SecurityException`: missing or denied overlay permission.
- `IllegalArgumentException`: removing a View that is not attached.

## Verification

Logcat:

```shell
adb logcat | grep FloatWindow
```

Expected logs:

- Permission check result.
- `show`: before and after `addView`.
- Drag updates with `x/y`.
- `remove`: before and after `removeView`.

Window dump:

```shell
adb shell dumpsys window | grep -i lifecycledemo
adb shell dumpsys window | grep -i application-overlay
adb shell dumpsys window windows
```

Expected observation:

- A window owned by package `com.example.lifecycledemo`.
- The overlay appears after `addView`.
- The overlay disappears after close or Activity destroy cleanup.

## Explanation Topics To Include

Implementation notes should explain:

- Activity window vs Dialog vs PopupWindow vs Toast vs system floating window.
- Why app overlays require `SYSTEM_ALERT_WINDOW`.
- Why Android 8.0+ uses `TYPE_APPLICATION_OVERLAY`.
- Why this demo does not need a Service.
- When a foreground `FloatWindowService` becomes appropriate.
- The system chain from `WindowManager.addView()` to `ViewRootImpl`, Binder, WindowManagerService, Surface, and SurfaceFlinger.

## Testing Plan

Manual verification:

- Launch app.
- Tap floating window button without permission and confirm settings opens.
- Grant overlay permission.
- Return to app and open floating window.
- Drag the card and verify position updates.
- Close the card and verify it disappears.
- Reopen it and jump to `SecondActivity`; observe whether the overlay remains while `MainActivity` is stopped.
- Finish `MainActivity`; verify `onDestroy()` removes the overlay.

Automated verification is limited because system overlay permission and real `WindowManager` behavior require device/emulator instrumentation. Existing unit tests can still be run after code changes once Java is configured.

## Open Constraints

The local shell currently cannot run Gradle because `JAVA_HOME` is not set and `java` is not on `PATH`.

The project directory is not a Git repository, so the design document cannot be committed from this workspace.
