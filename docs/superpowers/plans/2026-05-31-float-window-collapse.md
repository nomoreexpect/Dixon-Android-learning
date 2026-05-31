# Float Window Collapse Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dragging the demo overlay near the left or right screen edge collapses it into a small draggable ball instead of letting system edge gestures close the Activity.

**Architecture:** Keep `MainActivity` as the demo owner. Add a pure Java edge controller for testable clamp/snap math, then have `FloatWindowManager` switch one attached WindowManager view between expanded-card and collapsed-ball content.

**Tech Stack:** Android Java, `WindowManager`, local JUnit 4 tests.

---

### Task 1: Edge Math

**Files:**
- Create: `app/src/main/java/com/example/lifecycledemo/FloatWindowEdgeController.java`
- Test: `app/src/test/java/com/example/lifecycledemo/FloatWindowEdgeControllerTest.java`

- [ ] **Step 1: Write failing tests**

Cover three behaviors: clamp x/y inside a safe inset, collapse near the left edge, collapse near the right edge.

- [ ] **Step 2: Run tests and verify RED**

Run: `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :app:testDebugUnitTest`

- [ ] **Step 3: Implement the controller**

Expose `clampMove(int x, int y)` and `settleOnRelease(int x, int y)`.

- [ ] **Step 4: Run tests and verify GREEN**

Run the same Gradle test command.

### Task 2: Window Content State

**Files:**
- Modify: `app/src/main/java/com/example/lifecycledemo/FloatWindowManager.java`

- [ ] **Step 1: Keep one attached root view**

Use a `FrameLayout` root as the view passed to `WindowManager.addView()`.

- [ ] **Step 2: Add expanded and collapsed render modes**

Expanded mode shows the existing card and close button. Collapsed mode shows a 56dp circular ball. Tapping the ball expands the card.

- [ ] **Step 3: Clamp during drag**

On `ACTION_MOVE`, calculate the drag position, clamp it through `FloatWindowEdgeController`, and call `updateViewLayout()`.

- [ ] **Step 4: Collapse on release**

On `ACTION_UP`/`ACTION_CANCEL`, call `settleOnRelease()`. If the result says collapsed, swap to ball content and snap to the selected edge.

### Task 3: Verification

**Files:**
- Modify as needed only if compiler or tests expose a concrete issue.

- [ ] **Step 1: Run unit tests**

Run: `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :app:testDebugUnitTest`

- [ ] **Step 2: Run debug build**

Run: `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :app:assembleDebug`

- [ ] **Step 3: Manual check**

Install/run the app, enable the overlay, drag near both edges, confirm it becomes a ball, tap the ball, and confirm it expands.
