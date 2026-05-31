package com.example.lifecycledemo;

import android.animation.ValueAnimator;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Owns the learning-demo floating window.
 *
 * MainActivity decides when the demo starts or stops. This class owns the
 * lower-level window work: create View, build LayoutParams, add/update/remove,
 * and switch the same attached root view between expanded-card and ball modes.
 */
public class FloatWindowManager {

    private static final String TAG = "FloatWindow";

    private static final int BALL_SIZE_DP = 56;
    private static final int CARD_FALLBACK_WIDTH_DP = 220;
    private static final int CARD_FALLBACK_HEIGHT_DP = 116;
    private static final int EDGE_SAFE_INSET_DP = 24;
    private static final int EDGE_SNAP_THRESHOLD_DP = 96;
    private static final int TAP_SLOP_DP = 8;
    private static final long EDGE_ANIMATION_DURATION_MS = 220L;

    private final Context appContext;
    private final WindowManager windowManager;
    private final FloatWindowPositionTracker positionTracker = new FloatWindowPositionTracker();
    private final FloatWindowDragGestureTracker dragGestureTracker;

    private View floatView;
    private WindowManager.LayoutParams layoutParams;
    private boolean collapsed;
    private boolean modeChangePending;
    private int collapsedEdge = FloatWindowEdgeController.EDGE_NONE;
    private ValueAnimator positionAnimator;

    public FloatWindowManager(Context context) {
        appContext = context.getApplicationContext();
        windowManager = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
        dragGestureTracker = new FloatWindowDragGestureTracker(dp(TAP_SLOP_DP));
    }

    public boolean isShowing() {
        return floatView != null;
    }

