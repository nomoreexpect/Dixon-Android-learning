package com.example.lifecycledemo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * SecondActivity - 第二个界面
 *
 * 【学习重点：两个 Activity 切换时的生命周期顺序】
 *
 * ① Main → Second（startActivity）：
 *   MainActivity.onPause()
 *   SecondActivity.onCreate()
 *   SecondActivity.onStart()
 *   SecondActivity.onResume()
 *   MainActivity.onStop()       ← Main 完全不可见后才执行
 *
 * ② Second → Main（按返回键 / finish()）：
 *   SecondActivity.onPause()
 *   MainActivity.onRestart()
 *   MainActivity.onStart()
 *   MainActivity.onResume()
 *   SecondActivity.onStop()
 *   SecondActivity.onDestroy()  ← finish 后才销毁
 *
 * 关键原则：
 *   - 前一个 Activity.onPause() 执行完，下一个才能启动
 *   - 所以 onPause() 里不要做重操作，否则会卡顿
 */
public class SecondActivity extends AppCompatActivity {

    private static final String TAG = "Lifecycle-Second";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        Log.d(TAG, ">>> onCreate");

        // 接收从 MainActivity 传来的数据
        String message = getIntent().getStringExtra("message");
        TextView tvReceived = findViewById(R.id.tv_received_message);
        if (message != null) {
            tvReceived.setText("📨 收到：" + message);
        }

        // 返回按钮
        Button btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            // finish() 销毁当前 Activity，返回上一个
            finish();
        });

        // 演示：携带结果返回
        Button btnBackWithResult = findViewById(R.id.btn_back_with_result);
        btnBackWithResult.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("result", "SecondActivity 的回复数据");
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, ">>> onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, ">>> onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, ">>> onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, ">>> onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, ">>> onDestroy");
    }
}
