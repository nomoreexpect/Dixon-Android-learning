package com.example.lifecycledemo;

import android.content.ComponentName;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * MainActivity - 应用主界面
 *
 * 【学习重点：Activity 生命周期】
 * Activity 的完整生命周期回调顺序：
 *
 *  ┌─────────────────────────────────────────────────┐
 *  │  启动: onCreate → onStart → onResume            │
 *  │  暂停: onPause                                  │
 *  │  恢复: onResume                                 │
 *  │  停止: onPause → onStop                         │
 *  │  销毁: onPause → onStop → onDestroy             │
 *  └─────────────────────────────────────────────────┘
 *
 * 关键区分：
 * - onPause/onResume: Activity 部分可见（如弹出对话框）
 * - onStop/onStart:   Activity 完全不可见（跳转到新 Activity）
 * - onDestroy:        Activity 被销毁（finish() 或系统回收）
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Lifecycle-Main";

    // Service 相关
    private MusicService musicService;
    private boolean isBound = false;
    private TextView tvServiceStatus;
    private Button btnPlayPause;

    // 悬浮窗相关
    private FloatWindowManager floatWindowManager;
    private TextView tvFloatWindowStatus;
    private Button btnFloatWindow;
    private boolean pendingShowFloatWindow = false;

    // 广播接收器相关
    private NetworkChangeReceiver networkReceiver;
    private TextView tvNetworkStatus;

    // 生命周期日志面板
    private final LifecycleLogBuffer lifecycleLogBuffer = new LifecycleLogBuffer();
    private final SimpleDateFormat lifecycleTimeFormat =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private TextView tvLifecycleLog;

    /**
     * ServiceConnection 用于绑定 Service，获得 IBinder 后可直接调用 Service 方法。
     * 【知识点】绑定 Service 让 Activity 与 Service 双向通信。
     */
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            MusicService.MusicBinder musicBinder = (MusicService.MusicBinder) binder;
            musicService = musicBinder.getService();
            isBound = true;
            Log.d(TAG, "Service 已绑定");
            updateMusicUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            Log.d(TAG, "Service 连接断开");
        }
    };

    // ================================================================
    //  生命周期回调
    // ================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 【生命周期】onCreate: Activity 第一次创建时调用，只调用一次
        // 适合做：初始化视图、加载数据、绑定事件
        Log.d(TAG, ">>> onCreate");

        floatWindowManager = new FloatWindowManager(getApplicationContext());
        initViews();
        appendLifecycleLog("onCreate");
        setupButtons();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 【生命周期】onStart: Activity 变为可见时调用
        Log.d(TAG, ">>> onStart");
        appendLifecycleLog("onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 【生命周期】onResume: Activity 进入前台、可与用户交互时调用
        // 适合做：恢复动画、注册传感器、开始视频播放等
        Log.d(TAG, ">>> onResume");
        appendLifecycleLog("onResume");
        updateMusicUI();

        // ── 动态注册网络广播 ──────────────────────────────────
        // 【知识点】网络变化广播必须动态注册（Android 8.0+ 不允许静态注册隐式广播）
        // 在 onResume 注册、onPause 注销是推荐做法，与 Activity 可见性对齐
        networkReceiver = new NetworkChangeReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, filter);

        // 设置网络状态回调到 UI
        NetworkChangeReceiver.setNetworkChangeListener((connected, type) ->
                runOnUiThread(() -> {
                    if (tvNetworkStatus != null) {
                        tvNetworkStatus.setText(connected
                                ? "✅ 网络已连接 (" + type + ")"
                                : "❌ 网络已断开");
                    }
                })
        );
        Log.d(TAG, "网络广播已注册");

        Log.d(TAG, "悬浮窗权限检查: pending=" + pendingShowFloatWindow
                + ", canDrawOverlays=" + Settings.canDrawOverlays(this));
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 【生命周期】onPause: Activity 失去焦点但仍可见时调用（如弹出半透明 Activity）
        // 适合做：保存草稿、暂停动画（操作要轻量，不能做耗时操作）
        Log.d(TAG, ">>> onPause");
        appendLifecycleLog("onPause");

        // ── 注销网络广播，防止内存泄漏 ──────────────────────
        if (networkReceiver != null) {
            unregisterReceiver(networkReceiver);
            networkReceiver = null;
            NetworkChangeReceiver.setNetworkChangeListener(null);
            Log.d(TAG, "网络广播已注销");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 【生命周期】onStop: Activity 完全不可见时调用
        // 适合做：释放不需要的资源、停止不必要的后台任务
        Log.d(TAG, ">>> onStop");
        appendLifecycleLog("onStop");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // 【生命周期】onRestart: 从 onStop 状态重新变为可见时调用（注意：重建不会调用此方法）
        Log.d(TAG, ">>> onRestart");
        appendLifecycleLog("onRestart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 【生命周期】onDestroy: Activity 销毁前调用，释放所有资源
        Log.d(TAG, ">>> onDestroy");
        appendLifecycleLog("onDestroy");

        // 解绑 Service，防止内存泄漏
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }

        // 本 Demo 让悬浮窗跟随 MainActivity 生命周期，避免 Activity 销毁后窗口泄漏。
        if (floatWindowManager != null) {
            floatWindowManager.remove();
        }
    }

    // ================================================================
    //  UI 初始化
    // ================================================================

    private void initViews() {
        tvLifecycleLog = findViewById(R.id.tv_lifecycle_log);
        tvServiceStatus = findViewById(R.id.tv_service_status);
        tvFloatWindowStatus = findViewById(R.id.tv_float_window_status);
        tvNetworkStatus = findViewById(R.id.tv_network_status);
    }

    private void setupButtons() {
        Button btnClearLifecycleLog = findViewById(R.id.btn_clear_lifecycle_log);
        btnClearLifecycleLog.setOnClickListener(v -> {
            lifecycleLogBuffer.clear();
            updateLifecycleLogText();
        });

        // ── 跳转到 SecondActivity ──
        Button btnGoSecond = findViewById(R.id.btn_go_second);
        btnGoSecond.setOnClickListener(v -> {
            // 【知识点】显式 Intent 直接指定目标 Activity 类名。
            startActivitySafely(
                    IntentFactory.createSecondActivityIntent(this),
                    "无法打开 SecondActivity"
            );
            // 此时会依次触发: MainActivity.onPause → SecondActivity.onCreate/onStart/onResume
        });

        Button btnOpenWebsite = findViewById(R.id.btn_open_website);
        btnOpenWebsite.setOnClickListener(v -> {
            // 【知识点】隐式 Intent 用 ACTION_VIEW + Uri，让系统寻找浏览器等可处理的 App。
            startActivitySafely(
                    IntentFactory.createWebIntent(),
                    "没有可打开网页的应用"
            );
        });

        Button btnOpenDialer = findViewById(R.id.btn_open_dialer);
        btnOpenDialer.setOnClickListener(v -> {
            // 【知识点】ACTION_DIAL 只打开拨号盘，不直接拨出电话，因此不需要 CALL_PHONE 权限。
            startActivitySafely(
                    IntentFactory.createDialIntent(),
                    "没有可打开拨号盘的应用"
            );
        });

        Button btnShareText = findViewById(R.id.btn_share_text);
        btnShareText.setOnClickListener(v -> {
            // 【知识点】ACTION_SEND 配合 createChooser 展示系统分享面板。
            Intent chooser = Intent.createChooser(
                    IntentFactory.createShareTextIntent(),
                    "分享学习内容"
            );
            startActivitySafely(chooser, "没有可分享文本的应用");
        });

        // ── 启动/停止前台 Service ──
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnPlayPause.setOnClickListener(v -> toggleMusicService());

        // ── 绑定 Service（获取 IBinder）──
        Button btnBind = findViewById(R.id.btn_bind_service);
        btnBind.setOnClickListener(v -> {
            if (!isBound) {
                Intent intent = new Intent(this, MusicService.class);
                bindService(intent, serviceConnection, BIND_AUTO_CREATE);
                Toast.makeText(this, "绑定 Service...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Service 已绑定", Toast.LENGTH_SHORT).show();
            }
        });

        // ── WindowManager 悬浮窗 Demo ──
        btnFloatWindow = findViewById(R.id.btn_float_window);
        btnFloatWindow.setOnClickListener(v -> toggleFloatWindow());

        // ── 查询 ContentProvider 数据 ──
        Button btnQueryData = findViewById(R.id.btn_query_data);
        btnQueryData.setOnClickListener(v -> queryLocalData());
    }

    private void appendLifecycleLog(String eventName) {
        lifecycleLogBuffer.append(lifecycleTimeFormat.format(new Date()), eventName);
        updateLifecycleLogText();
    }

    private void updateLifecycleLogText() {
        if (tvLifecycleLog == null) return;
        String logText = lifecycleLogBuffer.getText();
        tvLifecycleLog.setText(logText.isEmpty() ? "等待生命周期事件..." : logText);
    }

    private void startActivitySafely(Intent intent, String errorMessage) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

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
        Toast.makeText(this, "将打开应用详情页，请进入权限或特殊权限里允许悬浮窗", Toast.LENGTH_LONG).show();

        List<OverlayPermissionIntentSpec> specs =
                OverlayPermissionIntentSpec.forPackage(getPackageName());
        for (OverlayPermissionIntentSpec spec : specs) {
            Intent intent = spec.dataUri == null
                    ? new Intent(spec.action)
                    : new Intent(spec.action, Uri.parse(spec.dataUri));
            if (startSettingsActivity(intent, spec.label)) {
                return;
            }
        }

        pendingShowFloatWindow = false;
        Toast.makeText(this, "无法打开悬浮窗权限设置页，请手动进入系统设置", Toast.LENGTH_LONG).show();
    }

    private boolean startSettingsActivity(Intent intent, String label) {
        try {
            Log.d(TAG, "打开" + label + ": " + intent);
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.w(TAG, "无法打开" + label, e);
            return false;
        }
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

    // ================================================================
    //  Service 控制
    // ================================================================

    private void toggleMusicService() {
        Intent serviceIntent = new Intent(this, MusicService.class);

        if (!MusicService.isRunning) {
            // 启动前台 Service
            // 【知识点】startForegroundService 是 Android 8.0+ 启动前台服务的方式
            startForegroundService(serviceIntent);
            btnPlayPause.setText("停止音乐");
            tvServiceStatus.setText("🎵 音乐播放中（前台 Service 运行）");
        } else {
            // 停止 Service
            stopService(serviceIntent);
            btnPlayPause.setText("播放音乐");
            tvServiceStatus.setText("⏹ 音乐已停止");
        }
    }

    private void updateMusicUI() {
        if (btnPlayPause == null || tvServiceStatus == null) return;
        if (MusicService.isRunning) {
            btnPlayPause.setText("停止音乐");
            tvServiceStatus.setText("🎵 音乐播放中（前台 Service 运行）");
        } else {
            btnPlayPause.setText("播放音乐");
            tvServiceStatus.setText("⏹ 音乐已停止");
        }
    }

    // ================================================================
    //  ContentProvider 查询
    // ================================================================

    private void queryLocalData() {
        // 【知识点】ContentProvider 通过 URI 暴露数据，URI 格式：
        //   content://<authority>/<path>/<id>
        Uri uri = LocalDataProvider.CONTENT_URI;

        try (Cursor cursor = getContentResolver().query(
                uri, null, null, null, null)) {

            if (cursor == null) {
                Toast.makeText(this, "查询失败，cursor 为 null", Toast.LENGTH_SHORT).show();
                return;
            }

            StringBuilder sb = new StringBuilder("📋 本地数据：\n");
            while (cursor.moveToNext()) {
                int idIdx    = cursor.getColumnIndex(LocalDataProvider.COLUMN_ID);
                int nameIdx  = cursor.getColumnIndex(LocalDataProvider.COLUMN_NAME);
                int valueIdx = cursor.getColumnIndex(LocalDataProvider.COLUMN_VALUE);

                int    id    = idIdx    >= 0 ? cursor.getInt(idIdx)    : -1;
                String name  = nameIdx  >= 0 ? cursor.getString(nameIdx)  : "?";
                String value = valueIdx >= 0 ? cursor.getString(valueIdx) : "?";

                sb.append(String.format("  [%d] %s = %s\n", id, name, value));
            }

            tvServiceStatus.setText(sb.toString());
            Log.d(TAG, "ContentProvider 查询结果：\n" + sb);

        } catch (Exception e) {
            Log.e(TAG, "查询 ContentProvider 出错", e);
            Toast.makeText(this, "查询出错：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
