package com.example.lifecycledemo;

public final class IntentDemoSpec {
    public static final String SECOND_ACTIVITY_EXTRA_KEY = "message";
    public static final String SECOND_ACTIVITY_MESSAGE = "来自 MainActivity 的问候！";

    public static final String WEB_ACTION = "android.intent.action.VIEW";
    public static final String WEB_URI = "https://developer.android.com";

    public static final String DIAL_ACTION = "android.intent.action.DIAL";
    public static final String DIAL_URI = "tel:10086";

    public static final String SHARE_ACTION = "android.intent.action.SEND";
    public static final String SHARE_TYPE = "text/plain";
    public static final String SHARE_TEXT =
            "我正在学习 Android Intent：显式跳转、隐式打开网页、拨号和分享文本。";

    private IntentDemoSpec() {
    }
}
