package com.example.lifecycledemo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

/**
 * MusicService - 前台音乐播放 Service
 *
 * 【学习重点：Service 的两种使用方式与生命周期】
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 启动方式一：startService / startForegroundService           │
 * │   onCreate → onStartCommand → [运行中] → onDestroy         │
 * │   特点：独立运行，Activity 销毁后 Service 仍存在            │
 * │                                                             │
 * │ 启动方式二：bindService                                     │
 * │   onCreate → onBind → [绑定中] → onUnbind → onDestroy     │
 * │   特点：与调用者生命周期绑定，无调用者即销毁                │
 * │                                                             │
 * │ 混合使用（本例）：先 startForegroundService 再 bindService  │
 * │   Service 只有在 stopService 且所有绑定解除后才会销毁       │
 * └─────────────────────────────────────────────────────────────┘
 *
 * 【前台 Service vs 后台 Service】
 * - 后台 Service：优先级低，系统内存不足时随时可能被杀死
 * - 前台 Service：必须显示通知栏通知，优先级高，系统不会轻易杀死
 * - Android 8.0+ 必须用 startForegroundService() 启动，并在 5 秒内调用 startForeground()
 */
public class MusicService extends Service {

    private static final String TAG = "Lifecycle-MusicService";
    private static final String CHANNEL_ID = "MusicServiceChannel";
    private static final int NOTIFICATION_ID = 1001;

    // 对外暴露的状态标志（实际项目可用 LiveData 替代）
    public static boolean isRunning = false;

    private MediaPlayer mediaPlayer;

    // ── Binder 内部类，用于 bindService 通信 ──────────────────
    /**
     * 【知识点】Binder 机制
     * Activity 通过 bindService 拿到这个 IBinder 对象，
     * 强转为 MusicBinder 后即可调用 Service 的公开方法。
     */
    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    private final MusicBinder binder = new MusicBinder();

    // ================================================================
    //  Service 生命周期回调
    // ================================================================

    @Override
    public void onCreate() {
        super.onCreate();
        // 【生命周期】onCreate: Service 第一次创建时调用，只调用一次
        // 适合做：初始化资源（MediaPlayer、数据库连接等）
        Log.d(TAG, ">>> onCreate");

        createNotificationChannel();
        initMediaPlayer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 【生命周期】onStartCommand: 每次 startService/startForegroundService 时调用
        // 可多次调用（每次调用 startService 都会触发）
        Log.d(TAG, ">>> onStartCommand  startId=" + startId);

        // 立即进入前台，显示通知（Android 8.0+ 必须在 5 秒内调用）
        Notification notification = buildNotification();
        startForeground(NOTIFICATION_ID, notification);

        isRunning = true;
        startPlaying();

        // 返回值含义：
        // START_STICKY:         Service 被杀死后自动重启，intent 为 null
        // START_NOT_STICKY:     Service 被杀死后不重启
        // START_REDELIVER_INTENT: Service 被杀死后重启，并重新传递最后一个 intent
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // 【生命周期】onBind: 有组件 bindService 时调用
        // 返回 IBinder 对象给调用方
        Log.d(TAG, ">>> onBind");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // 【生命周期】onUnbind: 所有绑定者都解绑时调用
        Log.d(TAG, ">>> onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 【生命周期】onDestroy: Service 销毁前调用，释放所有资源
        Log.d(TAG, ">>> onDestroy");

        isRunning = false;
        stopPlaying();
        stopForeground(true); // 移除通知
    }

    // ================================================================
    //  业务方法（供 Activity 通过 Binder 调用）
    // ================================================================

    public void startPlaying() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            Log.d(TAG, "▶ 开始播放");
        }
    }

    public void pausePlaying() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            Log.d(TAG, "⏸ 暂停播放");
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    // ================================================================
    //  私有辅助方法
    // ================================================================

    /**
     * 初始化 MediaPlayer
     * 【注意】res/raw/sample_music.wav 已由工具自动生成（C大调8秒旋律）
     * Android R.raw 资源按文件名（不含扩展名）匹配，.wav/.mp3 均可
     */
    private void initMediaPlayer() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.sample_music);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true); // 循环播放
                Log.d(TAG, "MediaPlayer 初始化成功");
            } else {
                Log.w(TAG, "MediaPlayer.create 返回 null，请检查 res/raw/sample_music.mp3 是否存在");
            }
        } catch (Exception e) {
            Log.e(TAG, "MediaPlayer 初始化失败", e);
        }
    }

    private void stopPlaying() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    /**
     * 创建通知渠道（Android 8.0+ 必须先创建渠道才能发通知）
     */
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "音乐播放",
                NotificationManager.IMPORTANCE_LOW // LOW 级别：静默通知，不打扰用户
        );
        channel.setDescription("前台 Service 音乐播放通知");

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * 构建前台通知
     */
    private Notification buildNotification() {
        // 点击通知返回 MainActivity
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🎵 音乐播放中")
                .setContentText("Android 组件学习 Demo - 前台 Service")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(pendingIntent)
                .setOngoing(true)   // 持续通知，用户不能滑动删除
                .build();
    }
}
