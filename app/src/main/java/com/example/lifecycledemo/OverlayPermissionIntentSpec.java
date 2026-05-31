package com.example.lifecycledemo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Describes Settings pages that can help the user grant overlay permission.
 *
 * Some Android builds return immediately from overlay-specific Settings pages.
 * The app details page is the most stable fallback because every Android build
 * exposes it for installed apps.
 */
public final class OverlayPermissionIntentSpec {

    public static final String ACTION_MANAGE_OVERLAY_PERMISSION =
            "android.settings.action.MANAGE_OVERLAY_PERMISSION";
    public static final String ACTION_APPLICATION_DETAILS_SETTINGS =
            "android.settings.APPLICATION_DETAILS_SETTINGS";

    public final String label;
    public final String action;
    public final String dataUri;

    private OverlayPermissionIntentSpec(String label, String action, String dataUri) {
        this.label = label;
        this.action = action;
        this.dataUri = dataUri;
    }

    public static List<OverlayPermissionIntentSpec> forPackage(String packageName) {
        String packageUri = "package:" + packageName;

        List<OverlayPermissionIntentSpec> specs = new ArrayList<>();
        specs.add(new OverlayPermissionIntentSpec(
                "应用详情页",
                ACTION_APPLICATION_DETAILS_SETTINGS,
                packageUri
        ));
        specs.add(new OverlayPermissionIntentSpec(
                "通用悬浮窗权限页",
                ACTION_MANAGE_OVERLAY_PERMISSION,
                null
        ));
        specs.add(new OverlayPermissionIntentSpec(
                "当前应用悬浮窗权限页",
                ACTION_MANAGE_OVERLAY_PERMISSION,
                packageUri
        ));
        return Collections.unmodifiableList(specs);
    }
}
