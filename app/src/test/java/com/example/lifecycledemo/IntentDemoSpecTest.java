package com.example.lifecycledemo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IntentDemoSpecTest {

    @Test
    public void explicitIntentDemoUsesSecondActivityMessage() {
        assertEquals("message", IntentDemoSpec.SECOND_ACTIVITY_EXTRA_KEY);
        assertEquals("来自 MainActivity 的问候！", IntentDemoSpec.SECOND_ACTIVITY_MESSAGE);
    }

    @Test
    public void webIntentDemoUsesActionViewWithAndroidDeveloperSite() {
        assertEquals("android.intent.action.VIEW", IntentDemoSpec.WEB_ACTION);
        assertEquals("https://developer.android.com", IntentDemoSpec.WEB_URI);
    }

    @Test
    public void dialIntentDemoUsesActionDialWithoutCalling() {
        assertEquals("android.intent.action.DIAL", IntentDemoSpec.DIAL_ACTION);
        assertEquals("tel:10086", IntentDemoSpec.DIAL_URI);
    }

    @Test
    public void shareIntentDemoUsesPlainTextPayload() {
        assertEquals("android.intent.action.SEND", IntentDemoSpec.SHARE_ACTION);
        assertEquals("text/plain", IntentDemoSpec.SHARE_TYPE);
        assertEquals(
                "我正在学习 Android Intent：显式跳转、隐式打开网页、拨号和分享文本。",
                IntentDemoSpec.SHARE_TEXT
        );
    }
}
