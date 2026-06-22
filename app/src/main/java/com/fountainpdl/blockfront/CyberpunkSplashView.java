package com.fountainpdl.blockfront;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Cyberpunk animated backdrop:
 * - dark bg with circuit-grid lines
 * - animated cyan radial fountain core with arcing streams
 * - soft pulsing glow
 */
public class CyberpunkSplashView extends View {

    private static final int NUM_STREAMS = 9;
    private static final int DROPS_PER_STREAM = 5;
    private static final long CYCLE_MS = 1400;

    private final Paint gridPaint = new Paint();
    private final Paint glowPaint = new Paint();
    private final Paint dropPaint = new Paint();
    private final Paint basePaint = new Paint();
    private final Paint spoutPaint = new Paint();

    private ValueAnimator animator;
    private long startMs = 0;

    public CyberpunkSplashView(Context context) { super(context); init(); }
    public CyberpunkSplashView(Context context, AttributeSet a) { super(context, a); init(); }

    private void init() {
        gridPaint.setAntiAlias(true);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        dropPaint.setAntiAlias(true);
        glowPaint.setAntiAlias(true);
        basePaint.setAntiAlias(true);
        basePaint.setStyle(Paint.Style.STROKE);
        basePaint.setStrokeWidth(3.5f);
        basePaint.setStrokeCap(Paint.Cap.ROUND);
        spoutPaint.setAntiAlias(true);
        spoutPaint.setColor(Color.rgb(0, 200, 240));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startMs = SystemClock.elapsedRealtime();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(CYCLE_MS);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> invalidate());
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) { animator.cancel(); animator = null; }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        canvas.drawColor(Color.rgb(6, 7, 13));

        float gridStep = Math.min(w, h) * 0.065f;
        gridPaint.setColor(Color.argb(22, 0, 200, 255));
        for (float x = 0; x < w; x += gridStep)
            canvas.drawLine(x, 0, x, h, gridPaint);
        for (float y = 0; y < h; y += gridStep)
            canvas.drawLine(0, y, w, y, gridPaint);
        gridPaint.setColor(Color.argb(14, 212, 175, 55));
        float gc2 = gridStep * 4;
        for (float x = 0; x < w; x += gc2)
            canvas.drawLine(x, 0, x, h, gridPaint);
        for (float y = 0; y < h; y += gc2)
            canvas.drawLine(0, y, w, y, gridPaint);

        long elapsed = SystemClock.elapsedRealtime() - startMs;
        long loopElapsed = elapsed % CYCLE_MS;
        float phase = loopElapsed / (float) CYCLE_MS;
        float pulse = 0.5f + 0.5f * (float) Math.sin(phase * Math.PI * 2);

        float cx = w / 2f;
        float cy = h * 0.42f;
        float arcH = Math.min(w, h) * 0.18f;
        float spread = Math.min(w, h) * 0.22f;

        float introT = Math.min(1f, elapsed / 600f);
        float easeIn = introT * introT * (3 - 2 * introT);

        float glowR = arcH * (1.8f + 0.35f * pulse) * easeIn;
        if (glowR > 1f) {
            glowPaint.setShader(new RadialGradient(cx, cy, glowR,
                    Color.argb((int)(110 + 55 * pulse), 0, 200, 255),
                    Color.argb(0, 0, 100, 180),
                    Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, glowR, glowPaint);
        }

        for (int s = 0; s < NUM_STREAMS; s++) {
            float t01 = NUM_STREAMS == 1 ? 0.5f : (float) s / (NUM_STREAMS - 1);
            float angle = (t01 - 0.5f) * (float) Math.toRadians(140);
            float sArc = arcH * (0.75f + 0.35f * (float) Math.abs(Math.sin(s * 1.9f)));
            float sSpread = spread;

            for (int d = 0; d < DROPS_PER_STREAM; d++) {
                float dp = (phase + (float) d / DROPS_PER_STREAM) % 1f;
                float x = cx + (float) Math.sin(angle) * sSpread * dp;
                float y = cy - 4f * sArc * dp * (1f - dp);
                float alpha = clamp01((1f - dp) * easeIn);
                float radius = 4.5f + 3.5f * (1f - dp);

                dropPaint.setColor(Color.argb(
                        (int)(220 * alpha), 0, 210, 255));
                canvas.drawCircle(x, y, radius, dropPaint);
            }
        }

        if (easeIn > 0.01f) {
            canvas.save();
            canvas.translate(cx, cy);
            canvas.scale(easeIn, easeIn);
            canvas.translate(-cx, -cy);

            spoutPaint.setAlpha(255);
            canvas.drawRect(cx - 8f, cy - 5f, cx + 8f, cy + 12f, spoutPaint);

            float bw = spread * 1.7f;
            basePaint.setColor(Color.rgb(0, 200, 255));
            basePaint.setAlpha(255);
            canvas.drawArc(cx - bw / 2f, cy + 6f, cx + bw / 2f, cy + 30f,
                    10f, 160f, false, basePaint);
            canvas.restore();
        }
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : v > 1f ? 1f : v;
    }
}
