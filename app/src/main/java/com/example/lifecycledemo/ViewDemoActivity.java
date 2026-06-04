package com.example.lifecycledemo;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ViewDemoActivity extends AppCompatActivity {

    private static final String TAG = "View-Demo";

    private final ViewDemoLogBuffer logBuffer = new ViewDemoLogBuffer(80);
    private DebugLearningView debugLearningView;
    private TextView tvViewLog;
    private TextView tvViewState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_demo);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("View 学习 Demo");
        }

        debugLearningView = findViewById(R.id.debug_learning_view);
        tvViewLog = findViewById(R.id.tv_view_log);
        tvViewState = findViewById(R.id.tv_view_state);

        debugLearningView.setDebugEventListener((eventName, detail) -> {
            Log.d(TAG, eventName + " | " + detail);
            logBuffer.append(eventName, detail);
            updateViewLog();
            updateViewState();
        });

        setupButtons();
        appendActivityEvent("Activity.onCreate", "setContentView() 已完成，等待 ViewRootImpl 首次遍历");
        updateViewState();
    }

    @Override
    protected void onDestroy() {
        if (debugLearningView != null) {
            debugLearningView.setDebugEventListener(null);
        }
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void setupButtons() {
        Button btnInvalidate = findViewById(R.id.btn_view_invalidate);
        btnInvalidate.setOnClickListener(v -> debugLearningView.forceRedraw());

        Button btnRequestLayout = findViewById(R.id.btn_view_request_layout);
        btnRequestLayout.setOnClickListener(v -> debugLearningView.forceLayoutPass());

        Button btnToggleSize = findViewById(R.id.btn_view_toggle_size);
        btnToggleSize.setOnClickListener(v -> debugLearningView.toggleSizeMode());

        Button btnClearLog = findViewById(R.id.btn_view_clear_log);
        btnClearLog.setOnClickListener(v -> {
            logBuffer.clear();
            appendActivityEvent("clearLog", "只清空日志，不重置 View 自身状态");
        });
    }

    private void appendActivityEvent(String eventName, String detail) {
        Log.d(TAG, eventName + " | " + detail);
        logBuffer.append(eventName, detail);
        updateViewLog();
    }

    private void updateViewLog() {
        if (tvViewLog == null) {
            return;
        }
        String text = logBuffer.getText();
        tvViewLog.setText(text.isEmpty() ? "等待 View 回调..." : text);
    }

    private void updateViewState() {
        if (tvViewState == null || debugLearningView == null) {
            return;
        }

        tvViewState.setText("resource-id: " + getResourceName(debugLearningView)
                + "\ncontentDescription: " + debugLearningView.getContentDescription()
                + "\nvisibility: " + visibilityToString(debugLearningView.getVisibility())
                + ", enabled: " + debugLearningView.isEnabled()
                + ", clickable: " + debugLearningView.isClickable()
                + "\nwidth/height: " + debugLearningView.getWidth() + "x" + debugLearningView.getHeight()
                + "\nstate: " + debugLearningView.getStateSummary());
    }

    private String getResourceName(View view) {
        try {
            return getResources().getResourceEntryName(view.getId());
        } catch (Exception e) {
            return "no-id";
        }
    }

    private String visibilityToString(int visibility) {
        if (visibility == View.VISIBLE) {
            return "VISIBLE";
        }
        if (visibility == View.INVISIBLE) {
            return "INVISIBLE";
        }
        return "GONE";
    }
}

