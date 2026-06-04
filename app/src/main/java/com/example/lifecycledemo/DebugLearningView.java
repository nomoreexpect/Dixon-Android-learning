package com.example.lifecycledemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Locale;

/**
 * A custom View used for learning View traversal and touch dispatch.
 *
 * The class deliberately logs framework callbacks so the demo page can connect
 * what users do on screen with measure/layout/draw/touch behavior.
 */
public class DebugLearningView extends View {

    interface DebugEventListener {
        void onDebugEvent(String eventName, String detail);
    }

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint touchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final DebugViewState state;
    private DebugEventListener debugEventListener;

    public DebugLearningView(Context context) {
        this(context, null);
    }

    public DebugLearningView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DebugLearningView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        state = new DebugViewState(dpToPx(240), dpToPx(160), dpToPx(320), dpToPx(220));
        initPaints();
        setClickable(true);
        setFocusable(true);
        setContentDescription("自定义 View 学习画布，可点击和拖动观察触摸事件");
    }

    void setDebugEventListener(DebugEventListener debugEventListener) {
        this.debugEventListener = debugEventListener;
    }

    String getStateSummary() {
        return state.getSummary();
    }

    void forceRedraw() {
        emit("invalidate()", "只请求重绘，通常会再次触发 onDraw()");
        invalidate();
    }

    void forceLayoutPass() {
        emit("requestLayout()", "请求重新测量和布局，随后可能再次绘制");
        requestLayout();
    }

    void toggleSizeMode() {
        state.toggleSizeMode();
        emit("toggleSize", "mode=" + state.getSizeModeLabel()
                + ", desired=" + state.getDesiredWidth() + "x" + state.getDesiredHeight());
        requestLayout();
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        emit("onAttached", "View 已挂到 Window，后续由 ViewRootImpl 发起遍历");
    }

    @Override
    protected void onDetachedFromWindow() {
        emit("onDetached", "View 从 Window 移除，测试中再找它会失败");
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = state.getDesiredWidth();
        int desiredHeight = state.getDesiredHeight();
        int measuredWidth = resolveSize(desiredWidth, widthMeasureSpec);
        int measuredHeight = resolveSize(desiredHeight, heightMeasureSpec);

        setMeasuredDimension(measuredWidth, measuredHeight);

        emit("onMeasure", "desired=" + desiredWidth + "x" + desiredHeight
                + ", result=" + measuredWidth + "x" + measuredHeight
                + ", widthSpec=" + measureSpecToString(widthMeasureSpec)
                + ", heightSpec=" + measureSpecToString(heightMeasureSpec));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        emit("onSizeChanged", "old=" + oldw + "x" + oldh + ", new=" + w + "x" + h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        state.markDraw();

        float width = getWidth();
        float height = getHeight();
        canvas.drawRoundRect(0, 0, width, height, dpToPx(12), dpToPx(12), fillPaint);
        canvas.drawRoundRect(dpToPx(1), dpToPx(1), width - dpToPx(1), height - dpToPx(1),
                dpToPx(12), dpToPx(12), strokePaint);

        float left = dpToPx(16);
        float baseline = dpToPx(32);
        canvas.drawText("DebugLearningView", left, baseline, textPaint);
        canvas.drawText("mode: " + state.getSizeModeLabel(), left, baseline + dpToPx(26), textPaint);
        canvas.drawText("draw: " + state.getDrawCount(), left, baseline + dpToPx(52), textPaint);
        canvas.drawText("touch: " + state.getLastTouchLabel(), left, baseline + dpToPx(78), textPaint);

        if (state.hasTouch()) {
            canvas.drawCircle(state.getLastTouchX(), state.getLastTouchY(), dpToPx(8), touchPaint);
        }

        emit("onDraw", state.getSummary());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        emit("dispatchTouch", motionActionToString(event.getActionMasked())
                + ", x=" + formatOne(event.getX()) + ", y=" + formatOne(event.getY()));
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        state.updateTouch(event.getX(), event.getY());
        emit("onTouch", motionActionToString(action)
                + ", consumed=true, " + state.getLastTouchLabel());

        if (action == MotionEvent.ACTION_UP) {
            performClick();
        }

        invalidate();
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        emit("performClick", "ACTION_UP 后触发，辅助无障碍和自动化点击语义");
        return true;
    }

    private void initPaints() {
        fillPaint.setColor(Color.rgb(232, 245, 233));

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dpToPx(2));
        strokePaint.setColor(Color.rgb(46, 125, 50));

        textPaint.setColor(Color.rgb(27, 94, 32));
        textPaint.setTextSize(dpToPx(15));

        touchPaint.setColor(Color.rgb(255, 112, 67));
    }

    private void emit(String eventName, String detail) {
        if (debugEventListener != null) {
            debugEventListener.onDebugEvent(eventName, detail);
        }
    }

    private String measureSpecToString(int measureSpec) {
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);
        String modeName;
        if (mode == MeasureSpec.EXACTLY) {
            modeName = "EXACTLY";
        } else if (mode == MeasureSpec.AT_MOST) {
            modeName = "AT_MOST";
        } else {
            modeName = "UNSPECIFIED";
        }
        return modeName + "(" + size + ")";
    }

    private String motionActionToString(int action) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                return "ACTION_DOWN";
            case MotionEvent.ACTION_MOVE:
                return "ACTION_MOVE";
            case MotionEvent.ACTION_UP:
                return "ACTION_UP";
            case MotionEvent.ACTION_CANCEL:
                return "ACTION_CANCEL";
            default:
                return "ACTION_" + action;
        }
    }

    private String formatOne(float value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private int dpToPx(float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}

