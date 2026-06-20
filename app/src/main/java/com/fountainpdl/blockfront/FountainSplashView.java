package com.fountainpdl.blockfront;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Animated, looping fountain graphic for the FountainPDL Ministries splash
 * screen — several water streams arcing from a central spout with a soft
 * pulsing glow behind it. Pure Canvas drawing, no image assets.
 */
public class FountainSplashView extends View {

    private static final int NUM_STREAMS = 7;
    private static final int DROPLETS_PER_STREAM = 4;
    private static final long CYCLE_DURATION_MS = 1600;

    private final Paint dropletPaint = new Paint();
    private final Paint basinPaint = new Paint();
    private final Paint spoutPaint = new Paint();
    private final Paint glowPaint = new Paint();

    private ValueAnimator animator;
    private float phase = 0f;

    public FountainSplashView(Context context) {
        super(context);
        init();
    }

    public FountainSplashView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        dropletPaint.setAntiAlias(true);
        dropletPaint.setColor(Color.rgb(212, 175, 55));

        basinPaint.setAntiAlias(true);
        basinPaint.setColor(Color.rgb(212, 175, 55));
        basinPaint.setStyle(Paint.Style.STROKE);
        basinPaint.setStrokeWidth(4f);
        basinPaint.setStrokeCap(Paint.Cap.ROUND);

        spoutPaint.setAntiAlias(true);
        spoutPaint.setColor(Color.rgb(180, 148, 46));

        glowPaint.setAntiAlias(true);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(CYCLE_DURATION_MS);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            phase = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float centerX = w / 2f;
        float basinY = h * 0.40f;
        float arcHeight = Math.min(w, h) * 0.16f;
        float spread = Math.min(w, h) * 0.20f;

        float pulse = 0.5f + 0.5f * (float) Math.sin(phase * Math.PI * 2);
        float glowRadius = arcHeight * (1.5f + 0.3f * pulse);
        glowPaint.setShader(new RadialGradient(
                centerX, basinY, glowRadius,
                Color.argb((int) (90 + 50 * pulse), 212, 175, 55),
                Color.argb(0, 212, 175, 55),
                Shader.TileMode.CLAMP));
        canvas.drawCircle(centerX, basinY, glowRadius, glowPaint);

        for (int s = 0; s < NUM_STREAMS; s++) {
            float t01 = NUM_STREAMS == 1 ? 0.5f : (float) s / (NUM_STREAMS - 1);
            float angle = (t01 - 0.5f) * (float) Math.toRadians(130);
            float streamArc = arcHeight * (0.85f + 0.3f * (float) Math.abs(Math.sin(s * 1.7f)));

            for (int d = 0; d < DROPLETS_PER_STREAM; d++) {
                float dropletPhase = (phase + (float) d / DROPLETS_PER_STREAM) % 1f;
                float x = centerX + (float) Math.sin(angle) * spread * dropletPhase;
                float y = basinY - 4f * streamArc * dropletPhase * (1f - dropletPhase);
                float alpha = 1f - dropletPhase;
                float radius = 5f + 3f * (1f - dropletPhase);

                dropletPaint.setAlpha((int) (210 * alpha));
                canvas.drawCircle(x, y, radius, dropletPaint);
            }
        }

        spoutPaint.setAlpha(255);
        canvas.drawRect(centerX - 10f, basinY - 6f, centerX + 10f, basinY + 14f, spoutPaint);

        float basinWidth = spread * 1.6f;
        basinPaint.setAlpha(255);
        canvas.drawArc(centerX - basinWidth / 2f, basinY + 8f, centerX + basinWidth / 2f, basinY + 32f,
                10f, 160f, false, basinPaint);
    }
}
