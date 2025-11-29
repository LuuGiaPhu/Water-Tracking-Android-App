package com.example.myapplication;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class CircularProgressView extends View {
    private Paint backgroundPaint;
    private Paint progressPaint;
    private float progress = 0f; // 0 - 100
    private RectF oval = new RectF();
    private int strokeWidth = 20;

    public CircularProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#EEEEEE"));
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(strokeWidth);
        backgroundPaint.setAntiAlias(true);

        progressPaint = new Paint();
        progressPaint.setColor(Color.parseColor("#03A9F4")); // Blue color
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float padding = strokeWidth / 2f;
        oval.set(padding, padding, getWidth() - padding, getHeight() - padding);

        canvas.drawArc(oval, 0, 360, false, backgroundPaint); // static background
        canvas.drawArc(oval, -90, 360 * progress / 100f, false, progressPaint); // animated progress
    }

    public void setProgressAnimated(float target) {
        ValueAnimator animator = ValueAnimator.ofFloat(this.progress, target);
        animator.setDuration(700);
        animator.addUpdateListener(animation -> {
            this.progress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    public void setProgressInstant(float value) {
        this.progress = value;
        invalidate();
    }
}
