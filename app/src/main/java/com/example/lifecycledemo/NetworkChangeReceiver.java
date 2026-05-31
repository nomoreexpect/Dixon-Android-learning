package com.example.lifecycledemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;
import android.widget.Toast;

/**
 * NetworkChangeReceiver - 网络状态监听广播接收器
 *
 * 【学习重点：BroadcastReceiver 的两种注册方式】
 *
 * ┌──────────────────────────────────────────────────────────────┐
 * │ 方式一：静态注册（AndroidManifest.xml）                      │
 * │   优点：即使 App 未运行也能收到广播                          │
 * │   缺点：耗电，Android 8.0+ 大幅限制静态注册的隐式广播        │
 * │                                                              │
 * │ 方式二：动态注册（代码中 registerReceiver）                  │
 * │   优点：更灵活，节省资源                                     │
 * │   缺点：需要手动注册/注销，必须在 onResume/onPause 成对使用  │
 * │   注意：必须在 onDestroy 或 onPause 中调用 unregisterReceiver│
 * └──────────────────────────────────────────────────────────────┘
 *
 * 【Android 8.0+ 限制说明】
 * - 网络变化广播 CONNECTIVITY_CHANGE 已被列为隐式广播，不能静态注册
 * - 必须动态注册（在 MainActivity 中通过 registerReceiver 注册）
 * - 现代推荐方案：使用 NetworkCallback 代替广播（更精确、更省电）
 *
 * 【BroadcastReceiver 生命周期】
 * - 静态广播：每次收到广播时系统临时创建实例，onReceive 执行完即销毁
 * - 动态广播：跟随注册它的组件生命周期
 * - onReceive 运行在主线程，不能做耗时操作（超过 10 秒会 ANR）
 */
public class NetworkChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "Lifecycle-Network";

    // 回调接口，用于通知 Activity 网络状态变化
    public interface NetworkChangeListener {
        void onNetworkChanged(boolean isConnected, String networkType);
    }

    private static NetworkChangeListener listener;

    public static void setNetworkChangeListener(NetworkChangeListener l) {
        listener = l;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // 【生命周期】onReceive: 收到广播时回调，运行在主线程
        Log.d(TAG, ">>> onReceive  action=" + intent.getAction());

        // 检测当前网络状态
        NetworkInfo info = getNetworkInfo(context);
        Log.d(TAG, "网络状态: connected=" + info.connected + " type=" + info.type);

        String statusMsg;
        if (info.connected) {
            statusMsg = "✅ 网络已连接 (" + info.type + ")";
        } else {
            statusMsg = "❌ 网络已断开";
        }

        // 通知 Activity 更新 UI
        if (listener != null) {
            listener.onNetworkChanged(info.connected, info.type);
        }

        // 显示 Toast（onReceive 中仍可用 Toast，但不能用 Dialog）
        Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show();
    }

    // ================================================================
    //  网络状态检测（兼容 Android 10+）
    // ================================================================

    private NetworkInfo getNetworkInfo(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return new NetworkInfo(false, "UNKNOWN");

        // Android 10+ 推荐使用 NetworkCapabilities 替代 deprecated 的 getActiveNetworkInfo
        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) return new NetworkInfo(false, "NONE");

        NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
        if (caps == null) return new NetworkInfo(false, "UNKNOWN");

        boolean isConnected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

        String type;
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            type = "WiFi";
        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            type = "移动数据";
        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            type = "以太网";
        } else {
            type = "其他";
        }

        return new NetworkInfo(isConnected, type);
    }

    /** 简单的网络信息封装类 */
    static class NetworkInfo {
        final boolean connected;
        final String type;

        NetworkInfo(boolean connected, String type) {
            this.connected = connected;
            this.type = type;
        }
    }
}