    /**
     * Creates and adds the overlay window.
     *
     * addView is the key API call. Internally Android creates a ViewRootImpl,
     * then ViewRootImpl talks to WindowManagerService through Binder.
     */
    public boolean show() {
        if (isShowing()) {
            Log.d(TAG, "show: already added, ignore duplicate addView");
            return true;
        }

        if (windowManager == null) {
            Log.e(TAG, "show: WindowManager service is unavailable");
            return false;
        }

        if (!Settings.canDrawOverlays(appContext)) {
            Log.w(TAG, "show: overlay permission denied, cannot add TYPE_APPLICATION_OVERLAY");
            return false;
        }

        collapsed = false;
        collapsedEdge = FloatWindowEdgeController.EDGE_NONE;
        floatView = createRootView();
        layoutParams = createLayoutParams();
        renderExpandedContent();

        try {
            Log.d(TAG, "addView: type=" + layoutParams.type
                    + ", flags=" + layoutParams.flags
                    + ", x=" + layoutParams.x
                    + ", y=" + layoutParams.y);
            windowManager.addView(floatView, layoutParams);
            Log.d(TAG, "addView: success");
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "addView: failed", e);
            floatView = null;
            layoutParams = null;
            collapsed = false;
            modeChangePending = false;
            collapsedEdge = FloatWindowEdgeController.EDGE_NONE;
            return false;
        }
    }

    /**
     * Updates LayoutParams.x/y and asks WindowManager to move the existing
     * overlay window. This is the visible updateViewLayout learning point.
     */
    public void updatePosition(int x, int y) {
        if (windowManager == null || floatView == null || layoutParams == null) {
            Log.w(TAG, "updatePosition: ignored because floating window is not showing");
            return;
        }

        layoutParams.x = x;
        layoutParams.y = y;

        try {
            windowManager.updateViewLayout(floatView, layoutParams);
            Log.d(TAG, "updateViewLayout: x=" + x + ", y=" + y
                    + ", collapsed=" + collapsed);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "updateViewLayout: view was not attached", e);
        }
    }

    /**
     * Removes the overlay View from WindowManager.
     */
    public void remove() {
        if (windowManager == null || floatView == null) {
            Log.d(TAG, "remove: no floating window to remove");
            return;
        }

        cancelPositionAnimation();
        try {
            Log.d(TAG, "removeView: start");
            windowManager.removeView(floatView);
            Log.d(TAG, "removeView: success");
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "removeView: view was not attached", e);
        } finally {
            floatView = null;
            layoutParams = null;
            collapsed = false;
            modeChangePending = false;
            collapsedEdge = FloatWindowEdgeController.EDGE_NONE;
            positionAnimator = null;
        }
    }

    private WindowManager.LayoutParams createLayoutParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        params.format = PixelFormat.TRANSLUCENT;
        params.gravity = Gravity.START | Gravity.TOP;
        params.x = dp(80);
        params.y = dp(160);
        return params;
    }

    private View createRootView() {
        FrameLayout root = new FrameLayout(appContext);
        root.setClipChildren(false);
        root.setClipToPadding(false);
        return root;
    }

    private void renderExpandedContent() {
        FrameLayout root = requireRoot();
        root.removeAllViews();
        root.addView(createCardView(), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));

        collapsed = false;
        collapsedEdge = FloatWindowEdgeController.EDGE_NONE;
        if (layoutParams != null) {
            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        }
    }

    private View createCardView() {
        LinearLayout card = new LinearLayout(appContext);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setMinimumWidth(dp(196));
        card.setBackground(createCardBackground());
        card.setElevation(dp(8));

        TextView title = new TextView(appContext);
        title.setText("\u60ac\u6d6e\u7a97 Demo");
        title.setTextColor(Color.parseColor("#1B1B1F"));
        title.setTextSize(15);
        title.setTypeface(Typeface.DEFAULT_BOLD);

        TextView body = new TextView(appContext);
        body.setText("\u62d6\u52a8\u5230\u5c4f\u5e55\u8fb9\u7f18\u4f1a\u6536\u7403");
        body.setTextColor(Color.parseColor("#44474F"));
        body.setTextSize(12);
        body.setPadding(0, dp(6), 0, dp(8));

        TextView close = new TextView(appContext);
        close.setText("\u5173\u95ed");
        close.setGravity(Gravity.CENTER);
        close.setTextColor(Color.WHITE);
        close.setTextSize(13);
        close.setTypeface(Typeface.DEFAULT_BOLD);
        close.setPadding(dp(12), dp(7), dp(12), dp(7));
        close.setBackground(createCloseBackground());
        close.setOnClickListener(v -> remove());

        card.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        card.addView(body, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        card.addView(close, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        attachDragBehavior(card);
        return card;
    }

    private void renderCollapsedContent(int edge) {
        FrameLayout root = requireRoot();
        root.removeAllViews();
        root.addView(createBallView(), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        collapsed = true;
        collapsedEdge = edge;
        if (layoutParams != null) {
            layoutParams.width = dp(BALL_SIZE_DP);
            layoutParams.height = dp(BALL_SIZE_DP);
        }
    }

    private View createBallView() {
        TextView ball = new TextView(appContext);
        ball.setText("\u6d6e");
        ball.setGravity(Gravity.CENTER);
        ball.setTextColor(Color.WHITE);
        ball.setTextSize(18);
        ball.setTypeface(Typeface.DEFAULT_BOLD);
        ball.setBackground(createBallBackground());
        ball.setElevation(dp(8));
        attachDragBehavior(ball);
        return ball;
    }

    private FrameLayout requireRoot() {
        return (FrameLayout) floatView;
    }

    private void attachDragBehavior(View dragTarget) {
        dragTarget.setOnTouchListener((view, event) -> {
            if (layoutParams == null) {
                return false;
            }

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    cancelPositionAnimation();
                    dragGestureTracker.start(event.getRawX(), event.getRawY());
                    positionTracker.startDrag(
                            event.getRawX(),
                            event.getRawY(),
                            layoutParams.x,
                            layoutParams.y
                    );
                    if (view.getParent() != null) {
                        view.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (!dragGestureTracker.updateMove(event.getRawX(), event.getRawY())) {
                        return true;
                    }
                    FloatWindowPositionTracker.Position dragged =
                            positionTracker.moveTo(event.getRawX(), event.getRawY());
                    FloatWindowEdgeController controller = createEdgeController(view);
                    FloatWindowEdgeController.Position clamped = collapsed
                            ? controller.clampCollapsedMove(dragged.x, dragged.y)
                            : controller.clampMove(dragged.x, dragged.y);
                    updatePosition(clamped.x, clamped.y);
                    return true;

                case MotionEvent.ACTION_UP:
                    if (collapsed && dragGestureTracker.isTap(event.getRawX(), event.getRawY())) {
                        scheduleExpandFromBall();
                        return true;
                    }
                    if (dragGestureTracker.hasDragged()) {
                        settleAfterDrag(view);
                    }
                    return true;

                case MotionEvent.ACTION_CANCEL:
                    if (!modeChangePending && dragGestureTracker.hasDragged()) {
                        settleAfterDrag(view);
                    }
                    return true;

                default:
                    return false;
            }
        });
    }

    private void settleAfterDrag(View currentView) {
        if (layoutParams == null || modeChangePending) {
            return;
        }

        FloatWindowEdgeController controller = createEdgeController(currentView);

        if (collapsed) {
            FloatWindowEdgeController.SettleResult result =
                    controller.settleCollapsedOnRelease(layoutParams.x, layoutParams.y);
            collapsedEdge = result.edge;
            animateToPosition(result.position.x, result.position.y, "settleCollapsed");
            return;
        }

        FloatWindowEdgeController.SettleResult result =
                controller.settleOnRelease(layoutParams.x, layoutParams.y);
        if (result.shouldCollapse) {
            scheduleCollapseToBall(result.edge, result.position.y);
        } else {
            updatePosition(result.position.x, result.position.y);
        }
    }

    private void scheduleCollapseToBall(int edge, int y) {
        if (floatView == null) {
            return;
        }

        modeChangePending = true;
        View attachedRoot = floatView;
        attachedRoot.post(() -> {
            if (floatView != attachedRoot || layoutParams == null) {
                modeChangePending = false;
                return;
            }

            modeChangePending = false;
            if (!collapsed) {
                collapseToBall(edge, y);
            }
        });
    }

    private void scheduleExpandFromBall() {
        if (floatView == null) {
            return;
        }

        modeChangePending = true;
        View attachedRoot = floatView;
        attachedRoot.post(() -> {
            if (floatView != attachedRoot || layoutParams == null) {
                modeChangePending = false;
                return;
            }

            modeChangePending = false;
            if (collapsed) {
                expandFromBall();
            }
        });
    }

    private void collapseToBall(int edge, int y) {
        renderCollapsedContent(edge);
        FloatWindowEdgeController.Position position =
                createEdgeControllerForSize(dp(BALL_SIZE_DP), dp(BALL_SIZE_DP))
                        .snapToEdge(edge, y);
        Log.d(TAG, "collapseToBall: edge=" + edge
                + ", x=" + position.x
                + ", y=" + position.y);
        animateToPosition(position.x, position.y, "collapseToBall");
    }

    private void expandFromBall() {
        FloatWindowEdgeController.Position position =
                createEdgeControllerForSize(
                        dp(CARD_FALLBACK_WIDTH_DP),
                        dp(CARD_FALLBACK_HEIGHT_DP)
                ).clampMove(layoutParams.x, layoutParams.y);

        Log.d(TAG, "expandFromBall: edge=" + collapsedEdge);
        renderExpandedContent();
        updatePosition(position.x, position.y);

        floatView.post(() -> {
            if (floatView == null || layoutParams == null || collapsed) {
                return;
            }
            FloatWindowEdgeController.Position exactPosition =
                    createEdgeController(floatView).clampMove(layoutParams.x, layoutParams.y);
            updatePosition(exactPosition.x, exactPosition.y);
        });
    }

    private void animateToPosition(int targetX, int targetY, String reason) {
        if (layoutParams == null) {
            return;
        }

        int startX = layoutParams.x;
        int startY = layoutParams.y;
        if (startX == targetX && startY == targetY) {
            updatePosition(targetX, targetY);
            return;
        }

        cancelPositionAnimation();
        Log.d(TAG, "animateToPosition: reason=" + reason
                + ", from=(" + startX + "," + startY + ")"
                + ", to=(" + targetX + "," + targetY + ")");

        positionAnimator = ValueAnimator.ofFloat(0f, 1f);
        positionAnimator.setDuration(EDGE_ANIMATION_DURATION_MS);
        positionAnimator.setInterpolator(new DecelerateInterpolator());
        positionAnimator.addUpdateListener(animation -> {
            if (layoutParams == null || floatView == null) {
                cancelPositionAnimation();
                return;
            }

            float fraction = (float) animation.getAnimatedValue();
            int nextX = startX + Math.round((targetX - startX) * fraction);
            int nextY = startY + Math.round((targetY - startY) * fraction);
            updatePosition(nextX, nextY);
        });
        positionAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (positionAnimator == animation) {
                    positionAnimator = null;
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (positionAnimator == animation) {
                    positionAnimator = null;
                }
            }
        });
        positionAnimator.start();
    }

    private void cancelPositionAnimation() {
        if (positionAnimator == null) {
            return;
        }

        positionAnimator.cancel();
        positionAnimator = null;
    }

    private FloatWindowEdgeController createEdgeController(View view) {
        int fallbackWidth = collapsed ? dp(BALL_SIZE_DP) : dp(CARD_FALLBACK_WIDTH_DP);
        int fallbackHeight = collapsed ? dp(BALL_SIZE_DP) : dp(CARD_FALLBACK_HEIGHT_DP);
        int width = view.getWidth() > 0 ? view.getWidth() : fallbackWidth;
        int height = view.getHeight() > 0 ? view.getHeight() : fallbackHeight;
        return createEdgeControllerForSize(width, height);
    }

    private FloatWindowEdgeController createEdgeControllerForSize(int width, int height) {
        DisplayMetrics metrics = appContext.getResources().getDisplayMetrics();
        return new FloatWindowEdgeController(
                metrics.widthPixels,
                metrics.heightPixels,
                width,
                height,
                dp(EDGE_SAFE_INSET_DP),
                dp(EDGE_SNAP_THRESHOLD_DP)
        );
    }

    private GradientDrawable createCardBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.WHITE);
        drawable.setCornerRadius(dp(8));
        drawable.setStroke(dp(1), Color.parseColor("#D7DAE0"));
        return drawable;
    }

    private GradientDrawable createCloseBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor("#D32F2F"));
        drawable.setCornerRadius(dp(6));
        return drawable;
    }

    private GradientDrawable createBallBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(Color.parseColor("#2F6FED"));
        drawable.setStroke(dp(2), Color.WHITE);
        return drawable;
    }

    private int dp(int value) {
        float density = appContext.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
