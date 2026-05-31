package com.example.lifecycledemo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public final class IntentFactory {
    private IntentFactory() {
    }

    public static Intent createSecondActivityIntent(Context context) {
        Intent intent = new Intent(context, SecondActivity.class);
        intent.putExtra(
                IntentDemoSpec.SECOND_ACTIVITY_EXTRA_KEY,
                IntentDemoSpec.SECOND_ACTIVITY_MESSAGE
        );
        return intent;
    }

    public static Intent createWebIntent() {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(IntentDemoSpec.WEB_URI));
    }

    public static Intent createDialIntent() {
        return new Intent(Intent.ACTION_DIAL, Uri.parse(IntentDemoSpec.DIAL_URI));
    }

    public static Intent createShareTextIntent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(IntentDemoSpec.SHARE_TYPE);
        intent.putExtra(Intent.EXTRA_TEXT, IntentDemoSpec.SHARE_TEXT);
        return intent;
    }
}
