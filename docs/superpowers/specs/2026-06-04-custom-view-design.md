# Custom View Learning Demo Design

## Goal

Add a View learning module that helps Android beginners and test developers understand both sides of View work:

- Framework flow: XML inflation, View tree creation, `ViewRootImpl`, `measure`, `layout`, `draw`, `invalidate`, `requestLayout`, and touch dispatch.
- Testing flow: view identity, text, `contentDescription`, visibility, enabled/clickable state, coordinates, scroll containers, and why automated clicks can miss.

## Project Context

- Language: Java and XML.
- Architecture: ordinary Activity-based Android app.
- SDK: `compileSdk 34`, `minSdk 26`, `targetSdk 34`.
- UI style: one main ScrollView page with numbered learning modules and separate Activity pages for focused demos.
- Existing docs pattern: every learning ability gets a `docs/<topic>-learning.md` entry and README/docs index links.

## Entry Point

`MainActivity` adds a new section:

```text
⑥ View 学习
→ button: 打开 View 学习 Demo
→ ViewDemoActivity
```

`ViewDemoActivity` shows:

- A custom `DebugLearningView`.
- A log panel that records lifecycle-like View callbacks.
- Buttons for `invalidate()`, `requestLayout()`, size toggle, and log clearing.
- Text that exposes testing-relevant properties such as view id, text, `contentDescription`, visibility, enabled/clickable state, and touch coordinates.

## Components

| Component | Responsibility |
| --- | --- |
| `ViewDemoActivity` | Page entry, button wiring, user-facing log display, calls into the custom View. |
| `DebugLearningView` | Custom View that logs `onAttachedToWindow`, `onMeasure`, `onSizeChanged`, `onDraw`, `dispatchTouchEvent`, `onTouchEvent`, and detach events. |
| `ViewDemoLogBuffer` | Pure Java bounded log buffer used by Activity and unit tests. |
| `DebugViewState` | Pure Java state holder for size mode, last touch coordinates, draw counter, and readable state summary. |
| `activity_view_demo.xml` | Layout for the View learning page. |
| `custom-view-learning.md` | Beginner-friendly learning notes for View internals and Android testing. |

## Framework Chain To Teach

```text
Activity.startActivity()
→ ViewDemoActivity.onCreate()
→ setContentView()
→ LayoutInflater parses XML
→ DebugLearningView is constructed
→ Window decor View is attached
→ ViewRootImpl performs traversal
→ measure()
→ layout()
→ draw()
→ user touches the custom View
→ dispatchTouchEvent()
→ onTouchEvent()
→ invalidate() schedules redraw
→ requestLayout() schedules new measure/layout/draw
```

## Testing Chain To Teach

```text
Test code / uiautomator / manual tap
→ find View by resource-id, text, class, or contentDescription
→ check visibility, enabled, clickable, focusable, bounds
→ scroll into viewport if needed
→ inject click or touch coordinates
→ app receives MotionEvent
→ View dispatch and onTouchEvent decide whether the event is consumed
→ UI state/log confirms the result
```

## Behavior

- Opening the demo immediately logs creation and first traversal callbacks.
- Tapping the custom View logs event dispatch and updates the last touch marker.
- Pressing `invalidate()` only asks for redraw and should mainly produce a new `onDraw` log.
- Pressing `requestLayout()` asks for a full traversal and should produce measure/layout/draw logs.
- Toggling size changes the desired View dimensions and calls `requestLayout()`.
- Clearing logs does not reset the core View state, only the visible log history.

## Error Handling And Testability

- The custom View should never assume the listener is non-null.
- Log buffer is bounded to avoid unbounded UI text growth.
- Pure logic is tested with JVM unit tests.
- Android framework callbacks are documented and manually verifiable with Logcat, Layout Inspector, and `adb shell dumpsys window`.

