package com.fountainpdl.blockfront;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Cinematic, studio-logo-style fountain animation for the FountainPDL
 * Ministries splash screen: a center flash, radiating light rays, an
 * oversized opening burst that settles into a gentle looping fountain.
 * Pure Canvas drawing — no image/video assets.
 */
public class FountainSplashView extends View {

    private static final int NUM_STREAMS = 7;
    private static final int DROPLETS_PER_STREAM = 4;
    private static final long CYCLE_DURATION_MS = 1600;
    private static final long INTRO_DURATION_MS = 1400;
    private static final long FLASH_DURATION_MS = 320;
    private static final long RAYS_DURATION_MS = 900;
    private static final int NUM_RAYS = 10;

    private final Paint dropletPaint = new Paint();
    private final Paint basinPaint = new Paint();
    private final Paint spoutPaint = new Paint();
    private final Paint glowPaint = new Paint();
    private final Paint flashPaint = new Paint();
    private final Paint rayPaint = new Paint();

    private ValueAnimator animator;
    private long startUptimeMs = 0;

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

        flashPaint.setAntiAlias(true);
        flashPaint.setColor(Color.WHITE);

        rayPaint.setAntiAlias(true);
        rayPaint.setColor(Color.rgb(255, 240, 200));
        rayPaint.setStrokeWidth(3f);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startUptimeMs = SystemClock.elapsedRealtime();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(CYCLE_DURATION_MS);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> invalidate());
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

        long elapsed = SystemClock.elapsedRealtime() - startUptimeMs;

        float centerX = w / 2f;
        float basinY = h * 0.40f;
        float arcHeight = Math.min(w, h) * 0.16f;
        float spread = Math.min(w, h) * 0.20f;

        float introProgress = clamp01(elapsed / (float) INTRO_DURATION_MS);
        float boost = 1f + 0.6f * (1f - introProgress);

        float scaleT = clamp01((elapsed - 200f) / 500f);
        float scaleIn = easeOutCubic(scaleT);

        if (elapsed < RAYS_DURATION_MS) {
            float rayProgress = elapsed / (float) RAYS_DURATION_MS;
            int rayAlpha = (int) ((1f - rayProgress) * 140f);
            float rayLength = Math.min(w, h) * 0.42f * Math.min(1f, elapsed / 250f);
            float rotation = elapsed * 0.05f;

            canvas.save();
            canvas.translate(centerX, basinY);
            canvas.rotate(rotation);
            rayPaint.setAlpha(Math.max(0, rayAlpha));
            for (int i = 0; i < NUM_RAYS; i++) {
                canvas.save();
                canvas.rotate(360f / NUM_RAYS * i);
                canvas.drawLine(0, 0, 0, -rayLength, rayPaint);
                canvas.restore();
            }
            canvas.restore();
        }

        if (elapsed < FLASH_DURATION_MS) {
            float ft = elapsed / (float) FLASH_DURATION_MS;
            float flashAlpha = ft < 0.25f ? (ft / 0.25f) : (1f - (ft - 0.25f) / 0.75f);
            flashAlpha = clamp01(flashAlpha);
            float flashRadius = Math.min(w, h) * (0.1f + 0.5f * ft);
            flashPaint.setAlpha((int) (flashAlpha * 200));
            canvas.drawCircle(centerX, basinY, flashRadius, flashPaint);
        }

        long loopElapsed = elapsed % CYCLE_DURATION_MS;
        float loopPhase = loopElapsed / (float) CYCLE_DURATION_MS;
        float pulse = 0.5f + 0.5f * (float) Math.sin(loopPhase * Math.PI * 2);
        float glowRadius = arcHeight * (1.5f + 0.3f * pulse) * scaleIn;
        if (glowRadius > 1f) {
            glowPaint.setShader(new RadialGradient(
                    centerX, basinY, glowRadius,
                    Color.argb((int) (90 + 50 * pulse), 212, 175, 55),
                    Color.argb(0, 212, 175, 55),
                    Shader.TileMode.CLAMP));
            canvas.drawCircle(centerX, basinY, glowRadius, glowPaint);
        }

        for (int s = 0; s < NUM_STREAMS; s++) {
            float t01 = NUM_STREAMS == 1 ? 0.5f : (float) s / (NUM_STREAMS - 1);
            float angle = (t01 - 0.5f) * (float) Math.toRadians(130);
            float streamArc = arcHeight * (0.85f + 0.3f * (float) Math.abs(Math.sin(s * 1.7f))) * boost;
            float streamSpread = spread * boost;

            for (int d = 0; d < DROPLETS_PER_STREAM; d++) {
                float dropletPhase = (loopPhase + (float) d / DROPLETS_PER_STREAM) % 1f;
                float x = centerX + (float) Math.sin(angle) * streamSpread * dropletPhase;
                float y = basinY - 4f * streamArc * dropletPhase * (1f - dropletPhase);
                float alpha = clamp01((1f - dropletPhase) * scaleIn);
                float radius = 5f + 3f * (1f - dropletPhase);

                dropletPaint.setAlpha((int) (210 * alpha));
                canvas.drawCircle(x, y, radius, dropletPaint);
            }
        }

        if (scaleIn > 0.01f) {
            canvas.save();
            canvas.translate(centerX, basinY);
            canvas.scale(scaleIn, scaleIn);
            canvas.translate(-centerX, -basinY);

            spoutPaint.setAlpha(255);
            canvas.drawRect(centerX - 10f, basinY - 6f, centerX + 10f, basinY + 14f, spoutPaint);

            float basinWidth = spread * 1.6f;
            basinPaint.setAlpha(255);
            canvas.drawArc(centerX - basinWidth / 2f, basinY + 8f, centerX + basinWidth / 2f, basinY + 32f,
                    10f, 160f, false, basinPaint);
            canvas.restore();
        }
    }

    private static float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    private static float easeOutCubic(float t) {
        float f = t - 1f;
        return f * f * f + 1f;
    }
}
