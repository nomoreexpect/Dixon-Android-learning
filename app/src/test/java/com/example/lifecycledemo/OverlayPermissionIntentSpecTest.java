package com.example.lifecycledemo;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OverlayPermissionIntentSpecTest {

    @Test
    public void forPackage_prefersStableAppDetailsBeforeVendorSpecificOverlayPages() {
        List<OverlayPermissionIntentSpec> specs =
                OverlayPermissionIntentSpec.forPackage("com.example.lifecycledemo");

        assertEquals(3, specs.size());

        assertEquals("应用详情页", specs.get(0).label);
        assertEquals("android.settings.APPLICATION_DETAILS_SETTINGS", specs.get(0).action);
        assertEquals("package:com.example.lifecycledemo", specs.get(0).dataUri);

        assertEquals("通用悬浮窗权限页", specs.get(1).label);
        assertEquals("android.settings.action.MANAGE_OVERLAY_PERMISSION", specs.get(1).action);
        assertNull(specs.get(1).dataUri);

        assertEquals("当前应用悬浮窗权限页", specs.get(2).label);
        assertEquals("android.settings.action.MANAGE_OVERLAY_PERMISSION", specs.get(2).action);
        assertEquals("package:com.example.lifecycledemo", specs.get(2).dataUri);
    }
}
