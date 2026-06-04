# Custom View Learning Demo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a focused Custom View learning module that explains Android View internals and testing-facing View behavior.

**Architecture:** Add a separate `ViewDemoActivity` with a `DebugLearningView` custom View. Keep callback logging and state formatting in small pure Java classes so local unit tests can cover the behavior without Android instrumentation.

**Tech Stack:** Java, XML layouts, AppCompat, local JUnit 4 tests, existing Gradle Android application.

---

### Task 1: Test Pure View Demo Logic

**Files:**
- Create: `app/src/test/java/com/example/lifecycledemo/ViewDemoLogBufferTest.java`
- Create: `app/src/test/java/com/example/lifecycledemo/DebugViewStateTest.java`

- [ ] **Step 1: Write failing tests**

Create tests for bounded log output and debug state changes.

- [ ] **Step 2: Run tests to verify red**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.lifecycledemo.ViewDemoLogBufferTest" --tests "com.example.lifecycledemo.DebugViewStateTest" --console=plain
```

Expected: compile failure because `ViewDemoLogBuffer` and `DebugViewState` do not exist yet.

### Task 2: Implement Pure View Demo Logic

**Files:**
- Create: `app/src/main/java/com/example/lifecycledemo/ViewDemoLogBuffer.java`
- Create: `app/src/main/java/com/example/lifecycledemo/DebugViewState.java`

- [ ] **Step 1: Implement minimal classes**

`ViewDemoLogBuffer` stores numbered bounded lines. `DebugViewState` tracks size mode, draw count, and last touch position.

- [ ] **Step 2: Run tests to verify green**

Run the same focused tests and then all unit tests.

### Task 3: Add View Demo UI

**Files:**
- Create: `app/src/main/java/com/example/lifecycledemo/DebugLearningView.java`
- Create: `app/src/main/java/com/example/lifecycledemo/ViewDemoActivity.java`
- Create: `app/src/main/res/layout/activity_view_demo.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/java/com/example/lifecycledemo/MainActivity.java`

- [ ] **Step 1: Add custom View**

Override constructor, attach/detach, measure, size, draw, dispatch, and touch callbacks. Emit logs through a listener.

- [ ] **Step 2: Add Activity and layout**

Wire buttons for `invalidate()`, `requestLayout()`, size toggle, and clear log.

- [ ] **Step 3: Add MainActivity entry and Manifest registration**

Add the sixth module on the main page and register `ViewDemoActivity`.

### Task 4: Document The Learning Module

**Files:**
- Create: `docs/custom-view-learning.md`
- Create: `docs/custom-view-architecture.svg`
- Modify: `README.md`
- Modify: `docs/README.md`

- [ ] **Step 1: Write beginner-friendly View notes**

Cover framework flow, test development flow, common failures, and verification commands.

- [ ] **Step 2: Update indexes**

Link the new docs from README and docs index.

### Task 5: Verify And Commit

**Files:**
- All files changed by the feature.

- [ ] **Step 1: Run tests**

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest --console=plain
```

- [ ] **Step 2: Build APK**

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug --console=plain
```

- [ ] **Step 3: Commit and push**

```powershell
git add README.md docs app
git commit -m "feat: add custom view learning demo"
git push origin main
```

